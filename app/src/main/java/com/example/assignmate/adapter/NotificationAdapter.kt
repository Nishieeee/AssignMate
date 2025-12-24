package com.example.assignmate.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemNotificationBinding
import com.example.assignmate.model.Notification
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val notifications: MutableList<Notification>,
    private val onMarkAsReadClicked: (Notification) -> Unit,
    private val onItemLongClicked: () -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<String>() // Use IDs for selection

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.bind(notification, isSelectionMode, selectedItems.contains(notification.id))
    }

    override fun getItemCount() = notifications.size

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) {
            selectedItems.clear()
        }
        notifyDataSetChanged()
    }

    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedNotifications(): List<Notification> {
        return notifications.filter { selectedItems.contains(it.id) }
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification, isSelectionMode: Boolean, isSelected: Boolean) {
            val messageParts = notification.message.split(":", limit = 2)
            val title = if (messageParts.isNotEmpty()) messageParts[0] else ""
            val message = if (messageParts.size > 1) messageParts[1].trim() else ""

            binding.notificationTitle.text = title
            binding.notificationMessage.text = message
            binding.notificationTimestamp.text = SimpleDateFormat("hh:mm a, dd/MM/yy", Locale.getDefault()).format(Date(notification.timestamp))

            val cardView = binding.root as? MaterialCardView
            
            if (notification.isRead) {
                val readColor = Color.parseColor("#FFF8F0")
                cardView?.setCardBackgroundColor(readColor)
                binding.markReadButton.visibility = View.GONE
            } else {
                val unreadColor = Color.WHITE
                cardView?.setCardBackgroundColor(unreadColor)
                binding.markReadButton.visibility = View.VISIBLE
            }

            binding.notificationTitle.setTextColor(Color.BLACK)
            binding.notificationMessage.setTextColor(Color.DKGRAY)
            binding.notificationTimestamp.setTextColor(Color.GRAY)

            binding.markReadButton.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    val updatedNotification = notification.copy(isRead = true)
                    notifications[currentPosition] = updatedNotification
                    notifyItemChanged(currentPosition)
                    onMarkAsReadClicked(notification)
                }
            }

            if (isSelectionMode) {
                binding.selectionCheckbox.visibility = View.VISIBLE
                binding.selectionCheckbox.isChecked = isSelected

                itemView.setOnClickListener { toggleSelection(notification.id) }
                binding.selectionCheckbox.setOnClickListener { toggleSelection(notification.id) }
            } else {
                binding.selectionCheckbox.visibility = View.GONE
                itemView.setOnClickListener(null)
                binding.selectionCheckbox.setOnClickListener(null)
                
                itemView.setOnLongClickListener {
                    onItemLongClicked()
                    true
                }
            }
        }

        private fun toggleSelection(notificationId: String) {
            val isCurrentlySelected = selectedItems.contains(notificationId)
            if (isCurrentlySelected) {
                selectedItems.remove(notificationId)
            } else {
                selectedItems.add(notificationId)
            }
            binding.selectionCheckbox.isChecked = !isCurrentlySelected
        }
    }
}
