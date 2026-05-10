package com.termux.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TerminalView extends View implements TerminalSessionClient {

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3.0f;
    private static final float DEFAULT_FONT_SIZE = 10f;

    private TerminalSession terminalSession;
    private TerminalRenderer terminalRenderer;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private float scaleFactor = 1.0f;
    private float translateX = 0f;
    private float translateY = 0f;

    private int terminalColumns = 80;
    private int terminalRows = 24;

    private int viewWidth;
    private int viewHeight;

    private ExecutorService renderExecutor;
    private boolean pendingRender = false;

    private OnScrollListener scrollListener;
    private int scrollY = 0;
    private int maxScrollY = 0;

    public interface TerminalViewClient {
        void onTerminalCursorStateChange(boolean state);
        void onCopyTextToClipboard(String text);
        void onPasteTextFromClipboard();
        void logDebug(String tag, String message);
    }

    public interface OnScrollListener {
        void onScroll(int scrollY, int maxScrollY);
    }

    private TerminalViewClient terminalViewClient;

    public TerminalView(Context context) {
        super(context);
        init(context);
    }

    public TerminalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TerminalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        terminalRenderer = new TerminalRenderer();
        terminalRenderer.setTextSize(DEFAULT_FONT_SIZE);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                translateY += -distanceY;
                constrainTranslation();
                invalidate();
                updateScrollState();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                toggleScale();
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));

                float newSize = DEFAULT_FONT_SIZE * scaleFactor;
                terminalRenderer.setTextSize(newSize);

                updateTerminalSize();
                invalidate();
                return true;
            }
        });

        renderExecutor = Executors.newSingleThreadExecutor();
    }

    public void attachToSession(TerminalSession session) {
        this.terminalSession = session;
        updateTerminalSize();
    }

    public void setTerminalViewClient(TerminalViewClient client) {
        this.terminalViewClient = client;
    }

    public void setTerminalSession(TerminalSession session) {
        this.terminalSession = session;
        updateTerminalSize();
    }

    public TerminalSession getSession() {
        return terminalSession;
    }

    public void onData(byte[] data) {
        if (terminalSession != null) {
            terminalSession.writeToSession(data);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.viewWidth = w;
        this.viewHeight = h;
        updateTerminalSize();
    }

    private void updateTerminalSize() {
        float charWidth = terminalRenderer.getCharWidth();
        float charHeight = terminalRenderer.getCharHeight();

        int newColumns = Math.max(1, (int) (viewWidth / charWidth));
        int newRows = Math.max(1, (int) (viewHeight / charHeight));

        if (newColumns != terminalColumns || newRows != terminalRows) {
            terminalColumns = newColumns;
            terminalRows = newRows;
            maxScrollY = Math.max(0, terminalRows - 1);

            if (terminalSession != null) {
                terminalSession.updateSize(terminalRows, terminalColumns);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        if (terminalSession != null) {
            TerminalEmulator emulator = terminalSession.getEmulator();
            terminalRenderer.render(emulator, canvas, scrollY, terminalRows);
        } else {
            terminalRenderer.render(null, canvas, 0, terminalRows);
        }

        canvas.restore();

        if (pendingRender) {
            pendingRender = false;
            postInvalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = scaleGestureDetector.onTouchEvent(event);
        handled = gestureDetector.onTouchEvent(event) || handled;
        return handled || super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (terminalSession == null || !terminalSession.isRunning()) {
            return false;
        }

        byte[] input = null;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
                input = new byte[]{0x7F};
                break;
            case KeyEvent.KEYCODE_ENTER:
                input = new byte[]{0x0D};
                break;
            case KeyEvent.KEYCODE_ESCAPE:
                input = new byte[]{0x1B};
                break;
            case KeyEvent.KEYCODE_TAB:
                input = new byte[]{0x09};
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                input = new byte[]{0x1B, 0x5B, 0x41};
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                input = new byte[]{0x1B, 0x5B, 0x42};
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                input = new byte[]{0x1B, 0x5B, 0x43};
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                input = new byte[]{0x1B, 0x5B, 0x44};
                break;
            case KeyEvent.KEYCODE_MOVE_END:
                input = new byte[]{0x1B, 0x5B, 0x34, 0x7E};
                break;
            case KeyEvent.KEYCODE_MOVE_HOME:
                input = new byte[]{0x1B, 0x5B, 0x48};
                break;
        }

        if (input != null) {
            terminalSession.writeToSession(input);
            return true;
        }

        char c = event.getDisplayLabel();
        if (c != 0) {
            terminalSession.writeToSession(Character.toString(c).getBytes());
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void toggleScale() {
        if (scaleFactor > 1.0f) {
            scaleFactor = 1.0f;
        } else {
            scaleFactor = 2.0f;
        }
        terminalRenderer.setTextSize(DEFAULT_FONT_SIZE * scaleFactor);
        updateTerminalSize();
        invalidate();
    }

    private void constrainTranslation() {
        float maxTranslateY = 0;
        float minTranslateY = -maxScrollY * terminalRenderer.getCharHeight();

        if (translateY > maxTranslateY) {
            translateY = maxTranslateY;
        } else if (translateY < minTranslateY) {
            translateY = minTranslateY;
        }
    }

    private void updateScrollState() {
        scrollY = (int) (-translateY / terminalRenderer.getCharHeight());
        scrollY = Math.max(0, Math.min(scrollY, maxScrollY));

        if (scrollListener != null) {
            scrollListener.onScroll(scrollY, maxScrollY);
        }
    }

    public void setOnScrollListener(OnScrollListener listener) {
        this.scrollListener = listener;
    }

    public void setFontSize(float size) {
        float oldSize = terminalRenderer.getTextSize();
        scaleFactor = size / DEFAULT_FONT_SIZE;
        terminalRenderer.setTextSize(size);
        updateTerminalSize();
        invalidate();
    }

    public float getFontSize() {
        return terminalRenderer.getTextSize();
    }

    public void setScale(float scale) {
        this.scaleFactor = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
        terminalRenderer.setTextSize(DEFAULT_FONT_SIZE * scaleFactor);
        updateTerminalSize();
        invalidate();
    }

    public float getScale() {
        return scaleFactor;
    }

    public void resetZoom() {
        scaleFactor = 1.0f;
        translateX = 0f;
        translateY = 0f;
        terminalRenderer.setTextSize(DEFAULT_FONT_SIZE);
        updateTerminalSize();
        invalidate();
    }

    @Override
    public void onSessionStarted(TerminalSession session) {
        postInvalidate();
    }

    @Override
    public void onSessionDataAvailable(TerminalSession session) {
        postInvalidate();
    }

    @Override
    public void onSessionFinished(TerminalSession session) {
        postInvalidate();
    }

    @Override
    public void onTerminalCursorStateChange(TerminalSession session, boolean cursorVisible) {
        terminalRenderer.setCursorVisible(cursorVisible);
        postInvalidate();
    }

    @Override
    public void onTerminalBufferSizeChanged(TerminalSession session, int width, int height) {
        updateTerminalSize();
        postInvalidate();
    }

    @Override
    public void onBell(TerminalSession session) {
    }

    @Override
    public void onTextChanged(TerminalSession session) {
        postInvalidate();
    }

    @Override
    public void onScroll(TerminalSession session, int scrollDelta) {
        updateScrollState();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (renderExecutor != null) {
            renderExecutor.shutdown();
        }
    }

    public TerminalRenderer getRenderer() {
        return terminalRenderer;
    }

    public int getTerminalColumns() {
        return terminalColumns;
    }

    public int getTerminalRows() {
        return terminalRows;
    }
}
