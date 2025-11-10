package com.example.wordlegame.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,  // Local database ID (auto-generated)
    val firestoreId: String = "",  // Firestore document ID
    val userId: String = "",
    val mode: GameMode,
    val word: String,
    val guessesUsed: Int,
    val datePlayed: Long,
    val isWin: Boolean,
    val guesses: List<String> = emptyList()
)

enum class GameMode {
    UNLIMITED, DAILY
}