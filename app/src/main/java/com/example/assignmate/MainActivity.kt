package com.example.assignmate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.adapter.UpcomingTasksAdapter
import com.example.assignmate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()

        currentUserId = intent.getStringExtra("USER_ID") ?: ""

        binding.notificationBell.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    // Already on the home screen, do nothing
                    true
                }
                R.id.action_groups -> {
                    val intent = Intent(this, GroupActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    true
                }
                R.id.action_create -> {
                    showCreateGroupDialog()
                    false
                }
                R.id.action_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUserInfo()
    }

    private fun updateUserInfo() {
        firebaseHelper.getUserDetails(currentUserId,
            onSuccess = {
                if (it != null) {
                    binding.welcomeMessage.text = "Welcome back, ${it.username}!"
                } else {
                    binding.welcomeMessage.text = "Welcome back, User!"
                }
            },
            onFailure = {
                binding.welcomeMessage.text = "Welcome back, User!"
            }
        )

        firebaseHelper.getGroupsForUser(currentUserId,
            onSuccess = { binding.totalGroups.text = it.size.toString() },
            onFailure = { binding.totalGroups.text = "0" }
        )

        firebaseHelper.getTotalTasksForUser(currentUserId,
            onSuccess = { binding.totalTasks.text = it.toString() },
            onFailure = { binding.totalTasks.text = "0" }
        )

        firebaseHelper.getPendingTasksForUser(currentUserId,
            onSuccess = { binding.pendingTasks.text = it.toString() },
            onFailure = { binding.pendingTasks.text = "0" }
        )

        firebaseHelper.getUpcomingTasksForUser(currentUserId,
            onSuccess = {
                if (it.isEmpty()) {
                    binding.upcomingDeadlinesRecyclerView.visibility = View.GONE
                    binding.noUpcomingDeadlinesText.visibility = View.VISIBLE
                } else {
                    binding.upcomingDeadlinesRecyclerView.visibility = View.VISIBLE
                    binding.noUpcomingDeadlinesText.visibility = View.GONE
                    binding.upcomingDeadlinesRecyclerView.adapter = UpcomingTasksAdapter(it) { task ->
                        val intent = Intent(this, TaskDetailActivity::class.java)
                        intent.putExtra("TASK_ID", task.uid)
                        intent.putExtra("USER_ID", currentUserId)
                        startActivity(intent)
                    }
                }
            },
            onFailure = {
                binding.upcomingDeadlinesRecyclerView.visibility = View.GONE
                binding.noUpcomingDeadlinesText.visibility = View.VISIBLE
            }
        )
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
                        updateUserInfo()
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

    private fun generateGroupCode(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
