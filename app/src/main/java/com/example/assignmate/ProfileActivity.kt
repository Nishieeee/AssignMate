package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private var currentUserId: String = ""
    private lateinit var firebaseHelper: FirebaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firebaseHelper = FirebaseHelper()
        currentUserId = intent.getStringExtra("USER_ID") ?: ""

        firebaseHelper.getUserDetails(currentUserId,
            onSuccess = {
                val profileName = findViewById<TextView>(R.id.profile_name)
                val profileEmail = findViewById<TextView>(R.id.profile_email)

                if (it != null) {
                    profileName.text = it.username
                    profileEmail.text = it.email
                } else {
                    profileName.text = "User Not Found"
                    profileEmail.text = ""
                }
            },
            onFailure = {}
        )

        val signOutButton = findViewById<Button>(R.id.sign_out_button)
        signOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.action_profile

        val notificationBell = findViewById<ImageView>(R.id.notification_bell)
        notificationBell.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val notificationBadge = findViewById<TextView>(R.id.notification_badge)
        firebaseHelper.getUnreadNotificationCount(currentUserId,
            onSuccess = {
                if (it > 0) {
                    notificationBadge.visibility = View.VISIBLE
                    notificationBadge.text = it.toString()
                } else {
                    notificationBadge.visibility = View.GONE
                }
            },
            onFailure = {}
        )
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_profile) {
            return true
        }

        val intent = when (item.itemId) {
            R.id.action_home -> Intent(this, MainActivity::class.java)
            R.id.action_groups -> Intent(this, GroupActivity::class.java)
            else -> null
        }
        intent?.putExtra("USER_ID", currentUserId)
        startActivity(intent)
        return true
    }
}
