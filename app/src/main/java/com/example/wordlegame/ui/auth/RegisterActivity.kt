package com.example.wordlegame.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wordlegame.data.model.User
import com.example.wordlegame.data.remote.AuthRepository
import com.example.wordlegame.data.remote.FirestoreRepository
import com.example.wordlegame.databinding.ActivityRegisterBinding
import com.example.wordlegame.ui.main.MainActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(name, email, password, confirmPassword)) {
                register(name, email, password)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return false
        }
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid email format"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return false
        }
        return true
    }

    private fun register(name: String, email: String, password: String) {
        showLoading(true)
        lifecycleScope.launch {
            val authResult = authRepository.signUp(email, password, name)

            if (authResult.isSuccess) {
                val firebaseUser = authResult.getOrNull()!!

                // Create user document in Firestore with initialized stats
                val user = User(
                    uid = firebaseUser.uid,
                    email = email,
                    displayName = name,
                    createdAt = System.currentTimeMillis(),
                    totalGames = 0,  // Initialize to 0
                    totalWins = 0    // Initialize to 0
                )

                val firestoreResult = firestoreRepository.createUser(user)
                showLoading(false)

                if (firestoreResult.isSuccess) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Failed to create user profile",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                showLoading(false)
                Toast.makeText(
                    this@RegisterActivity,
                    "Registration failed: ${authResult.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
    }
}