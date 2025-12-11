package com.example.assignmate.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String = "",
    val username: String = "",
    val email: String = ""
)
