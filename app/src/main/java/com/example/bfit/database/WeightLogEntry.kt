package com.example.bfit.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_log")
data class WeightLogEntry(
    @PrimaryKey val date: Long,
    val weightKg: Float
)
