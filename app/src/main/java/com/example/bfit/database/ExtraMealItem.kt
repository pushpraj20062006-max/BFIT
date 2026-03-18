package com.example.bfit.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extra_meal_items")
data class ExtraMealItem(
    @PrimaryKey val id: String,
    val date: Long,
    val text: String,
    val calories: Int,
    val protein: Int
)
