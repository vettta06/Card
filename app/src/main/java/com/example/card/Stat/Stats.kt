package com.example.card.Stat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stats")
data class Stats(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val cardId: Int,       // Long
    val rating: Int,        // Int
    val cardsSolved: Int,
    val isCompleted: Boolean
)