package com.example.wordlegame.data.db

import androidx.room.TypeConverter
import com.example.wordlegame.data.model.GameMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromGameMode(value: GameMode): String {
        return value.name
    }

    @TypeConverter
    fun toGameMode(value: String): GameMode {
        return GameMode.valueOf(value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}