package com.termux.deepseek

class DeepSeekBridge {
    companion object {
        init {
            System.loadLibrary("deepseek_core")
        }
    }

    external fun nativeInitEngine(configPath: String): Long
    external fun nativeDestroy()
    external fun nativeCreateSession(name: String): String?
    external fun nativeListSessions(): String?
    external fun nativeSendMessage(sessionId: String, message: String): String?
    external fun nativeGetSessionHistory(sessionId: String): String?
    external fun nativeDeleteSession(sessionId: String): Boolean
    external fun nativeCreateCheckpoint(sessionId: String, description: String): String?
    external fun nativeListCheckpoints(sessionId: String): String?
    external fun nativeRestoreCheckpoint(checkpointId: String): Boolean
}
