package com.example.card.database

import androidx.room.*

@Dao
interface CardDao {
    @Insert
    suspend fun insert(card: Card)

    @Update
    suspend fun update(card: Card)

    @Delete
    suspend fun delete(card: Card)

    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    suspend fun getAllCards(): List<Card>
}