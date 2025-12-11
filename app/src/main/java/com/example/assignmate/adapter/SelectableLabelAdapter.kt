package com.example.assignmate.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.model.Label

class SelectableLabelAdapter(
    private val allLabels: List<Label>,
    val selectedLabelIds: MutableSet<String>,
    private val onCreateLabel: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_LABEL = 0
        private const val VIEW_TYPE_CREATE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (allLabels.isEmpty()) VIEW_TYPE_CREATE else VIEW_TYPE_LABEL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_LABEL) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selectable_label, parent, false)
            SelectableLabelViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_create_label, parent, false)
            CreateLabelViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SelectableLabelViewHolder) {
            val label = allLabels[position]
            holder.bind(label)
        }
    }

    override fun getItemCount() = if (allLabels.isEmpty()) 1 else allLabels.size

    inner class SelectableLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelCheckbox: CheckBox = itemView.findViewById(R.id.label_checkbox)
        private val colorPreview: CardView = itemView.findViewById(R.id.color_preview)
        private val labelName: TextView = itemView.findViewById(R.id.label_name)

        fun bind(label: Label) {
            labelName.text = label.name
            labelCheckbox.isChecked = selectedLabelIds.contains(label.id)

            val color = Color.parseColor(label.color)
            colorPreview.setCardBackgroundColor(color)

            itemView.setOnClickListener {
                labelCheckbox.isChecked = !labelCheckbox.isChecked
            }

            labelCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedLabelIds.add(label.id)
                } else {
                    selectedLabelIds.remove(label.id)
                }
            }
        }
    }

    inner class CreateLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val createLabelButton: Button = itemView.findViewById(R.id.create_label_button)

        init {
            createLabelButton.setOnClickListener {
                onCreateLabel()
            }
        }
    }
}
