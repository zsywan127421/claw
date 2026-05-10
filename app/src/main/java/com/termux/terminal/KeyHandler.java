package com.termux.terminal;

import android.view.KeyEvent;

public final class KeyHandler {

    public static final int KEYCODE_FAKE_KEYDOWN = -10;
    public static final int KEYCODE_ESCAPE_PRINTABLE = -11;

    public static final byte[] ARROW_UP = {0x1B, 0x5B, 0x41};
    public static final byte[] ARROW_DOWN = {0x1B, 0x5B, 0x42};
    public static final byte[] ARROW_RIGHT = {0x1B, 0x5B, 0x43};
    public static final byte[] ARROW_LEFT = {0x1B, 0x5B, 0x44};

    public static final byte[] KEY_F1 = {0x1B, 0x4F, 0x50};
    public static final byte[] KEY_F2 = {0x1B, 0x4F, 0x51};
    public static final byte[] KEY_F3 = {0x1B, 0x4F, 0x52};
    public static final byte[] KEY_F4 = {0x1B, 0x4F, 0x53};
    public static final byte[] KEY_F5 = {0x1B, 0x5B, 0x31, 0x35, 0x7E};
    public static final byte[] KEY_F6 = {0x1B, 0x5B, 0x31, 0x37, 0x7E};
    public static final byte[] KEY_F7 = {0x1B, 0x5B, 0x31, 0x38, 0x7E};
    public static final byte[] KEY_F8 = {0x1B, 0x5B, 0x31, 0x39, 0x7E};
    public static final byte[] KEY_F9 = {0x1B, 0x5B, 0x32, 0x30, 0x7E};
    public static final byte[] KEY_F10 = {0x1B, 0x5B, 0x32, 0x31, 0x7E};
    public static final byte[] KEY_F11 = {0x1B, 0x5B, 0x32, 0x33, 0x7E};
    public static final byte[] KEY_F12 = {0x1B, 0x5B, 0x32, 0x34, 0x7E};

    public static final byte[] INSERT = {0x1B, 0x5B, 0x32, 0x7E};
    public static final byte[] DELETE = {0x1B, 0x5B, 0x33, 0x7E};
    public static final byte[] HOME = {0x1B, 0x5B, 0x48};
    public static final byte[] END = {0x1B, 0x5B, 0x46};
    public static final byte[] PAGE_UP = {0x1B, 0x5B, 0x35, 0x7E};
    public static final byte[] PAGE_DOWN = {0x1B, 0x5B, 0x36, 0x7E};

    public static final byte[] KEY_BACK_TAB = {0x1B, 0x5B, 0x5A};

    public static final byte[] KEY_CTRL_ARROW_UP = {0x1B, 0x5B, 0x31, 0x3B, 0x35, 0x41};
    public static final byte[] KEY_CTRL_ARROW_DOWN = {0x1B, 0x5B, 0x31, 0x3B, 0x35, 0x42};
    public static final byte[] KEY_CTRL_ARROW_RIGHT = {0x1B, 0x5B, 0x31, 0x3B, 0x35, 0x43};
    public static final byte[] KEY_CTRL_ARROW_LEFT = {0x1B, 0x5B, 0x31, 0x3B, 0x35, 0x44};

    public static final byte[] KEY_SHIFT_ARROW_UP = {0x1B, 0x5B, 0x31, 0x3B, 0x32, 0x41};
    public static final byte[] KEY_SHIFT_ARROW_DOWN = {0x1B, 0x5B, 0x31, 0x3B, 0x32, 0x42};
    public static final byte[] KEY_SHIFT_ARROW_RIGHT = {0x1B, 0x5B, 0x31, 0x3B, 0x32, 0x43};
    public static final byte[] KEY_SHIFT_ARROW_LEFT = {0x1B, 0x5B, 0x31, 0x3B, 0x32, 0x44};

    public static final byte[] KEY_ALT_ARROW_UP = {0x1B, 0x5B, 0x31, 0x3B, 0x33, 0x41};
    public static final byte[] KEY_ALT_ARROW_DOWN = {0x1B, 0x5B, 0x31, 0x3B, 0x33, 0x42};
    public static final byte[] KEY_ALT_ARROW_RIGHT = {0x1B, 0x5B, 0x31, 0x3B, 0x33, 0x43};
    public static final byte[] KEY_ALT_ARROW_LEFT = {0x1B, 0x5B, 0x31, 0x3B, 0x33, 0x44};

    public static byte[] getCode(boolean ctrl, boolean alt, boolean shift, int keyCode) {
        if (ctrl) {
            if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
                byte[] result = new byte[1];
                result[0] = (byte) (keyCode - KeyEvent.KEYCODE_A + 1);
                return result;
            }
            switch (keyCode) {
                case KeyEvent.KEYCODE_2:
                case KeyEvent.KEYCODE_SPACE:
                    return new byte[]{0x00};
                case KeyEvent.KEYCODE_3:
                    return new byte[]{0x1B};
                case KeyEvent.KEYCODE_4:
                    return new byte[]{0x1C};
                case KeyEvent.KEYCODE_5:
                    return new byte[]{0x1D};
                case KeyEvent.KEYCODE_6:
                    return new byte[]{0x1E};
                case KeyEvent.KEYCODE_7:
                    return new byte[]{0x1F};
                case KeyEvent.KEYCODE_8:
                    return new byte[]{0x7F};
                case KeyEvent.KEYCODE_BRACKET_LEFT:
                    return new byte[]{0x1B};
                case KeyEvent.KEYCODE_BACKSLASH:
                    return new byte[]{0x1C};
                case KeyEvent.KEYCODE_BRACKET_RIGHT:
                    return new byte[]{0x1D};
                case KeyEvent.KEYCODE_CARET:
                case KeyEvent.KEYCODE_6:
                    return new byte[]{0x1E};
                case KeyEvent.KEYCODE_MINUS:
                    return new byte[]{0x1F};
                case KeyEvent.KEYCODE_SLASH:
                    return new byte[]{0x1F};
                case KeyEvent.KEYCODE_PERIOD:
                    return new byte[]{0x1E};
                case KeyEvent.KEYCODE_COMMA:
                    return new byte[]{0x1F};
                case KeyEvent.KEYCODE_EQUALS:
                    return new byte[]{0x1D};
                case KeyEvent.KEYCODE_ENTER:
                    return new byte[]{0x0A};
                case KeyEvent.KEYCODE_TAB:
                    return new byte[]{0x09};
            }
        }

        if (alt) {
            if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
                byte[] result = new byte[2];
                result[0] = 0x1B;
                result[1] = (byte) (keyCode - KeyEvent.KEYCODE_A + 'a');
                return result;
            }
            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                byte[] result = new byte[2];
                result[0] = 0x1B;
                result[1] = (byte) ('0' + (keyCode - KeyEvent.KEYCODE_0));
                return result;
            }
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (shift) return KEY_SHIFT_ARROW_UP;
                if (ctrl) return KEY_CTRL_ARROW_UP;
                if (alt) return KEY_ALT_ARROW_UP;
                return ARROW_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (shift) return KEY_SHIFT_ARROW_DOWN;
                if (ctrl) return KEY_CTRL_ARROW_DOWN;
                if (alt) return KEY_ALT_ARROW_DOWN;
                return ARROW_DOWN;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (shift) return KEY_SHIFT_ARROW_RIGHT;
                if (ctrl) return KEY_CTRL_ARROW_RIGHT;
                if (alt) return KEY_ALT_ARROW_RIGHT;
                return ARROW_RIGHT;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (shift) return KEY_SHIFT_ARROW_LEFT;
                if (ctrl) return KEY_CTRL_ARROW_LEFT;
                if (alt) return KEY_ALT_ARROW_LEFT;
                return ARROW_LEFT;

            case KeyEvent.KEYCODE_MOVE_END:
                return END;
            case KeyEvent.KEYCODE_MOVE_HOME:
                return HOME;

            case KeyEvent.KEYCODE_INSERT:
                return INSERT;
            case KeyEvent.KEYCODE_FORWARD_DEL:
                return DELETE;

            case KeyEvent.KEYCODE_PAGE_UP:
                return PAGE_UP;
            case KeyEvent.KEYCODE_PAGE_DOWN:
                return PAGE_DOWN;

            case KeyEvent.KEYCODE_F1:
                return KEY_F1;
            case KeyEvent.KEYCODE_F2:
                return KEY_F2;
            case KeyEvent.KEYCODE_F3:
                return KEY_F3;
            case KeyEvent.KEYCODE_F4:
                return KEY_F4;
            case KeyEvent.KEYCODE_F5:
                return KEY_F5;
            case KeyEvent.KEYCODE_F6:
                return KEY_F6;
            case KeyEvent.KEYCODE_F7:
                return KEY_F7;
            case KeyEvent.KEYCODE_F8:
                return KEY_F8;
            case KeyEvent.KEYCODE_F9:
                return KEY_F9;
            case KeyEvent.KEYCODE_F10:
                return KEY_F10;
            case KeyEvent.KEYCODE_F11:
                return KEY_F11;
            case KeyEvent.KEYCODE_F12:
                return KEY_F12;

            case KeyEvent.KEYCODE_ESCAPE:
                return new byte[]{0x1B};
            case KeyEvent.KEYCODE_ENTER:
                return new byte[]{0x0D};
            case KeyEvent.KEYCODE_TAB:
                return new byte[]{0x09};

            case KeyEvent.KEYCODE_DEL:
                return new byte[]{0x7F};
        }

        return null;
    }

    public static boolean is_ctrl(char c) {
        return (c >= 0 && c < 32) || c == 127;
    }

    public static char ctrl_char(char c) {
        return (char) (c & 31);
    }
}
