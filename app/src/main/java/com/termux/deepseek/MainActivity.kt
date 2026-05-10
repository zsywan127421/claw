package com.termux.deepseek

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnNewSession: Button
    private lateinit var btnResumeSession: Button
    private lateinit var btnSettings: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvVersion: TextView
    private lateinit var sharedPrefs: SharedPreferences

    private var deepSeekBridge: DeepSeekBridge? = null
    private var currentSessionId: String? = null

    companion object {
        private const val PREFS_NAME = "termux_deepseek_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_WORKSPACE = "workspace_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        initializeViews()
        checkSetup()
    }

    private fun initializeViews() {
        btnNewSession = findViewById(R.id.btnNewSession)
        btnResumeSession = findViewById(R.id.btnResumeSession)
        btnSettings = findViewById(R.id.btnSettings)
        tvStatus = findViewById(R.id.tvStatus)
        tvVersion = findViewById(R.id.tvVersion)

        tvVersion.text = "TermuxDeepSeek v1.0.0"

        btnNewSession.setOnClickListener { startNewSession() }
        btnResumeSession.setOnClickListener { resumeSession() }
        btnSettings.setOnClickListener { openSettings() }
    }

    private fun checkSetup() {
        val apiKey = sharedPrefs.getString(KEY_API_KEY, null)

        if (apiKey.isNullOrEmpty()) {
            tvStatus.text = "⚠️ 请先配置 API Key"
            btnNewSession.isEnabled = false
            btnResumeSession.isEnabled = false

            btnNewSession.postDelayed({
                startActivity(Intent(this, SetupActivity::class.java))
            }, 1000)
        } else {
            tvStatus.text = "✅ 已配置: ${sharedPrefs.getString(KEY_MODEL, "deepseek-chat")}"
            btnNewSession.isEnabled = true
            btnResumeSession.isEnabled = true
            initializeDeepSeek()
        }
    }

    private fun initializeDeepSeek() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val workspacePath = sharedPrefs.getString(KEY_WORKSPACE, "/sdcard/TermuxDeepSeek")
                        ?: "/sdcard/TermuxDeepSeek"
                    File(workspacePath).mkdirs()

                    deepSeekBridge = DeepSeekBridge()
                    val configPath = createConfigFile()
                    val handle = deepSeekBridge?.nativeInitEngine(configPath)

                    withContext(Dispatchers.Main) {
                        if (handle != null && handle > 0) {
                            tvStatus.text = "✅ DeepSeek 引擎已初始化"
                        } else {
                            tvStatus.text = "❌ 引擎初始化失败"
                        }
                    }
                }
            } catch (e: Exception) {
                tvStatus.text = "❌ 初始化错误: ${e.message}"
            }
        }
    }

    private fun createConfigFile(): String {
        val configDir = File(filesDir, ".deepseek")
        configDir.mkdirs()
        val configFile = File(configDir, "config.toml")
        val apiKey = sharedPrefs.getString(KEY_API_KEY, "") ?: ""
        val model = sharedPrefs.getString(KEY_MODEL, "deepseek-chat") ?: "deepseek-chat"
        val workspace = sharedPrefs.getString(KEY_WORKSPACE, "/sdcard/TermuxDeepSeek")
            ?: "/sdcard/TermuxDeepSeek"

        val configContent = """
            |[api]
            |api_key = "$apiKey"
            |base_url = "https://api.deepseek.com"
            |model = "$model"
            |
            |[workspace]
            |path = "$workspace"
            |
            |[session]
            |auto_save = true
            |checkpoint_enabled = true
        """.trimMargin()

        configFile.writeText(configContent)
        return configFile.absolutePath
    }

    private fun startNewSession() {
        tvStatus.text = "🔄 启动新会话..."
        lifecycleScope.launch {
            try {
                val sessionId = deepSeekBridge?.nativeCreateSession("New Session")
                if (sessionId != null) {
                    currentSessionId = sessionId
                    tvStatus.text = "✅ 会话已创建"
                    val intent = Intent(this@MainActivity, TerminalActivity::class.java)
                    intent.putExtra(TerminalActivity.EXTRA_SESSION_ID, sessionId)
                    startActivity(intent)
                } else {
                    tvStatus.text = "❌ 会话创建失败"
                }
            } catch (e: Exception) {
                tvStatus.text = "❌ 错误: ${e.message}"
            }
        }
    }

    private fun resumeSession() {
        tvStatus.text = "📋 加载会话..."
        lifecycleScope.launch {
            try {
                val sessionsJson = deepSeekBridge?.nativeListSessions()
                if (sessionsJson != null && sessionsJson != "[]") {
                    tvStatus.text = "✅ 找到会话"
                    val intent = Intent(this@MainActivity, TerminalActivity::class.java)
                    intent.putExtra(TerminalActivity.EXTRA_SESSION_ID, "default_session")
                    startActivity(intent)
                } else {
                    tvStatus.text = "📭 暂无会话"
                }
            } catch (e: Exception) {
                tvStatus.text = "❌ 错误: ${e.message}"
            }
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, SetupActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        checkSetup()
    }

    override fun onDestroy() {
        super.onDestroy()
        deepSeekBridge?.nativeDestroy()
    }
}
