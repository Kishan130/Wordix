package com.example.wordlegame.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.wordlegame.data.model.Game
import com.example.wordlegame.data.model.GameMode

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGame(game: Game): Long

    @Query("SELECT * FROM games WHERE userId = :userId ORDER BY datePlayed DESC")
    fun getAllGames(userId: String): LiveData<List<Game>>

    @Query("SELECT * FROM games WHERE userId = :userId AND mode = :mode ORDER BY datePlayed DESC")
    fun getGamesByMode(userId: String, mode: GameMode): LiveData<List<Game>>

    @Query("SELECT * FROM games WHERE userId = :userId ORDER BY datePlayed DESC")
    suspend fun getUserGamesList(userId: String): List<Game>

    @Query("SELECT * FROM games WHERE userId = :userId AND mode = 'DAILY' AND datePlayed >= :startOfDay AND datePlayed < :endOfDay")
    suspend fun getDailyGameForToday(userId: String, startOfDay: Long, endOfDay: Long): Game?

    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId AND isWin = 1")
    fun getTotalWins(userId: String): LiveData<Int>

    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId")
    fun getTotalGames(userId: String): LiveData<Int>

    @Query("SELECT * FROM games WHERE userId = :userId AND firestoreId = :firestoreId LIMIT 1")
    suspend fun getGameByFirestoreId(userId: String, firestoreId: String): Game?

    @Query("DELETE FROM games WHERE userId = :userId")
    suspend fun deleteUserGames(userId: String)

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()
}
