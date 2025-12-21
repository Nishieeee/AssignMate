package com.example.assignmate

import android.view.View
import android.widget.TextView
import com.example.assignmate.model.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationHelper {

    private val db = FirebaseFirestore.getInstance()

    fun sendNotification(notification: Notification, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Sending to the user's subcollection to be consistent with the listener
        db.collection("users").document(notification.userId).collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun setupNotificationBadge(userId: String, badgeTextView: TextView): ListenerRegistration {
        // Listen to the notifications subcollection for the current user (path: users/{userId}/notifications)
        return db.collection("users").document(userId).collection("notifications")
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val count = snapshot.size()
                    if (count > 0) {
                        badgeTextView.visibility = View.VISIBLE
                        badgeTextView.text = if (count > 99) "99+" else count.toString()
                    } else {
                        badgeTextView.visibility = View.GONE
                    }
                }
            }
    }
}
