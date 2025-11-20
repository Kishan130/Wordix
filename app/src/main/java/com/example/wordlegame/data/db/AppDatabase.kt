package com.example.wordlegame.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.wordlegame.data.model.Game

@Database(entities = [Game::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ✅ Migration from version 1 → 2 (preserves data)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1️⃣ Create new table with correct schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS games_new (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        userId TEXT NOT NULL,
                        mode TEXT NOT NULL,
                        word TEXT NOT NULL,
                        guessesUsed INTEGER NOT NULL,
                        datePlayed INTEGER NOT NULL,
                        isWin INTEGER NOT NULL,
                        guesses TEXT NOT NULL,
                        firestoreId TEXT NOT NULL DEFAULT ''
                    )
                """)

                // 2️⃣ Copy old data into new table
                try {
                    database.execSQL("""
                        INSERT INTO games_new (userId, mode, word, guessesUsed, datePlayed, isWin, guesses, firestoreId)
                        SELECT userId, mode, word, guessesUsed, datePlayed, isWin, guesses, firestoreId
                        FROM games
                    """)
                } catch (e: Exception) {
                    // In case the old table doesn't have 'firestoreId' column
                    database.execSQL("""
                        INSERT INTO games_new (userId, mode, word, guessesUsed, datePlayed, isWin, guesses)
                        SELECT userId, mode, word, guessesUsed, datePlayed, isWin, guesses
                        FROM games
                    """)
                }

                // 3️⃣ Drop the old table and rename the new one
                database.execSQL("DROP TABLE games")
                database.execSQL("ALTER TABLE games_new RENAME TO games")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wordle_database"
                )
                    .addMigrations(MIGRATION_1_2)  // ✅ keep migration only
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
