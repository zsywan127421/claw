package com.termux.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalRow;

public class TerminalRenderer {

    private static final int[] TEXT_COLORS = {
            Color.parseColor("#000000"),
            Color.parseColor("#CC0000"),
            Color.parseColor("#4E9A06"),
            Color.parseColor("#C4A000"),
            Color.parseColor("#3465A4"),
            Color.parseColor("#75507B"),
            Color.parseColor("#06989A"),
            Color.parseColor("#D3D7CF"),
            Color.parseColor("#555753"),
            Color.parseColor("#EF2929"),
            Color.parseColor("#8AE234"),
            Color.parseColor("#FCE94F"),
            Color.parseColor("#729FCF"),
            Color.parseColor("#AD7FA8"),
            Color.parseColor("#34E2E2"),
            Color.parseColor("#EEEEEC")
    };

    private static final int DEFAULT_TEXT_COLOR = Color.parseColor("#FFFFFF");
    private static final int DEFAULT_BACKGROUND_COLOR = Color.parseColor("#000000");

    private final Paint textPaint;
    private final Paint cursorPaint;
    private final Paint cursorTextPaint;

    private int cursorX = -1;
    private int cursorY = -1;
    private boolean cursorVisible = true;
    private long lastCursorChange = 0;

    private float fontSize = 10f;
    private String fontFamily = "monospace";

    public TerminalRenderer() {
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.MONOSPACE);

        cursorPaint = new Paint();
        cursorPaint.setColor(Color.WHITE);
        cursorPaint.setAntiAlias(true);

        cursorTextPaint = new Paint();
        cursorTextPaint.setAntiAlias(true);
        cursorTextPaint.setTypeface(Typeface.MONOSPACE);
    }

    public void setTextSize(float size) {
        this.fontSize = size;
        textPaint.setTextSize(size);
        cursorTextPaint.setTextSize(size);
    }

    public float getTextSize() {
        return fontSize;
    }

    public void setFontFamily(String family) {
        this.fontFamily = family;
        textPaint.setTypeface(Typeface.create(family, Typeface.NORMAL));
        cursorTextPaint.setTypeface(Typeface.create(family, Typeface.NORMAL));
    }

    public void render(TerminalEmulator emulator, Canvas canvas, int topRow, int rowsCount) {
        if (emulator == null || canvas == null) return;

        TerminalBuffer buffer = emulator.getTerminalBuffer();
        if (buffer == null) return;

        int columns = buffer.getColumns();
        int terminalHeight = buffer.getActiveRows();

        float charWidth = textPaint.measureText("M");
        float charHeight = textPaint.getTextSize();

        int bottomRow = Math.min(topRow + rowsCount - 1, terminalHeight - 1);
        int startRow = Math.max(0, bottomRow - rowsCount + 1);

        canvas.drawColor(DEFAULT_BACKGROUND_COLOR);

        for (int row = startRow; row <= bottomRow; row++) {
            TerminalRow terminalRow = buffer.getRow(row);
            if (terminalRow == null) continue;

            int[] chars = terminalRow.chars;
            int[] styles = terminalRow.style;
            int cols = terminalRow.getColumns();

            int y = (int) ((row - startRow + 1) * charHeight) - 4;

            for (int col = 0; col < cols; col++) {
                int charCode = chars[col];
                int style = styles[col];

                if (charCode == 0) continue;

                int x = (int) (col * charWidth);
                int fgColor = extractForegroundColor(style);
                int bgColor = extractBackgroundColor(style);

                textPaint.setColor(fgColor);
                textPaint.setFakeBoldText((style & TerminalEmulator.STYLE_BOLD) != 0);

                if ((style & TerminalEmulator.STYLE_ITALIC) != 0) {
                    textPaint.setTypeface(Typeface.create(fontFamily, Typeface.ITALIC));
                } else {
                    textPaint.setTypeface(Typeface.create(fontFamily, Typeface.NORMAL));
                }

                if ((style & TerminalEmulator.STYLE_INVERSE) != 0) {
                    textPaint.setColor(bgColor);
                }

                String charString = new String(Character.toChars(charCode));
                canvas.drawText(charString, x, y, textPaint);

                if ((style & TerminalEmulator.STYLE_UNDERLINE) != 0) {
                    canvas.drawLine(x, y + 2, x + charWidth, y + 2, textPaint);
                }

                if ((style & TerminalEmulator.STYLE_STRIKETHROUGH) != 0) {
                    canvas.drawLine(x, y - charHeight / 2, x + charWidth, y - charHeight / 2, textPaint);
                }
            }
        }

        drawCursor(emulator, canvas, topRow, startRow, charWidth, charHeight);
    }

    private void drawCursor(TerminalEmulator emulator, Canvas canvas, int topRow,
                            int startRow, float charWidth, float charHeight) {
        int cursorRow = emulator.getCursorRow();
        int cursorCol = emulator.getCursorCol();

        if (cursorRow < startRow || cursorRow > startRow + 60) return;

        cursorX = (int) (cursorCol * charWidth);
        cursorY = (int) ((cursorRow - startRow + 1) * charHeight);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCursorChange > 500) {
            cursorVisible = !cursorVisible;
            lastCursorChange = currentTime;
        }

        if (cursorVisible) {
            canvas.drawRect(cursorX, cursorY - charHeight + 2, cursorX + charWidth, cursorY + 2, cursorPaint);
        }
    }

    private int extractForegroundColor(int style) {
        int colorIndex = style & 0xFF;
        if (colorIndex >= 0 && colorIndex < 16) {
            return TEXT_COLORS[colorIndex];
        }
        return DEFAULT_TEXT_COLOR;
    }

    private int extractBackgroundColor(int style) {
        int colorIndex = (style >> 8) & 0xFF;
        if (colorIndex >= 0 && colorIndex < 16) {
            return TEXT_COLORS[colorIndex];
        }
        return DEFAULT_BACKGROUND_COLOR;
    }

    public float getCharWidth() {
        return textPaint.measureText("M");
    }

    public float getCharHeight() {
        return textPaint.getTextSize();
    }

    public Paint getTextPaint() {
        return textPaint;
    }

    public void setCursorVisible(boolean visible) {
        this.cursorVisible = visible;
        this.lastCursorChange = System.currentTimeMillis();
    }

    public boolean isCursorVisible() {
        return cursorVisible;
    }

    public void setCursorPosition(int x, int y) {
        this.cursorX = x;
        this.cursorY = y;
    }

    public int getCursorX() {
        return cursorX;
    }

    public int getCursorY() {
        return cursorY;
    }
}
