package com.termux.terminal;

public class TerminalBuffer {

    public static final int DEFAULT_STYLE = 0;

    private TerminalRow[] mainBuffer;
    private TerminalRow[] altBuffer;
    private TerminalRow[] activeBuffer;

    private int scrollRegionTop;
    private int scrollRegionBottom;
    private int activeRows;
    private int columns;
    private boolean usingAlternateBuffer;

    public TerminalBuffer(int columns, int rows) {
        this.columns = columns;
        this.activeRows = rows;
        this.scrollRegionTop = 0;
        this.scrollRegionBottom = rows - 1;
        this.usingAlternateBuffer = false;

        this.mainBuffer = new TerminalRow[rows];
        this.altBuffer = new TerminalRow[rows];
        this.activeBuffer = mainBuffer;

        for (int i = 0; i < rows; i++) {
            mainBuffer[i] = new TerminalRow(columns, DEFAULT_STYLE);
            altBuffer[i] = new TerminalRow(columns, DEFAULT_STYLE);
        }
    }

    public void resize(int columns, int rows) {
        TerminalRow[] oldBuffer = activeBuffer;
        int oldRows = activeRows;
        int oldColumns = this.columns;

        this.columns = columns;
        this.activeRows = rows;
        this.scrollRegionBottom = rows - 1;

        mainBuffer = new TerminalRow[rows];
        altBuffer = new TerminalRow[rows];

        for (int i = 0; i < rows; i++) {
            mainBuffer[i] = new TerminalRow(columns, DEFAULT_STYLE);
            altBuffer[i] = new TerminalRow(columns, DEFAULT_STYLE);
        }

        int copyRows = Math.min(oldRows, rows);
        int copyCols = Math.min(oldColumns, columns);

        for (int r = 0; r < copyRows; r++) {
            System.arraycopy(oldBuffer[r].chars, 0, mainBuffer[r].chars, 0, copyCols);
            System.arraycopy(oldBuffer[r].style, 0, mainBuffer[r].style, 0, copyCols);
            System.arraycopy(oldBuffer[r].cols, 0, mainBuffer[r].cols, 0, copyCols);
            mainBuffer[r].lineWrap = oldBuffer[r].lineWrap;
        }

        activeBuffer = usingAlternateBuffer ? altBuffer : mainBuffer;
    }

    public void setActiveBuffer(boolean alternate) {
        usingAlternateBuffer = alternate;
        activeBuffer = alternate ? altBuffer : mainBuffer;
    }

    public void setScrollRegion(int top, int bottom) {
        if (bottom >= top && bottom < activeRows) {
            scrollRegionTop = top;
            scrollRegionBottom = bottom;
        }
    }

    public void resetScrollRegion() {
        scrollRegionTop = 0;
        scrollRegionBottom = activeRows - 1;
    }

    public void scroll(int lines) {
        if (lines > 0) {
            for (int i = 0; i < lines; i++) {
                System.arraycopy(activeBuffer, scrollRegionTop + 1, activeBuffer, scrollRegionTop, scrollRegionBottom - scrollRegionTop);
                activeBuffer[scrollRegionBottom] = new TerminalRow(columns, DEFAULT_STYLE);
            }
        } else if (lines < 0) {
            for (int i = 0; i < -lines; i++) {
                System.arraycopy(activeBuffer, scrollRegionTop, activeBuffer, scrollRegionTop + 1, scrollRegionBottom - scrollRegionTop);
                activeBuffer[scrollRegionTop] = new TerminalRow(columns, DEFAULT_STYLE);
            }
        }
    }

    public void clearScreen() {
        for (int i = 0; i < activeRows; i++) {
            activeBuffer[i].clear(DEFAULT_STYLE);
        }
    }

    public void clearScrollRegion() {
        for (int i = scrollRegionTop; i <= scrollRegionBottom; i++) {
            activeBuffer[i].clear(DEFAULT_STYLE);
        }
    }

    public void clearRow(int row) {
        if (row >= 0 && row < activeRows) {
            activeBuffer[row].clear(DEFAULT_STYLE);
        }
    }

    public void clearFromCursorToEndOfScreen() {
        for (int i = 0; i <= scrollRegionBottom; i++) {
            activeBuffer[i].clear(DEFAULT_STYLE);
        }
    }

    public void clearFromCursorToBeginningOfScreen() {
        for (int i = 0; i <= scrollRegionBottom; i++) {
            activeBuffer[i].clear(DEFAULT_STYLE);
        }
    }

    public TerminalRow getRow(int index) {
        if (index >= 0 && index < activeRows) {
            return activeBuffer[index];
        }
        return null;
    }

    public int getActiveRows() {
        return activeRows;
    }

    public int getColumns() {
        return columns;
    }

    public int getScrollRegionTop() {
        return scrollRegionTop;
    }

    public int getScrollRegionBottom() {
        return scrollRegionBottom;
    }

    public boolean isUsingAlternateBuffer() {
        return usingAlternateBuffer;
    }
}
