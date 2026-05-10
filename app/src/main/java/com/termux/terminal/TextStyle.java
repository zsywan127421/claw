package com.termux.terminal;

public final class TextStyle {
    public static final int ATTR_BOLD = 1;
    public static final int ATTR_ITALIC = 2;
    public static final int ATTR_UNDERLINE = 4;
    public static final int ATTR_BLINK = 5;
    public static final int ATTR_INVERSE = 7;
    public static final int ATTR_STRIKETHROUGH = 9;
    public static final int ATTR_DIM = 2;
    public static final int ATTR_HIDDEN = 8;

    public static final int COLOR_PALETTE_SIZE = 16;
    public static final int NUM_EXTRA_FG_COLORS = 2;
    public static final int NUM_EXTRA_BG_COLORS = 2;
    public static final int FIRST_EXTRA_FG_COLOR = 256;
    public static final int FIRST_PALETTE_COLOR = 0;
    public static final int FIRST_BRIGHT_COLOR = 8;
    public static final int DEFAULT_TEXT_COLOR = 257;
    public static final int DEFAULT_BACKGROUND_COLOR = 258;
    public static final int FOREGROUND_COLOR = 259;
    public static final int BACKGROUND_COLOR = 260;

    public static final int NUM_COLORS = 261;

    private final int mStyle;
    private final int mForeColor;
    private final int mBackColor;

    private static final TextStyle[] sCachedStyles = new TextStyle[128];
    private static int sCachedStyleCount = 0;

    public TextStyle(int style, int foreColor, int backColor) {
        mStyle = style;
        mForeColor = foreColor;
        mBackColor = backColor;
    }

    public static TextStyle of(int style) {
        return of(style, DEFAULT_TEXT_COLOR, DEFAULT_BACKGROUND_COLOR);
    }

    public static TextStyle of(int foreColor) {
        return of(0, foreColor, DEFAULT_BACKGROUND_COLOR);
    }

    public static TextStyle of(int foreColor, int backColor) {
        return of(0, foreColor, backColor);
    }

    public static synchronized TextStyle of(int style, int foreColor, int backColor) {
        int effectiveStyle = style;
        int effectiveFore = foreColor;
        int effectiveBack = backColor;

        if (foreColor == FOREGROUND_COLOR) {
            effectiveFore = DEFAULT_TEXT_COLOR;
        }
        if (backColor == BACKGROUND_COLOR) {
            effectiveBack = DEFAULT_BACKGROUND_COLOR;
        }

        int styleIndex = ((effectiveStyle & 0xF) << 16) | ((effectiveFore & 0x1FF) << 8) | (effectiveBack & 0x1FF);
        styleIndex = Math.abs(styleIndex) % sCachedStyles.length;

        if (sCachedStyles[styleIndex] != null) {
            TextStyle cached = sCachedStyles[styleIndex];
            if (cached.mStyle == effectiveStyle && cached.mForeColor == effectiveFore && cached.mBackColor == effectiveBack) {
                return cached;
            }
        }

        TextStyle result = new TextStyle(effectiveStyle, effectiveFore, effectiveBack);
        sCachedStyles[styleIndex] = result;
        sCachedStyleCount = Math.min(sCachedStyleCount + 1, sCachedStyles.length - 1);
        return result;
    }

    public static TextStyle fromEscapeSequence(int style, int foreColor, int backColor) {
        return of(style, foreColor, backColor);
    }

    public int getStyle() {
        return mStyle;
    }

    public int getForeColor() {
        return mForeColor;
    }

    public int getBackColor() {
        return mBackColor;
    }

    public boolean isBold() {
        return (mStyle & ATTR_BOLD) != 0;
    }

    public boolean isItalic() {
        return (mStyle & ATTR_ITALIC) != 0;
    }

    public boolean isUnderline() {
        return (mStyle & ATTR_UNDERLINE) != 0;
    }

    public boolean isBlink() {
        return (mStyle & ATTR_BLINK) != 0;
    }

    public boolean isInverse() {
        return (mStyle & ATTR_INVERSE) != 0;
    }

    public boolean isStrikethrough() {
        return (mStyle & ATTR_STRIKETHROUGH) != 0;
    }

    public boolean isDim() {
        return (mStyle & ATTR_DIM) != 0;
    }

    public boolean isHidden() {
        return (mStyle & ATTR_HIDDEN) != 0;
    }

    public static boolean isPaletteColor(int color) {
        return color >= 0 && color < 256;
    }

    public static boolean isDefaultTextColor(int color) {
        return color == DEFAULT_TEXT_COLOR;
    }

    public static boolean isDefaultBackgroundColor(int color) {
        return color == DEFAULT_BACKGROUND_COLOR;
    }

    public static boolean isTrueColor(int color) {
        return color >= 512;
    }

    public static int encodeTrueColor(int r, int g, int b) {
        return 0x2000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int decodeRed(int trueColor) {
        return (trueColor >> 16) & 0xFF;
    }

    public static int decodeGreen(int trueColor) {
        return (trueColor >> 8) & 0xFF;
    }

    public static int decodeBlue(int trueColor) {
        return trueColor & 0xFF;
    }

    public static int makeStyle(boolean bold, boolean italic, boolean underline, boolean blink, boolean inverse, boolean strikethrough) {
        int style = 0;
        if (bold) style |= ATTR_BOLD;
        if (italic) style |= ATTR_ITALIC;
        if (underline) style |= ATTR_UNDERLINE;
        if (blink) style |= ATTR_BLINK;
        if (inverse) style |= ATTR_INVERSE;
        if (strikethrough) style |= ATTR_STRIKETHROUGH;
        return style;
    }

    public static int addAttribute(int currentStyle, int attr) {
        return currentStyle | attr;
    }

    public static int removeAttribute(int currentStyle, int attr) {
        return currentStyle & ~attr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextStyle textStyle = (TextStyle) o;
        return mStyle == textStyle.mStyle && mForeColor == textStyle.mForeColor && mBackColor == textStyle.mBackColor;
    }

    @Override
    public int hashCode() {
        int result = mStyle;
        result = 31 * result + mForeColor;
        result = 31 * result + mBackColor;
        return result;
    }

    @Override
    public String toString() {
        return "TextStyle{style=" + mStyle + ", fore=" + mForeColor + ", back=" + mBackColor + "}";
    }
}
