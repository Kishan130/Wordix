package com.example.wordlegame.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wordlegame.R
import com.example.wordlegame.data.db.AppDatabase
import com.example.wordlegame.data.model.Game
import com.example.wordlegame.data.model.GameMode
import com.example.wordlegame.data.remote.AuthRepository
import com.example.wordlegame.data.remote.FirestoreRepository
import com.example.wordlegame.data.repository.GameRepository
import com.example.wordlegame.databinding.ActivityHistoryBinding
import com.example.wordlegame.utils.DateUtils
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var repository: GameRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var adapter: HistoryAdapter
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Game History"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val database = AppDatabase.getDatabase(this)
        val firestoreRepository = FirestoreRepository()
        repository = GameRepository(database.gameDao(), firestoreRepository)
        authRepository = AuthRepository()

        currentUserId = authRepository.getCurrentUser()?.uid ?: ""

        setupRecyclerView()
        setupFilters()
        loadGamesFromCloud()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener {
            binding.chipAll.isChecked = true
            observeAllGames()
        }

        binding.chipUnlimited.setOnClickListener {
            binding.chipUnlimited.isChecked = true
            observeGamesByMode(GameMode.UNLIMITED)
        }

        binding.chipDaily.setOnClickListener {
            binding.chipDaily.isChecked = true
            observeGamesByMode(GameMode.DAILY)
        }
    }

    private fun observeAllGames() {
        repository.getAllGames(currentUserId).observe(this) { games ->
            adapter.submitList(games)
            updateEmptyState(games.isEmpty())
        }
    }

    private fun observeGamesByMode(mode: GameMode) {
        repository.getGamesByMode(currentUserId, mode).observe(this) { games ->
            adapter.submitList(games)
            updateEmptyState(games.isEmpty())
        }
    }

    private fun loadGamesFromCloud() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = repository.syncGamesFromCloud(currentUserId)

            binding.progressBar.visibility = View.GONE

            if (result.isSuccess) {
                val games = result.getOrNull() ?: emptyList()
                games.forEach { game ->
                    try {
                        repository.insertGameLocally(game)
                    } catch (e: Exception) {
                        // Already exists
                    }
                }
                // Now load all games after sync
                observeAllGames()
            } else {
                // Even if cloud sync fails, show local games
                observeAllGames()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.GameViewHolder>() {

    private var games = listOf<Game>()

    fun submitList(newGames: List<Game>) {
        games = newGames
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_history, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(games[position])
    }

    override fun getItemCount() = games.size

    class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMode: TextView = itemView.findViewById(R.id.tv_mode)
        private val tvWord: TextView = itemView.findViewById(R.id.tv_word)
        private val tvResult: TextView = itemView.findViewById(R.id.tv_result)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvGuesses: TextView = itemView.findViewById(R.id.tv_guesses)

        fun bind(game: Game) {
            tvMode.text = if (game.mode == GameMode.DAILY) "Daily" else "Unlimited"
            tvWord.text = game.word
            tvResult.text = if (game.isWin) {
                "Won in ${game.guessesUsed} tries ✓"
            } else {
                "Lost"
            }
            tvDate.text = DateUtils.formatDate(game.datePlayed)

            if (game.guesses.isNotEmpty()) {
                tvGuesses.text = "Guesses: ${game.guesses.joinToString(", ")}"
                tvGuesses.visibility = View.VISIBLE
            } else {
                tvGuesses.visibility = View.GONE
            }

            val resultColor = if (game.isWin) {
                android.graphics.Color.parseColor("#6aaa64")
            } else {
                android.graphics.Color.parseColor("#787c7e")
            }
            tvResult.setTextColor(resultColor)
        }
    }
}