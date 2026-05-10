package com.termux.deepseek

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences

class SetupActivity : AppCompatActivity() {

    private lateinit var etApiKey: EditText
    private lateinit var spinnerModel: Spinner
    private lateinit var etWorkspace: EditText
    private lateinit var btnSave: Button
    private lateinit var tvTitle: TextView
    private lateinit var sharedPrefs: SharedPreferences

    companion object {
        private const val PREFS_NAME = "termux_deepseek_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_WORKSPACE = "workspace_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        supportActionBar?.apply {
            title = "设置"
            setDisplayHomeAsUpEnabled(true)
        }

        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        initializeViews()
        loadSettings()
    }

    private fun initializeViews() {
        tvTitle = findViewById(R.id.tvTitle)
        etApiKey = findViewById(R.id.etApiKey)
        spinnerModel = findViewById(R.id.spinnerModel)
        etWorkspace = findViewById(R.id.etWorkspace)
        btnSave = findViewById(R.id.btnSave)

        val models = arrayOf("deepseek-chat", "deepseek-coder", "deepseek-reasoner")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, models)
        spinnerModel.adapter = adapter

        btnSave.setOnClickListener { saveSettings() }
    }

    private fun loadSettings() {
        etApiKey.setText(sharedPrefs.getString(KEY_API_KEY, "") ?: "")
        spinnerModel.setSelection(
            when (sharedPrefs.getString(KEY_MODEL, "deepseek-chat")) {
                "deepseek-chat" -> 0
                "deepseek-coder" -> 1
                "deepseek-reasoner" -> 2
                else -> 0
            }
        )
        etWorkspace.setText(sharedPrefs.getString(KEY_WORKSPACE, "/sdcard/TermuxDeepSeek") ?: "")
    }

    private fun saveSettings() {
        val apiKey = etApiKey.text.toString().trim()
        val model = spinnerModel.selectedItem.toString()
        val workspace = etWorkspace.text.toString().trim()

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "请输入 API Key", Toast.LENGTH_SHORT).show()
            return
        }

        if (workspace.isEmpty()) {
            Toast.makeText(this, "请输入工作目录", Toast.LENGTH_SHORT).show()
            return
        }

        sharedPrefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_MODEL, model)
            .putString(KEY_WORKSPACE, workspace)
            .apply()

        Toast.makeText(this, "✅ 设置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
