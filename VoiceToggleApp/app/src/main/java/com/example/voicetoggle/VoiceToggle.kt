package com.example.voicetoggle

import java.util.UUID

/**
 * Represents a single user-defined toggle with custom voice commands.
 *
 * @property id          Unique identifier for this toggle
 * @property name        Display name shown in the UI (e.g. "WiFi")
 * @property icon        Emoji used as the toggle's visual icon
 * @property onCommand   Keyword phrase that turns this toggle ON (e.g. "turn on wifi")
 * @property offCommand  Keyword phrase that turns this toggle OFF (e.g. "turn off wifi")
 * @property isOn        Current state of the toggle
 */
data class VoiceToggle(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "💡",
    val onCommand: String,
    val offCommand: String,
    var isOn: Boolean = false
) {
    /**
     * Returns true if the given spoken text matches this toggle's ON command.
     * Matching is case-insensitive and partial (contains).
     */
    fun matchesOnCommand(spokenText: String): Boolean =
        spokenText.lowercase().contains(onCommand.lowercase().trim())

    /**
     * Returns true if the given spoken text matches this toggle's OFF command.
     * Matching is case-insensitive and partial (contains).
     */
    fun matchesOffCommand(spokenText: String): Boolean =
        spokenText.lowercase().contains(offCommand.lowercase().trim())
}
