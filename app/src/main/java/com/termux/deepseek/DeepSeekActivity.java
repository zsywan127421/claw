package com.termux.deepseek;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.terminal.KeyHandler;
import com.termux.terminal.Logger;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DeepSeekActivity extends AppCompatActivity implements TerminalView.TerminalViewClient {
    private static final String LOG_TAG = "DeepSeekActivity";
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

        if (isFirstRun()) {
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
        if (terminalSession != null) {
            if (isCtrlPressed && key.length() == 1) {
                char c = key.charAt(0);
                if (c >= 'a' && c <= 'z') terminalSession.write(String.valueOf((char)(c - 'a' + 1)));
                else if (c >= 'A' && c <= 'Z') terminalSession.write(String.valueOf((char)(c - 'A' + 1)));
                else terminalSession.write(key);
                isCtrlPressed = false;
                findViewById(R.id.btn_ctrl).setBackgroundColor(0xFF2D333B);
            } else {
                terminalSession.write(key);
            }
        }
    }

    private void toggleKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (isKeyboardVisible) {
            imm.hideSoftInputFromWindow(terminalView.getWindowToken(), 0);
            findViewById(R.id.extra_keys_scroll).setVisibility(View.GONE);
        } else {
            imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT);
            findViewById(R.id.extra_keys_scroll).setVisibility(View.VISIBLE);
        }
        isKeyboardVisible = !isKeyboardVisible;
    }

    private boolean isFirstRun() {
        return sharedPrefs.getBoolean(KEY_FIRST_RUN, true);
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
                    saveApiKey(apiKey);
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

    private void saveApiKey(String apiKey) {
        sharedPrefs.edit().putString(KEY_API_KEY, apiKey).putBoolean(KEY_FIRST_RUN, false).apply();
        File configDir = new File(getFilesDir(), ".deepseek");
        configDir.mkdirs();
        try {
            FileOutputStream fos = new FileOutputStream(new File(configDir, "config.toml"));
            fos.write(("[api]\napi_key = \"" + apiKey + "\"\nbase_url = \"https://api.deepseek.com\"\nmodel = \"deepseek-chat\"\n".getBytes()));
            fos.close();
        } catch (IOException e) {
            Logger.e(LOG_TAG, "Failed to save config", e);
        }
    }

    private void startTerminal() {
        String[] env = {
            "HOME=" + getFilesDir().getAbsolutePath(),
            "TERM=xterm-256color",
            "PATH=/system/bin:/vendor/bin",
            "LANG=en_US.UTF-8",
        };

        terminalSession = new TerminalSession(
            "/system/bin/sh",
            getFilesDir().getAbsolutePath(),
            new String[]{"-c", "echo '\u001b[34m\u001b[1mDeepSeek TUI Terminal Ready\u001b[0m'; echo ''; echo 'Type /help for commands'; echo ''; exec /system/bin/sh"},
            env,
            this
        );
        terminalView.setTerminalSession(terminalSession);
        terminalSession.start(24, 80);
    }

    @Override
    public void onTerminalCursorStateChange(boolean state) {}

    @Override
    public void onCopyTextToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("terminal", text));
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPasteTextFromClipboard() {
        if (terminalSession != null) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (text != null) terminalSession.write(text.toString());
            }
        }
    }

    @Override
    public void logDebug(String tag, String message) {
        Logger.d(tag, message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (terminalSession != null) terminalSession.finishIfRunning();
    }
}
