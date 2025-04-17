package com.example.card.Stat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface StatsDao {
    @Insert
    suspend fun insert(stat: Stats)

    @Query("SELECT * FROM stats WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): Stats?

    @Update
    suspend fun update(stat: Stats)

    @Query("SELECT * FROM stats ORDER BY date DESC")
    suspend fun getAll(): List<Stats>
}