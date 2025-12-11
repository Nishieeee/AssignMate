package com.example.assignmate.model

import com.google.firebase.firestore.DocumentId

data class Label(
    @DocumentId val id: String = "",
    val name: String = "",
    val color: String = "",
    val groupId: String = ""
)
