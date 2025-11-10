package com.example.wordlegame.utils

import android.content.Context
import com.example.wordlegame.data.api.DictionaryResponse
import com.example.wordlegame.data.api.RetrofitClient
import com.example.wordlegame.data.model.Guess
import com.example.wordlegame.data.model.LetterResult
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Date

object WordUtils {

    private var wordList: List<String> = emptyList()
    private val api = RetrofitClient.dictionaryApi

    fun loadWords(context: Context) {
        if (wordList.isEmpty()) {
            try {
                val inputStream = context.assets.open("words.txt")
                val reader = BufferedReader(InputStreamReader(inputStream))
                wordList = reader.readLines()
                    .map { it.trim().uppercase() }
                    .filter { it.length == 5 }
                reader.close()
            } catch (e: Exception) {
                wordList = listOf(
                    "APPLE", "BRAVE", "CRANE", "DANCE", "EAGLE",
                    "FRUIT", "GRAPE", "HOUSE", "IMAGE", "JUICE",
                    "KNIFE", "LEMON", "MOUSE", "NIGHT", "OCEAN",
                    "PEACE", "QUEEN", "RIVER", "STONE", "TIGER",
                    "UNCLE", "VOICE", "WATER", "XENON", "YOUTH",
                    "ZEBRA", "BLOOM", "CHESS", "DREAM", "FLAME"
                )
            }
        }
    }

    fun getRandomWord(): String {
        return wordList.random()
    }

    fun getDailyWord(date: Date): String {
        val daysSinceEpoch = date.time / (1000 * 60 * 60 * 24)
        val index = (daysSinceEpoch % wordList.size).toInt()
        return wordList[index]
    }

    fun isValidWord(word: String): Boolean {
        return wordList.contains(word.uppercase())
    }

    // NEW: API validation
    suspend fun validateWordWithApi(word: String): Boolean {
        return try {
            val response = api.getWordDefinition(word.lowercase())
            response.isSuccessful && response.body() != null
        } catch (e: Exception) {
            // Fallback to local validation
            isValidWord(word)
        }
    }

    // NEW: Get word definition
    suspend fun getWordDefinition(word: String): String? {
        return try {
            val response = api.getWordDefinition(word.lowercase())
            if (response.isSuccessful) {
                val definitions = response.body()?.firstOrNull()?.meanings?.firstOrNull()?.definitions
                definitions?.firstOrNull()?.definition
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun checkGuess(guess: String, target: String): Guess {
        val results = MutableList(5) { LetterResult.ABSENT }
        val targetChars = target.toCharArray().toMutableList()
        val guessChars = guess.uppercase().toCharArray()

        // First pass: mark correct positions
        for (i in guessChars.indices) {
            if (guessChars[i] == targetChars[i]) {
                results[i] = LetterResult.CORRECT
                targetChars[i] = '*'
            }
        }

        // Second pass: mark present letters
        for (i in guessChars.indices) {
            if (results[i] == LetterResult.ABSENT) {
                val index = targetChars.indexOf(guessChars[i])
                if (index != -1) {
                    results[i] = LetterResult.PRESENT
                    targetChars[index] = '*'
                }
            }
        }

        return Guess(guess.uppercase(), results)
    }
}