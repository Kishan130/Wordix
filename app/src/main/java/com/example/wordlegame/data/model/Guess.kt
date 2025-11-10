package com.example.wordlegame.data.model

data class Guess(
    val word: String,
    val results: List<LetterResult>
)

enum class LetterResult {
    CORRECT,    // Green - right letter, right position
    PRESENT,    // Yellow - right letter, wrong position
    ABSENT      // Gray - letter not in word
}