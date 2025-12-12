package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.model.Task

class GroupedTaskAdapter(
    private val onTaskClicked: (Task) -> Unit,
    private val onStatusChanged: (Task, String) -> Unit
) : ListAdapter<GroupedTaskAdapter.TaskListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    sealed class TaskListItem {
        data class Header(val title: String) : TaskListItem()
        data class TaskItem(val task: Task) : TaskListItem()
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TASK = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TaskListItem.Header -> VIEW_TYPE_HEADER
            is TaskListItem.TaskItem -> VIEW_TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_TASK -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task_grouped, parent, false)
                TaskViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TaskListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TaskListItem.TaskItem -> (holder as TaskViewHolder).bind(item.task, onTaskClicked, onStatusChanged)
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.header_title)

        fun bind(header: TaskListItem.Header) {
            title.text = header.title
        }
    }

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val taskName: TextView = view.findViewById(R.id.task_name)
        private val groupName: TextView = view.findViewById(R.id.group_name)
        private val statusText: TextView = view.findViewById(R.id.status_text)
        private val overflowMenu: ImageView = view.findViewById(R.id.task_overflow_menu)

        fun bind(task: Task, onTaskClicked: (Task) -> Unit, onStatusChanged: (Task, String) -> Unit) {
            taskName.text = task.name
            groupName.text = task.groupName
            statusText.text = task.status
            itemView.setOnClickListener { onTaskClicked(task) }

            val context = itemView.context
            val (backgroundColor, textColor) = when (task.status) {
                "Not Started" -> R.drawable.status_background_not_started to R.color.status_not_started
                "In Progress" -> R.drawable.status_background_in_progress to R.color.status_in_progress
                "Completed" -> R.drawable.status_background_complete to R.color.status_completed
                else -> R.drawable.status_background_not_started to R.color.status_not_started
            }
            statusText.setBackgroundResource(backgroundColor)
            statusText.setTextColor(ContextCompat.getColor(context, textColor))

            overflowMenu.setOnClickListener { showPopupMenu(it, task, onStatusChanged) }
        }

        private fun showPopupMenu(view: View, task: Task, onStatusChanged: (Task, String) -> Unit) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.task_status_menu, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                val newStatus = when (menuItem.itemId) {
                    R.id.status_not_started -> "Not Started"
                    R.id.status_in_progress -> "In Progress"
                    R.id.status_completed -> "Completed"
                    else -> ""
                }
                if (newStatus.isNotEmpty()) {
                    onStatusChanged(task, newStatus)
                }
                true
            }
            popup.show()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TaskListItem>() {
        override fun areItemsTheSame(oldItem: TaskListItem, newItem: TaskListItem): Boolean {
            return if (oldItem is TaskListItem.TaskItem && newItem is TaskListItem.TaskItem) {
                oldItem.task.uid == newItem.task.uid
            } else if (oldItem is TaskListItem.Header && newItem is TaskListItem.Header) {
                oldItem.title == newItem.title
            } else {
                false
            }
        }

        override fun areContentsTheSame(oldItem: TaskListItem, newItem: TaskListItem): Boolean {
            return if (oldItem is TaskListItem.TaskItem && newItem is TaskListItem.TaskItem) {
                oldItem.task == newItem.task
            } else if (oldItem is TaskListItem.Header && newItem is TaskListItem.Header) {
                oldItem == newItem
            } else {
                false
            }
        }
    }
}