package com.example.assignmate.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.FirebaseHelper
import com.example.assignmate.R
import com.example.assignmate.model.Task
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskAdapter(
    initialTasks: List<Task>,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskOptionsClick: (Task, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val firebaseHelper = FirebaseHelper()
    private val items = mutableListOf<TaskListItem>()

    init {
        updateItems(initialTasks)
    }

    private fun updateItems(tasks: List<Task>) {
        items.clear()
        val grouped = tasks.groupBy { it.status }
        val statusOrder = listOf("Not Started", "In progress", "Complete")
        
        statusOrder.forEach { status ->
            val tasksInStatus = grouped[status]
            if (!tasksInStatus.isNullOrEmpty()) {
                items.add(TaskListItem.Header(status))
                items.addAll(tasksInStatus.map { TaskListItem.TaskItem(it) })
            }
        }
        
        val otherStatuses = grouped.keys.filter { !statusOrder.contains(it) }
        otherStatuses.forEach { status ->
             val tasksInStatus = grouped[status]
            if (!tasksInStatus.isNullOrEmpty()) {
                items.add(TaskListItem.Header(status))
                items.addAll(tasksInStatus.map { TaskListItem.TaskItem(it) })
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
         return when (viewType) {
            0 -> {
                 val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
                 HeaderViewHolder(view)
            }
            1 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
                TaskViewHolder(view, firebaseHelper)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
         when (val item = items[position]) {
            is TaskListItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is TaskListItem.TaskItem -> (holder as TaskViewHolder).bind(item.task, onTaskClick, onTaskOptionsClick)
        }
    }

    override fun getItemCount(): Int = items.size
    
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TaskListItem.Header -> 0
            is TaskListItem.TaskItem -> 1
        }
    }

    fun updateTasks(newTasks: List<Task>) {
        updateItems(newTasks)
        notifyDataSetChanged()
    }

    sealed class TaskListItem {
        data class Header(val title: String) : TaskListItem()
        data class TaskItem(val task: Task) : TaskListItem()
    }
    
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.header_title)
        fun bind(headerTitle: String) {
            title.text = headerTitle
        }
    }

    class TaskViewHolder(itemView: View, private val firebaseHelper: FirebaseHelper) : RecyclerView.ViewHolder(itemView) {
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
        private val combinedInfoScrollView: View = itemView.findViewById(R.id.combined_info_scroll_view)


        fun bind(task: Task, onTaskClick: (Task) -> Unit, onTaskOptionsClick: (Task, View) -> Unit) {
            taskName.text = task.name
            taskDescription.text = task.description

            if (task.status == "Complete") {
                (itemView as? MaterialCardView)?.setCardBackgroundColor(Color.parseColor("#FFF8F0"))
                taskDescription.visibility = View.GONE
                dueDate.visibility = View.GONE
                overdueIndicator.visibility = View.GONE
                status.visibility = View.GONE
                combinedInfoScrollView.visibility = View.GONE
                
                taskName.paintFlags = taskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                (itemView as? MaterialCardView)?.setCardBackgroundColor(Color.WHITE)
                taskDescription.visibility = View.VISIBLE
                combinedInfoScrollView.visibility = View.VISIBLE
                
                // Due date visibility logic
                if (task.dueDate != 0L) {
                    dueDate.visibility = View.VISIBLE
                    dueDate.text = "Due: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.dueDate)}"

                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val todayStart = calendar.timeInMillis

                    if (task.dueDate < todayStart) {
                        overdueIndicator.visibility = View.VISIBLE
                    } else {
                        overdueIndicator.visibility = View.GONE
                    }
                } else {
                    dueDate.visibility = View.GONE
                    overdueIndicator.visibility = View.GONE
                }
                
                status.visibility = View.VISIBLE
                status.text = task.status
                val context = itemView.context
                val statusInfo = when (task.status) {
                    "Not Started" -> Pair(R.drawable.status_background_not_started, R.color.status_not_started)
                    "In progress" -> Pair(R.drawable.status_background_in_progress, R.color.status_in_progress)
                    else -> Pair(R.drawable.status_background_not_started, R.color.status_not_started)
                }

                status.setBackgroundResource(statusInfo.first)
                status.setTextColor(ContextCompat.getColor(context, statusInfo.second))
                
                taskName.paintFlags = taskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            itemView.setOnClickListener { onTaskClick(task) }
            overflowMenu.setOnClickListener { onTaskOptionsClick(task, it) }

            if (task.status != "Complete") {
                // Handle Assignees
                assignedMembersChipGroup.removeAllViews()
                if (task.assignedTo.isEmpty()) {
                    assigneesSection.visibility = View.GONE
                } else {
                    assigneesSection.visibility = View.VISIBLE
                    firebaseHelper.getUsers(task.assignedTo, onSuccess = { users ->
                        assignedMembersChipGroup.removeAllViews() // Clear again just in case
                        users.forEach { user ->
                            val chip = Chip(itemView.context)
                            chip.text = user.username
                            chip.isClickable = false
                            chip.isCheckable = false
                            assignedMembersChipGroup.addView(chip)
                        }
                    }, onFailure = {
                        // Handle failure or keep empty
                    })
                }

                // Handle Labels
                labelsChipGroup.removeAllViews()
                if (task.labels.isEmpty()) {
                    labelsSection.visibility = View.GONE
                } else {
                    labelsSection.visibility = View.VISIBLE
                    firebaseHelper.getLabelsForGroup(task.groupId, onSuccess = { allLabels ->
                        labelsChipGroup.removeAllViews()
                        val taskLabels = allLabels.filter { task.labels.contains(it.id) }
                        taskLabels.forEach { label ->
                            val chip = Chip(itemView.context)
                            chip.text = label.name
                            chip.isClickable = false
                            chip.isCheckable = false
                            try {
                                val color = Color.parseColor(label.color)
                                chip.chipBackgroundColor = ColorStateList.valueOf(color)
                                val brightness = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
                                if (brightness > 128) {
                                    chip.setTextColor(Color.BLACK)
                                } else {
                                    chip.setTextColor(Color.WHITE)
                                }
                            } catch (e: Exception) {
                                // Default color
                            }
                            labelsChipGroup.addView(chip)
                        }
                    }, onFailure = {
                        // Handle failure
                    })
                }
            }
        }
    }
}
