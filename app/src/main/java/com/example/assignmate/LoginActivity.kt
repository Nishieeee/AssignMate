package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.assignmate.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseHelper: FirebaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        
        // Remove onStart auth check logic from here or simplify it, 
        // but since Introduction activity now handles the "initial launch" logic,
        // LoginActivity is reached only if user is NOT logged in or has completed introduction.
        // However, if the user explicitly launched LoginActivity but is already logged in,
        // we should probably still redirect them to MainActivity.
        // The user said: "if a user is signed in, it should open activity_main.xml".
        // The Introduction activity already checks for signed-in user and redirects to MainActivity.
        // If we fall through to LoginActivity, it means either:
        // 1. We came from Introduction (user not signed in, first run done)
        // 2. We came from Introduction (user not signed in, first run just finished)
        // 3. We opened app and Introduction redirected us here because not signed in + first run done.
        
        // So checking auth here again is safe and good practice.

        if (auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("USER_ID", auth.currentUser!!.uid)
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper()

        binding.login.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseHelper.loginUser(email, password,
                    onSuccess = {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("USER_ID", it)
                        startActivity(intent)
                        finish()
                    },
                    onFailure = {
                        Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.registerPrompt.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgetPasswordActivity::class.java))
        }
    }
}