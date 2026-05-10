package com.termux.terminal;

public final class JNI {
    static {
        System.loadLibrary("termux");
    }

    public static native int createSubprocess(String cmd, String[] env, int rows, int cols, int[] masterFD, int[] pid);

    public static native void setPtyWindowSize(int masterFD, int rows, int cols, int pixelWidth, int pixelHeight);

    public static native int waitFor(int pid);

    public static native void close(int masterFD);

    public static native int read(int masterFD, ByteQueue queue, int timeout);

    public static native void write(int masterFD, byte[] data, int length);

    public static native void sendSignal(int pid, int signal);

    public static native void sendToast(String msg);

    public static native int setWindowSize(int rows, int cols);

    public static native void onExec(String cmd, String[] env);
}
