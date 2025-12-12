package com.example.assignmate

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.assignmate.databinding.ActivityTaskDetailsBinding
import com.example.assignmate.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailsBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private var taskId: String? = null
    private var task: Task? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()
        taskId = intent.getStringExtra("TASK_ID")

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadTaskDetails()

        binding.dueDateEditText.setOnClickListener {
            showDatePicker()
        }

        binding.saveButton.setOnClickListener {
            saveTaskDetails()
        }
    }

    private fun loadTaskDetails() {
        taskId?.let {
            firebaseHelper.getTask(it, 
                onSuccess = { task ->
                    this.task = task
                    task?.let { 
                        binding.taskTitleEditText.setText(task.name)
                        binding.taskDescriptionEditText.setText(task.description)
                        if (task.dueDate != 0L) {
                            calendar.timeInMillis = task.dueDate
                            updateDueDateInView()
                        }
                    }
                },
                onFailure = { 
                    Toast.makeText(this, "Failed to load task details", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDueDateInView()
        }

        DatePickerDialog(this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDueDateInView() {
        val myFormat = "MM/dd/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.dueDateEditText.setText(sdf.format(calendar.time))
    }

    private fun saveTaskDetails() {
        val title = binding.taskTitleEditText.text.toString()
        val description = binding.taskDescriptionEditText.text.toString()

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        task?.let {
            val updatedTask = it.copy(
                name = title,
                description = description,
                dueDate = calendar.timeInMillis
            )

            firebaseHelper.updateTask(it.uid, updatedTask.name, updatedTask.description, updatedTask.dueDate, updatedTask.status, updatedTask.assignedTo, updatedTask.subtasks, updatedTask.labels,
                onSuccess = {
                    Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = {
                    Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}