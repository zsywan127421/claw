package com.termux.terminal;

public interface TerminalSessionClient {

    void onSessionStarted(TerminalSession session);

    void onSessionDataAvailable(TerminalSession session);

    void onSessionFinished(TerminalSession session);

    void onTerminalCursorStateChange(TerminalSession session, boolean cursorVisible);

    void onTerminalBufferSizeChanged(TerminalSession session, int width, int height);

    void onBell(TerminalSession session);

    void onTextChanged(TerminalSession session);

    void onScroll(TerminalSession session, int scrollDelta);
}
