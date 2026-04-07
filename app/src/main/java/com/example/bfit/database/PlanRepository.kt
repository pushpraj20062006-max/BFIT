package com.example.bfit.database

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Calendar

data class WeeklyProgressReport(
    val completedDays: Int,
    val totalCalories: Int,
    val averageCalories: Int,
    val totalProtein: Int,
    val averageProtein: Int,
    val daysLogged: Int
)

class PlanRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("plan_prefs", Context.MODE_PRIVATE)
    private val planDao = PlanDatabase.getDatabase(context).planDao()

    fun markDayAsComplete(date: Long) {
        prefs.edit().putBoolean(date.toString(), true).apply()
    }

    fun isDayComplete(date: Long): Boolean {
        return prefs.getBoolean(date.toString(), false)
    }

    fun markPlanItemAsComplete(id: String, isCompleted: Boolean, calories: Int, protein: Int) {
        runBlocking(Dispatchers.IO) {
            planDao.insertPlanItemCompletion(PlanItemCompletion(id, isCompleted))
            val date = id.split("-").firstOrNull()?.toLongOrNull() ?: return@runBlocking
            updateNutrientsForDay(date, isCompleted, calories, protein)
        }
    }

    fun isPlanItemComplete(id: String): Boolean {
        return runBlocking(Dispatchers.IO) {
            planDao.getPlanItemCompletion(id)?.isCompleted ?: false
        }
    }

    private suspend fun updateNutrientsForDay(date: Long, isCompleted: Boolean, calories: Int, protein: Int) {
        val dailyLog = planDao.getDailyLog(date)
        if (dailyLog != null) {
            val updatedCalories = if (isCompleted) {
                dailyLog.totalCalories + calories
            } else {
                dailyLog.totalCalories - calories
            }
            val updatedProtein = if (isCompleted) {
                dailyLog.totalProtein + protein
            } else {
                dailyLog.totalProtein - protein
            }
            val updatedLog = dailyLog.copy(totalCalories = updatedCalories, totalProtein = updatedProtein)
            planDao.insertDailyLog(updatedLog)
        } else {
            if (isCompleted) {
                planDao.insertDailyLog(DailyLog(date, calories, protein))
            }
        }
    }

    fun addCaloriesToDailyLog(date: Long, calories: Int, protein: Int) {
        runBlocking(Dispatchers.IO) {
            val dailyLog = planDao.getDailyLog(date)
            if (dailyLog != null) {
                val updatedLog = dailyLog.copy(
                    totalCalories = dailyLog.totalCalories + calories,
                    totalProtein = dailyLog.totalProtein + protein
                )
                planDao.insertDailyLog(updatedLog)
            } else {
                planDao.insertDailyLog(DailyLog(date, calories, protein))
            }
        }
    }

    fun getDailyLog(date: Long): DailyLog? {
        return runBlocking(Dispatchers.IO) {
            planDao.getDailyLog(date)
        }
    }

    fun getDailyLogsBetween(startDate: Long, endDate: Long): List<DailyLog> {
        return runBlocking(Dispatchers.IO) {
            planDao.getDailyLogsBetween(startDate, endDate)
        }
    }

    fun updateDailyLog(date: Long, calories: Int, protein: Int) {
        runBlocking(Dispatchers.IO) {
            planDao.insertDailyLog(DailyLog(date, calories, protein))
        }
    }

    fun addExtraMealItem(extraMealItem: ExtraMealItem) {
        runBlocking(Dispatchers.IO) {
            planDao.insertExtraMealItem(extraMealItem)
        }
    }

    fun getExtraMealItems(date: Long): List<ExtraMealItem> {
        return runBlocking(Dispatchers.IO) {
            planDao.getExtraMealItems(date)
        }
    }

    fun addWeightLogEntry(date: Long, weightKg: Float) {
        runBlocking(Dispatchers.IO) {
            planDao.insertWeightLogEntry(WeightLogEntry(date, weightKg))
        }
    }

    fun getWeightLogEntriesBetween(startDate: Long, endDate: Long): List<WeightLogEntry> {
        return runBlocking(Dispatchers.IO) {
            planDao.getWeightLogEntriesBetween(startDate, endDate)
        }
    }

    fun getLatestWeightLogEntry(): WeightLogEntry? {
        return runBlocking(Dispatchers.IO) {
            planDao.getLatestWeightLogEntry()
        }
    }

    fun getWeeklyProgressReport(endDate: Long = startOfDay(System.currentTimeMillis())): WeeklyProgressReport {
        val calendar = Calendar.getInstance().apply { timeInMillis = endDate }
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = startOfDay(calendar.timeInMillis)

        val logs = getDailyLogsBetween(startDate, endDate)
        val completedDays = (0..6).count { offset ->
            val day = Calendar.getInstance().apply {
                timeInMillis = startDate
                add(Calendar.DAY_OF_YEAR, offset)
            }
            isDayComplete(startOfDay(day.timeInMillis))
        }

        val totalCalories = logs.sumOf { it.totalCalories }
        val totalProtein = logs.sumOf { it.totalProtein }
        val safeDays = logs.size.coerceAtLeast(1)

        return WeeklyProgressReport(
            completedDays = completedDays,
            totalCalories = totalCalories,
            averageCalories = totalCalories / safeDays,
            totalProtein = totalProtein,
            averageProtein = totalProtein / safeDays,
            daysLogged = logs.size
        )
    }

    private fun startOfDay(timeInMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getStreak(): Int {
        var streak = 0
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        while (isDayComplete(calendar.timeInMillis)) {
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }
}
