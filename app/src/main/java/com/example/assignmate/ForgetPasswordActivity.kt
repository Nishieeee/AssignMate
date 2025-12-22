package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.assignmate.databinding.ActivityForgetPasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgetPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.saveButton.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val oldPassword = binding.oldPassword.text.toString().trim()
            val newPassword = binding.newPassword.text.toString().trim()
            val confirmNewPassword = binding.confirmNewPassword.text.toString().trim()

            if (email.isEmpty() || oldPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmNewPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sign in with old password to re-authenticate
            auth.signInWithEmailAndPassword(email, oldPassword)
                .addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        // If sign in is successful, update the password
                        signInTask.result.user?.updatePassword(newPassword)
                            ?.addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                                    // Sign out the user after password update to force re-login
                                    auth.signOut()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to update password: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        // Sign in failed, likely wrong email or old password
                        Toast.makeText(this, "Authentication failed: Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
