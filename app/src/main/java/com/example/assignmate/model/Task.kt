package com.example.assignmate.model

import com.google.firebase.firestore.DocumentId

data class Task(
    @DocumentId val uid: String = "",
    val name: String = "",
    val description: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val dueDate: Long = 0,
    val status: String = "",
    val assignedTo: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val subtasks: List<Subtask> = emptyList()
)
