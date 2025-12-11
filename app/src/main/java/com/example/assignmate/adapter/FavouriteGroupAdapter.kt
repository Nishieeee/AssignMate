package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemFavouriteGroupBinding
import com.example.assignmate.model.Group

class FavouriteGroupAdapter(
    private var groups: List<Group>,
    private val onGroupClicked: (Group) -> Unit
) : RecyclerView.Adapter<FavouriteGroupAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemFavouriteGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.bind(group)
    }

    override fun getItemCount() = groups.size

    fun updateGroups(newGroups: List<Group>){
        groups = newGroups
        notifyDataSetChanged()
    }

    inner class GroupViewHolder(private val binding: ItemFavouriteGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: Group) {
            binding.groupName.text = group.name
            binding.groupProgress.progress = group.progress
            binding.root.setOnClickListener {
                onGroupClicked(group)
            }
        }
    }
}
