package com.termux.terminal;

public final class ByteQueue {
    private final byte[] buffer;
    private final int capacity;
    private int readOffset;
    private int writeOffset;
    private final Object lock = new Object();

    public ByteQueue(int capacity) {
        this.capacity = capacity;
        this.buffer = new byte[capacity];
        this.readOffset = 0;
        this.writeOffset = 0;
    }

    public int read(byte[] dest, int offset, int length) {
        synchronized (lock) {
            int available = available();
            if (available == 0) {
                return 0;
            }
            int toRead = Math.min(length, available);
            int firstPart = Math.min(toRead, capacity - readOffset);
            int secondPart = toRead - firstPart;

            System.arraycopy(buffer, readOffset, dest, offset, firstPart);
            if (secondPart > 0) {
                System.arraycopy(buffer, 0, dest, offset + firstPart, secondPart);
            }

            readOffset = (readOffset + toRead) % capacity;
            return toRead;
        }
    }

    public int write(byte[] src, int offset, int length) {
        synchronized (lock) {
            int freeSpace = freeSpace();
            if (freeSpace == 0) {
                return 0;
            }
            int toWrite = Math.min(length, freeSpace);
            int firstPart = Math.min(toWrite, capacity - writeOffset);
            int secondPart = toWrite - firstPart;

            System.arraycopy(src, offset, buffer, writeOffset, firstPart);
            if (secondPart > 0) {
                System.arraycopy(src, offset + firstPart, buffer, 0, secondPart);
            }

            writeOffset = (writeOffset + toWrite) % capacity;
            return toWrite;
        }
    }

    public int readSingleByte() {
        synchronized (lock) {
            if (available() == 0) {
                return -1;
            }
            int b = buffer[readOffset] & 0xFF;
            readOffset = (readOffset + 1) % capacity;
            return b;
        }
    }

    public int peek() {
        synchronized (lock) {
            if (available() == 0) {
                return -1;
            }
            return buffer[readOffset] & 0xFF;
        }
    }

    public int available() {
        synchronized (lock) {
            if (writeOffset >= readOffset) {
                return writeOffset - readOffset;
            } else {
                return capacity - readOffset + writeOffset;
            }
        }
    }

    public int freeSpace() {
        synchronized (lock) {
            int used = available();
            return capacity - used - 1;
        }
    }

    public void clear() {
        synchronized (lock) {
            readOffset = 0;
            writeOffset = 0;
        }
    }

    public int getCapacity() {
        return capacity;
    }
}
