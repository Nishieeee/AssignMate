package com.example.assignmate.model

import com.google.firebase.firestore.DocumentId

data class Subtask(
    @DocumentId val id: String = "",
    val name: String = "",
    val isCompleted: Boolean = false,
    val taskId: String = ""
)
