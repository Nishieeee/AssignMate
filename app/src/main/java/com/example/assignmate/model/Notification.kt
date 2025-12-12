package com.example.assignmate.model

import com.google.firebase.firestore.DocumentId

// Final check: Adding taskId parameter.
data class Notification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val message: String = "",
    val taskId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
