package com.termux.terminal;

public class TerminalEmulator {

    public static final int STATE_NORMAL = 0;
    public static final int STATE_ESCAPE = 1;
    public static final int STATE_CSI = 2;
    public static final int STATE_OSC = 3;

    public static final int COLOR_OFFSET = 256;
    public static final int STYLE_OFFSET = 512;

    public static final int STYLE_BOLD = 1 << STYLE_OFFSET;
    public static final int STYLE_ITALIC = 2 << STYLE_OFFSET;
    public static final int STYLE_UNDERLINE = 4 << STYLE_OFFSET;
    public static final int STYLE_BLINK = 8 << STYLE_OFFSET;
    public static final int STYLE_INVERSE = 16 << STYLE_OFFSET;
    public static final int STYLE_STRIKETHROUGH = 32 << STYLE_OFFSET;

    private int state = STATE_NORMAL;
    private final int[]CSIParams = new int[30];
    private int CSIParamsIndex;
    private final StringBuilder oscString = new StringBuilder();

    private int cursorCol;
    private int cursorRow;
    private int savedCursorCol;
    private int savedCursorRow;
    private int currentStyle = 0;
    private int defaultStyle = 0;
    private boolean originMode;
    private boolean autoWrap;
    private boolean applicationCursorKeys;
    private boolean applicationNumericKeypad;
    private boolean bracketedPasteMode;
    private boolean originModeDefault;

    private int terminalWidth;
    private int terminalHeight;

    private final TerminalBuffer terminalBuffer;
    private final TerminalOutput output;

    public TerminalEmulator(TerminalOutput output, int width, int height) {
        this.output = output;
        this.terminalWidth = width;
        this.terminalHeight = height;
        this.terminalBuffer = new TerminalBuffer(width, height);
        reset();
    }

    public void resize(int width, int height) {
        this.terminalWidth = width;
        this.terminalHeight = height;
        terminalBuffer.resize(width, height);
        if (cursorRow >= height) cursorRow = height - 1;
        if (cursorCol >= width) cursorCol = width - 1;
    }

    public void reset() {
        state = STATE_NORMAL;
        cursorCol = 0;
        cursorRow = 0;
        savedCursorCol = 0;
        savedCursorRow = 0;
        currentStyle = defaultStyle;
        originMode = originModeDefault;
        autoWrap = true;
        applicationCursorKeys = false;
        applicationNumericKeypad = false;
        bracketedPasteMode = false;
        terminalBuffer.resetScrollRegion();
        terminalBuffer.setActiveBuffer(false);
    }

    public void append(char c) {
        switch (state) {
            case STATE_NORMAL:
                handleNormal(c);
                break;
            case STATE_ESCAPE:
                handleEscape(c);
                break;
            case STATE_CSI:
                handleCSI(c);
                break;
            case STATE_OSC:
                handleOSC(c);
                break;
        }
    }

    public void append(byte[] bytes, int length) {
        for (int i = 0; i < length; i++) {
            append((char) (bytes[i] & 0xFF));
        }
    }

    private void handleNormal(char c) {
        switch (c) {
            case 0x1B:
                state = STATE_ESCAPE;
                break;
            case '\r':
                cursorCol = 0;
                break;
            case '\n':
                if (cursorRow < terminalBuffer.getScrollRegionBottom()) {
                    cursorRow++;
                } else {
                    terminalBuffer.scroll(1);
                }
                break;
            case '\t':
                cursorCol = ((cursorCol / 8) + 1) * 8;
                if (cursorCol >= terminalWidth) cursorCol = terminalWidth - 1;
                break;
            case '\b':
                if (cursorCol > 0) cursorCol--;
                break;
            default:
                if (c >= 0x20 && c <= 0x7E) {
                    writeCharacter(c);
                } else if (c >= 0x80 && c < 0xA0) {
                    handleC1Control(c);
                }
                break;
        }
    }

    private void writeCharacter(int codePoint) {
        TerminalRow row = terminalBuffer.getRow(cursorRow);
        if (row == null) return;

        if (cursorCol >= terminalWidth) {
            if (autoWrap) {
                row.lineWrap = true;
                cursorCol = 0;
                if (cursorRow < terminalBuffer.getScrollRegionBottom()) {
                    cursorRow++;
                } else {
                    terminalBuffer.scroll(1);
                }
                row = terminalBuffer.getRow(cursorRow);
            } else {
                cursorCol = terminalWidth - 1;
            }
        }

        if (row != null) {
            int charToWrite = codePoint;
            if (codePoint >= 0x80 && codePoint < 0x800) {
                charToWrite = codePoint;
            }
            row.setChar(cursorCol, charToWrite, currentStyle);
            cursorCol++;
        }
    }

    private void handleC1Control(char c) {
        if (c == 0x9B) {
            state = STATE_CSI;
            CSIParamsIndex = 0;
            CSIParams[0] = 0;
        }
    }

    private void handleEscape(char c) {
        state = STATE_NORMAL;
        switch (c) {
            case '[':
                state = STATE_CSI;
                CSIParamsIndex = 0;
                CSIParams[0] = 0;
                break;
            case ']':
                state = STATE_OSC;
                oscString.setLength(0);
                break;
            case '7':
                savedCursorCol = cursorCol;
                savedCursorRow = cursorRow;
                break;
            case '8':
                cursorCol = savedCursorCol;
                cursorRow = savedCursorRow;
                break;
            case 'D':
                if (cursorRow < terminalBuffer.getScrollRegionBottom()) {
                    cursorRow++;
                } else {
                    terminalBuffer.scroll(1);
                }
                break;
            case 'M':
                if (cursorRow > terminalBuffer.getScrollRegionTop()) {
                    cursorRow--;
                } else {
                    terminalBuffer.scroll(-1);
                }
                break;
            case 'c':
                reset();
                break;
            case '(':
            case ')':
                state = STATE_ESCAPE;
                break;
            default:
                break;
        }
    }

    private void handleCSI(char c) {
        if (c >= '0' && c <= '9') {
            CSIParams[CSIParamsIndex] = CSIParams[CSIParamsIndex] * 10 + (c - '0');
        } else if (c == ';' && CSIParamsIndex < CSIParams.length - 1) {
            CSIParamsIndex++;
            CSIParams[CSIParamsIndex] = 0;
        } else if (c == '?') {
            CSIParamsIndex++;
            CSIParams[CSIParamsIndex] = -1;
        } else {
            executeCSI(c);
            state = STATE_NORMAL;
        }
    }

    private void executeCSI(char c) {
        int param1 = CSIParams[0];
        int param2 = CSIParamsIndex > 0 ? CSIParams[1] : 0;
        int param3 = CSIParamsIndex > 1 ? CSIParams[2] : 0;

        switch (c) {
            case 'A':
                cursorRow -= (param1 == 0) ? 1 : param1;
                if (cursorRow < terminalBuffer.getScrollRegionTop()) {
                    cursorRow = terminalBuffer.getScrollRegionTop();
                }
                break;
            case 'B':
                cursorRow += (param1 == 0) ? 1 : param1;
                if (cursorRow > terminalBuffer.getScrollRegionBottom()) {
                    cursorRow = terminalBuffer.getScrollRegionBottom();
                }
                break;
            case 'C':
                cursorCol += (param1 == 0) ? 1 : param1;
                if (cursorCol >= terminalWidth) cursorCol = terminalWidth - 1;
                break;
            case 'D':
                cursorCol -= (param1 == 0) ? 1 : param1;
                if (cursorCol < 0) cursorCol = 0;
                break;
            case 'H':
            case 'f':
                int newRow = (param1 == 0) ? 0 : param1 - 1;
                int newCol = (param2 == 0) ? 0 : param2 - 1;
                cursorRow = Math.min(newRow, terminalHeight - 1);
                cursorCol = Math.min(newCol, terminalWidth - 1);
                break;
            case 'J':
                executeEraseInDisplay(param1);
                break;
            case 'K':
                executeEraseInLine(param1);
                break;
            case 'm':
                executeSGR();
                break;
            case 'r':
                if (CSIParams[0] == 0 && CSIParams[1] == 0) {
                    terminalBuffer.resetScrollRegion();
                } else {
                    int top = Math.max(0, param1 - 1);
                    int bottom = Math.min(terminalHeight, param2);
                    if (bottom > top) {
                        terminalBuffer.setScrollRegion(top, bottom - 1);
                    }
                }
                break;
            case 's':
                savedCursorCol = cursorCol;
                savedCursorRow = cursorRow;
                break;
            case 'u':
                cursorCol = savedCursorCol;
                cursorRow = savedCursorRow;
                break;
            case 'l':
                if (CSIParams[0] == -1 && CSIParams[1] == 3) {
                    terminalBuffer.setActiveBuffer(true);
                } else if (CSIParams[0] == 6) {
                    originMode = false;
                }
                break;
            case 'h':
                if (CSIParams[0] == -1 && CSIParams[1] == 3) {
                    terminalBuffer.setActiveBuffer(true);
                } else if (CSIParams[0] == 6) {
                    originMode = true;
                }
                break;
            default:
                break;
        }
    }

    private void executeEraseInDisplay(int param) {
        switch (param) {
            case 0:
                TerminalRow row = terminalBuffer.getRow(cursorRow);
                if (row != null) {
                    for (int i = cursorCol; i < terminalWidth; i++) {
                        row.setChar(i, 0, currentStyle);
                    }
                }
                for (int i = cursorRow + 1; i < terminalHeight; i++) {
                    terminalBuffer.clearRow(i);
                }
                break;
            case 1:
                for (int i = 0; i < cursorRow; i++) {
                    terminalBuffer.clearRow(i);
                }
                TerminalRow currentRow = terminalBuffer.getRow(cursorRow);
                if (currentRow != null) {
                    for (int i = 0; i <= cursorCol; i++) {
                        currentRow.setChar(i, 0, currentStyle);
                    }
                }
                break;
            case 2:
            case 3:
                terminalBuffer.clearScreen();
                break;
        }
    }

    private void executeEraseInLine(int param) {
        TerminalRow row = terminalBuffer.getRow(cursorRow);
        if (row == null) return;

        switch (param) {
            case 0:
                for (int i = cursorCol; i < terminalWidth; i++) {
                    row.setChar(i, 0, currentStyle);
                }
                break;
            case 1:
                for (int i = 0; i <= cursorCol; i++) {
                    row.setChar(i, 0, currentStyle);
                }
                break;
            case 2:
                for (int i = 0; i < terminalWidth; i++) {
                    row.setChar(i, 0, currentStyle);
                }
                break;
        }
    }

    private void executeSGR() {
        for (int i = 0; i <= CSIParamsIndex; i++) {
            int param = CSIParams[i];
            if (param == 0) {
                currentStyle = defaultStyle;
            } else if (param == 1) {
                currentStyle |= STYLE_BOLD;
            } else if (param == 3) {
                currentStyle |= STYLE_ITALIC;
            } else if (param == 4) {
                currentStyle |= STYLE_UNDERLINE;
            } else if (param == 5) {
                currentStyle |= STYLE_BLINK;
            } else if (param == 7) {
                currentStyle |= STYLE_INVERSE;
            } else if (param == 9) {
                currentStyle |= STYLE_STRIKETHROUGH;
            } else if (param == 22) {
                currentStyle &= ~STYLE_BOLD;
            } else if (param == 23) {
                currentStyle &= ~STYLE_ITALIC;
            } else if (param == 24) {
                currentStyle &= ~STYLE_UNDERLINE;
            } else if (param == 25) {
                currentStyle &= ~STYLE_BLINK;
            } else if (param == 27) {
                currentStyle &= ~STYLE_INVERSE;
            } else if (param == 29) {
                currentStyle &= ~STYLE_STRIKETHROUGH;
            } else if (param >= 30 && param <= 37) {
                currentStyle = (currentStyle & ~0xFF) | (COLOR_OFFSET + param - 30);
            } else if (param == 39) {
                currentStyle = (currentStyle & ~0xFF) | defaultStyle;
            } else if (param >= 40 && param <= 47) {
                currentStyle = (currentStyle & ~(0xFF << 8)) | ((COLOR_OFFSET + param - 40) << 8);
            } else if (param == 49) {
                currentStyle = (currentStyle & ~(0xFF << 8)) | (defaultStyle & (0xFF << 8));
            }
        }
    }

    private void handleOSC(char c) {
        if (c == '\x07' || (oscString.length() > 0 && c == '\x1B')) {
            if (c == '\x1B' && oscString.length() > 0) {
                oscString.setLength(oscString.length() - 1);
            }
            state = STATE_NORMAL;
        } else if (c != '\x1B') {
            oscString.append(c);
        }
    }

    public int getCursorCol() {
        return cursorCol;
    }

    public int getCursorRow() {
        return cursorRow;
    }

    public int getTerminalWidth() {
        return terminalWidth;
    }

    public int getTerminalHeight() {
        return terminalHeight;
    }

    public TerminalBuffer getTerminalBuffer() {
        return terminalBuffer;
    }

    public int getState() {
        return state;
    }

    public int getCurrentStyle() {
        return currentStyle;
    }
}
