package com.termux.terminal;

public abstract class TerminalOutput {

    public void write(byte[] data, int offset, int length) {}

    public void close() {}
}
