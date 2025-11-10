package com.example.wordlegame.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.wordlegame.data.db.GameDao
import com.example.wordlegame.data.model.Game
import com.example.wordlegame.data.model.GameMode
import com.example.wordlegame.data.remote.FirestoreRepository
import java.util.*

class GameRepository(
    private val gameDao: GameDao,
    private val firestoreRepository: FirestoreRepository
) {

    // Local database operations with userId
    fun getAllGames(userId: String): LiveData<List<Game>> = gameDao.getAllGames(userId)

    fun getGamesByMode(userId: String, mode: GameMode): LiveData<List<Game>> =
        gameDao.getGamesByMode(userId, mode)

    suspend fun insertGameLocally(game: Game): Long {
        Log.d("GameRepository", "Inserting game locally: ${game.word}, userId: ${game.userId}")
        return gameDao.insertGame(game)
    }

    // Save game to both local and cloud
    suspend fun saveGame(game: Game): Result<String> {
        Log.d("GameRepository", "Saving game: ${game.word}, userId: ${game.userId}")

        // First save to cloud
        val cloudResult = firestoreRepository.saveGame(game)

        if (cloudResult.isSuccess) {
            val firestoreId = cloudResult.getOrNull() ?: ""
            Log.d("GameRepository", "Saved to cloud with ID: $firestoreId")

            // Save to local database with Firestore ID
            val localGame = game.copy(firestoreId = firestoreId)
            insertGameLocally(localGame)

            // Update user stats in Firestore
            updateUserStatsInFirestore(game.userId, game.isWin)

            return Result.success(firestoreId)
        } else {
            // If cloud save fails, still save locally
            Log.e("GameRepository", "Cloud save failed, saving locally only")
            insertGameLocally(game)
            return cloudResult
        }
    }

    // Sync games from cloud to local
    suspend fun syncGamesFromCloud(userId: String): Result<List<Game>> {
        Log.d("GameRepository", "Syncing games from cloud for user: $userId")

        val result = firestoreRepository.getUserGames(userId)

        if (result.isSuccess) {
            val cloudGames = result.getOrNull() ?: emptyList()
            Log.d("GameRepository", "Found ${cloudGames.size} games in cloud")

            // Save each game to local database if not already exists
            cloudGames.forEach { cloudGame ->
                val existingGame = gameDao.getGameByFirestoreId(userId, cloudGame.firestoreId)
                if (existingGame == null) {
                    Log.d("GameRepository", "Inserting new game from cloud: ${cloudGame.word}")
                    insertGameLocally(cloudGame)
                } else {
                    Log.d("GameRepository", "Game already exists locally: ${cloudGame.word}")
                }
            }
        } else {
            Log.e("GameRepository", "Failed to sync from cloud: ${result.exceptionOrNull()?.message}")
        }

        return result
    }

    suspend fun hasPlayedDailyToday(userId: String): Boolean {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        // Check both local and cloud
        val localGame = gameDao.getDailyGameForToday(userId, startOfDay, endOfDay)
        if (localGame != null) {
            return true
        }

        val cloudResult = firestoreRepository.checkDailyGamePlayed(userId, startOfDay, endOfDay)
        return cloudResult.getOrDefault(false)
    }

    fun getTotalWins(userId: String): LiveData<Int> = gameDao.getTotalWins(userId)

    fun getTotalGames(userId: String): LiveData<Int> = gameDao.getTotalGames(userId)

    private suspend fun updateUserStatsInFirestore(userId: String, isWin: Boolean) {
        val userResult = firestoreRepository.getUser(userId)
        if (userResult.isSuccess) {
            val user = userResult.getOrNull()
            user?.let {
                val newTotalGames = it.totalGames + 1
                val newTotalWins = if (isWin) it.totalWins + 1 else it.totalWins
                Log.d("GameRepository", "Updating Firestore stats: games=$newTotalGames, wins=$newTotalWins")
                firestoreRepository.updateUserStats(userId, newTotalGames, newTotalWins)
            }
        }
    }
}