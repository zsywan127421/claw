package com.termux.deepseek;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.terminal.Logger;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DeepSeekActivity extends AppCompatActivity implements TerminalView.TerminalViewClient {
    private static final String PREFS_NAME = "deepseek_prefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_FIRST_RUN = "first_run";

    private TerminalView terminalView;
    private TerminalSession terminalSession;
    private SharedPreferences sharedPrefs;
    private boolean isCtrlPressed = false;
    private boolean isKeyboardVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deepseek);

        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        terminalView = findViewById(R.id.terminal_view);
        terminalView.setTerminalViewClient(this);
        setupExtraKeys();

        if (sharedPrefs.getBoolean(KEY_FIRST_RUN, true)) {
            showApiKeyDialog();
        } else {
            startTerminal();
        }
    }

    private void setupExtraKeys() {
        findViewById(R.id.btn_ctrl).setOnClickListener(v -> {
            isCtrlPressed = !isCtrlPressed;
            v.setBackgroundColor(isCtrlPressed ? 0xFF0066FF : 0xFF2D333B);
        });
        findViewById(R.id.btn_tab).setOnClickListener(v -> sendKey("\t"));
        findViewById(R.id.btn_esc).setOnClickListener(v -> sendKey("\u001b"));
        findViewById(R.id.btn_up).setOnClickListener(v -> sendKey("\u001b[A"));
        findViewById(R.id.btn_down).setOnClickListener(v -> sendKey("\u001b[B"));
        findViewById(R.id.btn_left).setOnClickListener(v -> sendKey("\u001b[D"));
        findViewById(R.id.btn_right).setOnClickListener(v -> sendKey("\u001b[C"));
        findViewById(R.id.btn_home).setOnClickListener(v -> sendKey("\u001b[H"));
        findViewById(R.id.btn_end).setOnClickListener(v -> sendKey("\u001b[F"));
        findViewById(R.id.btn_pipe).setOnClickListener(v -> sendKey("|"));
        findViewById(R.id.btn_slash).setOnClickListener(v -> sendKey("/"));
        findViewById(R.id.btn_minus).setOnClickListener(v -> sendKey("-"));
        findViewById(R.id.btn_interrupt).setOnClickListener(v -> {
            if (terminalSession != null) terminalSession.write("\u0003");
        });
        findViewById(R.id.btn_hide).setOnClickListener(v -> toggleKeyboard());
    }

    private void sendKey(String key) {
        if (terminalSession == null) return;
        if (isCtrlPressed && key.length() == 1) {
            char c = key.charAt(0);
            if (c >= 'a' && c <= 'z') {
                terminalSession.write(String.valueOf((char) (c - 'a' + 1)));
            } else {
                terminalSession.write(key);
            }
            isCtrlPressed = false;
            findViewById(R.id.btn_ctrl).setBackgroundColor(0xFF2D333B);
        } else {
            terminalSession.write(key);
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
            FileOutputStream fos = new FileOutputStream(new File(configDir, "config.toml"));
            String config = "[api]\napi_key = \"" + apiKey + "\"\nbase_url = \"https://api.deepseek.com\"\nmodel = \"deepseek-chat\"\n";
            fos.write(config.getBytes());
            fos.close();
        } catch (IOException e) {
            Logger.e("DeepSeek", "Failed to save config: " + e.getMessage());
        }
    }

    private void startTerminal() {
        String homeDir = getFilesDir().getAbsolutePath();
        String[] env = {
            "HOME=" + homeDir,
            "TERM=xterm-256color",
            "PATH=/system/bin:/vendor/bin",
            "LANG=en_US.UTF-8",
            "DEEPSEEK_API_KEY=" + sharedPrefs.getString(KEY_API_KEY, ""),
        };

        terminalSession = new TerminalSession(null, env, homeDir);
        terminalView.setTerminalSession(terminalSession);

        terminalSession.start(
            "/system/bin/sh",
            new String[]{"sh", "-c",
                "echo '\\033[34m\\033[1m╔══════════════════════════════════════╗\\033[0m';" +
                "echo '\\033[34m\\033[1m║    DeepSeek TUI Terminal v1.0.0       ║\\033[0m';" +
                "echo '\\033[34m\\033[1m╚══════════════════════════════════════╝\\033[0m';" +
                "echo ''; echo '  输入 /help 查看命令'; echo ''; exec /system/bin/sh"},
            env,
            homeDir,
            new TerminalSession.SessionCallback() {
                @Override public void onTextChanged(TerminalSession s) { terminalView.invalidate(); }
                @Override public void onTitleChanged(TerminalSession s) {}
                @Override public void onSessionFinished(TerminalSession s) {
                    runOnUiThread(() -> Toast.makeText(DeepSeekActivity.this, "会话已结束", Toast.LENGTH_SHORT).show());
                }
            }
        );
    }

    @Override public void onTerminalCursorStateChange(boolean state) {}
    @Override public void onCopyTextToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("terminal", text));
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
    }
    @Override public void onPasteTextFromClipboard() {
        if (terminalSession == null) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
            CharSequence t = cm.getPrimaryClip().getItemAt(0).getText();
            if (t != null) terminalSession.write(t.toString());
        }
    }
    @Override public void logDebug(String tag, String message) { Logger.d(tag, message); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (terminalSession != null) terminalSession.finishIfRunning();
    }
}
