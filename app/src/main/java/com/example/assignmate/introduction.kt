package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth

class introduction : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var proceedButton: Button
    private lateinit var dots: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check authentication and first-run status
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val prefs = getSharedPreferences("AssignMatePrefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("isFirstRun", true)

        if (!isFirstRun) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_introduction)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewPager = findViewById(R.id.viewPager)
        proceedButton = findViewById(R.id.proceedButton)
        dots = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        )

        val items = listOf(
            IntroItem(
                imageRes = R.drawable.pic_1,
                title = "Together, Every\nTask is Manageable",
                body = "AssignMate makes group work easier. Create groups, assign tasks, set deadlines, and stay updated—all in one place. Teamwork has never been this organized."
            ),
            IntroItem(
                imageRes = R.drawable.pic_2,
                title = "Organize your groups effortlessly",
                body = "Create new groups or join existing ones using an invite link or code."
            ),
            IntroItem(
                imageRes = R.drawable.pic_3,
                title = "Assign tasks to the right people",
                body = "Add deadlines, descriptions, and assign tasks so everyone knows what to do."
            ),
            IntroItem(
                imageRes = R.drawable.pic_4,
                title = "Never miss a deadline",
                body = "Get reminders, see real-time updates, and keep your group on track."
            )
        )

        val adapter = IntroductionAdapter(items)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
            }
        })

        proceedButton.setOnClickListener {
            // Mark first run as completed
            prefs.edit().putBoolean("isFirstRun", false).apply()

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updateIndicators(position: Int) {
        dots.forEach { it.setImageResource(R.drawable.dot_inactive) }
        dots[position].setImageResource(R.drawable.dot_active)

        if (position == dots.size - 1) {
            proceedButton.visibility = View.VISIBLE
        } else {
            proceedButton.visibility = View.GONE
        }
    }
}