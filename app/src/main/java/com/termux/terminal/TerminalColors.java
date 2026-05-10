package com.termux.terminal;

import android.graphics.Color;

public final class TerminalColors {

    public static final int COLOR_INDEX_BLACK = 0;
    public static final int COLOR_INDEX_RED = 1;
    public static final int COLOR_INDEX_GREEN = 2;
    public static final int COLOR_INDEX_YELLOW = 3;
    public static final int COLOR_INDEX_BLUE = 4;
    public static final int COLOR_INDEX_MAGENTA = 5;
    public static final int COLOR_INDEX_CYAN = 6;
    public static final int COLOR_INDEX_WHITE = 7;

    public static final int COLOR_INDEX_BRIGHT_BLACK = 8;
    public static final int COLOR_INDEX_BRIGHT_RED = 9;
    public static final int COLOR_INDEX_BRIGHT_GREEN = 10;
    public static final int COLOR_INDEX_BRIGHT_YELLOW = 11;
    public static final int COLOR_INDEX_BRIGHT_BLUE = 12;
    public static final int COLOR_INDEX_BRIGHT_MAGENTA = 13;
    public static final int COLOR_INDEX_BRIGHT_CYAN = 14;
    public static final int COLOR_INDEX_BRIGHT_WHITE = 15;

    public static final int COLOR_INDEX_FOREGROUND = 258;
    public static final int COLOR_INDEX_BACKGROUND = 259;
    public static final int COLOR_INDEX_CURSOR_TEXT = 260;
    public static final int COLOR_INDEX_CURSOR_BACKGROUND = 261;

    public static final int BASIC_COLOR_COUNT = 16;
    public static final int PALETTE_SIZE = 256;
    public static final int TOTAL_COLORS = 512;

    private final int[] mCurrentColors;
    private int[][] mColorPalette;

    public TerminalColors() {
        mCurrentColors = new int[TOTAL_COLORS];
        mColorPalette = new int[PALETTE_SIZE][];
        initializeDefaultColors();
        initializeColorPalette();
    }

    private void initializeDefaultColors() {
        mCurrentColors[COLOR_INDEX_BLACK] = 0xFF000000;
        mCurrentColors[COLOR_INDEX_RED] = 0xFFCD0000;
        mCurrentColors[COLOR_INDEX_GREEN] = 0xFF00CD00;
        mCurrentColors[COLOR_INDEX_YELLOW] = 0xFFCDCD00;
        mCurrentColors[COLOR_INDEX_BLUE] = 0xFF0000CD;
        mCurrentColors[COLOR_INDEX_MAGENTA] = 0xFFCD00CD;
        mCurrentColors[COLOR_INDEX_CYAN] = 0xFF00CDCD;
        mCurrentColors[COLOR_INDEX_WHITE] = 0xFFE5E5E5;

        mCurrentColors[COLOR_INDEX_BRIGHT_BLACK] = 0xFF7F7F7F;
        mCurrentColors[COLOR_INDEX_BRIGHT_RED] = 0xFFFF0000;
        mCurrentColors[COLOR_INDEX_BRIGHT_GREEN] = 0xFF00FF00;
        mCurrentColors[COLOR_INDEX_BRIGHT_YELLOW] = 0xFFFFFF00;
        mCurrentColors[COLOR_INDEX_BRIGHT_BLUE] = 0xFF0000FF;
        mCurrentColors[COLOR_INDEX_BRIGHT_MAGENTA] = 0xFFFF00FF;
        mCurrentColors[COLOR_INDEX_BRIGHT_CYAN] = 0xFF00FFFF;
        mCurrentColors[COLOR_INDEX_BRIGHT_WHITE] = 0xFFFFFFFF;
    }

    private void initializeColorPalette() {
        for (int i = 0; i < PALETTE_SIZE; i++) {
            mColorPalette[i] = new int[3];
        }

        for (int i = 0; i < 16; i++) {
            mColorPalette[i][0] = Color.red(mCurrentColors[i]);
            mColorPalette[i][1] = Color.green(mCurrentColors[i]);
            mColorPalette[i][2] = Color.blue(mCurrentColors[i]);
        }

        for (int i = 16; i < 232; i++) {
            int r = (i - 16) / 36;
            int g = ((i - 16) % 36) / 6;
            int b = (i - 16) % 6;
            mColorPalette[i][0] = (r == 0) ? 0 : (r * 40 + 55);
            mColorPalette[i][1] = (g == 0) ? 0 : (g * 40 + 55);
            mColorPalette[i][2] = (b == 0) ? 0 : (b * 40 + 55);
        }

        for (int i = 232; i < 256; i++) {
            int gray = (i - 232) * 10 + 8;
            mColorPalette[i][0] = gray;
            mColorPalette[i][1] = gray;
            mColorPalette[i][2] = gray;
        }
    }

    public void setColor(int index, int color) {
        if (index >= 0 && index < TOTAL_COLORS) {
            mCurrentColors[index] = color;
        }
    }

    public void setColorFromRgb(int index, int r, int g, int b) {
        if (index >= 0 && index < PALETTE_SIZE) {
            mColorPalette[index][0] = r;
            mColorPalette[index][1] = g;
            mColorPalette[index][2] = b;
            mCurrentColors[index] = Color.rgb(r, g, b);
        }
    }

    public int getColor(int index) {
        if (index >= 0 && index < TOTAL_COLORS) {
            return mCurrentColors[index];
        }
        return 0xFF000000;
    }

    public int[] getColorComponents(int index) {
        if (index >= 0 && index < PALETTE_SIZE) {
            return mColorPalette[index];
        }
        return new int[]{0, 0, 0};
    }

    public boolean isTrueColor(int index) {
        return index >= 512;
    }

    public static int getTrueColor(int r, int g, int b) {
        return 0x2000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int extractRed(int trueColor) {
        return (trueColor >> 16) & 0xFF;
    }

    public static int extractGreen(int trueColor) {
        return (trueColor >> 8) & 0xFF;
    }

    public static int extractBlue(int trueColor) {
        return trueColor & 0xFF;
    }

    public void reset() {
        initializeDefaultColors();
        initializeColorPalette();
    }

    public void resetColor(int index) {
        if (index >= 0 && index < BASIC_COLOR_COUNT) {
            int[] defaultColors = new int[]{
                0xFF000000, 0xFFCD0000, 0xFF00CD00, 0xFFCDCD00,
                0xFF0000CD, 0xFFCD00CD, 0xFF00CDCD, 0xFFE5E5E5,
                0xFF7F7F7F, 0xFFFF0000, 0xFF00FF00, 0xFFFFFF00,
                0xFF0000FF, 0xFFFF00FF, 0xFF00FFFF, 0xFFFFFFFF
            };
            mCurrentColors[index] = defaultColors[index];
            mColorPalette[index][0] = Color.red(defaultColors[index]);
            mColorPalette[index][1] = Color.green(defaultColors[index]);
            mColorPalette[index][2] = Color.blue(defaultColors[index]);
        }
    }

    public int[] getCurrentColors() {
        return mCurrentColors;
    }

    public int[][] getColorPalette() {
        return mColorPalette;
    }

    public int getBlendedColor(int foreColor, int backColor) {
        int foreAlpha = Color.alpha(foreColor);
        int backAlpha = Color.alpha(backColor);
        
        int alpha = foreAlpha + backAlpha * (255 - foreAlpha) / 255;
        int red = (Color.red(foreColor) * foreAlpha + Color.red(backColor) * backAlpha * (255 - foreAlpha) / 255) / alpha;
        int green = (Color.green(foreColor) * foreAlpha + Color.green(backColor) * backAlpha * (255 - foreAlpha) / 255) / alpha;
        int blue = (Color.blue(foreColor) * foreAlpha + Color.blue(backColor) * backAlpha * (255 - foreAlpha) / 255) / alpha;
        
        return Color.argb(alpha, red, green, blue);
    }

    public static int makeBlendedColor(int color1, int color2, int ratio) {
        int inverseRatio = 255 - ratio;
        int r = (Color.red(color1) * ratio + Color.red(color2) * inverseRatio) / 255;
        int g = (Color.green(color1) * ratio + Color.green(color2) * inverseRatio) / 255;
        int b = (Color.blue(color1) * ratio + Color.blue(color2) * inverseRatio) / 255;
        int a = (Color.alpha(color1) * ratio + Color.alpha(color2) * inverseRatio) / 255;
        return Color.argb(a, r, g, b);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TerminalColors:\n");
        for (int i = 0; i < BASIC_COLOR_COUNT; i++) {
            sb.append("Color ").append(i).append(": #");
            sb.append(String.format("%08X", mCurrentColors[i]));
            if (i < BASIC_COLOR_COUNT - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
