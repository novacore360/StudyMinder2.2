package com.studyminder.ui.subjects

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.studyminder.data.model.Subject
import com.studyminder.data.repository.StudyRepository
import com.studyminder.databinding.DialogAddSubjectBinding
import com.studyminder.util.SubjectColors

class AddEditSubjectDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddSubjectBinding? = null
    private val binding get() = _binding!!
    var onSaved: (() -> Unit)? = null
    private var editSubject: Subject? = null

    private val seekMax = 1000
    private var selectedColorHex: String = SubjectColors.PRESET_COLORS.first()
    private val swatchViews = mutableListOf<View>()

    companion object {
        private const val ARG_SUBJECT = "subject"
        fun newInstance(subject: Subject?) = AddEditSubjectDialog().apply {
            arguments = Bundle().apply { putParcelable(ARG_SUBJECT, subject) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editSubject = arguments?.getParcelable(ARG_SUBJECT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddSubjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repo = StudyRepository.getInstance(requireContext())

        binding.tvTitle.text = if (editSubject != null) "Edit Subject" else "Add Subject"

        editSubject?.let {
            binding.etSubjectName.setText(it.name)
            binding.etSubjectId.setText(it.subjectId)
            binding.etProfessorName.setText(it.professorName)
            binding.etDefaultRoom.setText(it.defaultRoom)
        }

        // Initial color: existing subject's color, or first preset for new subjects
        selectedColorHex = SubjectColors.sanitizeColor(
            editSubject?.colorHex?.takeIf { it.isNotBlank() } ?: SubjectColors.PRESET_COLORS.first()
        )

        setupColorPicker()
        updateColorPreview()

        binding.btnSave.setOnClickListener {
            val name = binding.etSubjectName.text.toString().trim()
            val subjectId = binding.etSubjectId.text.toString().trim()
            val professor = binding.etProfessorName.text.toString().trim()
            val room = binding.etDefaultRoom.text.toString().trim()

            if (name.isEmpty()) { binding.etSubjectName.error = "Required"; return@setOnClickListener }

            val subject = (editSubject ?: Subject()).copy(
                name = name,
                subjectId = subjectId,
                professorName = professor,
                defaultRoom = room,
                colorHex = selectedColorHex
            )
            repo.saveSubject(subject)
            onSaved?.invoke()
            dismiss()
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun setupColorPicker() {
        // Gradient background for the seek bar (excludes red & green hues)
        binding.seekColorGradient.progressDrawable = SubjectColors.createGradientDrawable()
        binding.seekColorGradient.max = seekMax
        binding.seekColorGradient.progress = SubjectColors.colorToProgress(selectedColorHex, seekMax)

        binding.seekColorGradient.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                selectedColorHex = SubjectColors.progressToColor(progress, seekMax)
                updateColorPreview()
                highlightMatchingSwatch()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        buildPresetSwatches()
        highlightMatchingSwatch()
    }

    private fun buildPresetSwatches() {
        binding.layoutColorSwatches.removeAllViews()
        swatchViews.clear()

        val sizePx = (32 * resources.displayMetrics.density).toInt()
        val marginPx = (8 * resources.displayMetrics.density).toInt()

        SubjectColors.PRESET_COLORS.forEach { hex ->
            val swatch = View(requireContext())
            val params = android.widget.LinearLayout.LayoutParams(sizePx, sizePx).apply {
                marginEnd = marginPx
            }
            swatch.layoutParams = params

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(hex))
            }
            swatch.background = drawable

            swatch.setOnClickListener {
                selectedColorHex = hex
                binding.seekColorGradient.progress = SubjectColors.colorToProgress(hex, seekMax)
                updateColorPreview()
                highlightMatchingSwatch()
            }

            binding.layoutColorSwatches.addView(swatch)
            swatchViews.add(swatch)
        }
    }

    private fun highlightMatchingSwatch() {
        val borderPx = (2 * resources.displayMetrics.density)
        val selectedBorderPx = (3 * resources.displayMetrics.density)

        swatchViews.forEachIndexed { index, view ->
            val hex = SubjectColors.PRESET_COLORS[index]
            val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
            val drawable = view.background as? GradientDrawable
            drawable?.setStroke(
                if (isSelected) selectedBorderPx.toInt() else borderPx.toInt(),
                if (isSelected) Color.parseColor("#1C2B1E") else Color.parseColor("#E0D8CC")
            )
            view.scaleX = if (isSelected) 1.15f else 1f
            view.scaleY = if (isSelected) 1.15f else 1f
        }
    }

    private fun updateColorPreview() {
        val drawable = binding.viewColorPreview.background as? GradientDrawable
        drawable?.setColor(Color.parseColor(selectedColorHex))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
