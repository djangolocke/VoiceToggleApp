package com.example.voicetoggle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voicetoggle.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ToggleAdapter
    private lateinit var voiceManager: VoiceRecognitionManager
    private lateinit var repository: ToggleRepository

    private val toggles: MutableList<VoiceToggle> = mutableListOf()
    private var isListening = false

    companion object {
        private const val REQUEST_RECORD_AUDIO = 101
    }

    // ───────────────────────────────── Lifecycle ──────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository   = ToggleRepository(this)
        voiceManager = VoiceRecognitionManager(this)

        setupRecyclerView()
        setupFab()
        setupVoiceButton()
        loadToggles()

        if (!voiceManager.isAvailable) {
            binding.tvVoiceStatusText.text = "Voice recognition not supported on this device"
            binding.btnVoice.isEnabled = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }

    // ────────────────────────────────── Setup ─────────────────────────────────

    private fun setupRecyclerView() {
        adapter = ToggleAdapter(
            onToggleChanged = { toggle, isOn ->
                toggle.isOn = isOn
                repository.saveToggles(toggles)
            },
            onEditClicked   = { toggle -> showAddEditDialog(toggle) },
            onDeleteClicked = { toggle -> confirmDelete(toggle) }
        )

        binding.recyclerToggles.layoutManager = LinearLayoutManager(this)
        binding.recyclerToggles.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddToggle.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun setupVoiceButton() {
        binding.btnVoice.setOnClickListener {
            if (isListening) {
                stopVoiceListening()
            } else {
                requestMicAndListen()
            }
        }
    }

    // ─────────────────────────────── Data ─────────────────────────────────────

    private fun loadToggles() {
        toggles.clear()
        toggles.addAll(repository.loadToggles())
        adapter.submitList(toggles.toList())
    }

    private fun saveAndRefresh() {
        repository.saveToggles(toggles)
        adapter.submitList(toggles.toList())
    }

    // ──────────────────────────────── Dialogs ─────────────────────────────────

    private fun showAddEditDialog(existing: VoiceToggle?) {
        val dialog = AddToggleDialog(existing) { savedToggle ->
            if (existing == null) {
                toggles.add(savedToggle)
            } else {
                val idx = toggles.indexOfFirst { it.id == savedToggle.id }
                if (idx >= 0) toggles[idx] = savedToggle
            }
            saveAndRefresh()
        }
        dialog.show(supportFragmentManager, AddToggleDialog.TAG)
    }

    private fun confirmDelete(toggle: VoiceToggle) {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Delete \"${toggle.name}\"?")
            .setMessage("This will remove the toggle and its voice commands.")
            .setPositiveButton("Delete") { _, _ ->
                toggles.removeAll { it.id == toggle.id }
                saveAndRefresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─────────────────────────────── Voice ────────────────────────────────────

    private fun requestMicAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceListening()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceListening()
        } else {
            Toast.makeText(this, "Microphone permission is required for voice commands", Toast.LENGTH_LONG).show()
        }
    }

    private fun startVoiceListening() {
        isListening = true
        updateVoiceUI(listening = true)

        voiceManager.startListening(
            onReady = {
                runOnUiThread {
                    binding.tvVoiceStatusText.text = "Listening… speak now"
                    binding.tvLastCommand.text     = "🎙 Speak your command…"
                }
            },
            onBeginSpeech = {
                runOnUiThread {
                    binding.tvVoiceStatusText.text = "Hearing you…"
                }
            },
            onEndSpeech = {
                runOnUiThread {
                    binding.tvVoiceStatusText.text = "Processing…"
                }
            },
            onResult = { text ->
                runOnUiThread {
                    handleVoiceResult(text)
                    stopVoiceListening()
                }
            },
            onError = { message ->
                runOnUiThread {
                    binding.tvLastCommand.text     = "⚠️ $message"
                    binding.tvVoiceStatusText.text = "Tap to try again"
                    stopVoiceListening()
                }
            }
        )
    }

    private fun stopVoiceListening() {
        isListening = false
        voiceManager.stopListening()
        updateVoiceUI(listening = false)
    }

    private fun updateVoiceUI(listening: Boolean) {
        binding.btnVoice.text = if (listening) "Stop" else "Listen"
        binding.ivVoiceIcon.alpha = if (listening) 1f else 0.7f

        if (!listening && binding.tvVoiceStatusText.text == "Listening… speak now") {
            binding.tvVoiceStatusText.text = "Tap the mic to start"
        }
    }

    /**
     * Match the recognised [text] against all toggle commands and apply any matches.
     * Reports the result in the UI status bar.
     */
    private fun handleVoiceResult(text: String) {
        binding.tvLastCommand.text = "🗣 \"$text\""

        var matched = false
        toggles.forEach { toggle ->
            when {
                toggle.matchesOnCommand(text) -> {
                    toggle.isOn = true
                    matched = true
                    showMatchFeedback(toggle, true)
                }
                toggle.matchesOffCommand(text) -> {
                    toggle.isOn = false
                    matched = true
                    showMatchFeedback(toggle, false)
                }
            }
        }

        if (matched) {
            saveAndRefresh()
        } else {
            binding.tvVoiceStatusText.text = "No matching command found"
            Toast.makeText(this, "No toggle matched \"$text\"", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMatchFeedback(toggle: VoiceToggle, isOn: Boolean) {
        val state = if (isOn) "ON ✅" else "OFF 🔴"
        binding.tvVoiceStatusText.text = "${toggle.icon} ${toggle.name} → $state"
        Toast.makeText(this, "${toggle.name} turned $state", Toast.LENGTH_SHORT).show()
    }
}
