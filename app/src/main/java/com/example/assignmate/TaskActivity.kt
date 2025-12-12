package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.assignmate.databinding.ActivityTaskBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class TaskActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityTaskBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        updateNotificationBadge()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.action_tasks
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        val notificationBell = findViewById<ImageView>(R.id.notification_bell)
        notificationBell.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }
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
            R.id.action_tasks -> {
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

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
    }
}
