package com.example.assignmate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.adapter.TaskAdapter
import com.example.assignmate.model.Task

class GroupTasksFragment : Fragment() {

    private lateinit var firebaseHelper: FirebaseHelper
    private var groupId: String = ""

    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter

    private val taskDetailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadTasks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getString(ARG_GROUP_ID) ?: ""
        }
        firebaseHelper = FirebaseHelper()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_tasks, container, false)
        tasksRecyclerView = view.findViewById(R.id.tasks_recycler_view)
        tasksRecyclerView.layoutManager = LinearLayoutManager(context)

        loadTasks()

        return view
    }

    private fun loadTasks() {
        val currentUserId = (activity as? SingleGroupActivity)?.intent?.getStringExtra("USER_ID") ?: ""
        firebaseHelper.getTasksForGroup(groupId,
            onSuccess = {
                taskAdapter = TaskAdapter(it, currentUserId,
                    onItemClicked = { task ->
                        val intent = Intent(requireContext(), TaskDetailActivity::class.java).apply {
                            putExtra("TASK_ID", task.uid)
                            putExtra("USER_ID", currentUserId)
                        }
                        taskDetailLauncher.launch(intent)
                    },
                    onDeleteClicked = { task ->
                        showDeleteConfirmationDialog(task)
                    })
                tasksRecyclerView.adapter = taskAdapter
            },
            onFailure = {
                Toast.makeText(requireContext(), "Failed to load tasks", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showDeleteConfirmationDialog(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ ->
                firebaseHelper.deleteTask(task.uid,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                        loadTasks()
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Failed to delete task", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val ARG_GROUP_ID = "GROUP_ID"

        @JvmStatic
        fun newInstance(groupId: String) =
            GroupTasksFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_GROUP_ID, groupId)
                }
            }
    }
}
