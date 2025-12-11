package com.example.assignmate.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemLabelBinding
import com.example.assignmate.model.Label

class LabelAdapter(
    private val labels: MutableList<Label>,
    private val onEditClicked: (Label) -> Unit,
    private val onDeleteClicked: (Label) -> Unit
) : RecyclerView.Adapter<LabelAdapter.LabelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val binding = ItemLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LabelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        holder.bind(labels[position])
    }

    override fun getItemCount() = labels.size

    inner class LabelViewHolder(private val binding: ItemLabelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(label: Label) {
            binding.labelName.text = label.name
            binding.colorPreview.setBackgroundColor(Color.parseColor(label.color))
            binding.editButton.setOnClickListener { onEditClicked(label) }
            binding.deleteButton.setOnClickListener { onDeleteClicked(label) }
        }
    }
}
