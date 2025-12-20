package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.adapter.GroupedTaskAdapter
import com.example.assignmate.databinding.ActivityTaskBinding
import com.example.assignmate.model.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class TaskActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityTaskBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: GroupedTaskAdapter
    private var currentUserId: String = ""
    private val allTasks = mutableListOf<Task>()
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: intent.getStringExtra("USER_ID") ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        setupRecyclerView()
        setupFilterAndSearch()
        loadTasks()

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.action_tasks
        bottomNavigation.setOnNavigationItemSelectedListener(this)

        val notificationBell = findViewById<ImageView>(R.id.notification_bell)
        notificationBell.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }
    }

    private fun setupFilterAndSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterAndDisplayTasks()
            }
        })

        val filterOptions = arrayOf("All", "In progress", "Completed")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filterOptions)
        binding.filterDropdown.setAdapter(adapter)
        binding.filterDropdown.setOnItemClickListener { _, _, position, _ ->
            currentFilter = filterOptions[position]
            filterAndDisplayTasks()
        }
    }

    private fun filterAndDisplayTasks() {
        val query = binding.searchInput.text.toString()

        val filteredByQuery = if (query.isEmpty()) {
            allTasks
        } else {
            allTasks.filter {
                it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }
        }

        val filteredByStatus = if (currentFilter == "All") {
            filteredByQuery
        } else {
            filteredByQuery.filter { it.status == currentFilter }
        }

        displayTasks(filteredByStatus)
    }

    private fun setupRecyclerView() {
        adapter = GroupedTaskAdapter(
            onTaskClicked = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java)
                intent.putExtra("TASK_ID", task.uid)
                startActivity(intent)
            },
            onStatusChanged = { task, newStatus ->
                updateTaskStatus(task, newStatus)
            }
        )
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tasksRecyclerView.adapter = adapter
    }

    private fun loadTasks() {
        if (currentUserId.isEmpty()) return
        
        binding.progressBar.visibility = View.VISIBLE
        firebaseHelper.getGroupsForUser(currentUserId, { groups ->
            val groupMap = groups.associateBy({ it.id }, { it.name })
            firebaseHelper.getAllTasksForUser(currentUserId, { tasks ->
                allTasks.clear()
                val updatedTasks = tasks.map { task ->
                    if (task.groupName.isEmpty() && groupMap.containsKey(task.groupId)) {
                        task.copy(groupName = groupMap[task.groupId] ?: "")
                    } else {
                        task
                    }
                }
                allTasks.addAll(updatedTasks)
                filterAndDisplayTasks()
            }, { e ->
                handleFailure(e)
            })
        }, {
            firebaseHelper.getAllTasksForUser(currentUserId, { tasks ->
                allTasks.clear()
                allTasks.addAll(tasks)
                filterAndDisplayTasks()
            }, { e ->
                handleFailure(e)
            })
        })
    }

    private fun handleFailure(exception: Exception) {
        binding.progressBar.visibility = View.GONE
        allTasks.clear()
        filterAndDisplayTasks()
        Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
    }

    private fun displayTasks(tasks: List<Task>) {
        binding.progressBar.visibility = View.GONE
        if (tasks.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.tasksRecyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.tasksRecyclerView.visibility = View.VISIBLE
            val groupedTasks = groupTasks(tasks)
            adapter.submitList(groupedTasks)
        }
    }

    private fun groupTasks(tasks: List<Task>): List<GroupedTaskAdapter.TaskListItem> {
        val listItems = mutableListOf<GroupedTaskAdapter.TaskListItem>()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tomorrowStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

        val overdueTasks = tasks.filter { it.dueDate != 0L && it.dueDate < todayStart.timeInMillis && it.status != "Completed" }
        val todayTasks = tasks.filter { it.dueDate >= todayStart.timeInMillis && it.dueDate < tomorrowStart.timeInMillis && it.status != "Completed" }
        val upcomingTasks = tasks.filter { it.dueDate >= tomorrowStart.timeInMillis && it.status != "Completed" }
        val noDateTasks = tasks.filter { it.dueDate == 0L && it.status != "Completed" }
        val completedTasks = tasks.filter { it.status == "Completed" }

        if (overdueTasks.isNotEmpty()) {
            listItems.add(GroupedTaskAdapter.TaskListItem.Header("Overdue"))
            listItems.addAll(overdueTasks.sortedBy { it.dueDate }.map { GroupedTaskAdapter.TaskListItem.TaskItem(it) })
        }
        if (todayTasks.isNotEmpty()) {
            listItems.add(GroupedTaskAdapter.TaskListItem.Header("Today"))
            listItems.addAll(todayTasks.sortedBy { it.dueDate }.map { GroupedTaskAdapter.TaskListItem.TaskItem(it) })
        }
        if (upcomingTasks.isNotEmpty()) {
            val sortedUpcoming = upcomingTasks.sortedBy { it.dueDate }
            val dateFormat = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
            val groupedByDate = sortedUpcoming.groupBy {
                dateFormat.format(java.util.Date(it.dueDate))
            }

            groupedByDate.forEach { (dateString, tasksForDate) ->
                listItems.add(GroupedTaskAdapter.TaskListItem.Header(dateString))
                listItems.addAll(tasksForDate.map { GroupedTaskAdapter.TaskListItem.TaskItem(it) })
            }
        }
        if (noDateTasks.isNotEmpty()) {
            listItems.add(GroupedTaskAdapter.TaskListItem.Header("No Date"))
            listItems.addAll(noDateTasks.map { GroupedTaskAdapter.TaskListItem.TaskItem(it) })
        }
        if (completedTasks.isNotEmpty()) {
            listItems.add(GroupedTaskAdapter.TaskListItem.Header("Completed"))
            listItems.addAll(completedTasks.sortedByDescending { it.dueDate }.map { GroupedTaskAdapter.TaskListItem.TaskItem(it) })
        }

        return listItems
    }

    private fun updateTaskStatus(task: Task, newStatus: String) {
        firebaseHelper.updateTask(task.uid, task.name, task.description, task.dueDate, newStatus, task.assignedTo, task.subtasks, task.labels,
            onSuccess = {
                Toast.makeText(this, "Task status updated", Toast.LENGTH_SHORT).show()
                loadTasks()
            },
            onFailure = {
                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateNotificationBadge() {
        val notificationBadge = findViewById<TextView>(R.id.notification_badge)
        if (currentUserId.isNotEmpty()) {
            firebaseHelper.getUnreadNotificationCount(currentUserId, {
                if (it > 0) {
                    notificationBadge.visibility = View.VISIBLE
                    notificationBadge.text = it.toString()
                } else {
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
                val intent = Intent(this, GroupActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
                return true
            }
            R.id.action_create -> {
                val intent = Intent(this, GroupActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                intent.putExtra("SHOW_CREATE_DIALOG", true)
                startActivity(intent)
                return false
            }
            R.id.action_tasks -> return true
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

    override fun onResume() {
        super.onResume()
        currentUserId = auth.currentUser?.uid ?: intent.getStringExtra("USER_ID") ?: ""
        loadTasks()
        updateNotificationBadge()
    }
}
