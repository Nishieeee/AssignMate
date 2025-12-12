package com.example.assignmate.model

sealed class TaskListItem {
    data class Header(val title: String, val showReschedule: Boolean = false) : TaskListItem()
    data class TaskItem(val task: Task) : TaskListItem()
}
