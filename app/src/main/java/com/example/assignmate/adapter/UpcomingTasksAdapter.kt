package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemUpcomingGroupTasksBinding
import com.example.assignmate.databinding.ItemUpcomingTaskBinding
import com.example.assignmate.model.Group
import com.example.assignmate.model.Task
import java.util.Calendar
import java.util.concurrent.TimeUnit

sealed class UpcomingItem {
    data class TaskItem(val task: Task) : UpcomingItem()
    data class GroupSummaryItem(val group: Group, val taskCount: Int, val earliestDueDate: Long) : UpcomingItem()
}

class UpcomingTasksAdapter(
    private var items: List<UpcomingItem>,
    private val onTaskClicked: (Task) -> Unit,
    private val onGroupClicked: (Group) -> Unit,
    private val getGroup: (String, (Group?) -> Unit) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TASK = 0
        private const val TYPE_GROUP_SUMMARY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is UpcomingItem.TaskItem -> TYPE_TASK
            is UpcomingItem.GroupSummaryItem -> TYPE_GROUP_SUMMARY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TASK -> {
                val binding = ItemUpcomingTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TaskViewHolder(binding)
            }
            TYPE_GROUP_SUMMARY -> {
                val binding = ItemUpcomingGroupTasksBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                GroupSummaryViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is UpcomingItem.TaskItem -> (holder as TaskViewHolder).bind(item.task)
            is UpcomingItem.GroupSummaryItem -> (holder as GroupSummaryViewHolder).bind(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<UpcomingItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun getDaysDifference(dueDate: Long): Long {
        val todayCalendar = Calendar.getInstance()
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        todayCalendar.set(Calendar.MINUTE, 0)
        todayCalendar.set(Calendar.SECOND, 0)
        todayCalendar.set(Calendar.MILLISECOND, 0)

        val dueCalendar = Calendar.getInstance()
        dueCalendar.timeInMillis = dueDate
        dueCalendar.set(Calendar.HOUR_OF_DAY, 0)
        dueCalendar.set(Calendar.MINUTE, 0)
        dueCalendar.set(Calendar.SECOND, 0)
        dueCalendar.set(Calendar.MILLISECOND, 0)

        val diff = dueCalendar.timeInMillis - todayCalendar.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    inner class TaskViewHolder(private val binding: ItemUpcomingTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.taskName.text = task.name

            getGroup(task.groupId) { group ->
                binding.groupName.text = group?.name ?: ""
            }

            val daysDiff = getDaysDifference(task.dueDate)

            binding.dueDate.text = when {
                daysDiff == 0L -> "Due today"
                daysDiff == 1L -> "1 day left"
                daysDiff > 1L -> "$daysDiff days left"
                else -> "Due today"
            }

            binding.root.setOnClickListener {
                onTaskClicked(task)
            }
        }
    }

    inner class GroupSummaryViewHolder(private val binding: ItemUpcomingGroupTasksBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UpcomingItem.GroupSummaryItem) {
            binding.groupName.text = item.group.name
            binding.tasksDueCount.text = "${item.taskCount} tasks due"
            
            if (item.earliestDueDate > 0) {
                binding.earliestDueDate.visibility = View.VISIBLE
                
                val daysDiff = getDaysDifference(item.earliestDueDate)

                val dueDateText = when {
                    daysDiff == 0L -> "Due today"
                    daysDiff == 1L -> "1 day left"
                    daysDiff > 1L -> "$daysDiff days left"
                    else -> "Due today"
                }
                binding.earliestDueDate.text = "Earliest due: $dueDateText"
            } else {
                binding.earliestDueDate.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onGroupClicked(item.group)
            }
        }
    }
}
