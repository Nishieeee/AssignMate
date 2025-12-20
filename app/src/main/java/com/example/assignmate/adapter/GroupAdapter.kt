package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assignmate.R
import com.example.assignmate.databinding.ItemGroupCardBinding
import com.example.assignmate.model.Group
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GroupAdapter(
    private var groups: MutableList<Group>,
    private val currentUserId: String,
    private val onGroupClicked: (Group) -> Unit,
    private val onEditClicked: (Group) -> Unit,
    private val onDeleteClicked: (Group) -> Unit,
    private val onFavouriteClicked: (Group) -> Unit,
    private val getUsername: (String, (String) -> Unit) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    private var allGroups: List<Group> = ArrayList(groups)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.bind(group)
    }

    override fun getItemCount() = groups.size

    fun setGroups(groups: List<Group>){
        this.allGroups = ArrayList(groups)
        filter("All", "") // Initially show all groups
    }

    fun filter(filterType: String, query: String) {
        var filteredList = when (filterType) {
            "Favourite" -> allGroups.filter { it.favouriteBy.contains(currentUserId) }
            "Leader" -> allGroups.filter { it.members[currentUserId] == "leader" }
            "Co-leader/Member" -> allGroups.filter { it.members[currentUserId] == "co-leader" || it.members[currentUserId] == "member" }
            else -> allGroups
        }

        if (query.isNotEmpty()) {
            filteredList = filteredList.filter { group ->
                group.name.contains(query, ignoreCase = true) ||
                        group.description.contains(query, ignoreCase = true)
            }
        }
        
        // Sorting logic based on filterType if needed, but for now sorting is not explicitly requested other than filtering
        // The original logic had sorting options in the dropdown text, let's respect that if they were combined
        // But the prompt asked for filtering by Leader vs Member.
        // If the dropdown also includes sort options like "Date Created", we should handle them.
        // Based on GroupActivity.kt: "All", "Favourite", "Date Created", "Last Updated", "Most Tasks Assigned"
        // Wait, I replaced the dropdown options in GroupActivity.kt in the previous turn.
        // The new options are: "All", "Leader", "Co-leader/Member", "Favourite".
        // So I don't need to handle sorting here anymore unless requested.

        groups.clear()
        groups.addAll(filteredList)
        notifyDataSetChanged()
    }

    // Overload for backward compatibility if needed, but GroupActivity uses the new signature
    fun filter(query: String) {
        filter("All", query)
    }

    inner class GroupViewHolder(private val binding: ItemGroupCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: Group) {
            binding.groupName.text = group.name
            getUsername(group.leaderId) { username ->
                binding.groupLeader.text = "Group Leader: $username"
            }
            binding.groupMembers.text = "Members: ${group.members.size}"
            binding.assignedTasks.text = "Assigned Tasks: ${group.assignedTasksCount}"
            binding.groupDescription.text = group.description
            binding.lastUpdated.text = "Last updated: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(group.lastUpdated))}"
            binding.groupProgress.progress = group.progress
            
            if (group.profileImage.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(group.profileImage)
                    .placeholder(R.drawable.scrim)
                    .error(R.drawable.scrim)
                    .centerCrop()
                    .into(binding.groupImage)
            } else {
                binding.groupImage.setImageResource(R.drawable.scrim)
            }

            binding.root.setOnClickListener {
                onGroupClicked(group)
            }

            binding.groupOverflowMenu.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.group_card_menu, popup.menu)

                val isFavourite = group.favouriteBy.contains(currentUserId)
                popup.menu.findItem(R.id.action_add_to_favourite).title = if (isFavourite) "Remove from Favourites" else "Add to Favourites"

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_edit_group -> {
                            onEditClicked(group)
                            true
                        }
                        R.id.action_delete_group -> {
                            onDeleteClicked(group)
                            true
                        }
                        R.id.action_add_to_favourite -> {
                            onFavouriteClicked(group)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }
}
