package com.example.assignmate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.assignmate.model.Notification

class NotificationHelper(private val context: Context) {

    private val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java) as NotificationManager
    private val firebaseHelper = FirebaseHelper()

    companion object {
        const val CHANNEL_ID = "assignmate_channel_id"
        const val CHANNEL_NAME = "AssignMate Notifications"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for AssignMate"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(notification: Notification, notificationId: Int) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications) // Ensure you have this drawable
            .setContentTitle("AssignMate")
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        firebaseHelper.addNotification(notification, 
            onSuccess = { 
                notificationManager.notify(notificationId, builder.build())
            }, 
            onFailure = { 
                // Do nothing
            }
        )
    }
}
