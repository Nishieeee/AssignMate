package com.example.assignmate.model

import com.google.firebase.firestore.DocumentId

data class Notification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false
)
