package com.termux.terminal;

public final class JNI {
    static {
        System.loadLibrary("termux");
    }

    public static native int createSubprocess(
        String cmd, String cwd, String[] env, String[] args,
        int[] processId, int rows, int cols, int pixelWidth, int pixelHeight);

    public static native void setPtyWindowSize(int fd, int rows, int cols, int pixelWidth, int pixelHeight);

    public static native int waitFor(int processId);

    public static native void close(int fd);

    public static native int read(int fd, byte[] buffer, int length);

    public static native int write(int fd, byte[] buffer, int length);
}
