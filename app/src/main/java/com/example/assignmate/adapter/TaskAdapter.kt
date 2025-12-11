package com.example.assignmate.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.databinding.ItemTaskBinding
import com.example.assignmate.model.Task
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private var tasks: List<Task>,
    private val currentUserId: String,
    private val onItemClicked: (Task) -> Unit, // Changed to a lambda
    private val onDeleteClicked: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>){
        tasks = newTasks
        notifyDataSetChanged()
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            val context = itemView.context
            binding.taskName.text = task.name
            binding.taskDescription.text = task.description
            if (task.dueDate != 0L) {
                binding.dueDate.visibility = View.VISIBLE
                binding.dueDate.text = "Due: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(task.dueDate))}"
            } else {
                binding.dueDate.visibility = View.GONE
            }

            binding.status.text = task.status

            val (statusColor, statusBackground) = when (task.status) {
                "Not Started" -> R.color.status_not_started to R.drawable.status_background_not_started
                "In progress" -> R.color.status_in_progress to R.drawable.status_background_in_progress
                "Complete" -> R.color.status_complete to R.drawable.status_background_complete
                else -> android.R.color.black to R.drawable.status_background_in_progress // Default
            }
            binding.status.setTextColor(ContextCompat.getColor(context, statusColor))
            binding.status.setBackgroundResource(statusBackground)

            if (task.dueDate != 0L && task.dueDate < System.currentTimeMillis() && task.status != "Complete") {
                binding.overdueIndicator.visibility = View.VISIBLE
            } else {
                binding.overdueIndicator.visibility = View.GONE
            }

            binding.assigneesSection.visibility = View.GONE
            binding.labelsSection.visibility = View.GONE

            binding.root.setOnClickListener {
                onItemClicked(task)
            }

            binding.taskOverflowMenu.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.group_task_menu, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_delete_task -> {
                            onDeleteClicked(task)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }
}
