package com.termux.terminal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class TerminalSession extends TerminalOutput {

    public interface SessionCallback {
        void onTextChanged(TerminalSession session);
        void onTitleChanged(TerminalSession session);
        void onSessionFinished(TerminalSession session);
    }

    private static final String TAG = "TerminalSession";
    private static final int BUFFER_SIZE = 4096;

    private final TerminalEmulator emulator;
    private final ByteQueue unreadBuffer;
    private final Handler mainHandler;

    private int mFd = -1;
    private int mPid = -1;
    private Thread mReaderThread;
    private volatile boolean mIsRunning = false;
    private SessionCallback mCallback;

    static {
        System.loadLibrary("termux");
    }

    public TerminalSession(String[] args, String[] env, String cwd) {
        this.unreadBuffer = new ByteQueue(BUFFER_SIZE);
        this.emulator = new TerminalEmulator(this, 80, 24);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void start(String binaryPath, String[] args, String[] env, String cwd,
                      SessionCallback callback) {
        this.mCallback = callback;
        this.mIsRunning = true;

        try {
            String[] finalArgs = (args != null && args.length > 0) ? args : new String[]{binaryPath};
            String[] finalEnv = (env != null) ? env : new String[]{"TERM=xterm-256color"};

            int[] pidArr = new int[1];
            mFd = JNI.createSubprocess(binaryPath, cwd, finalEnv, finalArgs, pidArr, 24, 80, 0, 0);
            mPid = pidArr[0];

            if (mFd < 0) {
                Log.e(TAG, "Failed to create subprocess");
                mIsRunning = false;
                return;
            }

            startReaderThread();

            if (mCallback != null) {
                mainHandler.post(() -> mCallback.onTextChanged(this));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start session", e);
            mIsRunning = false;
        }
    }

    private void startReaderThread() {
        mReaderThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (mIsRunning) {
                try {
                    int bytesRead = JNI.read(mFd, buffer, buffer.length);
                    if (bytesRead > 0) {
                        synchronized (unreadBuffer) {
                            unreadBuffer.write(buffer, 0, bytesRead);
                        }
                        mainHandler.post(() -> {
                            byte[] data = new byte[BUFFER_SIZE];
                            int len;
                            synchronized (unreadBuffer) {
                                len = unreadBuffer.read(data, 0, data.length);
                            }
                            if (len > 0) {
                                emulator.append(data, len);
                            }
                            if (mCallback != null) {
                                mCallback.onTextChanged(TerminalSession.this);
                            }
                        });
                    } else if (bytesRead < 0) {
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Reader error", e);
                    break;
                }
            }
            mIsRunning = false;
            mainHandler.post(() -> {
                if (mCallback != null) {
                    mCallback.onSessionFinished(TerminalSession.this);
                }
            });
        }, "TerminalReader");
        mReaderThread.start();
    }

    public void write(String data) {
        if (!mIsRunning || mFd < 0) return;
        try {
            byte[] bytes = data.getBytes("UTF-8");
            JNI.write(mFd, bytes, bytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Write error", e);
        }
    }

    public void writeToSession(byte[] data) {
        write(new String(data, 0, data.length));
    }

    public void updateSize(int rows, int cols) {
        if (mPid > 0) {
            emulator.resize(cols, rows);
            JNI.setPtyWindowSize(mFd, rows, cols, 0, 0);
        }
    }

    public void finishIfRunning() {
        if (!mIsRunning) return;
        mIsRunning = false;
        try {
            if (mPid > 0) {
                Runtime.getRuntime().exec(new String[]{"kill", String.valueOf(mPid)});
            }
            if (mFd >= 0) {
                JNI.close(mFd);
                mFd = -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finishing session", e);
        }
        if (mReaderThread != null) {
            try { mReaderThread.join(1000); } catch (InterruptedException ignored) {}
        }
    }

    public boolean isRunning() { return mIsRunning; }
    public TerminalEmulator getEmulator() { return emulator; }
    public int getPid() { return mPid; }

    @Override
    public void write(byte[] data, int offset, int length) {
        write(new String(data, offset, length));
    }

    @Override
    public void close() {
        finishIfRunning();
    }
}
