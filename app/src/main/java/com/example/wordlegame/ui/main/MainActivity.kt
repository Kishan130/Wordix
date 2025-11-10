package com.example.wordlegame.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wordlegame.R
import com.example.wordlegame.data.db.AppDatabase
import com.example.wordlegame.data.model.GameMode
import com.example.wordlegame.data.remote.AuthRepository
import com.example.wordlegame.data.remote.FirestoreRepository
import com.example.wordlegame.data.repository.GameRepository
import com.example.wordlegame.databinding.ActivityMainBinding
import com.example.wordlegame.ui.auth.LoginActivity
import com.example.wordlegame.ui.game.GameActivity
import com.example.wordlegame.ui.history.HistoryActivity
import com.example.wordlegame.ui.profile.ProfileActivity
import com.example.wordlegame.utils.WordUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: GameRepository
    private lateinit var authRepository: AuthRepository
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check authentication
        authRepository = AuthRepository()
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        currentUserId = currentUser.uid

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize repository
        val database = AppDatabase.getDatabase(this)
        val firestoreRepository = FirestoreRepository()
        repository = GameRepository(database.gameDao(), firestoreRepository)

        // Load words
        WordUtils.loadWords(this)

        setupUI()
        setupClickListeners()
        observeStats()
        syncGamesFromCloud()
    }

    private fun setupUI() {
        val user = authRepository.getCurrentUser()
        binding.tvWelcome.text = "Welcome, ${user?.displayName ?: user?.email}!"
    }

    private fun setupClickListeners() {
        binding.btnUnlimitedMode.setOnClickListener {
            startGame(GameMode.UNLIMITED)
        }

        binding.btnDailyChallenge.setOnClickListener {
            lifecycleScope.launch {
                if (!repository.hasPlayedDailyToday(currentUserId)) {
                    startGame(GameMode.DAILY)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "You've already played today's challenge!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun observeStats() {
        Log.d("MainActivity", "Observing stats for user: $currentUserId")

        // Remove old observers to prevent duplicates
        repository.getTotalGames(currentUserId).removeObservers(this)
        repository.getTotalWins(currentUserId).removeObservers(this)

        // Add new observers
        repository.getTotalGames(currentUserId).observe(this) { total ->
            Log.d("MainActivity", "Total games updated: $total")
            binding.tvTotalGames.text = "Total Games: ${total ?: 0}"
        }

        repository.getTotalWins(currentUserId).observe(this) { wins ->
            Log.d("MainActivity", "Total wins updated: $wins")
            binding.tvTotalWins.text = "Wins: ${wins ?: 0}"
        }
    }

    private fun syncGamesFromCloud() {
        lifecycleScope.launch {
            val result = repository.syncGamesFromCloud(currentUserId)

            if (result.isSuccess) {
                val games = result.getOrNull() ?: emptyList()
                // Save to local database
                games.forEach { game ->
                    try {
                        repository.insertGameLocally(game)
                    } catch (e: Exception) {
                        // Game already exists locally
                    }
                }
                if (games.isNotEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Synced ${games.size} games from cloud",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startGame(mode: GameMode) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("GAME_MODE", mode.name)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            R.id.action_sync -> {
                syncGamesFromCloud()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                authRepository.signOut()
                navigateToLogin()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")
        // Force refresh stats and sync
        observeStats()
        syncGamesFromCloud()
    }
}