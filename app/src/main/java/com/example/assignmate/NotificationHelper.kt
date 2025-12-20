package com.example.assignmate

import com.example.assignmate.model.Notification
import com.google.firebase.firestore.FirebaseFirestore

class NotificationHelper {

    private val db = FirebaseFirestore.getInstance()

    fun sendNotification(notification: Notification, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}