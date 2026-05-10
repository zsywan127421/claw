package com.termux.terminal;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class TerminalSession extends TerminalOutput {

    public interface SessionCallback {
        void onSessionStarted(TerminalSession session);
        void onSessionDataAvailable(TerminalSession session);
        void onSessionFinished(TerminalSession session);
        void onSessionOutputAvailable(TerminalSession session);
    }

    private static final int BYTE_QUEUE_SIZE = 4096;

    private final TerminalEmulator emulator;
    private final ByteQueue unreadBuffer;
    private final int[] processId = new int[1];
    private final Object processIdLock = new Object();
    private final Handler mainHandler;

    private ParcelFileDescriptor masterFd;
    private ParcelFileDescriptor slaveFd;
    private Thread readerThread;
    private volatile boolean isRunning;
    private volatile boolean isFinished;
    private String mStartupMessage;

    private SessionCallback callback;

    private static native int createSubprocess(String cmd, String[] args, String[] env,
                                                FileDescriptor[] files, int[] processId);
    private native void closeSubprocess(int processId);

    static {
        System.loadLibrary("termux");
    }

    public TerminalSession(String[] args, String[] env, String cwd) {
        this.unreadBuffer = new ByteQueue(BYTE_QUEUE_SIZE);
        this.emulator = new TerminalEmulator(this, 80, 24);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.isRunning = false;
        this.isFinished = false;
    }

    public void start(String binaryPath, String[] args, String[] env, String cwd,
                      SessionCallback callback) {
        this.callback = callback;

        try {
            FileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            masterFd = pipe[0];
            slaveFd = pipe[1];

            FileDescriptor masterFileDescriptor = masterFd.getFileDescriptor();
            FileDescriptor[] files = new FileDescriptor[]{masterFileDescriptor, masterFileDescriptor, masterFileDescriptor};

            if (args == null || args.length == 0) {
                args = new String[]{"/system/bin/sh"};
            }

            if (env == null) {
                env = new String[]{
                        "TERM=xterm-256color",
                        "HOME=" + System.getProperty("user.home"),
                        "PATH=" + System.getenv("PATH")
                };
            }

            synchronized (processIdLock) {
                int result = createSubprocess(binaryPath, args, env, files, processId);
                if (result != 0) {
                    throw new IOException("Failed to create subprocess, error code: " + result);
                }
            }

            isRunning = true;
            isFinished = false;

            startReaderThread();

            if (callback != null) {
                mainHandler.post(() -> callback.onSessionStarted(this));
            }

        } catch (Exception e) {
            isFinished = true;
            isRunning = false;
            e.printStackTrace();
        }
    }

    private void startReaderThread() {
        readerThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            FileInputStream inputStream = new FileInputStream(masterFd.getFileDescriptor());

            try {
                while (isRunning) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        unreadBuffer.write(buffer, bytesRead);
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onSessionDataAvailable(TerminalSession.this);
                            }
                        });
                    } else if (bytesRead == -1) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                onProcessExited();
            }
        });
        readerThread.setName("TerminalReader");
        readerThread.start();
    }

    public void writeToSession(byte[] data) {
        writeToSession(data, 0, data.length);
    }

    public void writeToSession(byte[] data, int offset, int length) {
        if (!isRunning || masterFd == null) return;

        try {
            FileOutputStream outputStream = new FileOutputStream(masterFd.getFileDescriptor());
            outputStream.write(data, offset, length);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToSession(int codepoint) {
        byte[] buffer;
        if (codepoint < 0x80) {
            buffer = new byte[]{(byte) codepoint};
        } else if (codepoint < 0x800) {
            buffer = new byte[]{
                    (byte) (0xC0 | (codepoint >> 6)),
                    (byte) (0x80 | (codepoint & 0x3F))
            };
        } else if (codepoint < 0x10000) {
            buffer = new byte[]{
                    (byte) (0xE0 | (codepoint >> 12)),
                    (byte) (0x80 | ((codepoint >> 6) & 0x3F)),
                    (byte) (0x80 | (codepoint & 0x3F))
            };
        } else {
            buffer = new byte[]{
                    (byte) (0xF0 | (codepoint >> 18)),
                    (byte) (0x80 | ((codepoint >> 12) & 0x3F)),
                    (byte) (0x80 | ((codepoint >> 6) & 0x3F)),
                    (byte) (0x80 | (codepoint & 0x3F))
            };
        }
        writeToSession(buffer);
    }

    @Override
    public void write(byte[] data, int length) {
        emulator.append(data, length);
    }

    @Override
    public void close() {
        stop();
    }

    public void stop() {
        isRunning = false;

        synchronized (processIdLock) {
            if (processId[0] != 0) {
                closeSubprocess(processId[0]);
                processId[0] = 0;
            }
        }

        try {
            if (masterFd != null) {
                masterFd.close();
                masterFd = null;
            }
            if (slaveFd != null) {
                slaveFd.close();
                slaveFd = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (readerThread != null) {
            try {
                readerThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            readerThread = null;
        }

        isFinished = true;
    }

    private void onProcessExited() {
        isRunning = false;
        isFinished = true;

        if (callback != null) {
            mainHandler.post(() -> callback.onSessionFinished(this));
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public int getPid() {
        synchronized (processIdLock) {
            return processId[0];
        }
    }

    public TerminalEmulator getEmulator() {
        return emulator;
    }

    public int getCursorX() {
        return emulator.getCursorCol();
    }

    public int getCursorY() {
        return emulator.getCursorRow();
    }

    public int readFromQueue(byte[] buffer) {
        return unreadBuffer.read(buffer);
    }

    public int getUnreadByteCount() {
        return unreadBuffer.available();
    }

    public void resize(int columns, int rows) {
        emulator.resize(columns, rows);
    }

    public void setStartupMessage(String message) {
        this.mStartupMessage = message;
    }

    public String getStartupMessage() {
        return mStartupMessage;
    }

    public static final class ByteQueue {
        private final byte[] buffer;
        private final int capacity;
        private int head;
        private int tail;
        private int count;

        public ByteQueue(int capacity) {
            this.capacity = capacity;
            this.buffer = new byte[capacity];
            this.head = 0;
            this.tail = 0;
            this.count = 0;
        }

        public synchronized int write(byte[] data, int length) {
            int bytesWritten = 0;
            int toWrite = Math.min(length, capacity - count);

            for (int i = 0; i < toWrite; i++) {
                buffer[(tail + i) % capacity] = data[i];
            }
            tail = (tail + toWrite) % capacity;
            count += toWrite;
            bytesWritten = toWrite;

            notifyAll();
            return bytesWritten;
        }

        public synchronized int read(byte[] data) {
            while (count == 0 && isRunning()) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return 0;
                }
            }

            int bytesRead = 0;
            int toRead = Math.min(data.length, count);

            for (int i = 0; i < toRead; i++) {
                data[i] = buffer[(head + i) % capacity];
            }
            head = (head + toRead) % capacity;
            count -= toRead;
            bytesRead = toRead;

            return bytesRead;
        }

        public synchronized int available() {
            return count;
        }

        public synchronized boolean isFull() {
            return count == capacity;
        }

        public boolean isRunning() {
            return true;
        }
    }
}
