package com.example.assignmate

import android.app.Activity
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.adapter.CommentAdapter
import com.example.assignmate.adapter.SelectableLabelAdapter
import com.example.assignmate.adapter.SubtaskAdapter
import com.example.assignmate.databinding.ActivityTaskDetailBinding
import com.example.assignmate.model.Comment
import com.example.assignmate.model.Label
import com.example.assignmate.model.Notification
import com.example.assignmate.model.Subtask
import com.example.assignmate.model.Task
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import yuku.ambilwarna.AmbilWarnaDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var taskId: String
    private lateinit var currentUserId: String
    private var originalTask: Task? = null
    private var modifiedTask: Task? = null
    private var currentUserRole: String? = null

    private lateinit var subtaskAdapter: SubtaskAdapter
    private var hasUnsavedChanges = false
    private var taskUpdated = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()
        notificationHelper = NotificationHelper(this)
        taskId = intent.getStringExtra("TASK_ID") ?: ""
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        if (taskId.isEmpty()) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadTask()
        loadComments()
        setupListeners()
    }

    private fun loadTask() {
        firebaseHelper.getTask(taskId,
            onSuccess = { task ->
                if (task != null) {
                    originalTask = task
                    modifiedTask = task.copy()
                    firebaseHelper.getGroup(task.groupId,
                        onSuccess = { group ->
                            currentUserRole = group?.members?.get(currentUserId)
                            updateUI()
                        },
                        onFailure = {
                            updateUI() // still update UI even if group fails to load
                        }
                    )
                } else {
                    Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            onFailure = {
                Toast.makeText(this, "Failed to load task", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun updateUI() {
        binding.taskTitleInput.setText(originalTask?.name)
        binding.taskDescriptionInput.setText(originalTask?.description)
        binding.groupName.text = originalTask?.groupName

        // Setup Status Dropdown
        val statusOptions = arrayOf("Not Started", "In progress", "Complete")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statusOptions)
        binding.statusDropdown.setAdapter(adapter)
        binding.statusDropdown.setText(originalTask?.status, false)
        originalTask?.status?.let { setStatusColor(it) }

        val canManageTask = currentUserRole == "leader" || currentUserRole == "co-leader"
        binding.taskTitleInput.isFocusable = canManageTask
        binding.taskTitleInput.isFocusableInTouchMode = canManageTask
        binding.taskDescriptionInput.isFocusable = canManageTask
        binding.taskDescriptionInput.isFocusableInTouchMode = canManageTask
        binding.statusDropdown.isEnabled = canManageTask
        binding.addAssigneeIcon.visibility = if (canManageTask) View.VISIBLE else View.GONE
        binding.addLabelIcon.visibility = if (canManageTask) View.VISIBLE else View.GONE
        binding.addSubtaskButton.visibility = if (canManageTask) View.VISIBLE else View.GONE
        binding.addSubtaskButton.isEnabled = canManageTask

        if (originalTask?.dueDate != 0L) {
            originalTask?.dueDate?.let {
                binding.dueDateInput.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it))
            }
        } else {
            binding.dueDateInput.setText("")
        }

        updateAssignedMembersChips()
        updateLabelChips()
        setupSubtasksRecyclerView()
    }

    private fun updateAssignedMembersChips() {
        binding.assignedMembersChipGroup.removeAllViews()
        originalTask?.let {
            firebaseHelper.getGroupMembers(it.groupId,
                onSuccess = { members ->
                    val assignedMembers = members.filter { member -> modifiedTask?.assignedTo?.contains(member.id) == true }
                    for (member in assignedMembers) {
                        val chip = Chip(this)
                        chip.text = member.name
                        chip.isCloseIconVisible = true
                        chip.setOnCloseIconClickListener {
                            (modifiedTask?.assignedTo as? MutableList)?.remove(member.id)
                            updateAssignedMembersChips()
                            checkForChanges()
                        }
                        binding.assignedMembersChipGroup.addView(chip)
                    }
                },
                onFailure = {}
            )
        }
    }

    private fun updateLabelChips() {
        binding.labelsChipGroup.removeAllViews()
        originalTask?.groupId?.let {
            firebaseHelper.getLabelsForGroup(it, {
                val selectedLabels = it.filter { label -> modifiedTask?.labels?.contains(label.id) == true }
                for (label in selectedLabels) {
                    val chip = Chip(this)
                    chip.text = label.name
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(label.color))
                    chip.isCloseIconVisible = true
                    chip.setOnCloseIconClickListener {
                        (modifiedTask?.labels as? MutableList)?.remove(label.id)
                        updateLabelChips()
                        checkForChanges()
                    }
                    binding.labelsChipGroup.addView(chip)
                }
            }, {})
        }
    }

    private fun setupSubtasksRecyclerView() {
        subtaskAdapter = SubtaskAdapter(modifiedTask?.subtasks as MutableList<Subtask>) {
            checkForChanges()
        }
        binding.subtasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.subtasksRecyclerView.adapter = subtaskAdapter
    }

    private fun setupListeners() {
        binding.taskTitleInput.addTextChangedListener(textWatcher)
        binding.taskDescriptionInput.addTextChangedListener(textWatcher)

        binding.statusDropdown.setOnItemClickListener { _, _, position, _ ->
            val newStatus = (binding.statusDropdown.adapter.getItem(position)) as String
            modifiedTask = modifiedTask?.copy(status = newStatus)
            setStatusColor(newStatus)
            checkForChanges()
        }

        binding.dueDateInput.setOnClickListener {
            if (currentUserRole == "leader" || currentUserRole == "co-leader") {
                showDatePickerDialog()
            }
        }

        binding.assignToLayout.setOnClickListener {
            if (currentUserRole == "leader" || currentUserRole == "co-leader") {
                showEditAssignmentsDialog()
            }
        }

        binding.addAssigneeIcon.setOnClickListener {
            if (currentUserRole == "leader" || currentUserRole == "co-leader") {
                showEditAssignmentsDialog()
            }
        }

        binding.addLabelLayout.setOnClickListener {
            if (currentUserRole == "leader" || currentUserRole == "co-leader") {
                showLabelDialog()
            }
        }

        binding.addLabelIcon.setOnClickListener {
            if (currentUserRole == "leader" || currentUserRole == "co-leader") {
                showLabelDialog()
            }
        }

        binding.addSubtaskButton.setOnClickListener {
            showAddSubtaskDialog()
        }

        binding.addCommentButton.setOnClickListener {
            val commentText = binding.commentInput.text.toString()
            if (commentText.isNotEmpty()) {
                val comment = Comment(
                    taskId = taskId,
                    userId = currentUserId,
                    commentText = commentText,
                    timestamp = System.currentTimeMillis()
                )
                firebaseHelper.addComment(comment,
                    onSuccess = { 
                        loadComments()
                        binding.commentInput.text?.clear()
                        val notificationMessage = "New comment on task: ${originalTask?.name}"
                        originalTask?.assignedTo?.forEach { userId ->
                            if (userId != currentUserId) { // Don't notify the user who commented
                                notificationHelper.sendNotification(Notification(userId = userId, message = notificationMessage, taskId = taskId), userId.hashCode())
                            }
                        }
                    },
                    onFailure = {
                        Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            checkForChanges()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun checkForChanges() {
        val currentTitle = binding.taskTitleInput.text.toString()
        val currentDescription = binding.taskDescriptionInput.text.toString()

        hasUnsavedChanges = originalTask?.name != currentTitle ||
                originalTask?.description != currentDescription ||
                originalTask?.status != modifiedTask?.status ||
                originalTask?.dueDate != modifiedTask?.dueDate ||
                originalTask?.assignedTo?.toSet() != modifiedTask?.assignedTo?.toSet() ||
                originalTask?.labels?.toSet() != modifiedTask?.labels?.toSet() ||
                originalTask?.subtasks != modifiedTask?.subtasks

        invalidateOptionsMenu()
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        if (modifiedTask?.dueDate != null && modifiedTask!!.dueDate != 0L) {
            calendar.timeInMillis = modifiedTask!!.dueDate
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, {
            _, selectedYear, selectedMonth, selectedDay ->
            val newDueDateCalendar = Calendar.getInstance()
            newDueDateCalendar.set(selectedYear, selectedMonth, selectedDay)
            modifiedTask = modifiedTask?.copy(dueDate = newDueDateCalendar.timeInMillis)
            binding.dueDateInput.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(modifiedTask!!.dueDate))
            checkForChanges()
        }, year, month, day)

        datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Clear") { _, _ ->
            modifiedTask = modifiedTask?.copy(dueDate = 0L)
            binding.dueDateInput.setText("")
            checkForChanges()
        }

        datePickerDialog.show()
    }

    private fun showEditAssignmentsDialog() {
        originalTask?.let {
            firebaseHelper.getGroupMembers(it.groupId,
                onSuccess = { members ->
                    val memberNames = members.map { it.name }.toTypedArray()
                    val selectedMembers = BooleanArray(members.size) {
                        modifiedTask?.assignedTo?.contains(members[it].id) == true
                    }

                    AlertDialog.Builder(this)
                        .setTitle("Assign Members")
                        .setMultiChoiceItems(memberNames, selectedMembers) { _, which, isChecked ->
                            selectedMembers[which] = isChecked
                        }
                        .setPositiveButton("OK") { _, _ ->
                            val newAssignedTo = mutableListOf<String>()
                            for (i in selectedMembers.indices) {
                                if (selectedMembers[i]) {
                                    newAssignedTo.add(members[i].id)
                                }
                            }
                            modifiedTask = modifiedTask!!.copy(assignedTo = newAssignedTo)
                            updateAssignedMembersChips()
                            checkForChanges()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                },
                onFailure = {}
            )
        }
    }

    private fun showLabelDialog() {
        originalTask?.groupId?.let {
            firebaseHelper.getLabelsForGroup(it, {
                val view = layoutInflater.inflate(R.layout.dialog_select_label, null)
                val recyclerView = view.findViewById<RecyclerView>(R.id.labels_recycler_view)
                val addLabelButton = view.findViewById<View>(R.id.add_label_button)

                val dialog = AlertDialog.Builder(this)
                    .setView(view)
                    .setPositiveButton("OK") { _, _ ->
                        modifiedTask = modifiedTask?.copy(labels = (recyclerView.adapter as SelectableLabelAdapter).selectedLabelIds.toList())
                        updateLabelChips()
                        checkForChanges()
                    }
                    .setNegativeButton("Cancel", null)
                    .create()

                val labelAdapter = SelectableLabelAdapter(it, modifiedTask?.labels?.toMutableSet() ?: mutableSetOf()) {
                    dialog.dismiss()
                    showAddEditLabelDialog(null) { showLabelDialog() }
                }
                recyclerView.adapter = labelAdapter
                recyclerView.layoutManager = LinearLayoutManager(this)

                addLabelButton.setOnClickListener {
                    dialog.dismiss()
                    showAddEditLabelDialog(null) { showLabelDialog() }
                }

                dialog.show()
            }, {})
        }
    }

    private fun showAddEditLabelDialog(label: Label?, onLabelAdded: () -> Unit) {
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
                    originalTask?.groupId?.let {
                        firebaseHelper.addLabel(Label(name = labelName, color = colorString, groupId = it), { _ ->
                            onLabelAdded()
                        }, {})
                    }
                } else {
                    firebaseHelper.updateLabel(label.id, labelName, colorString, {
                        onLabelAdded()
                    }, {})
                }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showAddSubtaskDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_subtask, null)
        builder.setView(view)

        val subtaskNameInput = view.findViewById<EditText>(R.id.subtask_name_input)

        builder.setPositiveButton("Add") { _, _ ->
            val subtaskName = subtaskNameInput.text.toString()

            if (subtaskName.isNotEmpty()) {
                val subtask = Subtask(name = subtaskName)
                (modifiedTask?.subtasks as? MutableList)?.add(subtask)
                subtaskAdapter.addSubtask(subtask)
                checkForChanges()
            } else {
                Toast.makeText(this, "Please enter a subtask name", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveChanges() {
        val newTitle = binding.taskTitleInput.text.toString()
        val newDescription = binding.taskDescriptionInput.text.toString()

        if (newTitle.isEmpty()) {
            Toast.makeText(this, "Task title cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        modifiedTask?.let {
            firebaseHelper.updateTask(
                taskId,
                newTitle,
                newDescription,
                it.dueDate,
                it.status,
                it.assignedTo,
                it.subtasks,
                it.labels,
                onSuccess = {
                    taskUpdated = true
                    hasUnsavedChanges = false
                    Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show()

                    originalTask?.let { task ->
                        Log.d("TaskDetailActivity", "Task update successful. Preparing to send notifications for task: ${task.name}")
                        Log.d("TaskDetailActivity", "Assigned users: ${task.assignedTo}. Current user: $currentUserId")
                        val notificationMessage = "Task updated: ${task.name}"
                        task.assignedTo.forEach { userId ->
                            if (userId != currentUserId) {
                                Log.d("TaskDetailActivity", "Sending notification to user: $userId")
                                notificationHelper.sendNotification(Notification(userId = userId, message = notificationMessage, taskId = taskId), userId.hashCode())
                            }
                        }
                    }

                    finish()
                },
                onFailure = {
                    Toast.makeText(this, "Failed to save changes", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun deleteTask() {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ ->
                firebaseHelper.deleteTask(taskId,
                    onSuccess = {
                        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
                        taskUpdated = true
                        finish()
                    },
                    onFailure = {
                        Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadComments() {
        firebaseHelper.getCommentsForTask(taskId,
            onSuccess = { comments ->
                binding.commentsRecyclerView.layoutManager = LinearLayoutManager(this)
                binding.commentsRecyclerView.adapter = CommentAdapter(comments)
            },
            onFailure = {}
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.task_detail_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val saveMenuItem = menu?.findItem(R.id.action_save_task)
        saveMenuItem?.isVisible = hasUnsavedChanges
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save_task -> {
                saveChanges()
                true
            }
            R.id.action_delete_task -> {
                deleteTask()
                true
            }
            android.R.id.home -> {
                if (hasUnsavedChanges) {
                    showUnsavedChangesDialog()
                } else {
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun finish() {
        if (taskUpdated) {
            setResult(Activity.RESULT_OK)
        }
        super.finish()
    }
    
    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    private fun setStatusColor(status: String) {
        val colorRes = when (status) {
            "Not Started" -> R.color.status_not_started
            "In progress" -> R.color.status_in_progress
            "Complete" -> R.color.status_completed
            else -> android.R.color.black
        }
        binding.statusDropdown.setTextColor(ContextCompat.getColor(this, colorRes))
    }
}
