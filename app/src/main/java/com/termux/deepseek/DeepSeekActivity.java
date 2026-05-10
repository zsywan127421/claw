package com.termux.deepseek;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.terminal.JNI;
import com.termux.terminal.Logger;
import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalOutput;
import com.termux.terminal.TerminalRow;
import com.termux.view.TerminalView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DeepSeekActivity extends AppCompatActivity implements TerminalView.TerminalViewClient {
    private static final String TAG = "DeepSeekActivity";
    private static final String PREFS_NAME = "deepseek_prefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_FIRST_RUN = "first_run";

    private TerminalView terminalView;
    private SharedPreferences sharedPrefs;
    private boolean isCtrlPressed = false;
    private boolean isKeyboardVisible = true;

    private int terminalFd = -1;
    private int terminalPid = -1;
    private Thread readerThread;
    private volatile boolean isRunning = false;
    private TerminalEmulator emulator;
    private final Object bufferLock = new Object();
    private byte[] readBuffer = new byte[4096];
    private int readStart = 0;
    private int readEnd = 0;

    private Handler mainHandler;

    private boolean handleKeyDown(int keyCode, KeyEvent event) {
        if (terminalFd < 0) return false;

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
                input = "\u001B[A".getBytes();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                input = "\u001B[B".getBytes();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                input = "\u001B[C".getBytes();
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                input = "\u001B[D".getBytes();
                break;
            case KeyEvent.KEYCODE_MOVE_END:
                input = "\u001B[F".getBytes();
                break;
            case KeyEvent.KEYCODE_MOVE_HOME:
                input = "\u001B[H".getBytes();
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return false;
        }

        if (input != null) {
            writeToTerminal(input);
            return true;
        }

        char c = event.getDisplayLabel();
        if (c != 0) {
            writeToTerminal(Character.toString(c).getBytes());
            return true;
        }

        String text = event.getCharacters();
        if (text != null && !text.isEmpty()) {
            writeToTerminal(text.getBytes());
            return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deepseek);

        mainHandler = new Handler(Looper.getMainLooper());
        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        terminalView = findViewById(R.id.terminal_view);
        terminalView.setTerminalViewClient(this);
        terminalView.setExternalKeyListener(new TerminalView.OnKeyListener() {
            @Override
            public boolean onKeyDown(int keyCode, KeyEvent event) {
                return handleKeyDown(keyCode, event);
            }

            @Override
            public boolean onKeyUp(int keyCode, KeyEvent event) {
                return false;
            }
        });
        setupExtraKeys();

        if (sharedPrefs.getBoolean(KEY_FIRST_RUN, true)) {
            showApiKeyDialog();
        } else {
            String apiKey = sharedPrefs.getString(KEY_API_KEY, "");
            if (apiKey.isEmpty()) {
                showApiKeyDialog();
            } else {
                saveConfig(apiKey);
                startTerminal();
            }
        }
    }

    private void setupExtraKeys() {
        Button btnCtrl = findViewById(R.id.btn_ctrl);
        if (btnCtrl != null) {
            btnCtrl.setOnClickListener(v -> {
                isCtrlPressed = !isCtrlPressed;
                v.setBackgroundColor(isCtrlPressed ? 0xFF0066FF : 0xFF2D333B);
            });
        }

        View btnTab = findViewById(R.id.btn_tab);
        if (btnTab != null) btnTab.setOnClickListener(v -> sendKey("\t"));
        View btnEsc = findViewById(R.id.btn_esc);
        if (btnEsc != null) btnEsc.setOnClickListener(v -> sendKey("\u001B"));
        View btnUp = findViewById(R.id.btn_up);
        if (btnUp != null) btnUp.setOnClickListener(v -> sendKey("\u001B[A"));
        View btnDown = findViewById(R.id.btn_down);
        if (btnDown != null) btnDown.setOnClickListener(v -> sendKey("\u001B[B"));
        View btnLeft = findViewById(R.id.btn_left);
        if (btnLeft != null) btnLeft.setOnClickListener(v -> sendKey("\u001B[D"));
        View btnRight = findViewById(R.id.btn_right);
        if (btnRight != null) btnRight.setOnClickListener(v -> sendKey("\u001B[C"));
        View btnHome = findViewById(R.id.btn_home);
        if (btnHome != null) btnHome.setOnClickListener(v -> sendKey("\u001B[H"));
        View btnEnd = findViewById(R.id.btn_end);
        if (btnEnd != null) btnEnd.setOnClickListener(v -> sendKey("\u001B[F"));
        View btnPipe = findViewById(R.id.btn_pipe);
        if (btnPipe != null) btnPipe.setOnClickListener(v -> sendKey("|"));
        View btnSlash = findViewById(R.id.btn_slash);
        if (btnSlash != null) btnSlash.setOnClickListener(v -> sendKey("/"));
        View btnMinus = findViewById(R.id.btn_minus);
        if (btnMinus != null) btnMinus.setOnClickListener(v -> sendKey("-"));
        View btnInterrupt = findViewById(R.id.btn_interrupt);
        if (btnInterrupt != null) btnInterrupt.setOnClickListener(v -> {
            if (terminalFd >= 0) writeToTerminal(new byte[]{0x03});
        });
        View btnHide = findViewById(R.id.btn_hide);
        if (btnHide != null) btnHide.setOnClickListener(v -> toggleKeyboard());
    }

    private void sendKey(String key) {
        if (terminalFd < 0) return;
        if (isCtrlPressed && key.length() == 1) {
            char c = key.charAt(0);
            if (c >= 'a' && c <= 'z') {
                writeToTerminal(new byte[]{(byte) (c - 'a' + 1)});
            } else {
                writeToTerminal(key.getBytes());
            }
            isCtrlPressed = false;
            Button btnCtrl = findViewById(R.id.btn_ctrl);
            if (btnCtrl != null) btnCtrl.setBackgroundColor(0xFF2D333B);
        } else {
            writeToTerminal(key.getBytes());
        }
    }

    private void writeToTerminal(byte[] data) {
        if (terminalFd < 0) return;
        try {
            JNI.write(terminalFd, data, data.length);
        } catch (Exception e) {
            Log.e(TAG, "Write error: " + e.getMessage());
        }
    }

    private void toggleKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (isKeyboardVisible) {
            imm.hideSoftInputFromWindow(terminalView.getWindowToken(), 0);
        } else {
            imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT);
        }
        isKeyboardVisible = !isKeyboardVisible;
    }

    private void showApiKeyDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_api_key, null);
        android.widget.EditText etApiKey = dialogView.findViewById(R.id.et_api_key);

        new AlertDialog.Builder(this)
            .setTitle("配置 DeepSeek API Key")
            .setMessage("请输入 DeepSeek API Key\n\n获取: https://platform.deepseek.com/")
            .setView(dialogView)
            .setPositiveButton("保存并开始", (dialog, which) -> {
                String apiKey = etApiKey.getText().toString().trim();
                if (!apiKey.isEmpty()) {
                    sharedPrefs.edit().putString(KEY_API_KEY, apiKey).putBoolean(KEY_FIRST_RUN, false).apply();
                    saveConfig(apiKey);
                    startTerminal();
                } else {
                    Toast.makeText(this, "请输入有效的 API Key", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .setNegativeButton("退出", (d, w) -> finish())
            .setCancelable(false)
            .show();
    }

    private void saveConfig(String apiKey) {
        File configDir = new File(getFilesDir(), ".deepseek");
        configDir.mkdirs();
        try {
            File configFile = new File(configDir, "config.toml");
            FileOutputStream fos = new FileOutputStream(configFile);
            String config = "[api]\napi_key = \"" + apiKey + "\"\nbase_url = \"https://api.deepseek.com\"\nmodel = \"deepseek-chat\"\n";
            fos.write(config.getBytes());
            fos.close();
            Log.d(TAG, "Config saved to: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to save config: " + e.getMessage());
        }
    }

    private void startTerminal() {
        String homeDir = getFilesDir().getAbsolutePath();
        String[] env = {
            "HOME=" + homeDir,
            "TERM=xterm-256color",
            "PATH=/data/user/0/com.termux.deepseek/files/usr/bin:/system/bin:/vendor/bin",
            "LANG=en_US.UTF-8",
            "DEEPSEEK_API_KEY=" + sharedPrefs.getString(KEY_API_KEY, ""),
        };

        int rows = 24;
        int cols = 80;

        try {
            int[] pidArr = new int[1];
            terminalFd = JNI.createSubprocess(
                "/bin/sh",
                homeDir,
                env,
                new String[]{"sh"},
                pidArr,
                rows,
                cols,
                0,
                0
            );
            terminalPid = pidArr[0];

            if (terminalFd < 0) {
                Log.e(TAG, "Failed to create subprocess");
                Toast.makeText(this, "终端创建失败", Toast.LENGTH_LONG).show();
                return;
            }

            Log.d(TAG, "Subprocess created: pid=" + terminalPid + ", fd=" + terminalFd);
            isRunning = true;

            emulator = new TerminalEmulator(new TerminalOutput() {
                @Override
                public void write(byte[] data, int offset, int length) {
                }

                @Override
                public void close() {
                }
            }, cols, rows);

            terminalView.attachEmulator(emulator);

            showWelcomeMessage();
            startReaderThread();

        } catch (Exception e) {
            Log.e(TAG, "Failed to start terminal", e);
            Toast.makeText(this, "终端启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showWelcomeMessage() {
        String welcome =
            "\033[34m\033[1m╔════════════════════════════════════════╗\033[0m\n" +
            "\033[34m\033[1m║     DeepSeek TUI Terminal v1.0.0      ║\033[0m\n" +
            "\033[34m\033[1m╚════════════════════════════════════════╝\033[0m\n" +
            "\n" +
            "  \033[32m✓\033[0m 终端初始化完成\n" +
            "  \033[32m✓\033[0m API Key 已配置\n" +
            "\n" +
            "  输入 \033[33m/deepseek\033[0m 开始聊天\n" +
            "  输入 \033[33m/help\033[0m 查看帮助\n" +
            "\n" +
            "  \033[90m提示: 使用下方工具栏输入特殊键\033[0m\n" +
            "\n$ ";

        byte[] welcomeBytes = welcome.getBytes();
        synchronized (bufferLock) {
            for (int i = 0; i < welcomeBytes.length; i++) {
                int pos = (readEnd + 1) % readBuffer.length;
                if (pos != readStart) {
                    readBuffer[readEnd] = welcomeBytes[i];
                    readEnd = pos;
                }
            }
        }
        mainHandler.post(() -> {
            if (terminalView != null) terminalView.invalidate();
        });
    }

    private void startReaderThread() {
        readerThread = new Thread(() -> {
            Log.d(TAG, "Reader thread started");
            byte[] buffer = new byte[2048];

            while (isRunning) {
                try {
                    if (terminalFd < 0) break;

                    int bytesRead = JNI.read(terminalFd, buffer, buffer.length);

                    if (bytesRead > 0) {
                        synchronized (bufferLock) {
                            for (int i = 0; i < bytesRead; i++) {
                                int pos = (readEnd + 1) % readBuffer.length;
                                if (pos != readStart) {
                                    readBuffer[readEnd] = buffer[i];
                                    readEnd = pos;
                                }
                            }
                        }

                        mainHandler.post(this::processBuffer);
                    } else if (bytesRead < 0) {
                        Log.d(TAG, "Read returned: " + bytesRead);
                        break;
                    }

                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Reader error: " + e.getMessage());
                    break;
                }
            }

            isRunning = false;
            Log.d(TAG, "Reader thread ended");

            mainHandler.post(() -> {
                Toast.makeText(DeepSeekActivity.this, "会话已结束", Toast.LENGTH_SHORT).show();
            });
        }, "TerminalReader");
        readerThread.start();
    }

    private void processBuffer() {
        if (emulator == null) return;

        synchronized (bufferLock) {
            while (readStart != readEnd) {
                byte b = readBuffer[readStart];
                readStart = (readStart + 1) % readBuffer.length;
                emulator.append((char) (b & 0xFF));
            }
        }

        if (terminalView != null) {
            terminalView.invalidate();
        }
    }

    @Override
    public void onTerminalCursorStateChange(boolean state) {
    }

    @Override
    public void onCopyTextToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("terminal", text));
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPasteTextFromClipboard() {
        if (terminalFd < 0) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
            CharSequence t = cm.getPrimaryClip().getItemAt(0).getText();
            if (t != null) {
                writeToTerminal(t.toString().getBytes());
            }
        }
    }

    @Override
    public void logDebug(String tag, String message) {
        Log.d(tag, message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;

        if (terminalFd >= 0) {
            try {
                JNI.close(terminalFd);
            } catch (Exception e) {
                Log.e(TAG, "Error closing fd: " + e.getMessage());
            }
            terminalFd = -1;
        }

        if (readerThread != null) {
            try {
                readerThread.join(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle("退出")
            .setMessage("确定要退出终端吗？")
            .setPositiveButton("退出", (d, w) -> finish())
            .setNegativeButton("取消", null)
            .show();
    }
}
