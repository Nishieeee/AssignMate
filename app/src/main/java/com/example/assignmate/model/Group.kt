package com.example.assignmate.model

import com.google.firebase.firestore.DocumentId

data class Group(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val leaderId: String = "",
    val code: String = "",
    val members: Map<String, String> = emptyMap(),
    val lastUpdated: Long = 0,
    val favouriteBy: List<String> = emptyList(),
    val progress: Int = 0,
    val assignedTasksCount: Int = 0,
    val pendingTaskCount: Int = 0
)
