package com.example.card.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val question: String,
    val answer: String,
    var interval: Int = 1,
    var repetition: Int = 0,
    var efactor: Double = 2.5,
    var nextReviewDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)