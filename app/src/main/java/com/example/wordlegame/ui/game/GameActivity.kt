package com.example.wordlegame.ui.game

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.wordlegame.R
import com.example.wordlegame.data.db.AppDatabase
import com.example.wordlegame.data.model.GameMode
import com.example.wordlegame.data.model.LetterResult
import com.example.wordlegame.data.remote.AuthRepository
import com.example.wordlegame.data.remote.FirestoreRepository
import com.example.wordlegame.data.repository.GameRepository
import com.example.wordlegame.databinding.ActivityGameBinding

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var repository: GameRepository
    private lateinit var authRepository: AuthRepository
    private val viewModel: GameViewModel by viewModels {
        GameViewModelFactory(repository)
    }

    private val letterBoxes = mutableListOf<List<TextView>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize
        val database = AppDatabase.getDatabase(this)
        val firestoreRepository = FirestoreRepository()
        repository = GameRepository(database.gameDao(), firestoreRepository)
        authRepository = AuthRepository()

        // Set user ID
        val userId = authRepository.getCurrentUser()?.uid ?: ""
        viewModel.setUserId(userId)

        // Get game mode
        val modeString = intent.getStringExtra("GAME_MODE") ?: GameMode.UNLIMITED.name
        val mode = GameMode.valueOf(modeString)

        setupLetterGrid()
        setupKeyboard()
        observeViewModel()

        viewModel.startGame(mode)

        supportActionBar?.title = if (mode == GameMode.DAILY) "Daily Challenge" else "Unlimited Mode"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupLetterGrid() {
        letterBoxes.clear()

        for (row in 0 until 6) {
            val rowBoxes = mutableListOf<TextView>()
            for (col in 0 until 5) {
                val boxId = resources.getIdentifier("letter_${row}_$col", "id", packageName)
                val box = findViewById<TextView>(boxId)
                rowBoxes.add(box)
            }
            letterBoxes.add(rowBoxes)
        }
    }

    private fun setupKeyboard() {
        val keys = "QWERTYUIOPASDFGHJKLZXCVBNM"

        binding.keyboardRow1.removeAllViews()
        binding.keyboardRow2.removeAllViews()
        binding.keyboardRow3.removeAllViews()

        keys.substring(0, 10).forEach { char ->
            addKeyButton(char, binding.keyboardRow1)
        }

        keys.substring(10, 19).forEach { char ->
            addKeyButton(char, binding.keyboardRow2)
        }

        addSpecialButton("ENTER", binding.keyboardRow3) {
            viewModel.submitGuess()
        }

        keys.substring(19).forEach { char ->
            addKeyButton(char, binding.keyboardRow3)
        }

        addSpecialButton("⌫", binding.keyboardRow3) {
            viewModel.deleteLetter()
        }
    }

    private fun addKeyButton(char: Char, container: android.widget.LinearLayout) {
        val button = Button(this).apply {
            text = char.toString()
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(4, 4, 4, 4)
            }
            setOnClickListener {
                viewModel.addLetter(char)
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.key_background))
            setTextColor(Color.WHITE)
        }
        container.addView(button)
    }

    private fun addSpecialButton(
        text: String,
        container: android.widget.LinearLayout,
        onClick: () -> Unit
    ) {
        val button = Button(this).apply {
            this.text = text
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(4, 4, 4, 4)
            }
            setOnClickListener { onClick() }
            setBackgroundColor(ContextCompat.getColor(context, R.color.key_special))
            setTextColor(Color.WHITE)
        }
        container.addView(button)
    }

    private fun observeViewModel() {
        viewModel.guesses.observe(this) { guesses ->
            updateGrid(guesses)
        }

        viewModel.currentGuess.observe(this) { guess ->
            updateCurrentRow(guess)
        }

        viewModel.gameStatus.observe(this) { status ->
            when (status) {
                GameStatus.WON -> showGameEndDialog(true)
                GameStatus.LOST -> showGameEndDialog(false)
                GameStatus.PLAYING -> { }
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.isValidating.observe(this) { isValidating ->
            binding.progressBar.visibility = if (isValidating) View.VISIBLE else View.GONE
        }
    }

    private fun updateGrid(guesses: List<com.example.wordlegame.data.model.Guess>) {
        guesses.forEachIndexed { rowIndex, guess ->
            guess.word.forEachIndexed { colIndex, char ->
                val box = letterBoxes[rowIndex][colIndex]
                box.text = char.toString()

                val colorRes = when (guess.results[colIndex]) {
                    LetterResult.CORRECT -> R.color.correct
                    LetterResult.PRESENT -> R.color.present
                    LetterResult.ABSENT -> R.color.absent
                }
                box.setBackgroundColor(ContextCompat.getColor(this, colorRes))
                box.setTextColor(Color.WHITE)
            }
        }
    }

    private fun updateCurrentRow(guess: String) {
        val currentRow = viewModel.guesses.value?.size ?: 0
        if (currentRow < 6) {
            val rowBoxes = letterBoxes[currentRow]

            guess.forEachIndexed { index, char ->
                rowBoxes[index].text = char.toString()
                rowBoxes[index].setBackgroundColor(
                    ContextCompat.getColor(this, R.color.box_filled)
                )
            }

            for (i in guess.length until 5) {
                rowBoxes[i].text = ""
                rowBoxes[i].setBackgroundColor(
                    ContextCompat.getColor(this, R.color.box_empty)
                )
            }
        }
    }

    private fun showGameEndDialog(won: Boolean) {
        val word = viewModel.targetWord.value ?: ""
        val definition = viewModel.wordDefinition.value

        val message = buildString {
            if (won) {
                append("Congratulations! You guessed the word in ${viewModel.guesses.value?.size} tries!\n\n")
            } else {
                append("Game Over! The word was: $word\n\n")
            }

            if (definition != null) {
                append("Definition: $definition")
            }
        }

        AlertDialog.Builder(this)
            .setTitle(if (won) "You Won! 🎉" else "Better Luck Next Time")
            .setMessage(message)
            .setPositiveButton("New Game") { _, _ ->
                if (viewModel.gameMode.value == GameMode.UNLIMITED) {
                    viewModel.startGame(GameMode.UNLIMITED)
                    resetGrid()
                } else {
                    finish()
                }
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun resetGrid() {
        letterBoxes.forEach { row ->
            row.forEach { box ->
                box.text = ""
                box.setBackgroundColor(ContextCompat.getColor(this, R.color.box_empty))
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}