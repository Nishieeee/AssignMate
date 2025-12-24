package com.example.assignmate.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assignmate.R
import com.example.assignmate.model.Member
import de.hdodenhof.circleimageview.CircleImageView

class MembersAdapter(
    private val members: List<Member>,
    private val currentUserRole: String,
    private val currentUserId: String,
    private val onMemberActionListener: (Member, String) -> Unit
) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member, member.id == currentUserId)

        // Rule 1: Do not show menu if the member is the current user
        if (member.id == currentUserId) {
            holder.memberActionsButton.visibility = View.GONE
            return
        }

        // Rule 2: Logic for showing the menu based on roles
        if (currentUserRole == "leader") {
            // Leader can manage everyone else
            holder.memberActionsButton.visibility = View.VISIBLE
            holder.memberActionsButton.setOnClickListener { view ->
                showPopupMenu(view, member)
            }
        } else {
            // Co-leaders and regular members cannot manage anyone
            holder.memberActionsButton.visibility = View.GONE
        }
    }

    private fun showPopupMenu(view: View, member: Member) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.member_actions_menu, popup.menu)

        // Configure menu items based on role
        if (member.role == "co-leader") {
            popup.menu.findItem(R.id.action_assign_co_leader).title = "Remove as Co-Leader"
        } else {
            popup.menu.findItem(R.id.action_assign_co_leader).title = "Assign as Co-Leader"
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_assign_co_leader -> {
                    val action = if (member.role == "co-leader") "remove_co_leader" else "assign_co_leader"
                    onMemberActionListener(member, action)
                    true
                }
                R.id.action_remove_member -> {
                    onMemberActionListener(member, "remove_member")
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun getItemCount() = members.size

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val memberName: TextView = itemView.findViewById(R.id.member_name)
        val memberActionsButton: ImageButton = itemView.findViewById(R.id.member_actions_button)
        private val memberImage: CircleImageView = itemView.findViewById(R.id.member_image)

        fun bind(member: Member, isCurrentUser: Boolean) {
            var name = member.name
            if (member.role == "leader") {
                name += " (Leader)"
            } else if (member.role == "co-leader") {
                name += " (Co-Leader)"
            }
            memberName.text = name
            
            if (isCurrentUser) {
                memberName.setTextColor(Color.parseColor("#6DBE45"))
            } else {
                // Reset to default color (e.g., black) for other items to avoid recycling issues
                memberName.setTextColor(Color.BLACK) 
            }

            // Load profile image
            if (member.profileImage.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(member.profileImage)
                    .placeholder(R.drawable.ic_profile_user)
                    .into(memberImage)
            } else {
                memberImage.setImageResource(R.drawable.ic_profile_user)
            }
        }
    }
}
