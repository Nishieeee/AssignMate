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

    private var isDeleteMode = false
    private val selectedSubtasks = mutableSetOf<Subtask>()

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

    fun setDeleteMode(enabled: Boolean) {
        isDeleteMode = enabled
        if (!enabled) {
            selectedSubtasks.clear()
        }
        notifyDataSetChanged()
    }

    fun getSelectedSubtasks(): List<Subtask> {
        return selectedSubtasks.toList()
    }

    fun removeSubtasks(toRemove: List<Subtask>) {
        subtasks.removeAll(toRemove)
        selectedSubtasks.clear()
        notifyDataSetChanged()
    }

    fun getAllSubtasks(): List<Subtask> {
        return subtasks.toList()
    }

    inner class SubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val subtaskCheckbox: CheckBox = itemView.findViewById(R.id.subtask_checkbox)
        private val subtaskName: TextView = itemView.findViewById(R.id.subtask_name)
        private val subtaskDeleteCheckbox: CheckBox = itemView.findViewById(R.id.subtask_delete_checkbox)

        fun bind(subtask: Subtask) {
            subtaskName.text = subtask.name
            
            if (isDeleteMode) {
                subtaskCheckbox.visibility = View.GONE
                subtaskDeleteCheckbox.visibility = View.VISIBLE
                subtaskDeleteCheckbox.setOnCheckedChangeListener(null)
                subtaskDeleteCheckbox.isChecked = selectedSubtasks.contains(subtask)
                
                subtaskDeleteCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedSubtasks.add(subtask)
                    } else {
                        selectedSubtasks.remove(subtask)
                    }
                }
                itemView.setOnClickListener {
                     subtaskDeleteCheckbox.isChecked = !subtaskDeleteCheckbox.isChecked
                }
            } else {
                subtaskCheckbox.visibility = View.VISIBLE
                subtaskDeleteCheckbox.visibility = View.GONE
                
                subtaskCheckbox.setOnCheckedChangeListener(null)
                subtaskCheckbox.isChecked = subtask.isCompleted

                subtaskCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    subtask.isCompleted = isChecked
                    onSubtaskCheckedChangeListener?.invoke()
                }
                itemView.setOnClickListener(null)
            }
        }
    }
}
