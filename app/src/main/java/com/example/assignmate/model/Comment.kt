package com.example.assignmate.model

import com.google.firebase.firestore.DocumentId

data class Comment(
    @DocumentId val id: String = "",
    val taskId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val commentText: String = "",
    val timestamp: Long = 0,
    val attachments: List<Attachment> = emptyList() 
)
