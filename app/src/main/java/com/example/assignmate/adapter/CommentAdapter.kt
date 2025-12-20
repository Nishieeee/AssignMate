package com.example.assignmate.adapter

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assignmate.FullScreenImageActivity
import com.example.assignmate.R
import com.example.assignmate.model.Attachment
import com.example.assignmate.model.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val comments: List<Comment>,
    private var isDeleteMode: Boolean = false,
    private val selectedComments: MutableSet<String> = mutableSetOf(),
    private val onSelectionChanged: () -> Unit = {}
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount() = comments.size

    fun setDeleteMode(enabled: Boolean) {
        isDeleteMode = enabled
        if (!enabled) {
            selectedComments.clear()
        }
        notifyDataSetChanged()
    }

    fun getSelectedCommentIds(): List<String> {
        return selectedComments.toList()
    }

    fun getAllCommentIds(): List<String> {
        return comments.map { it.id }
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val username: TextView = itemView.findViewById(R.id.comment_username)
        private val commentText: TextView = itemView.findViewById(R.id.comment_text)
        private val timestamp: TextView = itemView.findViewById(R.id.comment_timestamp)
        private val attachmentsLayout: LinearLayout = itemView.findViewById(R.id.comment_attachments_layout)
        private val deleteCheckbox: CheckBox = itemView.findViewById(R.id.delete_checkbox)
        private val profileImage: ImageView = itemView.findViewById(R.id.comment_profile_image)

        fun bind(comment: Comment) {
            username.text = comment.username
            commentText.text = comment.commentText
            timestamp.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(comment.timestamp))

            if (comment.userProfileImage.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(comment.userProfileImage)
                    .placeholder(R.drawable.ic_profile_user)
                    .error(R.drawable.ic_profile_user)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.ic_profile_user)
            }

            deleteCheckbox.visibility = if (isDeleteMode) View.VISIBLE else View.GONE
            // Avoid triggering listener during bind
            deleteCheckbox.setOnCheckedChangeListener(null)
            deleteCheckbox.isChecked = selectedComments.contains(comment.id)
            
            deleteCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedComments.add(comment.id)
                } else {
                    selectedComments.remove(comment.id)
                }
                onSelectionChanged()
            }

            attachmentsLayout.removeAllViews()
            if (comment.attachments.isNotEmpty()) {
                attachmentsLayout.visibility = View.VISIBLE
                comment.attachments.forEach { attachment ->
                    val url = attachment.url
                    val isImage = attachment.type.startsWith("image/")

                    if (isImage) {
                        val imageView = ImageView(itemView.context)
                        val params = LinearLayout.LayoutParams(200, 200) // Adjust size as needed
                        params.setMargins(0, 0, 16, 0)
                        imageView.layoutParams = params
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                        
                        Glide.with(itemView.context)
                            .load(url)
                            .placeholder(R.drawable.ic_attach_image)
                            .error(R.drawable.ic_attach_file)
                            .into(imageView)

                        imageView.setOnClickListener {
                            val intent = Intent(itemView.context, FullScreenImageActivity::class.java)
                            intent.putExtra("IMAGE_URL", url)
                            intent.putExtra("IMAGE_NAME", attachment.filename)
                            itemView.context.startActivity(intent)
                        }
                        attachmentsLayout.addView(imageView)

                    } else {
                        // For non-image files, use the custom layout
                        val fileView = LayoutInflater.from(itemView.context).inflate(R.layout.item_attachment_file, attachmentsLayout, false)
                        val fileNameView = fileView.findViewById<TextView>(R.id.file_name)
                        val fileSizeView = fileView.findViewById<TextView>(R.id.file_size)

                        fileNameView.text = attachment.filename
                        fileSizeView.text = formatFileSize(attachment.size)

                        fileView.setOnClickListener {
                            downloadFile(itemView.context, url, attachment.filename)
                        }
                        attachmentsLayout.addView(fileView)
                    }
                }
            } else {
                attachmentsLayout.visibility = View.GONE
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    private fun downloadFile(context: Context, url: String, fileName: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)
            
            var name = fileName
            if (name.isBlank()) {
                name = "downloaded_file"
            }
            
            request.setTitle(name)
            request.setDescription("Downloading attachment...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            
            downloadManager.enqueue(request)
            Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
