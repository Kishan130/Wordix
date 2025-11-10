package com.example.wordlegame.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val totalGames: Int = 0,
    val totalWins: Int = 0
)