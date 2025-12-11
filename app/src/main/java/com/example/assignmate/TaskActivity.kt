package com.example.assignmate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.databinding.ActivityTaskBinding
import com.example.assignmate.model.Task

class TaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // Will implement this after creating the GroupedTaskAdapter
    }
}
