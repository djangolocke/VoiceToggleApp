package com.example.voicetoggle

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.voicetoggle.databinding.DialogAddToggleBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom-sheet dialog for creating or editing a [VoiceToggle].
 * Pass an existing toggle via [VoiceToggle] to pre-fill the fields (edit mode).
 */
class AddToggleDialog(
    private val existingToggle: VoiceToggle? = null,
    private val onSave: (VoiceToggle) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogAddToggleBinding? = null
    private val binding get() = _binding!!

    private val availableIcons = listOf(
        "💡", "📶", "🔵", "🌙", "🔕", "📷", "🔦", "🎵",
        "🌡️", "🔒", "🛡️", "⚡", "🌐", "📱", "🎮", "🖨️"
    )
    private var selectedIcon = "💡"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.BottomSheetTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddToggleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill if editing
        existingToggle?.let { t ->
            binding.etToggleName.setText(t.name)
            binding.etOnCommand.setText(t.onCommand)
            binding.etOffCommand.setText(t.offCommand)
            selectedIcon = t.icon
        }

        buildIconPicker()
    }

    private fun buildIconPicker() {
        val context = requireContext()
        binding.llIconPicker.removeAllViews()

        availableIcons.forEach { emoji ->
            val tv = TextView(context).apply {
                text = emoji
                textSize = 26f
                setPadding(16, 12, 16, 12)
                isClickable = true
                isFocusable = true
                background = ContextCompat.getDrawable(context,
                    if (emoji == selectedIcon) R.drawable.bg_icon_selected
                    else R.drawable.bg_toggle_icon
                )
                setOnClickListener {
                    selectedIcon = emoji
                    // Refresh the picker to show the new selection
                    buildIconPicker()
                }
            }
            val lp = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 8, 0) }
            binding.llIconPicker.addView(tv, lp)
        }
    }

    /** Validates inputs and returns a [VoiceToggle], or null if validation fails. */
    private fun buildToggle(): VoiceToggle? {
        val name       = binding.etToggleName.text?.toString()?.trim() ?: ""
        val onCommand  = binding.etOnCommand.text?.toString()?.trim() ?: ""
        val offCommand = binding.etOffCommand.text?.toString()?.trim() ?: ""

        if (name.isEmpty()) {
            binding.tilToggleName.error = "Name is required"
            return null
        }
        binding.tilToggleName.error = null

        if (onCommand.isEmpty()) {
            binding.tilOnCommand.error = "ON command is required"
            return null
        }
        binding.tilOnCommand.error = null

        if (offCommand.isEmpty()) {
            binding.tilOffCommand.error = "OFF command is required"
            return null
        }
        binding.tilOffCommand.error = null

        return VoiceToggle(
            id         = existingToggle?.id ?: java.util.UUID.randomUUID().toString(),
            name       = name,
            icon       = selectedIcon,
            onCommand  = onCommand,
            offCommand = offCommand,
            isOn       = existingToggle?.isOn ?: false
        )
    }

    override fun onStart() {
        super.onStart()
        // Attach the Save action to the dialog's positive button after inflation
        val sheetDialog = dialog as? BottomSheetDialog ?: return

        // We add our own button row via the bottom sheet's container view
        view?.findViewById<View>(R.id.btnSaveToggle)?.setOnClickListener {
            buildToggle()?.let { toggle ->
                onSave(toggle)
                dismiss()
            }
        }
        view?.findViewById<View>(R.id.btnCancelToggle)?.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddToggleDialog"
    }
}
