package com.example.schach1337

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.schach1337.databinding.ActivityPlayChessMenuBinding

class PlayChessMenu : DialogFragment() {

    private var _binding: ActivityPlayChessMenuBinding? = null
    private val binding get() = _binding!!

    interface OnSettingsConfirmedListener {
        fun onSettingsConfirmed(playAsWhite: Boolean,
                                isLevel : Boolean, eloOrLevelLimitValue: Int,
                                isTime: Boolean, depthOrTimeLimitValue: Int,
                                startPos: String)
    }

    var listener: OnSettingsConfirmedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = ActivityPlayChessMenuBinding.inflate(layoutInflater)

        val dialog = Dialog(requireContext())
        dialog.setContentView(binding.root)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        fun updateEloSlider(isLevel: Boolean) {
            if (isLevel) {
                binding.sliderEloSkill.valueFrom = 0f
                binding.sliderEloSkill.valueTo = 20f
                binding.sliderEloSkill.stepSize = 1f
                binding.sliderEloSkill.value = 20f
                binding.sliderEloSkillValue.text = "Level: 20"
            } else {
                binding.sliderEloSkill.valueFrom = 1320f
                binding.sliderEloSkill.valueTo = 3120f
                binding.sliderEloSkill.stepSize = 180f
                binding.sliderEloSkill.value = 3120f
                binding.sliderEloSkillValue.text = "Elo: 3120"
            }
        }

        binding.switchEloSkill.setOnCheckedChangeListener { _, isChecked ->
            updateEloSlider(isChecked)
        }

        binding.sliderEloSkill.addOnChangeListener { _, value, _ ->
            val isLevel = binding.switchEloSkill.isChecked
            binding.sliderEloSkillValue.text = if (isLevel) "Level: ${value.toInt()}" else "Elo: ${value.toInt()}"
        }

        fun updateDepthSlider(isTime: Boolean) {
            if (isTime) {
                binding.sliderDepthTime.valueFrom = 1000f
                binding.sliderDepthTime.valueTo = 30000f
                binding.sliderDepthTime.stepSize = 1000f
                binding.sliderDepthTime.value = 1000f
                binding.textViewDepthTimeValue.text = "Time: 1000"
            } else {
                binding.sliderDepthTime.valueFrom = 1f
                binding.sliderDepthTime.valueTo = 30f
                binding.sliderDepthTime.stepSize = 1f
                binding.sliderDepthTime.value = 12f
                binding.textViewDepthTimeValue.text = "Depth: 12"
            }
        }

        binding.switchDepthTime.setOnCheckedChangeListener { _, isChecked ->
            updateDepthSlider(isChecked)
        }

        binding.sliderDepthTime.addOnChangeListener { _, value, _ ->
            val isTime = binding.switchDepthTime.isChecked
            binding.textViewDepthTimeValue.text = if (isTime) "Time: ${value.toInt()}" else "Depth: ${value.toInt()}"
        }

        binding.btnStartGame.setOnClickListener {
            val playAsWhite = binding.radioButtonWhite.isChecked
            val isLevel = binding.switchEloSkill.isChecked
            val eloOrLevel = binding.sliderEloSkill.value.toInt()
            val isTime = binding.switchDepthTime.isChecked
            val depthOrTime = binding.sliderDepthTime.value.toInt()
            val startPos = binding.editTextStartPos.text.toString()

            listener?.onSettingsConfirmed(playAsWhite, isLevel, eloOrLevel, isTime, depthOrTime, startPos)
            dismiss()
        }

        updateEloSlider(binding.switchEloSkill.isChecked)
        updateDepthSlider(binding.switchDepthTime.isChecked)

        return dialog
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
