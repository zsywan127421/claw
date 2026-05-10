package com.termux.terminal;

public abstract class TerminalOutput {

    public abstract void write(byte[] data, int length);

    public abstract void close();

    public void onTerminalCursorStateChange(boolean cursorVisible) {
    }

    public void onTerminalBufferSizeChanged(int width, int height) {
    }
}
