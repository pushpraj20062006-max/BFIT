package com.example.bfit.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItemCompletion(planItemCompletion: PlanItemCompletion)

    @Query("SELECT * FROM plan_item_completion WHERE id = :id")
    suspend fun getPlanItemCompletion(id: String): PlanItemCompletion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(dailyLog: DailyLog)

    @Query("SELECT * FROM daily_log WHERE date = :date")
    suspend fun getDailyLog(date: Long): DailyLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraMealItem(extraMealItem: ExtraMealItem)

    @Query("SELECT * FROM extra_meal_items WHERE date = :date")
    suspend fun getExtraMealItems(date: Long): List<ExtraMealItem>
}
