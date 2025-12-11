package com.example.assignmate

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.assignmate.databinding.ActivitySingleGroupBinding
import com.google.android.material.tabs.TabLayoutMediator

class SingleGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingleGroupBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private var groupId: String = ""
    private var currentUserId: String = ""
    private var currentUserRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()
        groupId = intent.getStringExtra("GROUP_ID") ?: ""
        currentUserId = intent.getStringExtra("USER_ID") ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firebaseHelper.getGroup(groupId,
            onSuccess = {
                if (it != null) {
                    supportActionBar?.title = it.name
                    currentUserRole = it.members[currentUserId]
                    invalidateOptionsMenu()
                }
            },
            onFailure = {
                Toast.makeText(this, "Failed to load group details", Toast.LENGTH_SHORT).show()
            }
        )

        val viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Group Tasks"
                1 -> "Members"
                else -> null
            }
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.single_group_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val canManageGroup = currentUserRole == "leader" || currentUserRole == "co-leader"
        menu?.findItem(R.id.action_edit_group)?.isVisible = canManageGroup
        menu?.findItem(R.id.action_delete_group)?.isVisible = canManageGroup
        menu?.findItem(R.id.action_add_to_favourite)?.isVisible = false
        menu?.findItem(R.id.action_manage_labels)?.isVisible = false
        menu?.findItem(R.id.action_add_members)?.isVisible = canManageGroup
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_edit_group -> showEditGroupDialog()
            R.id.action_delete_group -> showDeleteGroupDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showEditGroupDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Group")

        val view = layoutInflater.inflate(R.layout.dialog_create_group, null)
        builder.setView(view)

        val groupNameInput = view.findViewById<EditText>(R.id.group_name_input)
        val groupDescriptionInput = view.findViewById<EditText>(R.id.group_description_input)

        firebaseHelper.getGroup(groupId,
            onSuccess = {
                if (it != null) {
                    groupNameInput.setText(it.name)
                    groupDescriptionInput.setText(it.description)
                }
            },
            onFailure = {}
        )

        builder.setPositiveButton("Save") { dialog, _ ->
            val newGroupName = groupNameInput.text.toString()
            val newGroupDescription = groupDescriptionInput.text.toString()
            if (newGroupName.isNotEmpty()) {
                firebaseHelper.updateGroup(groupId, newGroupName, newGroupDescription,
                    onSuccess = {
                        Toast.makeText(this, "Group updated successfully", Toast.LENGTH_SHORT).show()
                        supportActionBar?.title = newGroupName
                    },
                    onFailure = {
                        Toast.makeText(this, "Failed to update group", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun showDeleteGroupDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete this group?")
            .setPositiveButton("Delete") { _, _ ->
                firebaseHelper.deleteGroup(groupId,
                    onSuccess = {
                        Toast.makeText(this, "Group deleted successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onFailure = {
                        Toast.makeText(this, "Failed to delete group", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GroupTasksFragment.newInstance(groupId)
                1 -> MembersFragment.newInstance(groupId, currentUserId)
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}
