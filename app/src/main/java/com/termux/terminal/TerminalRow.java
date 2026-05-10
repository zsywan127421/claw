package com.termux.terminal;

public class TerminalRow {

    public final int[] chars;
    public final int[] style;
    public final boolean[] cols;
    public boolean lineWrap;

    public TerminalRow(int columns, int defaultStyle) {
        this.chars = new int[columns];
        this.style = new int[columns];
        this.cols = new boolean[columns];
        this.lineWrap = false;
        clear(defaultStyle);
    }

    public void clear(int defaultStyle) {
        for (int i = 0; i < chars.length; i++) {
            chars[i] = 0;
            style[i] = defaultStyle;
            cols[i] = false;
        }
        lineWrap = false;
    }

    public void setChar(int column, int codePoint, int style) {
        if (column >= 0 && column < chars.length) {
            chars[column] = codePoint;
            this.style[column] = style;
        }
    }

    public int getChar(int column) {
        if (column >= 0 && column < chars.length) {
            return chars[column];
        }
        return 0;
    }

    public int getStyle(int column) {
        if (column >= 0 && column < style.length) {
            return style[column];
        }
        return 0;
    }

    public void setStyle(int column, int style) {
        if (column >= 0 && column < this.style.length) {
            this.style[column] = style;
        }
    }

    public boolean isWidth(int column) {
        if (column >= 0 && column < cols.length) {
            return cols[column];
        }
        return false;
    }

    public void setWidth(int column, boolean wide) {
        if (column >= 0 && column < cols.length) {
            cols[column] = wide;
        }
    }

    public int getColumns() {
        return chars.length;
    }

    public TerminalRow clone() {
        TerminalRow row = new TerminalRow(chars.length, 0);
        System.arraycopy(this.chars, 0, row.chars, 0, chars.length);
        System.arraycopy(this.style, 0, row.style, 0, style.length);
        System.arraycopy(this.cols, 0, row.cols, 0, cols.length);
        row.lineWrap = this.lineWrap;
        return row;
    }
}
