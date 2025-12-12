package com.example.assignmate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.adapter.GroupAdapter
import com.example.assignmate.databinding.ActivityGroupBinding
import com.example.assignmate.model.Group
import com.example.assignmate.model.Task
import com.google.android.material.bottomnavigation.BottomNavigationView

class GroupActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityGroupBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var groupAdapter: GroupAdapter
    private val groups = mutableListOf<Group>()
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()
        currentUserId = intent.getStringExtra("USER_ID") ?: ""

        setupRecyclerView()
        setupFilterAndSort()
        updateNotificationBadge()

        binding.addGroupButton.setOnClickListener {
            showJoinGroupDialog()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.action_groups
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        val notificationBell = findViewById<ImageView>(R.id.notification_bell)
        notificationBell.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadGroups()
        updateNotificationBadge()
    }

    private fun setupFilterAndSort(){
        val filterOptions = arrayOf("All", "Favourite", "Date Created", "Last Updated", "Most Tasks Assigned")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filterOptions)
        binding.filterDropdown.setAdapter(adapter)

        binding.searchInput.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                groupAdapter.filter(query)
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.filterDropdown.setOnItemClickListener { _, _, position, _ ->
            loadGroups()
        }
    }

    private fun setupRecyclerView() {
        groupAdapter = GroupAdapter(groups, currentUserId,
            onGroupClicked = { group ->
                val intent = Intent(this, SingleGroupActivity::class.java)
                intent.putExtra("GROUP_NAME", group.name)
                intent.putExtra("GROUP_ID", group.id)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
            },
            onEditClicked = { group ->
                showEditGroupDialog(group)
            },
            onDeleteClicked = { group ->
                showDeleteGroupConfirmationDialog(group)
            },
            onFavouriteClicked = { group ->
                val isFavourite = group.favouriteBy.contains(currentUserId)
                firebaseHelper.setFavourite(group.id, currentUserId, !isFavourite, {
                    loadGroups()
                    val message = if (!isFavourite) "Group added to favourites" else "Group removed from favourites"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }, {})
            },
            getUsername = { userId, callback ->
                firebaseHelper.getUserDetails(userId, {
                    it?.let { callback(it.username) }
                }, {})
            }
        )
        binding.groupsRecyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(this@GroupActivity)
        }
    }

    private fun loadGroups() {
        firebaseHelper.getGroupsForUser(currentUserId,
            onSuccess = { groups ->
                val updatedGroups = mutableListOf<Group>()
                var groupsProcessed = 0
                for (group in groups) {
                    firebaseHelper.getTasksForGroup(group.id, {
                        val progress = calculateProgress(it)
                        val assignedTasksCount = it.count { task -> task.assignedTo.contains(currentUserId) }
                        updatedGroups.add(group.copy(progress = progress, assignedTasksCount = assignedTasksCount))
                        groupsProcessed++
                        if(groupsProcessed == groups.size) {
                            groupAdapter.setGroups(updatedGroups)
                            updateUI()
                        }
                    }, {})
                }
                if (groups.isEmpty()){
                    groupAdapter.setGroups(emptyList())
                    updateUI()
                }
            },
            onFailure = {
                Toast.makeText(this, "Failed to load groups", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun calculateProgress(tasks: List<Task>): Int {
        if (tasks.isEmpty()) return 0
        var totalProgress = 0.0
        for (task in tasks) {
            totalProgress += when (task.status) {
                "Complete" -> 100
                "In progress" -> 50
                else -> 0
            }
        }
        return (totalProgress / tasks.size).toInt()
    }

    private fun updateUI() {
        if (groupAdapter.itemCount == 0) {
            binding.groupsRecyclerView.visibility = View.GONE
            binding.noGroupsLayout.visibility = View.VISIBLE
        } else {
            binding.groupsRecyclerView.visibility = View.VISIBLE
            binding.noGroupsLayout.visibility = View.GONE
        }
    }

    private fun showCreateGroupDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_group, null)
        builder.setView(view)

        val groupNameInput = view.findViewById<EditText>(R.id.group_name_input)
        val groupDescriptionInput = view.findViewById<EditText>(R.id.group_description_input)
        val groupCodeText = view.findViewById<TextView>(R.id.group_code_text)
        val copyCodeButton = view.findViewById<ImageButton>(R.id.copy_code_button)
        val joinGroupInsteadButton = view.findViewById<TextView>(R.id.join_group_instead_button)

        val dialog = builder.create()

        groupNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    val groupCode = generateGroupCode()
                    groupCodeText.text = "Group Code: $groupCode"
                    copyCodeButton.visibility = View.VISIBLE
                } else if (s.isNullOrEmpty()) {
                    groupCodeText.text = "Group Code: "
                    copyCodeButton.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        copyCodeButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val code = groupCodeText.text.toString().substringAfter("Group Code: ")
            val clip = ClipData.newPlainText("Group Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Group code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Create") { _, _ ->
            val groupName = groupNameInput.text.toString()
            val groupDescription = groupDescriptionInput.text.toString()
            val groupCode = groupCodeText.text.toString().substringAfter("Group Code: ")

            if (groupName.isNotEmpty() && groupCode.length == 6) {
                firebaseHelper.createGroup(groupName, groupDescription, currentUserId, groupCode,
                    onSuccess = {
                        Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, SingleGroupActivity::class.java)
                        intent.putExtra("GROUP_ID", it)
                        intent.putExtra("USER_ID", currentUserId)
                        startActivity(intent)
                    },
                    onFailure = {
                        Toast.makeText(this, "Failed to create group. The code might already exist.", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ -> dialog.dismiss() }

        joinGroupInsteadButton.setOnClickListener {
            dialog.dismiss()
            showJoinGroupDialog()
        }

        dialog.show()
    }

    private fun showJoinGroupDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_join_group, null)
        builder.setView(view)

        val groupCodeInput = view.findViewById<EditText>(R.id.group_code_input)
        val createGroupInsteadButton = view.findViewById<TextView>(R.id.create_group_instead_button)

        val dialog = builder.create()

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Join") { _, _ ->
            val groupCode = groupCodeInput.text.toString()
            if (groupCode.isNotEmpty()) {
                firebaseHelper.joinGroup(groupCode, currentUserId,
                    onSuccess = {
                        Toast.makeText(this, "Group Joined Successfully", Toast.LENGTH_SHORT).show()
                        loadGroups()
                    },
                    onFailure = {
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(this, "Please enter a group code", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ -> dialog.dismiss() }

        createGroupInsteadButton.setOnClickListener {
            dialog.dismiss()
            showCreateGroupDialog()
        }

        dialog.show()
    }

    private fun showEditGroupDialog(group: com.example.assignmate.model.Group) {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_group, null)
        builder.setView(view)

        val groupNameInput = view.findViewById<EditText>(R.id.group_name_input)
        val groupDescriptionInput = view.findViewById<EditText>(R.id.group_description_input)
        val groupCodeText = view.findViewById<TextView>(R.id.group_code_text)
        val copyCodeButton = view.findViewById<ImageButton>(R.id.copy_code_button)
        val joinGroupInsteadButton = view.findViewById<TextView>(R.id.join_group_instead_button)

        groupNameInput.setText(group.name)
        groupDescriptionInput.setText(group.description)
        groupCodeText.text = "Group Code: ${group.code}"
        copyCodeButton.visibility = View.VISIBLE
        joinGroupInsteadButton.visibility = View.GONE

        val dialog = builder.create()

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Save Changes") { _, _ ->
            val newGroupName = groupNameInput.text.toString()
            val newGroupDescription = groupDescriptionInput.text.toString()

            if (newGroupName.isNotEmpty()) {
                firebaseHelper.updateGroup(group.id, newGroupName, newGroupDescription,
                    onSuccess = {
                        Toast.makeText(this, "Group updated successfully", Toast.LENGTH_SHORT).show()
                        loadGroups()
                    },
                    onFailure = {
                        Toast.makeText(this, "Failed to update group", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(this, "Group name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ -> dialog.dismiss() }

        dialog.show()
    }

    private fun showDeleteGroupConfirmationDialog(group: com.example.assignmate.model.Group) {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete \"${group.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                firebaseHelper.deleteGroup(group.id,
                    onSuccess = {
                        Toast.makeText(this, "Group deleted", Toast.LENGTH_SHORT).show()
                        loadGroups()
                    },
                    onFailure = {
                        Toast.makeText(this, "Failed to delete group", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateGroupCode(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun updateNotificationBadge(){
        val notificationBadge = findViewById<TextView>(R.id.notification_badge)
        if (currentUserId.isNotEmpty()) {
            firebaseHelper.getUnreadNotificationCount(currentUserId, {
                if(it > 0){
                    notificationBadge.visibility = View.VISIBLE
                    notificationBadge.text = it.toString()
                }else{
                    notificationBadge.visibility = View.GONE
                }
            }, {})
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
                return true
            }
            R.id.action_groups -> {
                return true
            }
            R.id.action_create -> {
                showCreateGroupDialog()
                return false
            }
            R.id.action_tasks -> {
                val intent = Intent(this, TaskActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
                return true
            }
            R.id.action_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
                return true
            }
        }
        return false
    }
}
