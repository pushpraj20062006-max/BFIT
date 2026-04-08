package com.example.bfit

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.bfit.charts.CalorieBarChartView
import com.example.bfit.charts.WeightLineChartView
import com.example.bfit.database.PlanRepository
import com.example.bfit.databinding.ActivityWeeklyReportBinding
import java.util.Calendar
import java.util.Locale

class WeeklyReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeeklyReportBinding
    private lateinit var planRepository: PlanRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeeklyReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        planRepository = PlanRepository(this)

        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val targetCalories = intent.getIntExtra("targetCalories", 2000)
        val targetProtein = intent.getIntExtra("targetProtein", 100)

        generateReport(targetCalories, targetProtein)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun generateReport(targetCalories: Int, targetProtein: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val today = calendar.timeInMillis
        val dayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        var totalCalories = 0
        var totalProtein = 0
        var daysCompleted = 0
        var daysWithData = 0
        val dailyData = mutableListOf<DayData>()

        // Go back 7 days
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = today
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayMillis = cal.timeInMillis
            val dayOfWeek = dayNames[(cal.get(Calendar.DAY_OF_WEEK) + 5) % 7]

            val log = planRepository.getDailyLog(dayMillis)
            val dayCals = log?.totalCalories ?: 0
            val dayProt = log?.totalProtein ?: 0
            val isComplete = planRepository.isDayComplete(dayMillis)

            if (dayCals > 0 || isComplete) daysWithData++
            if (isComplete) daysCompleted++

            totalCalories += dayCals
            totalProtein += dayProt

            dailyData.add(DayData(dayOfWeek, dayCals, dayProt, isComplete, dayMillis == today))
        }

        // --- Summary Stats ---
        val avgCalories = if (daysWithData > 0) totalCalories / daysWithData else 0
        val avgProtein = if (daysWithData > 0) totalProtein / daysWithData else 0
        val adherencePercent = (daysCompleted * 100) / 7

        binding.avgCaloriesText.text = "$avgCalories kcal"
        binding.avgProteinText.text = "${avgProtein}g"
        binding.adherenceText.text = "$adherencePercent%"
        binding.daysCompletedText.text = "$daysCompleted / 7"

        // Adherence color
        binding.adherenceText.setTextColor(when {
            adherencePercent >= 80 -> Color.parseColor("#00C853")
            adherencePercent >= 50 -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#F44336")
        })

        // Streak
        val streak = planRepository.getStreak()
        binding.streakValueText.text = "🔥 $streak days"


        // --- Calorie Bar Chart (Custom Canvas) ---
        val barData = dailyData.map { day ->
            CalorieBarChartView.BarData(
                label = day.dayName,
                value = day.calories,
                isToday = day.isToday,
                isComplete = day.isComplete
            )
        }
        binding.calorieBarChart.setData(barData, targetCalories)

        // --- Insights ---
        val insights = mutableListOf<String>()
        if (adherencePercent >= 80) {
            insights.add("🏆 Outstanding! You completed $daysCompleted out of 7 days this week!")
        } else if (adherencePercent >= 50) {
            insights.add("👍 Good effort! Try to complete ${7 - daysCompleted} more days next week.")
        } else {
            insights.add("💪 Keep going! Consistency is key to reaching your goals.")
        }

        if (avgCalories > targetCalories * 1.1) {
            insights.add("⚠️ You're averaging ${avgCalories - targetCalories} kcal above your target. Consider smaller portions.")
        } else if (avgCalories < targetCalories * 0.7 && avgCalories > 0) {
            insights.add("📉 You're under-eating. Make sure you're fueling your body enough.")
        } else if (avgCalories > 0) {
            insights.add("✅ Your calorie intake is on track. Keep it up!")
        }

        if (streak >= 7) {
            insights.add("🔥 Amazing $streak-day streak! You're building a great habit.")
        } else if (streak >= 3) {
            insights.add("🔥 Nice $streak-day streak! Keep the momentum going.")
        }

        binding.insightsText.text = insights.joinToString("\n\n")

        // --- Weight Trend Line Chart ---
        val weightEntries = planRepository.getRecentWeightEntries(7)
        if (weightEntries.isNotEmpty()) {
            binding.weightTrendCard.visibility = View.VISIBLE

            // Build line chart data
            val chartPoints = weightEntries.reversed().map { entry ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = entry.date
                val dateStr = String.format(Locale.getDefault(), "%02d/%02d",
                    cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
                WeightLineChartView.WeightPoint(dateStr, entry.weight, entry.bmi)
            }
            binding.weightLineChart.setData(chartPoints)

            // Show weight change
            val latestWeight = weightEntries.first().weight
            val oldestWeight = weightEntries.last().weight
            val diff = latestWeight - oldestWeight
            val changeText = when {
                diff > 0 -> String.format(Locale.getDefault(), "📈 +%.1f kg this week", diff)
                diff < 0 -> String.format(Locale.getDefault(), "📉 %.1f kg this week", diff)
                else -> "⚖️ Weight stable this week"
            }
            binding.weightChangeText.text = changeText
        } else {
            binding.weightTrendCard.visibility = View.GONE
        }
    }

    data class DayData(
        val dayName: String,
        val calories: Int,
        val protein: Int,
        val isComplete: Boolean,
        val isToday: Boolean
    )
}
