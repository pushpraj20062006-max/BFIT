package com.example.bfit.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_log")
data class DailyLog(
    @PrimaryKey val date: Long,
    val totalCalories: Int,
    val totalProtein: Int
)
