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

    @Query("SELECT * FROM daily_log WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getDailyLogsBetween(startDate: Long, endDate: Long): List<DailyLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraMealItem(extraMealItem: ExtraMealItem)

    @Query("SELECT * FROM extra_meal_items WHERE date = :date")
    suspend fun getExtraMealItems(date: Long): List<ExtraMealItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLogEntry(weightLogEntry: WeightLogEntry)

    @Query("SELECT * FROM weight_log WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getWeightLogEntriesBetween(startDate: Long, endDate: Long): List<WeightLogEntry>

    @Query("SELECT * FROM weight_log ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeightLogEntry(): WeightLogEntry?
}
