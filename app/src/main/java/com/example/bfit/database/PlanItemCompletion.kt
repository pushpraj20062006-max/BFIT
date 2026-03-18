package com.example.bfit.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_item_completion")
data class PlanItemCompletion(
    @PrimaryKey val id: String,
    val isCompleted: Boolean
)
