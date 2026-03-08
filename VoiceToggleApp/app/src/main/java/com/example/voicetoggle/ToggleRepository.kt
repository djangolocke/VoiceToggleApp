package com.example.voicetoggle

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists toggles in SharedPreferences as a JSON array.
 * All read/write operations are synchronous and lightweight.
 */
class ToggleRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("voice_toggles", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOGGLES = "toggles"
    }

    /** Load all saved toggles from disk. Returns an empty list if none saved yet. */
    fun loadToggles(): MutableList<VoiceToggle> {
        val json = prefs.getString(KEY_TOGGLES, null) ?: return getDefaultToggles()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                VoiceToggle(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    icon = obj.optString("icon", "💡"),
                    onCommand = obj.getString("onCommand"),
                    offCommand = obj.getString("offCommand"),
                    isOn = obj.getBoolean("isOn")
                )
            }.toMutableList()
        } catch (e: Exception) {
            getDefaultToggles()
        }
    }

    /** Save all toggles to disk. */
    fun saveToggles(toggles: List<VoiceToggle>) {
        val array = JSONArray()
        toggles.forEach { toggle ->
            array.put(JSONObject().apply {
                put("id", toggle.id)
                put("name", toggle.name)
                put("icon", toggle.icon)
                put("onCommand", toggle.onCommand)
                put("offCommand", toggle.offCommand)
                put("isOn", toggle.isOn)
            })
        }
        prefs.edit().putString(KEY_TOGGLES, array.toString()).apply()
    }

    /** Pre-loaded example toggles shown on first launch. */
    private fun getDefaultToggles(): MutableList<VoiceToggle> = mutableListOf(
        VoiceToggle(
            name = "WiFi",
            icon = "📶",
            onCommand = "turn on wifi",
            offCommand = "turn off wifi",
            isOn = true
        ),
        VoiceToggle(
            name = "Bluetooth",
            icon = "🔵",
            onCommand = "enable bluetooth",
            offCommand = "disable bluetooth"
        ),
        VoiceToggle(
            name = "Dark Mode",
            icon = "🌙",
            onCommand = "enable dark mode",
            offCommand = "disable dark mode",
            isOn = true
        ),
        VoiceToggle(
            name = "Do Not Disturb",
            icon = "🔕",
            onCommand = "turn on do not disturb",
            offCommand = "turn off do not disturb"
        )
    )
}
