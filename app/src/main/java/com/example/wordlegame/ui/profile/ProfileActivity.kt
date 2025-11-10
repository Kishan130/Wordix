package com.example.wordlegame.ui.profile

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wordlegame.data.remote.AuthRepository
import com.example.wordlegame.data.remote.FirestoreRepository
import com.example.wordlegame.databinding.ActivityProfileBinding
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val authRepository = AuthRepository()
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadUserProfile()
    }

    private fun loadUserProfile() {
        binding.progressBar.visibility = View.VISIBLE

        val firebaseUser = authRepository.getCurrentUser()
        if (firebaseUser != null) {
            binding.tvName.text = firebaseUser.displayName ?: "No name"
            binding.tvEmail.text = firebaseUser.email ?: "No email"

            lifecycleScope.launch {
                val result = firestoreRepository.getUser(firebaseUser.uid)
                binding.progressBar.visibility = View.GONE

                if (result.isSuccess) {
                    val user = result.getOrNull()
                    user?.let {
                        binding.tvTotalGames.text = it.totalGames.toString()
                        binding.tvTotalWins.text = it.totalWins.toString()
                        val winRate = if (it.totalGames > 0) {
                            ((it.totalWins.toFloat() / it.totalGames) * 100).toInt()
                        } else {
                            0
                        }
                        binding.tvWinRate.text = "$winRate%"
                    }
                } else {
                    // Show zeros if can't load from Firestore
                    binding.tvTotalGames.text = "0"
                    binding.tvTotalWins.text = "0"
                    binding.tvWinRate.text = "0%"
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}