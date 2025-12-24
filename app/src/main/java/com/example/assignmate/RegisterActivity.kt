package com.example.assignmate

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.assignmate.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private var selectedImageUri: Uri? = null
    private var profileImageView: ImageView? = null
    private var saveButton: Button? = null
    private var laterButton: Button? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            profileImageView?.let {
                Glide.with(this).load(uri).into(it)
            }
            saveButton?.visibility = View.VISIBLE
            laterButton?.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()

        binding.registerButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show profile upload dialog before registering
            showProfileUploadDialog(username, email, password)
        }

        binding.loginText.setOnClickListener {
            // Go back to the login screen
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showProfileUploadDialog(username: String, email: String, pass: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_upload, null)
        profileImageView = dialogView.findViewById(R.id.dialog_profile_image)
        val tapText = dialogView.findViewById<TextView>(R.id.dialog_tap_to_change)
        laterButton = dialogView.findViewById(R.id.dialog_btn_later)
        saveButton = dialogView.findViewById(R.id.dialog_btn_save)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        profileImageView?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        
        tapText.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        laterButton?.setOnClickListener {
            dialog.dismiss()
            performRegistration(username, email, pass, null)
        }

        saveButton?.setOnClickListener {
            dialog.dismiss()
            if (selectedImageUri != null) {
                performRegistration(username, email, pass, selectedImageUri)
            } else {
                performRegistration(username, email, pass, null)
            }
        }

        dialog.show()
    }

    private fun performRegistration(username: String, email: String, pass: String, imageUri: Uri?) {
        firebaseHelper.registerUser(username, email, pass,
            onSuccess = {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null && imageUri != null) {
                     firebaseHelper.uploadFile(this, imageUri, "profile_images",
                        onSuccess = { imageUrl ->
                            firebaseHelper.updateUserProfile(userId, username, imageUrl, 
                                onSuccess = {
                                    finishRegistration()
                                },
                                onFailure = {
                                    Toast.makeText(this, "Failed to save profile photo but account created.", Toast.LENGTH_LONG).show()
                                    finishRegistration()
                                }
                            )
                        },
                        onFailure = {
                            Toast.makeText(this, "Failed to upload image but account created.", Toast.LENGTH_LONG).show()
                             finishRegistration()
                        }
                    )
                } else {
                    finishRegistration()
                }
            },
            onFailure = {
                Toast.makeText(this, "Registration failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun finishRegistration() {
        // Sign out the user immediately after registration
        FirebaseAuth.getInstance().signOut()
        showVerificationDialog()
    }

    private fun showVerificationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Registration Successful")
            .setMessage("Thank you for Registering! To Successfully Login to Your Account, please verify your email through the verification link sent to your email account")
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, LoginActivity::class.java)
                // Clear the back stack so the user can't go back to the registration screen
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
