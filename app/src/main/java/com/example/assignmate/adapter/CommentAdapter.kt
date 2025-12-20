package com.example.assignmate.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assignmate.R
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

        fun bind(comment: Comment) {
            username.text = comment.username
            commentText.text = comment.commentText
            timestamp.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(comment.timestamp))

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
                comment.attachments.forEach { url ->
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
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        itemView.context.startActivity(intent)
                    }
                    
                    attachmentsLayout.addView(imageView)
                }
            } else {
                attachmentsLayout.visibility = View.GONE
            }
        }
    }
}
