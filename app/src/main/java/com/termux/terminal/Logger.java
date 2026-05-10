package com.termux.terminal;

import android.util.Log;

public final class Logger {
    public static final String LOG_TAG = "Termux";
    
    public static final int LOG_LEVEL_VERBOSE = 0;
    public static final int LOG_LEVEL_DEBUG = 1;
    public static final int LOG_LEVEL_INFO = 2;
    public static final int LOG_LEVEL_WARN = 3;
    public static final int LOG_LEVEL_ERROR = 4;
    
    private static int currentLogLevel = LOG_LEVEL_DEBUG;
    
    private Logger() {
    }
    
    public static void setLogLevel(int level) {
        currentLogLevel = level;
    }
    
    public static void d(String tag, String message) {
        if (currentLogLevel <= LOG_LEVEL_DEBUG) {
            Log.d(tag, message);
        }
    }
    
    public static void d(String message) {
        d(LOG_TAG, message);
    }
    
    public static void i(String tag, String message) {
        if (currentLogLevel <= LOG_LEVEL_INFO) {
            Log.i(tag, message);
        }
    }
    
    public static void i(String message) {
        i(LOG_TAG, message);
    }
    
    public static void w(String tag, String message) {
        if (currentLogLevel <= LOG_LEVEL_WARN) {
            Log.w(tag, message);
        }
    }
    
    public static void w(String message) {
        w(LOG_TAG, message);
    }
    
    public static void e(String tag, String message) {
        if (currentLogLevel <= LOG_LEVEL_ERROR) {
            Log.e(tag, message);
        }
    }
    
    public static void e(String message) {
        e(LOG_TAG, message);
    }
    
    public static void e(String tag, String message, Throwable throwable) {
        if (currentLogLevel <= LOG_LEVEL_ERROR) {
            Log.e(tag, message, throwable);
        }
    }
    
    public static void e(String message, Throwable throwable) {
        e(LOG_TAG, message, throwable);
    }
    
    public static void v(String tag, String message) {
        if (currentLogLevel <= LOG_LEVEL_VERBOSE) {
            Log.v(tag, message);
        }
    }
    
    public static void v(String message) {
        v(LOG_TAG, message);
    }
    
    public static void log(int priority, String tag, String message) {
        Log.println(priority, tag, message);
    }
    
    public static void log(int priority, String message) {
        log(priority, LOG_TAG, message);
    }
    
    public static void println(String message) {
        System.out.println("[" + LOG_TAG + "] " + message);
    }
    
    public static void print(String message) {
        System.out.print("[" + LOG_TAG + "] " + message);
    }
}
