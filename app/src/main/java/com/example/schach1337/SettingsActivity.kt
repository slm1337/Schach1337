package com.example.schach1337

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchEloSkill: SwitchCompat
    private lateinit var editSkillElo: EditText
    private lateinit var switchDepthTime: SwitchCompat
    private lateinit var editDepthTime: EditText
    private lateinit var spinnerNumaPolicy: Spinner
    private lateinit var editThreads: EditText
    private lateinit var editHash: EditText
    private lateinit var editMoveOverhead: EditText
    private lateinit var editNodestime: EditText
    private lateinit var editSyzygyProbeDepth: EditText
    private lateinit var checkboxSyzygy50: CheckBox
    private lateinit var editSyzygyProbeLimit: EditText
    private lateinit var buttonClearHash: Button
    private lateinit var buttonClose: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        buttonClose = findViewById(R.id.button_close)
        switchEloSkill = findViewById(R.id.switch_elo_skill)
        editSkillElo = findViewById(R.id.edit_skill_elo)
        switchDepthTime = findViewById(R.id.switch_depth_time)
        editDepthTime = findViewById(R.id.edit_depth_time)
        spinnerNumaPolicy = findViewById(R.id.spinner_numa_policy)
        editThreads = findViewById(R.id.edit_threads)
        editHash = findViewById(R.id.edit_hash)
        editMoveOverhead = findViewById(R.id.edit_move_overhead)
        editNodestime = findViewById(R.id.edit_nodestime)
        editSyzygyProbeDepth = findViewById(R.id.edit_syzygy_probe_depth)
        checkboxSyzygy50 = findViewById(R.id.checkbox_syzygy50)
        editSyzygyProbeLimit = findViewById(R.id.edit_syzygy_probe_limit)
        buttonClearHash = findViewById(R.id.button_clear_hash)

        switchEloSkill.isChecked = false // Default to Skill
        editSkillElo.setText("20") // Default Skill level: 20
        switchDepthTime.isChecked = true // Default to Depth
        editDepthTime.setText("10") // Default Depth: 10
        editThreads.setText("1") // Default Threads: 1
        editHash.setText("16") // Default Hash: 16
        editMoveOverhead.setText("10") // Default Move Overhead: 10
        editNodestime.setText("0") // Default nodestime: 0
        editSyzygyProbeDepth.setText("1") // Default SyzygyProbeDepth: 1
        checkboxSyzygy50.isChecked = true // Default Syzygy50MoveRule: true
        editSyzygyProbeLimit.setText("0") // Default SyzygyProbeLimit: 0

        val numaPolicies = arrayOf("none", "system", "auto", "hardware")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, numaPolicies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNumaPolicy.adapter = adapter
        spinnerNumaPolicy.setSelection(numaPolicies.indexOf("auto"))

        setupListeners()
    }

    private fun setupListeners() {
        buttonClose.setOnClickListener {
            lifecycleScope.launch {
                applySettings()
            }

            finish()
        }

        editSkillElo.hint = "Skill (0-20)"
        switchEloSkill.setOnCheckedChangeListener { _, isChecked ->
            editSkillElo.hint = if (isChecked) "Elo (1320-3190)" else "Skill (0-20)"
            validateSkillElo()
        }

        editDepthTime.hint = "Depth (≥1)"
        switchDepthTime.setOnCheckedChangeListener { _, isChecked ->
            editDepthTime.hint = if (isChecked) "Depth (≥1)" else "Time (≥500 ms)"
            validateDepthTime()
        }

        editSkillElo.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateSkillElo()
        }

        editDepthTime.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateDepthTime()
        }

        editThreads.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateIntRange(editThreads, 1, 1024)
        }

        editHash.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateIntRange(editHash, 1, 2048)
        }

        editMoveOverhead.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateIntRange(editMoveOverhead, 0, 5000)
        }

        editNodestime.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateIntRange(editNodestime, 0, 10000)
        }

        editSyzygyProbeDepth.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateIntRange(editSyzygyProbeDepth, 1, 100)
        }

        editSyzygyProbeLimit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateIntRange(editSyzygyProbeLimit, 0, 7)
        }

        buttonClearHash.setOnClickListener {
            StockfishEngine.sendCommand("setoption name Clear Hash")
        }
    }

    private fun validateSkillElo() {
        val text = editSkillElo.text.toString()
        val value = text.toIntOrNull()
        if (switchEloSkill.isChecked) {
            val newVal = value?.coerceIn(1320, 3190) ?: 1320
            if (value != newVal) editSkillElo.setText(newVal.toString())
        } else {
            val newVal = value?.coerceIn(0, 20) ?: 20
            if (value != newVal) editSkillElo.setText(newVal.toString())
        }
    }

    private fun validateDepthTime() {
        val text = editDepthTime.text.toString()
        val value = text.toIntOrNull()
        if (switchDepthTime.isChecked) {
            val newVal = value?.coerceAtLeast(1) ?: 10
            if (value != newVal) editDepthTime.setText(newVal.toString())
        } else {
            val newVal = value?.coerceAtLeast(500) ?: 500
            if (value != newVal) editDepthTime.setText(newVal.toString())
        }
    }

    private fun validateIntRange(editText: EditText, min: Int, max: Int) {
        val text = editText.text.toString()
        val value = text.toIntOrNull()
        val newVal = value?.coerceIn(min, max) ?: min
        if (value != newVal) editText.setText(newVal.toString())
    }

    private suspend fun applySettings() = withContext(Dispatchers.IO) {
        StockfishEngine.skillLevel = if (switchEloSkill.isChecked) null else editSkillElo.text.toString().toIntOrNull() ?: 20
        StockfishEngine.skillElo = if (switchEloSkill.isChecked) editSkillElo.text.toString().toIntOrNull() ?: 1320 else null
        StockfishEngine.depth = if (switchDepthTime.isChecked) editDepthTime.text.toString().toIntOrNull() ?: 10 else null
        StockfishEngine.searchTime = if (!switchDepthTime.isChecked) editDepthTime.text.toString().toIntOrNull() ?: 500 else null
        StockfishEngine.numThreads = editThreads.text.toString().toIntOrNull() ?: 1
        StockfishEngine.hash = editHash.text.toString().toIntOrNull() ?: 16
        StockfishEngine.numaPolicy = spinnerNumaPolicy.selectedItem.toString()
        StockfishEngine.moveOverhead = editMoveOverhead.text.toString().toIntOrNull() ?: 10
        StockfishEngine.nodesTime = editNodestime.text.toString().toIntOrNull() ?: 0
        StockfishEngine.syzygy50MoveRule = checkboxSyzygy50.isChecked
        StockfishEngine.syzygyProbeDepth = editSyzygyProbeDepth.text.toString().toIntOrNull() ?: 1
        StockfishEngine.syzygyProbeLimit = editSyzygyProbeLimit.text.toString().toIntOrNull() ?: 0

        StockfishEngine.applyEngineSettings()
    }
}