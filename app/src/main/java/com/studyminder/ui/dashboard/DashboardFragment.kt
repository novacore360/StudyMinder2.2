package com.studyminder.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.studyminder.adapter.ScheduleAdapter
import com.studyminder.data.model.Schedule
import com.studyminder.data.model.ScheduleStatus
import com.studyminder.data.repository.StudyRepository
import com.studyminder.databinding.FragmentDashboardBinding
import com.studyminder.service.AlarmReceiver
import com.studyminder.ui.schedule.AddEditScheduleDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: StudyRepository
    private lateinit var upcomingAdapter: ScheduleAdapter
    private lateinit var allAdapter: ScheduleAdapter

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

        setupRecyclerViews()
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

    private fun setupRecyclerViews() {
        upcomingAdapter = ScheduleAdapter(
            onEdit = { openEditDialog(it) },
            onDelete = { deleteSchedule(it) },
            onMarkDone = { markDone(it) }
        )
        allAdapter = ScheduleAdapter(
            onEdit = { openEditDialog(it) },
            onDelete = { deleteSchedule(it) },
            onMarkDone = { markDone(it) }
        )

        binding.rvUpcoming.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = upcomingAdapter
            isNestedScrollingEnabled = false
        }
        binding.rvAll.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = allAdapter
            isNestedScrollingEnabled = false
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

        upcomingAdapter.submitList(upcoming)
        allAdapter.submitList(all)

        binding.tvUpcomingCount.text = "${upcoming.size} in next 48h"
        binding.tvTotalCount.text = "${allSchedules.size} total"

        val doneCount = allSchedules.count { it.status == ScheduleStatus.DONE }
        val missedCount = allSchedules.count { it.status == ScheduleStatus.MISSED }
        binding.tvStats.text = "✅ $doneCount done  ⚠️ $missedCount missed"

        binding.emptyUpcoming.visibility = if (upcoming.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyAll.visibility = if (allSchedules.isEmpty()) View.VISIBLE else View.GONE
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
