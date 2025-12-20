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

    inner class TaskViewHolder(private val binding: ItemUpcomingTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.taskName.text = task.name

            getGroup(task.groupId) { group ->
                binding.groupName.text = group?.name ?: ""
            }

            val diff = task.dueDate - System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_YEAR)
            calendar.timeInMillis = task.dueDate
            val dueDateDay = calendar.get(Calendar.DAY_OF_YEAR)

            binding.dueDate.text = when {
                today == dueDateDay -> "Due today"
                days > 1 -> "$days days left"
                days == 1L -> "1 day left"
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
                
                val diff = item.earliestDueDate - System.currentTimeMillis()
                val days = TimeUnit.MILLISECONDS.toDays(diff)

                val calendar = Calendar.getInstance()
                val today = calendar.get(Calendar.DAY_OF_YEAR)
                calendar.timeInMillis = item.earliestDueDate
                val dueDateDay = calendar.get(Calendar.DAY_OF_YEAR)

                val dueDateText = when {
                    today == dueDateDay -> "Due today"
                    days > 1 -> "$days days left"
                    days == 1L -> "1 day left"
                    else -> "Due today"
                }
                binding.earliestDueDate.text = "Earliest due: $dueDateText"
            } else {
                binding.earliestDueDate.visibility = View.GONE
            }
        }
    }
}
