package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.assignmate.databinding.ActivityProfileBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = "Profile"

        loadUserProfile()
        updateNotificationBadge()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.action_profile
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        val notificationBell = findViewById<ImageView>(R.id.notification_bell)
        notificationBell.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.aboutButton.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        binding.helpButton.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        binding.signOutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun loadUserProfile() {
        if (currentUserId.isNotEmpty()) {
            firebaseHelper.getUserDetails(currentUserId, { user ->
                if (user != null) {
                    binding.profileName.text = user.username
                    binding.profileEmail.text = user.email
                    if (user.profileImage.isNotEmpty()) {
                        binding.profileImage.imageTintList = null
                        Glide.with(this).load(user.profileImage).into(binding.profileImage)
                    } else {
                        binding.profileImage.setImageResource(R.drawable.ic_profile_user)
                        // Re-apply tint if necessary for default image, assuming #AAA39A as per xml
                         binding.profileImage.setColorFilter(android.graphics.Color.parseColor("#AAA39A"))
                    }
                }
            }, {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            })
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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
                intent.putExtra("SHOW_CREATE_DIALOG", true) // Inform GroupActivity to show the dialog
                startActivity(intent)
                return false // Do not select the item
            }
            R.id.action_tasks -> {
                val intent = Intent(this, TaskActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
                return true
            }
            R.id.action_profile -> {
                // Already here
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
        loadUserProfile()
    }
}
