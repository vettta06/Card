package com.example.card.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CardDao {
    @Insert
    suspend fun insert(card: Card)

    @Update
    suspend fun update(card: Card)

    @Delete
    suspend fun delete(card: Card)

    @Query("SELECT * FROM cards WHERE nextReviewDate <= :currentTime ORDER BY nextReviewDate ASC")
    suspend fun getDueCards(currentTime: Long): List<Card>

    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    suspend fun getAllCards(): List<Card>
    @Query("SELECT COUNT(*) FROM cards")

    fun getTotalCards(): LiveData<Int>

    @Query("SELECT AVG(interval) FROM cards")
    fun getAverageInterval(): LiveData<Double>
}