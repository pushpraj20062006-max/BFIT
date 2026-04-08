package com.example.bfit.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking weight entries over time.
 * Each entry stores the user's weight on a specific date
 * for trend analysis and progress reporting.
 */
@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weight: Float,
    val bmi: Float = 0f
)
