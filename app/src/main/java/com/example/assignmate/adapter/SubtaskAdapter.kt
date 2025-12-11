package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.model.Subtask

class SubtaskAdapter(
    private val subtasks: MutableList<Subtask>,
    private val onSubtaskCheckedChangeListener: (() -> Unit)? = null
) : RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subtask, parent, false)
        return SubtaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
        val subtask = subtasks[position]
        holder.bind(subtask)
    }

    override fun getItemCount() = subtasks.size

    fun addSubtask(subtask: Subtask) {
        subtasks.add(subtask)
        notifyItemInserted(subtasks.size - 1)
    }

    inner class SubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val subtaskCheckbox: CheckBox = itemView.findViewById(R.id.subtask_checkbox)
        private val subtaskName: TextView = itemView.findViewById(R.id.subtask_name)

        fun bind(subtask: Subtask) {
            subtaskCheckbox.isChecked = subtask.isCompleted
            subtaskName.text = subtask.name

            subtaskCheckbox.setOnCheckedChangeListener { _, isChecked ->
                subtask.isCompleted = isChecked
                onSubtaskCheckedChangeListener?.invoke()
            }
        }
    }
}
