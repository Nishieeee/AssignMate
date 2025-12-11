package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemUpcomingTaskBinding
import com.example.assignmate.model.Group
import com.example.assignmate.model.Task
import java.util.concurrent.TimeUnit

class UpcomingTasksAdapter(
    private var tasks: List<Task>,
    private val onTaskClicked: (Task) -> Unit,
    private val getGroup: (String, (Group?) -> Unit) -> Unit
) : RecyclerView.Adapter<UpcomingTasksAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemUpcomingTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
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
            binding.dueDate.text = when {
                days > 1 -> "$days days left"
                days == 1L -> "1 day left"
                else -> "Due today"
            }

            binding.root.setOnClickListener {
                onTaskClicked(task)
            }
        }
    }
}
