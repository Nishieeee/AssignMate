package com.example.assignmate.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.model.Task
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Locale

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskOptionsClick: (Task, View) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task, onTaskClick, onTaskOptionsClick)
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskName: TextView = itemView.findViewById(R.id.task_name)
        private val taskDescription: TextView = itemView.findViewById(R.id.task_description)
        private val dueDate: TextView = itemView.findViewById(R.id.due_date)
        private val overdueIndicator: TextView = itemView.findViewById(R.id.overdue_indicator)
        private val status: TextView = itemView.findViewById(R.id.status)
        private val overflowMenu: ImageView = itemView.findViewById(R.id.task_overflow_menu)
        private val assignedMembersChipGroup: ChipGroup = itemView.findViewById(R.id.assigned_members_chip_group)
        private val labelsChipGroup: ChipGroup = itemView.findViewById(R.id.labels_chip_group)
        private val assigneesSection: LinearLayout = itemView.findViewById(R.id.assignees_section)
        private val labelsSection: LinearLayout = itemView.findViewById(R.id.labels_section)


        fun bind(task: Task, onTaskClick: (Task) -> Unit, onTaskOptionsClick: (Task, View) -> Unit) {
            taskName.text = task.name
            taskDescription.text = task.description

            if (task.dueDate != 0L) {
                dueDate.visibility = View.VISIBLE
                dueDate.text = "Due: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.dueDate)}"
                if (task.dueDate < System.currentTimeMillis() && task.status != "Complete") {
                    overdueIndicator.visibility = View.VISIBLE
                } else {
                    overdueIndicator.visibility = View.GONE
                }
            } else {
                dueDate.visibility = View.GONE
                overdueIndicator.visibility = View.GONE
            }

            status.text = task.status
            val context = itemView.context
            val statusInfo = when (task.status) {
                "Not Started" -> Pair(R.drawable.status_background_not_started, R.color.status_not_started)
                "In progress" -> Pair(R.drawable.status_background_in_progress, R.color.status_in_progress)
                "Complete" -> Pair(R.drawable.status_background_complete, R.color.status_completed)
                else -> Pair(R.drawable.status_background_not_started, R.color.status_not_started)
            }

            status.setBackgroundResource(statusInfo.first)
            status.setTextColor(ContextCompat.getColor(context, statusInfo.second))

            itemView.setOnClickListener { onTaskClick(task) }
            overflowMenu.setOnClickListener { onTaskOptionsClick(task, it) }

            if (task.assignedTo.isEmpty()) {
                assigneesSection.visibility = View.GONE
            } else {
                assigneesSection.visibility = View.VISIBLE
                // TODO: Populate assignees
            }

            if (task.labels.isEmpty()) {
                labelsSection.visibility = View.GONE
            } else {
                labelsSection.visibility = View.VISIBLE
                // TODO: Populate labels
            }

        }
    }
}