package com.studyminder.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.studyminder.adapter.ScheduleAdapter
import com.studyminder.data.model.Schedule
import com.studyminder.data.model.ScheduleStatus
import com.studyminder.data.repository.StudyRepository
import com.studyminder.databinding.FragmentScheduleBinding
import com.studyminder.service.AlarmReceiver
import com.studyminder.ui.MainActivity

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: StudyRepository
    private lateinit var adapter: ScheduleAdapter

    private val filterOptions = listOf("All Schedules", "Upcoming", "Missed", "Done")
    private var selectedFilterIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = StudyRepository.getInstance(requireContext())

        adapter = ScheduleAdapter(
            onEdit = { openEditDialog(it) },
            onDelete = { deleteSchedule(it) },
            onMarkDone = { markDone(it) }
        )

        binding.rvSchedules.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ScheduleFragment.adapter
        }

        binding.fabAdd.setOnClickListener { openAddDialog() }
        setupFilterSpinner()
        loadData()
    }

    private fun setupFilterSpinner() {
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerScheduleFilter.adapter = arrayAdapter
        binding.spinnerScheduleFilter.setSelection(selectedFilterIndex)
        binding.spinnerScheduleFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (selectedFilterIndex != position) {
                    selectedFilterIndex = position
                    loadData()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadData() {
        repo.checkAndMarkMissed()
        val allSchedules = repo.getSchedules().sortedBy { it.dateTimeMillis }

        val filtered = when (selectedFilterIndex) {
            1 -> allSchedules.filter { it.status == ScheduleStatus.UPCOMING }
            2 -> allSchedules.filter { it.status == ScheduleStatus.MISSED }
            3 -> allSchedules.filter { it.status == ScheduleStatus.DONE }
            else -> allSchedules
        }

        adapter.submitList(filtered)
        binding.emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openAddDialog() {
        AddEditScheduleDialog.newInstance(null).apply {
            onSaved = { loadData() }
        }.show(parentFragmentManager, "AddSchedule")
    }

    private fun openEditDialog(schedule: Schedule) {
        AddEditScheduleDialog.newInstance(schedule).apply {
            onSaved = { loadData() }
        }.show(parentFragmentManager, "EditSchedule")
    }

    private fun deleteSchedule(schedule: Schedule) {
        AlarmReceiver.cancelAlarm(requireContext(), schedule.id)
        repo.deleteSchedule(schedule.id)
        loadData()
    }

    private fun markDone(schedule: Schedule) {
        AlarmReceiver.cancelAlarm(requireContext(), schedule.id)
        repo.markScheduleDone(schedule.id)
        loadData()
        // Trigger rank unlock check
        (activity as? MainActivity)?.checkAndShowRankNotification()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
