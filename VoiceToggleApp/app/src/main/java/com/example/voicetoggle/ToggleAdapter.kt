package com.example.voicetoggle

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.voicetoggle.databinding.ItemToggleBinding

/**
 * RecyclerView adapter for the list of [VoiceToggle] items.
 * Uses ListAdapter + DiffUtil for efficient, animated updates.
 */
class ToggleAdapter(
    private val onToggleChanged: (VoiceToggle, Boolean) -> Unit,
    private val onEditClicked: (VoiceToggle) -> Unit,
    private val onDeleteClicked: (VoiceToggle) -> Unit
) : ListAdapter<VoiceToggle, ToggleAdapter.ToggleViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<VoiceToggle>() {
            override fun areItemsTheSame(old: VoiceToggle, new: VoiceToggle) = old.id == new.id
            override fun areContentsTheSame(old: VoiceToggle, new: VoiceToggle) = old == new
        }
    }

    inner class ToggleViewHolder(
        private val binding: ItemToggleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(toggle: VoiceToggle) {
            binding.tvToggleName.text = toggle.name
            binding.tvIcon.text = toggle.icon
            binding.tvVoiceKeywords.text = buildKeywordHint(toggle)
            binding.tvOnCommand.text  = "ON: \"${toggle.onCommand}\""
            binding.tvOffCommand.text = "OFF: \"${toggle.offCommand}\""

            // Prevent the listener from firing during bind
            binding.switchToggle.setOnCheckedChangeListener(null)
            binding.switchToggle.isChecked = toggle.isOn

            binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
                onToggleChanged(toggle, isChecked)
                animateToggleIcon(isChecked)
            }

            binding.btnEditToggle.setOnClickListener   { onEditClicked(toggle) }
            binding.btnDeleteToggle.setOnClickListener { onDeleteClicked(toggle) }
        }

        /** Brief scale-bounce on the icon when state changes. */
        private fun animateToggleIcon(isOn: Boolean) {
            val icon = binding.tvIcon
            val target = if (isOn) 1.3f else 0.85f
            ValueAnimator.ofFloat(1f, target, 1f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { anim ->
                    val v = anim.animatedValue as Float
                    icon.scaleX = v
                    icon.scaleY = v
                }
                start()
            }
        }

        private fun buildKeywordHint(toggle: VoiceToggle): String =
            "Say: \"${toggle.onCommand}\" / \"${toggle.offCommand}\""
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToggleViewHolder {
        val binding = ItemToggleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ToggleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ToggleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Programmatically flip a toggle by its index and trigger the change callback.
     * Called from MainActivity when a voice command matches.
     */
    fun setToggleState(toggleId: String, isOn: Boolean) {
        val index = currentList.indexOfFirst { it.id == toggleId }
        if (index >= 0) notifyItemChanged(index)
    }
}
