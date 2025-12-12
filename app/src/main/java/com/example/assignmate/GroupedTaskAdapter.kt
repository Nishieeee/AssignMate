package com.example.assignmate

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.model.Label
import com.example.assignmate.model.Task
import com.example.assignmate.model.User
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

private const val VIEW_TYPE_GROUP = 0
private const val VIEW_TYPE_TASK = 1

class GroupedTaskAdapter(
    private val tasksByGroup: Map<String, List<Task>>,
    private val currentUserId: String,
    private val getUsers: (List<String>, (List<User>) -> Unit) -> Unit,
    private val getLabels: (String, (List<Label>) -> Unit) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()

    init {
        tasksByGroup.forEach { (groupName, tasks) ->
            items.add(groupName)
            items.addAll(tasks)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) {
            VIEW_TYPE_GROUP
        } else {
            VIEW_TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_GROUP) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_header, parent, false)
            GroupViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
            TaskViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GroupViewHolder) {
            holder.bind(items[position] as String)
        } else if (holder is TaskViewHolder) {
            holder.bind(items[position] as Task)
        }
    }

    override fun getItemCount(): Int = items.size

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupNameTextView: TextView = itemView.findViewById(R.id.group_name_header)
        fun bind(groupName: String) {
            groupNameTextView.text = groupName
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskNameTextView: TextView = itemView.findViewById(R.id.task_name)
        private val taskStatusTextView: TextView = itemView.findViewById(R.id.status)
        private val assignedMembersChipGroup: ChipGroup = itemView.findViewById(R.id.assigned_members_chip_group)
        private val labelsChipGroup: ChipGroup = itemView.findViewById(R.id.labels_chip_group)
        private val assigneesSection: View = itemView.findViewById(R.id.assignees_section)
        private val labelsSection: View = itemView.findViewById(R.id.labels_section)

        fun bind(task: Task) {
            taskNameTextView.text = task.name
            taskStatusTextView.text = "Status: ${task.status}"
            taskStatusTextView.visibility = View.VISIBLE

            if (task.assignedTo.isNotEmpty()) {
                assigneesSection.visibility = View.VISIBLE
                getUsers(task.assignedTo) { users ->
                    assignedMembersChipGroup.removeAllViews()
                    for (user in users) {
                        val chip = Chip(itemView.context)
                        chip.text = user.username
                        assignedMembersChipGroup.addView(chip)
                    }
                }
            } else {
                assigneesSection.visibility = View.GONE
            }

            if (task.labels.isNotEmpty()) {
                labelsSection.visibility = View.VISIBLE
                getLabels(task.groupId) { labels ->
                    labelsChipGroup.removeAllViews()
                    val selectedLabels = labels.filter { task.labels.contains(it.id) }
                    for (label in selectedLabels) {
                        val chip = Chip(itemView.context)
                        chip.text = label.name
                        chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(label.color))
                        labelsChipGroup.addView(chip)
                    }
                }
            } else {
                labelsSection.visibility = View.GONE
            }

            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, TaskDetailActivity::class.java).apply {
                    putExtra("TASK_ID", task.uid)
                    putExtra("USER_ID", currentUserId)
                }
                context.startActivity(intent)
            }
        }
    }
}
