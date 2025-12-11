package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemUpcomingTaskBinding
import com.example.assignmate.model.Task
import java.util.concurrent.TimeUnit

class UpcomingTasksAdapter(tasks: List<Task>, private val onTaskClicked: (Task) -> Unit) : RecyclerView.Adapter<UpcomingTasksAdapter.UpcomingTaskViewHolder>() {

    private val filteredTasks = tasks.filter { it.dueDate != 0L }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpcomingTaskViewHolder {
        val binding = ItemUpcomingTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UpcomingTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpcomingTaskViewHolder, position: Int) {
        val task = filteredTasks[position]
        holder.bind(task)
    }

    override fun getItemCount() = filteredTasks.size

    inner class UpcomingTaskViewHolder(private val binding: ItemUpcomingTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.taskName.text = task.name

            val diff = task.dueDate - System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            binding.dueDate.text = when {
                days < -1 -> "${-days} days ago"
                days == -1L -> "1 day ago"
                days == 0L -> "Due today"
                days == 1L -> "1 day left"
                else -> "$days days left"
            }

            binding.root.setOnClickListener { onTaskClicked(task) }
        }
    }
}
