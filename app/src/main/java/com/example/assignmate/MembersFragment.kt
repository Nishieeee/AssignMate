package com.example.assignmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.adapter.MembersAdapter
import com.example.assignmate.model.Member

class MembersFragment : Fragment() {

    private lateinit var firebaseHelper: FirebaseHelper
    private var groupId: String = ""
    private var currentUserId: String = ""

    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var memberAdapter: MembersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getString(ARG_GROUP_ID) ?: ""
            currentUserId = it.getString(ARG_CURRENT_USER_ID) ?: ""
        }
        firebaseHelper = FirebaseHelper()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_members, container, false)
        membersRecyclerView = view.findViewById(R.id.members_recycler_view)
        membersRecyclerView.layoutManager = LinearLayoutManager(context)

        loadMembers()

        return view
    }

    fun loadMembers() {
        firebaseHelper.getGroupMembers(groupId,
            onSuccess = {
                val currentUser = it.find { member -> member.id == currentUserId }
                memberAdapter = MembersAdapter(it, currentUser?.role ?: "member", currentUserId) { member, action ->
                    handleMemberAction(member, action)
                }
                membersRecyclerView.adapter = memberAdapter
            },
            onFailure = {
                Toast.makeText(requireContext(), "Failed to load members", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun handleMemberAction(member: Member, action: String) {
        when (action) {
            "assign_co_leader" -> updateMemberRole(member, "co-leader", "Co-leader role assigned.")
            "remove_co_leader" -> updateMemberRole(member, "member", "Member role assigned.")
            "remove_member" -> showRemoveMemberConfirmationDialog(member)
        }
    }

    private fun updateMemberRole(member: Member, role: String, message: String) {
        firebaseHelper.updateMemberRole(groupId, member.id, role,
            onSuccess = {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                loadMembers()
            },
            onFailure = {
                Toast.makeText(requireContext(), "Failed to update role", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showRemoveMemberConfirmationDialog(member: Member) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove ${member.name} from the group?")
            .setPositiveButton("Remove") { _, _ ->
                firebaseHelper.removeMemberFromGroup(groupId, member.id,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Member removed", Toast.LENGTH_SHORT).show()
                        loadMembers()
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Failed to remove member", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val ARG_GROUP_ID = "GROUP_ID"
        private const val ARG_CURRENT_USER_ID = "CURRENT_USER_ID"

        @JvmStatic
        fun newInstance(groupId: String, currentUserId: String) =
            MembersFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_GROUP_ID, groupId)
                    putString(ARG_CURRENT_USER_ID, currentUserId)
                }
            }
    }
}
