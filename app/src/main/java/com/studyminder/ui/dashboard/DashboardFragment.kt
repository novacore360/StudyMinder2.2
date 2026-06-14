package com.studyminder.ui.dashboard

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
import com.studyminder.databinding.FragmentDashboardBinding
import com.studyminder.service.AlarmReceiver
import com.studyminder.ui.schedule.AddEditScheduleDialog
import java.util.Calendar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: StudyRepository
    private lateinit var listAdapter: ScheduleAdapter

    private val filterOptions = listOf("Upcoming (Next 48h)", "All Schedules", "Missed", "Done")
    private var selectedFilterIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = StudyRepository.getInstance(requireContext())

        val name = repo.getUserName()
        val greeting = getGreeting()
        binding.tvGreeting.text = "$greeting, $name! 👋"

        setupRecyclerView()
        setupFilterSpinner()
        loadData()
    }

    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    private fun setupRecyclerView() {
        listAdapter = ScheduleAdapter(
            onEdit = { openEditDialog(it) },
            onDelete = { deleteSchedule(it) },
            onMarkDone = { markDone(it) }
        )

        binding.rvList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFilterSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDashboardFilter.adapter = adapter
        binding.spinnerDashboardFilter.setSelection(selectedFilterIndex)
        binding.spinnerDashboardFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
        val now = System.currentTimeMillis()
        val next48h = now + 48 * 60 * 60 * 1000L

        val upcoming = allSchedules.filter {
            it.status == ScheduleStatus.UPCOMING && it.dateTimeMillis in now..next48h
        }
        val all = allSchedules.sortedWith(compareBy({ it.status.ordinal }, { it.dateTimeMillis }))
        val missed = allSchedules.filter { it.status == ScheduleStatus.MISSED }
        val done = allSchedules.filter { it.status == ScheduleStatus.DONE }

        val (headerText, listData, emptyMessage) = when (selectedFilterIndex) {
            1 -> Triple("📋 All Schedules", all, getString(com.studyminder.R.string.no_schedules))
            2 -> Triple("⚠️ Missed", missed, "No missed schedules")
            3 -> Triple("✅ Done", done, "No completed schedules yet")
            else -> Triple("⚡ Upcoming (Next 48h)", upcoming, getString(com.studyminder.R.string.no_upcoming))
        }

        binding.tvListSectionHeader.text = headerText
        listAdapter.submitList(listData)

        binding.tvUpcomingCount.text = "${upcoming.size} in next 48h"
        binding.tvTotalCount.text = "${allSchedules.size} total"

        val doneCount = allSchedules.count { it.status == ScheduleStatus.DONE }
        val missedCount = allSchedules.count { it.status == ScheduleStatus.MISSED }
        binding.tvStats.text = "✅ $doneCount done  ⚠️ $missedCount missed"

        binding.emptyList.text = emptyMessage
        binding.emptyList.visibility = if (listData.isEmpty()) View.VISIBLE else View.GONE
        binding.rvList.visibility = if (listData.isEmpty()) View.GONE else View.VISIBLE
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
        (activity as? com.studyminder.ui.MainActivity)?.checkAndShowRankNotification()
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
        (activity as? com.studyminder.ui.MainActivity)?.checkAndShowRankNotification()
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
