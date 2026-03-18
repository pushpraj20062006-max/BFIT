package com.example.bfit.database

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Calendar

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
