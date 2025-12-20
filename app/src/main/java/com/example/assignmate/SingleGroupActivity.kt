package com.example.assignmate

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.example.assignmate.adapter.LabelAdapter
import com.example.assignmate.adapter.SelectableLabelAdapter
import com.example.assignmate.adapter.SubtaskAdapter
import com.example.assignmate.databinding.ActivitySingleGroupBinding
import com.example.assignmate.model.Label
import com.example.assignmate.model.Subtask
import com.example.assignmate.model.Task
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayoutMediator
import yuku.ambilwarna.AmbilWarnaDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SingleGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingleGroupBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var notificationHelper: NotificationHelper
    private var groupId: String = ""
    private var groupName: String = ""
    private var currentUserId: String = ""
    private var currentUserRole: String? = null
    private var allTasks = listOf<Task>()
    private var currentFilterStatus = "All"
    private var currentSearchQuery = ""
    
    private var selectedImageUri: Uri? = null
    private var currentGroupImagePreview: ImageView? = null
    private var currentRemoveImageButton: ImageButton? = null
    private var currentUploadImageButton: TextView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                selectedImageUri = uri
                currentGroupImagePreview?.let {
                    Glide.with(this).load(uri).into(it)
                }
                currentRemoveImageButton?.visibility = View.VISIBLE
                currentUploadImageButton?.text = "Change Group Photo"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()
        notificationHelper = NotificationHelper(this)
        groupId = intent.getStringExtra("GROUP_ID") ?: ""
        currentUserId = intent.getStringExtra("USER_ID") ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firebaseHelper.getGroup(groupId,
            onSuccess = {
                if (it != null) {
                    supportActionBar?.title = it.name
                    groupName = it.name
                    currentUserRole = it.members[currentUserId]
                    if (currentUserRole == "leader" || currentUserRole == "co-leader") {
                        binding.fabAddTaskButton.visibility = View.VISIBLE
                    }
                    invalidateOptionsMenu()
                }
            },
            onFailure = {
                Toast.makeText(this, "Failed to load group details", Toast.LENGTH_SHORT).show()
            }
        )

        binding.fabAddTaskButton.setOnClickListener {
            showCreateTaskDialog()
        }

        val viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Group Tasks"
                1 -> "Members"
                else -> null
            }
        }.attach()

        setupFilter()
        loadTasks()
    }

    private fun setupFilter() {
        // Search Input Logic
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s.toString()
                filterTasks()
            }
        })

        // Dropdown Logic
        val filterOptions = arrayOf("All", "Not Started", "In progress", "Complete")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filterOptions)
        binding.filterDropdown.setAdapter(adapter)
        binding.filterDropdown.setOnItemClickListener { _, _, position, _ ->
            currentFilterStatus = filterOptions[position]
            filterTasks()
        }
    }

    private fun filterTasks() {
        val fragment = supportFragmentManager.fragments.find { it is GroupTasksFragment } as? GroupTasksFragment
        
        val filteredByStatus = if (currentFilterStatus == "All") {
            allTasks
        } else {
            allTasks.filter { it.status == currentFilterStatus }
        }

        val finalFilteredList = if (currentSearchQuery.isEmpty()) {
            filteredByStatus
        } else {
            filteredByStatus.filter { 
                it.name.contains(currentSearchQuery, ignoreCase = true) || 
                it.description.contains(currentSearchQuery, ignoreCase = true) 
            }
        }

        fragment?.displayTasks(finalFilteredList)
    }

    fun loadTasks() {
        firebaseHelper.getTasksForGroup(groupId, {
            allTasks = it
            filterTasks()
        }, {})
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
        menu?.findItem(R.id.action_manage_labels)?.isVisible = canManageGroup
        menu?.findItem(R.id.action_add_members)?.isVisible = canManageGroup
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_edit_group -> showEditGroupDialog()
            R.id.action_delete_group -> showDeleteGroupDialog()
            R.id.action_add_members -> showAddMembersDialog()
            R.id.action_manage_labels -> showManageLabelsDialog()
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
        
        joinGroupInsteadButton.visibility = View.GONE

        firebaseHelper.getGroup(groupId,
            onSuccess = {
                if (it != null) {
                    groupNameInput.setText(it.name)
                    groupDescriptionInput.setText(it.description)
                    groupCodeText.text = "Group Code: ${it.code}"
                    copyCodeButton.visibility = View.VISIBLE
                    
                    if (it.profileImage.isNotEmpty()) {
                        Glide.with(this).load(it.profileImage).into(groupImagePreview)
                        removeImageButton.visibility = View.VISIBLE
                        uploadImageButton.text = "Change Group Photo"
                    } else {
                        removeImageButton.visibility = View.GONE
                        uploadImageButton.text = "Upload Image Photo"
                    }
                }
            },
            onFailure = {}
        )
        
        uploadImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }
        
        groupImagePreview.setOnClickListener {
             val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
             pickImageLauncher.launch(intent)
        }
        
        removeImageButton.setOnClickListener {
            selectedImageUri = null
            groupImagePreview.setImageResource(R.drawable.ic_group)
            removeImageButton.visibility = View.GONE
            uploadImageButton.text = "Upload Image Photo"
            groupImagePreview.tag = "removed"
        }

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        val dialog = builder.create()
        dialog.show()
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newGroupName = groupNameInput.text.toString()
            val newGroupDescription = groupDescriptionInput.text.toString()
            
            if (newGroupName.isNotEmpty()) {
                val isImageRemoved = groupImagePreview.tag == "removed"
                
                firebaseHelper.getGroup(groupId, onSuccess = { group ->
                    if (group != null) {
                        if (selectedImageUri != null) {
                            firebaseHelper.uploadFile(this, selectedImageUri!!, "group_images",
                                onSuccess = { imageUrl ->
                                    updateGroup(groupId, newGroupName, newGroupDescription, imageUrl)
                                    dialog.dismiss()
                                },
                                onFailure = {
                                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else if (isImageRemoved) {
                            updateGroup(groupId, newGroupName, newGroupDescription, "")
                            dialog.dismiss()
                        } else {
                            updateGroup(groupId, newGroupName, newGroupDescription, group.profileImage)
                            dialog.dismiss()
                        }
                    }
                }, onFailure = {
                     Toast.makeText(this, "Failed to fetch group info", Toast.LENGTH_SHORT).show()
                })
            } else {
                 Toast.makeText(this, "Group name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateGroup(id: String, name: String, description: String, imageUrl: String) {
        firebaseHelper.updateGroup(id, name, description, imageUrl,
            onSuccess = {
                Toast.makeText(this, "Group updated successfully", Toast.LENGTH_SHORT).show()
                supportActionBar?.title = name
            },
            onFailure = {
                Toast.makeText(this, "Failed to update group", Toast.LENGTH_SHORT).show()
            }
        )
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

    private fun showAddMembersDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_member, null)
        builder.setView(view)

        val groupCodeText = view.findViewById<TextView>(R.id.group_code_text)
        firebaseHelper.getGroup(groupId, onSuccess = {
            if (it != null) {
                groupCodeText.text = "Group Code: ${it.code}"
            }
        }, onFailure = {})

        view.findViewById<View>(R.id.copy_icon).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val code = groupCodeText.text.toString().substringAfter("Group Code: ")
            val clip = ClipData.newPlainText("Group Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Group code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        val emailInput = view.findViewById<EditText>(R.id.email_input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val email = emailInput.text.toString()
            if (email.isNotEmpty()) {
                firebaseHelper.getUserByEmail(email, onSuccess = { user ->
                    if (user != null) {
                        firebaseHelper.updateMemberRole(groupId, user.id, "member",
                            onSuccess = {
                                Toast.makeText(this, "Member added successfully", Toast.LENGTH_SHORT).show()
                                val membersFragment = supportFragmentManager.fragments.find { it is MembersFragment } as? MembersFragment
                                membersFragment?.loadMembers()
                            },
                            onFailure = {
                                Toast.makeText(this, "Failed to add member", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }, onFailure = {
                    Toast.makeText(this, "Failed to find user", Toast.LENGTH_SHORT).show()
                })
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun showCreateTaskDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_task, null)
        builder.setView(view)

        // View references
        val taskNameInput = view.findViewById<EditText>(R.id.task_name_input)
        val taskDescriptionInput = view.findViewById<EditText>(R.id.task_description_input)
        val dueDateInput = view.findViewById<EditText>(R.id.due_date_input)
        val assignToLayout = view.findViewById<View>(R.id.assign_to_layout)
        val assignedMembersChipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.assigned_members_chip_group)
        val addLabelLayout = view.findViewById<View>(R.id.add_label_layout)
        val labelsChipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.labels_chip_group)
        val addSubtaskButton = view.findViewById<View>(R.id.add_subtask_button)
        val subtasksRecyclerView = view.findViewById<RecyclerView>(R.id.subtasks_recycler_view)

        // Data holders
        var dueDateMillis: Long = 0
        val assignedTo = mutableListOf<String>()
        val selectedLabelIds = mutableSetOf<String>()
        val subtasks = mutableListOf<Subtask>()

        // Setup Subtasks
        val subtaskAdapter = SubtaskAdapter(subtasks)

        subtasksRecyclerView.layoutManager = LinearLayoutManager(this)
        subtasksRecyclerView.adapter = subtaskAdapter

        dueDateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (dueDateMillis != 0L) {
                calendar.timeInMillis = dueDateMillis
            }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val newDueDateCalendar = Calendar.getInstance()
                newDueDateCalendar.set(selectedYear, selectedMonth, selectedDay)
                dueDateMillis = newDueDateCalendar.timeInMillis
                dueDateInput.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dueDateMillis))
            }, year, month, day)

            datePickerDialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Clear") { _, _ ->
                dueDateMillis = 0L
                dueDateInput.setText("")
            }
            datePickerDialog.show()
        }

        assignToLayout.setOnClickListener {
            firebaseHelper.getGroupMembers(groupId, onSuccess = { members ->
                val memberNames = members.map { it.name }.toTypedArray()
                val selectedItems = BooleanArray(members.size) { i -> assignedTo.contains(members[i].id) }

                AlertDialog.Builder(this)
                    .setTitle("Assign Members")
                    .setMultiChoiceItems(memberNames, selectedItems) { _, which, isChecked ->
                        val memberId = members[which].id
                        if (isChecked) {
                            if (!assignedTo.contains(memberId)) assignedTo.add(memberId)
                        } else {
                            assignedTo.remove(memberId)
                        }
                    }
                    .setPositiveButton("OK") { _, _ ->
                        assignedMembersChipGroup.removeAllViews()
                        firebaseHelper.getUsers(assignedTo, onSuccess = { users ->
                            for (user in users) {
                                val chip = Chip(this)
                                chip.text = user.username
                                chip.isCloseIconVisible = true
                                chip.setOnCloseIconClickListener {
                                    assignedMembersChipGroup.removeView(chip)
                                    assignedTo.remove(user.id)
                                }
                                assignedMembersChipGroup.addView(chip)
                            }
                        }, onFailure = {})
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }, onFailure = {
                Toast.makeText(this, "Could not load members to assign", Toast.LENGTH_SHORT).show()
            })
        }

        addLabelLayout.setOnClickListener {
            showLabelDialog(selectedLabelIds) { labelIds ->
                selectedLabelIds.clear()
                selectedLabelIds.addAll(labelIds)
                
                labelsChipGroup.removeAllViews()
                firebaseHelper.getLabelsForGroup(groupId, {
                    val selectedLabels = it.filter { label -> selectedLabelIds.contains(label.id) }
                    for (label in selectedLabels) {
                        val chip = Chip(this)
                        chip.text = label.name
                        chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.parseColor(label.color))
                        chip.isCloseIconVisible = true
                        chip.setOnCloseIconClickListener {
                            labelsChipGroup.removeView(chip)
                            selectedLabelIds.remove(label.id)
                        }
                        labelsChipGroup.addView(chip)
                    }
                }, {})
            }
        }

        addSubtaskButton.setOnClickListener {
            val subtaskBuilder = AlertDialog.Builder(this)
            val subtaskView = layoutInflater.inflate(R.layout.dialog_add_subtask, null)
            subtaskBuilder.setView(subtaskView)
            val subtaskNameInput = subtaskView.findViewById<EditText>(R.id.subtask_name_input)

            subtaskBuilder.setPositiveButton("Add") { _, _ ->
                val subtaskName = subtaskNameInput.text.toString()
                if (subtaskName.isNotEmpty()) {
                    val newSubtask = Subtask(name = subtaskName)
                    subtaskAdapter.addSubtask(newSubtask)
                }
            }
                .setNegativeButton("Cancel", null)
                .show()
        }

        builder.setPositiveButton("Create") { _, _ ->
            val taskName = taskNameInput.text.toString()
            if (taskName.isEmpty()) {
                Toast.makeText(this, "Task title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val taskDescription = taskDescriptionInput.text.toString()
            val finalDueDate = if (dueDateMillis == 0L) 0L else dueDateMillis

            val task = Task(
                name = taskName,
                description = taskDescription,
                groupId = groupId,
                groupName = groupName,
                dueDate = finalDueDate,
                assignedTo = assignedTo,
                status = "Not Started",
                labels = selectedLabelIds.toList(),
                subtasks = subtasks
            )

            firebaseHelper.createTask(task,
                onSuccess = { taskId ->
                    Toast.makeText(this, "Task created successfully", Toast.LENGTH_SHORT).show()
                    loadTasks()
                    firebaseHelper.getGroup(groupId, {
                        val groupName = it?.name ?: "a group"
                        val notificationMessage = "You have been assigned a new task: $taskName in $groupName"
                        assignedTo.forEach { userId ->
                            notificationHelper.sendNotification(com.example.assignmate.model.Notification(userId = userId, message = notificationMessage, taskId = taskId), userId.hashCode())
                        }
                    }, {})
                },
                onFailure = {
                    Toast.makeText(this, "Failed to create task", Toast.LENGTH_SHORT).show()
                }
            )
        }
            .setNegativeButton("Cancel", null)

        builder.create().show()
    }

    private fun showLabelDialog(selectedLabelIds: MutableSet<String>, onLabelsSelected: (Set<String>) -> Unit) {
        firebaseHelper.getLabelsForGroup(groupId, { labels ->
            val view = layoutInflater.inflate(R.layout.dialog_select_label, null)
            val recyclerView = view.findViewById<RecyclerView>(R.id.labels_recycler_view)
            val addLabelButton = view.findViewById<View>(R.id.add_label_button)

            val dialog = AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("OK") { _, _ ->
                    onLabelsSelected((recyclerView.adapter as SelectableLabelAdapter).selectedLabelIds.toSet())
                }
                .setNegativeButton("Cancel", null)
                .create()

            val labelAdapter = SelectableLabelAdapter(labels, selectedLabelIds) {
                // Adapter selection logic
                dialog.dismiss()
                showAddEditLabelDialog(null) { newLabelId ->
                    if (newLabelId != null) {
                        selectedLabelIds.add(newLabelId)
                    }
                    showLabelDialog(selectedLabelIds, onLabelsSelected)
                }
            }
            recyclerView.adapter = labelAdapter
            recyclerView.layoutManager = LinearLayoutManager(this)

            addLabelButton.setOnClickListener {
                dialog.dismiss()
                showAddEditLabelDialog(null) { newLabelId ->
                    if (newLabelId != null) {
                        selectedLabelIds.add(newLabelId)
                    }
                    showLabelDialog(selectedLabelIds, onLabelsSelected)
                }
            }

            dialog.show()
        }, {})
    }

    private fun showManageLabelsDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_manage_labels, null)
        builder.setView(view)

        val labelsRecyclerView = view.findViewById<RecyclerView>(R.id.labels_recycler_view)
        val addLabelButton = view.findViewById<View>(R.id.add_label_button)

        labelsRecyclerView.layoutManager = LinearLayoutManager(this)

        fun loadLabels() {
            firebaseHelper.getLabelsForGroup(groupId, {
                val adapter = LabelAdapter(it.toMutableList(),
                    onEditClicked = { label -> showAddEditLabelDialog(label) { loadLabels() } },
                    onDeleteClicked = { label ->
                        firebaseHelper.deleteLabel(label.id, {
                            loadLabels()
                        }, {})
                    })
                labelsRecyclerView.adapter = adapter
            }, {})
        }

        loadLabels()

        addLabelButton.setOnClickListener {
            showAddEditLabelDialog(null) { loadLabels() }
        }

        builder.setPositiveButton("Close", null)
        builder.show()
    }

    private fun showAddEditLabelDialog(label: Label? = null, onLabelAdded: (String?) -> Unit) {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_label, null)
        builder.setView(view)

        val labelNameInput = view.findViewById<EditText>(R.id.label_name_input)
        val colorPreview = view.findViewById<View>(R.id.color_preview)
        var selectedColor = Color.LTGRAY

        if (label != null) {
            builder.setTitle("Edit Label")
            labelNameInput.setText(label.name)
            selectedColor = Color.parseColor(label.color)
        } else {
            builder.setTitle("Add Label")
        }
        colorPreview.setBackgroundColor(selectedColor)

        colorPreview.setOnClickListener {
            AmbilWarnaDialog(this, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {}
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    selectedColor = color
                    colorPreview.setBackgroundColor(selectedColor)
                }
            }).show()
        }

        builder.setPositiveButton(if (label == null) "Add" else "Save") { _, _ ->
            val labelName = labelNameInput.text.toString()
            if (labelName.isNotEmpty()) {
                val colorString = String.format("#%06X", 0xFFFFFF and selectedColor)
                if (label == null) {
                    firebaseHelper.addLabel(Label(name = labelName, color = colorString, groupId = groupId), { labelId ->
                        onLabelAdded(labelId)
                    }, {})
                } else {
                    firebaseHelper.updateLabel(label.id, labelName, colorString, {
                        onLabelAdded(null)
                    }, {})
                }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
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
