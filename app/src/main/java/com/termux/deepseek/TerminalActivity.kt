package com.termux.deepseek

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.text.Html

class TerminalActivity : AppCompatActivity() {

    private lateinit var tvOutput: TextView
    private lateinit var etInput: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var btnSend: Button

    private var sessionId: String? = null
    private var deepSeekBridge: DeepSeekBridge? = null
    private var currentMode = "Agent"

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)

        supportActionBar?.apply {
            title = "DeepSeek Terminal"
            setDisplayHomeAsUpEnabled(true)
        }

        sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: run {
            Toast.makeText(this, "无效的会话", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        initializeBridge()
        showWelcome()
    }

    private fun initializeViews() {
        tvOutput = findViewById(R.id.tvOutput)
        etInput = findViewById(R.id.etInput)
        scrollView = findViewById(R.id.scrollView)
        btnSend = findViewById(R.id.btnSend)

        btnSend.setOnClickListener { sendMessage() }
    }

    private fun initializeBridge() {
        deepSeekBridge = DeepSeekBridge()
    }

    private fun showWelcome() {
        val welcome = """
╔═══════════════════════════════════════════════════════════╗
║           TermuxDeepSeek Terminal v1.0.0                 ║
╠═══════════════════════════════════════════════════════════╣
║  🤖 DeepSeek-TUI Android 终端                             ║
║  • Plan 模式: 只读分析，规划任务                         ║
║  • Agent 模式: 完整编程代理，执行操作                     ║
║  • YOLO 模式: 快速执行，无限制                           ║
╚═══════════════════════════════════════════════════════════╝

输入你的问题或任务，AI 将协助你完成。

命令:
  /help     - 显示帮助
  /mode     - 显示/切换模式
  /clear    - 清屏
  /checkpoint - 创建检查点
        """.trimIndent()

        appendOutput(welcome, "#0066FF")
    }

    private fun appendOutput(text: String, color: String = "#E0E0E0") {
        runOnUiThread {
            val coloredText = "<font color='$color'>$text</font>"
            tvOutput.append(Html.fromHtml(coloredText, Html.FROM_HTML_MODE_LEGACY))
            tvOutput.append("\n")
            scrollView.post {
                scrollView.fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
    }

    private fun sendMessage() {
        val input = etInput.text.toString().trim()
        if (input.isEmpty()) return

        appendOutput("\n$ ", "#00FF00")
        appendOutput(input, "#FFFFFF")
        etInput.text.clear()

        when {
            input.startsWith("/help") -> showHelp()
            input.startsWith("/mode") -> handleModeCommand(input)
            input.startsWith("/clear") -> clearOutput()
            input.startsWith("/checkpoint") -> createCheckpoint()
            input.startsWith("/sessions") -> listSessions()
            else -> processUserMessage(input)
        }
    }

    private fun showHelp() {
        val help = """
╔═══════════════════════════════════════════════╗
║         可用命令                               ║
╠═══════════════════════════════════════════════╣
║ /help      - 显示此帮助                       ║
║ /mode      - 显示当前模式                     ║
║ /mode <p|a|y> - 切换模式                     ║
║ /clear     - 清屏                            ║
║ /checkpoint - 创建检查点                      ║
║ /sessions  - 列出所有会话                     ║
╚═══════════════════════════════════════════════╝

模式说明:
  Plan  - 只读模式，只能分析文件
  Agent - 完整模式，可执行操作
  YOLO  - 无限制模式，可执行所有操作
        """.trimIndent()
        appendOutput(help, "#FFD700")
    }

    private fun handleModeCommand(input: String) {
        val parts = input.split(" ")
        if (parts.size < 2) {
            appendOutput("当前模式: $currentMode", "#B0B0B0")
            appendOutput("使用 /mode <p|a|y> 切换模式", "#B0B0B0")
            return
        }

        currentMode = when (parts[1].lowercase()) {
            "p", "plan" -> "Plan"
            "a", "agent" -> "Agent"
            "y", "yolo" -> "YOLO"
            else -> {
                appendOutput("无效模式. 使用: p (Plan), a (Agent), y (YOLO)", "#FF4444")
                return
            }
        }
        appendOutput("✅ 已切换到 $currentMode 模式", "#00FF00")
    }

    private fun clearOutput() {
        tvOutput.text = ""
        appendOutput("屏幕已清空", "#666666")
    }

    private fun createCheckpoint() {
        appendOutput("🔄 正在创建检查点...", "#B0B0B0")
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    deepSeekBridge?.nativeCreateCheckpoint(sessionId ?: "", "Auto checkpoint")
                }
                appendOutput("✅ 检查点已创建: $result", "#00FF00")
            } catch (e: Exception) {
                appendOutput("❌ 错误: ${e.message}", "#FF4444")
            }
        }
    }

    private fun listSessions() {
        appendOutput("📋 正在加载会话列表...", "#B0B0B0")
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    deepSeekBridge?.nativeListSessions()
                }
                appendOutput("会话: ${result ?: "[]"}", "#00FF00")
            } catch (e: Exception) {
                appendOutput("❌ 错误: ${e.message}", "#FF4444")
            }
        }
    }

    private fun processUserMessage(message: String) {
        appendOutput("\n🤖 正在思考...", "#B0B0B0")

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    deepSeekBridge?.nativeSendMessage(sessionId ?: "", message)
                }

                if (response != null) {
                    appendOutput("\n$response", "#E0E0E0")
                } else {
                    appendOutput("\n❌ 未收到响应", "#FF4444")
                }
            } catch (e: Exception) {
                appendOutput("\n❌ 错误: ${e.message}", "#FF4444")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_terminal, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_mode_plan -> {
                currentMode = "Plan"
                appendOutput("切换到 Plan 模式", "#0066FF")
                true
            }
            R.id.action_mode_agent -> {
                currentMode = "Agent"
                appendOutput("切换到 Agent 模式", "#0066FF")
                true
            }
            R.id.action_mode_yolo -> {
                currentMode = "YOLO"
                appendOutput("切换到 YOLO 模式", "#0066FF")
                true
            }
            R.id.action_clear -> {
                clearOutput()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deepSeekBridge?.nativeDestroy()
    }
}
