package com.example.assignmate

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.assignmate.databinding.ActivitySettingsBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseHelper: FirebaseHelper
    private var originalUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firebaseHelper = FirebaseHelper()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        loadUserSettings()

        binding.saveButton.isEnabled = false
        binding.saveButton.visibility = View.GONE

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val usernameChanged = binding.usernameEditText.text.toString().trim() != originalUsername
                val passwordChanged = binding.passwordEditText.text.toString().trim().isNotEmpty()
                binding.saveButton.isEnabled = usernameChanged || passwordChanged
                if (binding.saveButton.isEnabled) {
                    binding.saveButton.visibility = View.VISIBLE
                } else {
                    binding.saveButton.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.usernameEditText.addTextChangedListener(textWatcher)
        binding.passwordEditText.addTextChangedListener(textWatcher)

        binding.saveButton.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun loadUserSettings() {
        val user = auth.currentUser
        if (user != null) {
            binding.emailEditText.setText(user.email)
            firebaseHelper.getUserDetails(user.uid, {
                if (it != null) {
                    originalUsername = it.username
                    binding.usernameEditText.setText(it.username)
                }
            }, {})
        }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Save Changes")
            .setMessage("Are you sure you want to save these changes?")
            .setPositiveButton("Save") { _, _ ->
                saveChanges()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveChanges() {
        val user = auth.currentUser
        if (user != null) {
            val newUsername = binding.usernameEditText.text.toString().trim()
            val newPassword = binding.passwordEditText.text.toString().trim()

            if (newUsername != originalUsername) {
                firebaseHelper.updateUsername(user.uid, newUsername, {}, {})
            }

            if (newPassword.isNotEmpty()) {
                user.updatePassword(newPassword)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
