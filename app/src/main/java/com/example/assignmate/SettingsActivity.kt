package com.example.assignmate

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.assignmate.databinding.ActivitySettingsBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseHelper: FirebaseHelper
    private var originalUsername: String = ""
    private var originalProfileImage: String = ""
    private var selectedImageUri: Uri? = null
    private var isImageRemoved = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            Glide.with(this).load(uri).into(binding.profileImage)
            binding.removeImageButton.visibility = View.VISIBLE
            binding.uploadImageText.text = "Change Image Photo"
            isImageRemoved = false
            checkIfChangesMade()
        }
    }

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
                checkIfChangesMade()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.usernameEditText.addTextChangedListener(textWatcher)
        binding.oldPasswordEditText.addTextChangedListener(textWatcher)
        binding.newPasswordEditText.addTextChangedListener(textWatcher)
        binding.confirmPasswordEditText.addTextChangedListener(textWatcher)

        binding.profileImage.setOnClickListener {
             pickImageLauncher.launch("image/*")
        }

        binding.uploadImageText.setOnClickListener {
             pickImageLauncher.launch("image/*")
        }

        binding.removeImageButton.setOnClickListener {
            selectedImageUri = null
            isImageRemoved = true
            binding.profileImage.setImageResource(R.drawable.ic_profile)
            binding.removeImageButton.visibility = View.GONE
            binding.uploadImageText.text = "Upload Image Photo"
            checkIfChangesMade()
        }

        binding.saveButton.setOnClickListener {
            validateAndSave()
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
                    originalProfileImage = it.profileImage
                    if (it.profileImage.isNotEmpty()) {
                        Glide.with(this).load(it.profileImage).into(binding.profileImage)
                        binding.removeImageButton.visibility = View.VISIBLE
                        binding.uploadImageText.text = "Change Image Photo"
                    }
                }
            }, {})
        }
    }

    private fun checkIfChangesMade() {
        val usernameChanged = binding.usernameEditText.text.toString().trim() != originalUsername
        
        val oldPassword = binding.oldPasswordEditText.text.toString().trim()
        val newPassword = binding.newPasswordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()
        val passwordFieldsTouched = oldPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()
        
        val imageChanged = selectedImageUri != null || (isImageRemoved && originalProfileImage.isNotEmpty())

        binding.saveButton.isEnabled = usernameChanged || passwordFieldsTouched || imageChanged
        if (binding.saveButton.isEnabled) {
            binding.saveButton.visibility = View.VISIBLE
        } else {
            binding.saveButton.visibility = View.GONE
        }
    }

    private fun validateAndSave() {
        val oldPassword = binding.oldPasswordEditText.text.toString().trim()
        val newPassword = binding.newPasswordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()
        
        val changingPassword = oldPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()
        
        if (changingPassword) {
            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all password fields to change password", Toast.LENGTH_SHORT).show()
                return
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return
            }
            if (newPassword.length < 8) {
                Toast.makeText(this, "New password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        showConfirmationDialog(changingPassword)
    }

    private fun showConfirmationDialog(changingPassword: Boolean) {
        AlertDialog.Builder(this)
            .setTitle("Save Changes")
            .setMessage("Are you sure you want to save these changes?")
            .setPositiveButton("Save") { _, _ ->
                saveChanges(changingPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveChanges(changingPassword: Boolean) {
        val user = auth.currentUser
        if (user != null) {
            val newUsername = binding.usernameEditText.text.toString().trim()

            if (selectedImageUri != null) {
                firebaseHelper.uploadFile(this, selectedImageUri!!, "profile_images",
                    onSuccess = { imageUrl ->
                        updateProfile(user.uid, newUsername, imageUrl, changingPassword)
                    },
                    onFailure = {
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                )
            } else if (isImageRemoved) {
                 updateProfile(user.uid, newUsername, "", changingPassword)
            } else {
                updateProfile(user.uid, newUsername, originalProfileImage, changingPassword)
            }
        }
    }

    private fun updateProfile(userId: String, username: String, profileImage: String, changingPassword: Boolean) {
        val user = auth.currentUser
        firebaseHelper.updateUserProfile(userId, username, profileImage,
            onSuccess = {
                if (changingPassword) {
                    val oldPassword = binding.oldPasswordEditText.text.toString().trim()
                    val newPassword = binding.newPasswordEditText.text.toString().trim()
                    val credential = EmailAuthProvider.getCredential(user!!.email!!, oldPassword)
                    
                    user.reauthenticate(credential)
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(this, "Profile and password updated successfully", Toast.LENGTH_SHORT).show()
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Failed to update password: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Old password is incorrect", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            onFailure = {
                 Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
