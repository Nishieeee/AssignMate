package com.example.assignmate

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.adapter.NotificationAdapter
import com.example.assignmate.databinding.ActivityNotificationsBinding
import com.example.assignmate.model.Notification

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var notificationAdapter: NotificationAdapter
    private var currentUserId: String = ""
    private val notifications = mutableListOf<Notification>()
    private var isSelectionMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()
        currentUserId = intent.getStringExtra("USER_ID") ?: ""

        if (currentUserId.isEmpty()) {
            Log.e("NotificationsActivity", "USER_ID extra is missing or empty. Cannot load notifications.")
            Toast.makeText(this, "Could not load user data. Please try again.", Toast.LENGTH_LONG).show()
            finish() // Exit the activity since it's in an invalid state.
            return   // Stop further execution of onCreate.
        }

        setupToolbar()
        setupRecyclerView()
        loadNotifications()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            notifications,
            onMarkAsReadClicked = { notification ->
                firebaseHelper.markNotificationAsRead(notification.id, { loadNotifications() }, {})
            },
            onItemLongClicked = {
                toggleSelectionMode()
            }
        )
        binding.notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationAdapter
        }
    }

    private fun loadNotifications() {
        firebaseHelper.getNotificationsForUser(currentUserId, 
            onSuccess = {
                notifications.clear()
                notifications.addAll(it)
                notificationAdapter.notifyDataSetChanged()

                if (notifications.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.notificationsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.notificationsRecyclerView.visibility = View.VISIBLE
                }
            },
            onFailure = { e ->
                Log.e("NotificationsActivity", "Error loading notifications", e)
                Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show()
                binding.emptyView.visibility = View.VISIBLE
                binding.notificationsRecyclerView.visibility = View.GONE
            }
        )
    }

    private fun setupClickListeners() {
        binding.markAllReadButton.setOnClickListener {
            showMarkAllReadConfirmationDialog()
        }

        binding.deleteButton.setOnClickListener {
            toggleSelectionMode()
        }

        binding.cancelSelectionButton.setOnClickListener {
            toggleSelectionMode()
        }

        binding.deleteSelectionButton.setOnClickListener {
            val selectedNotifications = notificationAdapter.getSelectedNotifications()
            if (selectedNotifications.isNotEmpty()) {
                showDeleteConfirmationDialog(selectedNotifications)
            } else {
                toggleSelectionMode()
            }
        }

        binding.deleteAllButton.setOnClickListener {
            showDeleteAllConfirmationDialog()
        }
    }

    private fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        notificationAdapter.setSelectionMode(isSelectionMode)
        if (isSelectionMode) {
            binding.normalToolbarLayout.visibility = View.GONE
            binding.selectionToolbarLayout.visibility = View.VISIBLE
        } else {
            binding.normalToolbarLayout.visibility = View.VISIBLE
            binding.selectionToolbarLayout.visibility = View.GONE
            notificationAdapter.clearSelections()
        }
    }

    private fun showMarkAllReadConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Mark All As Read")
            .setMessage("Are you sure you want to mark all messages as read?")
            .setPositiveButton("Yes") { _, _ ->
                firebaseHelper.markAllNotificationsAsRead(currentUserId, { loadNotifications() }, {})
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(selectedNotifications: List<Notification>) {
        AlertDialog.Builder(this)
            .setTitle("Delete Notifications")
            .setMessage("Are you sure you want to delete ${selectedNotifications.size} selected notifications?")
            .setPositiveButton("Delete") { _, _ ->
                selectedNotifications.forEach { notification ->
                    firebaseHelper.deleteNotification(notification.id, { loadNotifications() }, {})
                }
                toggleSelectionMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAllConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Notifications")
            .setMessage("Are you sure you want to delete all notifications?")
            .setPositiveButton("Delete") { _, _ ->
                firebaseHelper.deleteAllNotifications(currentUserId, { loadNotifications() }, {})
                toggleSelectionMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
