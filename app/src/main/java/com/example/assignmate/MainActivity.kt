package com.example.assignmate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.assignmate.adapter.FavouriteGroupAdapter
import com.example.assignmate.adapter.UpcomingItem
import com.example.assignmate.adapter.UpcomingTasksAdapter
import com.example.assignmate.databinding.ActivityMainBinding
import com.example.assignmate.model.Group
import com.example.assignmate.model.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = ""
    private var selectedImageUri: Uri? = null
    private var currentGroupImagePreview: ImageView? = null
    private var currentRemoveImageButton: ImageButton? = null
    private var currentUploadImageButton: TextView? = null
    private var notificationListener: ListenerRegistration? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            currentGroupImagePreview?.let {
                Glide.with(this).load(uri).into(it)
            }
            currentRemoveImageButton?.visibility = View.VISIBLE
            currentUploadImageButton?.text = "Change Group Photo"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()
        notificationHelper = NotificationHelper()
        auth = FirebaseAuth.getInstance()
        // Use FirebaseAuth as primary source, fallback to Intent
        currentUserId = auth.currentUser?.uid ?: intent.getStringExtra("USER_ID") ?: ""

        if (currentUserId.isNotEmpty()) {
            notificationListener = notificationHelper.setupNotificationBadge(currentUserId, binding.notificationBadge)
        }

        binding.notificationBell.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
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
                R.id.action_tasks -> {
                    val intent = Intent(this, TaskActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    true
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

    override fun onDestroy() {
        super.onDestroy()
        notificationListener?.remove()
    }

    override fun onResume() {
        super.onResume()
        // Re-fetch current user ID just in case
        currentUserId = auth.currentUser?.uid ?: intent.getStringExtra("USER_ID") ?: ""
        updateUserInfo()
    }

    private fun updateUserInfo() {
        if (currentUserId.isEmpty()) return

        firebaseHelper.getUserDetails(currentUserId,
            onSuccess = {
                val username = it?.username ?: "User"
                val welcomeText = "Welcome back, $username!"
                val spannableString = SpannableString(welcomeText)
                
                // Calculate start and end indices of the username
                val startIndex = welcomeText.indexOf(username)
                val endIndex = startIndex + username.length

                // Apply the color span
                spannableString.setSpan(
                    ForegroundColorSpan(Color.parseColor("#6DBE45")),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Apply the bold style span
                spannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                binding.welcomeMessage.text = spannableString
            },
            onFailure = {
                binding.welcomeMessage.text = "Welcome back, User!"
            }
        )

        // Notification count is now handled by notificationListener in onCreate

        firebaseHelper.getGroupsForUser(currentUserId,
            onSuccess = { groups ->
                binding.totalGroups.text = groups.size.toString()
                val favouriteGroups = groups.filter { group -> group.favouriteBy.contains(currentUserId) }
                if (favouriteGroups.isEmpty()) {
                    binding.favouriteGroupsRecyclerView.visibility = View.GONE
                    binding.noFavouriteGroupText.visibility = View.VISIBLE
                } else {
                    val updatedFavouriteGroups = mutableListOf<Group>()
                    var processedCount = 0

                    for (group in favouriteGroups) {
                        firebaseHelper.getTasksForGroup(group.id, { tasks ->
                            val progress = calculateProgress(tasks)
                            updatedFavouriteGroups.add(group.copy(progress = progress))
                            processedCount++

                            if (processedCount == favouriteGroups.size) {
                                updatedFavouriteGroups.sortBy { it.name }
                                binding.favouriteGroupsRecyclerView.visibility = View.VISIBLE
                                binding.noFavouriteGroupText.visibility = View.GONE
                                binding.favouriteGroupsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                                binding.favouriteGroupsRecyclerView.adapter = FavouriteGroupAdapter(updatedFavouriteGroups) { group ->
                                    val intent = Intent(this, SingleGroupActivity::class.java)
                                    intent.putExtra("GROUP_ID", group.id)
                                    intent.putExtra("USER_ID", currentUserId)
                                    startActivity(intent)
                                }
                            }
                        }, {
                            updatedFavouriteGroups.add(group)
                            processedCount++
                            if (processedCount == favouriteGroups.size) {
                                updatedFavouriteGroups.sortBy { it.name }
                                binding.favouriteGroupsRecyclerView.visibility = View.VISIBLE
                                binding.noFavouriteGroupText.visibility = View.GONE
                                binding.favouriteGroupsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                                binding.favouriteGroupsRecyclerView.adapter = FavouriteGroupAdapter(updatedFavouriteGroups) { group ->
                                    val intent = Intent(this, SingleGroupActivity::class.java)
                                    intent.putExtra("GROUP_ID", group.id)
                                    intent.putExtra("USER_ID", currentUserId)
                                    startActivity(intent)
                                }
                            }
                        })
                    }
                }
            },
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

        firebaseHelper.getDueTasksForUser(currentUserId,
            onSuccess = { binding.dueTasks.text = it.toString() },
            onFailure = { binding.dueTasks.text = "0" }
        )

        firebaseHelper.getUpcomingTasksForUser(currentUserId, true,
            onSuccess = { leaderGroupTasks, userTasks ->
                val upcomingItems = mutableListOf<UpcomingItem>()
                
                leaderGroupTasks.forEach { (group, tasks) ->
                    if (tasks.isNotEmpty()) {
                        val earliestTask = tasks.minByOrNull { it.dueDate }
                        val earliestDueDate = earliestTask?.dueDate ?: 0L
                        upcomingItems.add(UpcomingItem.GroupSummaryItem(group, tasks.size, earliestDueDate))
                    }
                }
                
                userTasks.forEach { task ->
                    upcomingItems.add(UpcomingItem.TaskItem(task))
                }
                
                val sortedUserTasks = userTasks.sortedBy { it.dueDate }
                
                val finalItems = mutableListOf<UpcomingItem>()
                finalItems.addAll(upcomingItems.filterIsInstance<UpcomingItem.GroupSummaryItem>())
                finalItems.addAll(sortedUserTasks.map { UpcomingItem.TaskItem(it) })

                if (finalItems.isEmpty()) {
                    binding.upcomingDeadlinesRecyclerView.visibility = View.GONE
                    binding.noUpcomingDeadlinesText.visibility = View.VISIBLE
                } else {
                    binding.upcomingDeadlinesRecyclerView.visibility = View.VISIBLE
                    binding.noUpcomingDeadlinesText.visibility = View.GONE
                    binding.upcomingDeadlinesRecyclerView.layoutManager = LinearLayoutManager(this)
                    val adapter = UpcomingTasksAdapter(finalItems, {
                        task ->
                        val intent = Intent(this, SingleGroupActivity::class.java)
                        intent.putExtra("GROUP_ID", task.groupId)
                        intent.putExtra("USER_ID", currentUserId)
                        startActivity(intent)
                    }, {
                        group ->
                        val intent = Intent(this, SingleGroupActivity::class.java)
                        intent.putExtra("GROUP_ID", group.id)
                        intent.putExtra("USER_ID", currentUserId)
                        startActivity(intent)
                    }, { groupId, callback ->
                        firebaseHelper.getGroup(groupId, { group -> callback(group) }, { e: Exception -> })
                    })
                    binding.upcomingDeadlinesRecyclerView.adapter = adapter
                }
            },
            onFailure = {
                binding.upcomingDeadlinesRecyclerView.visibility = View.GONE
                binding.noUpcomingDeadlinesText.visibility = View.VISIBLE
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

    private fun showCreateGroupDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_group, null)
        builder.setView(view)

        val groupNameInput = view.findViewById<EditText>(R.id.group_name_input)
        val groupDescriptionInput = view.findViewById<EditText>(R.id.group_description_input)
        val groupCodeText = view.findViewById<TextView>(R.id.group_code_text)
        val copyCodeButton = view.findViewById<ImageButton>(R.id.copy_code_button)
        val joinGroupInsteadButton = view.findViewById<TextView>(R.id.join_group_instead_button)
        val uploadImageButton = view.findViewById<TextView>(R.id.upload_image_button)
        val groupImagePreview = view.findViewById<ImageView>(R.id.group_image_preview)
        val removeImageButton = view.findViewById<ImageButton>(R.id.remove_image_button)

        currentGroupImagePreview = groupImagePreview
        currentRemoveImageButton = removeImageButton
        currentUploadImageButton = uploadImageButton
        selectedImageUri = null

        uploadImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        groupImagePreview.setOnClickListener {
             pickImageLauncher.launch("image/*")
        }

        removeImageButton.setOnClickListener {
            selectedImageUri = null
            groupImagePreview.setImageResource(R.drawable.ic_group)
            removeImageButton.visibility = View.GONE
            uploadImageButton.text = "Upload Image Photo"
        }

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
             // Prevent auto-dismiss
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ -> dialog.dismiss() }

        joinGroupInsteadButton.setOnClickListener {
            dialog.dismiss()
            showJoinGroupDialog()
        }

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val groupName = groupNameInput.text.toString()
            val groupDescription = groupDescriptionInput.text.toString()
            val groupCode = groupCodeText.text.toString().substringAfter("Group Code: ")

            if (groupName.isNotEmpty() && groupCode.length == 6) {
                if (selectedImageUri != null) {
                    firebaseHelper.uploadFile(this, selectedImageUri!!, "group_images",
                        onSuccess = { imageUrl ->
                            createGroup(groupName, groupDescription, groupCode, imageUrl)
                            dialog.dismiss()
                        },
                        onFailure = {
                            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    createGroup(groupName, groupDescription, groupCode, "")
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createGroup(name: String, description: String, code: String, imageUrl: String) {
        firebaseHelper.createGroup(name, description, currentUserId, code, imageUrl,
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
