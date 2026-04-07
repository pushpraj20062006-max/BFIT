package com.example.bfit

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bfit.database.PlanRepository
import com.example.bfit.database.WeeklyProgressReport
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProgressActivity : AppCompatActivity() {

    private lateinit var planRepository: PlanRepository

    private lateinit var completedDaysText: TextView
    private lateinit var completedDaysProgress: LinearProgressIndicator
    private lateinit var weeklyCaloriesText: TextView
    private lateinit var weeklyProteinText: TextView
    private lateinit var weeklyAverageText: TextView
    private lateinit var loggedDaysText: TextView

    private lateinit var weightInput: TextInputEditText
    private lateinit var saveWeightButton: MaterialButton
    private lateinit var latestWeightText: TextView
    private lateinit var weightGraph: WeightGraphView

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        planRepository = PlanRepository(this)

        completedDaysText = findViewById(R.id.completedDaysText)
        completedDaysProgress = findViewById(R.id.completedDaysProgress)
        weeklyCaloriesText = findViewById(R.id.weeklyCaloriesText)
        weeklyProteinText = findViewById(R.id.weeklyProteinText)
        weeklyAverageText = findViewById(R.id.weeklyAverageText)
        loggedDaysText = findViewById(R.id.loggedDaysText)

        weightInput = findViewById(R.id.weightInput)
        saveWeightButton = findViewById(R.id.saveWeightButton)
        latestWeightText = findViewById(R.id.latestWeightText)
        weightGraph = findViewById(R.id.weightGraph)

        saveWeightButton.setOnClickListener {
            val enteredWeight = weightInput.text?.toString()?.toFloatOrNull()
            if (enteredWeight == null || enteredWeight <= 0f) {
                Toast.makeText(this, "Enter a valid weight", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            planRepository.addWeightLogEntry(todayStart, enteredWeight)
            weightInput.setText("")
            Toast.makeText(this, "Weight saved", Toast.LENGTH_SHORT).show()
            refreshWeightSection()
        }

        val report = planRepository.getWeeklyProgressReport()
        bindWeeklyReport(report)
        refreshWeightSection()
    }

    private fun bindWeeklyReport(report: WeeklyProgressReport) {
        completedDaysText.text = "Days Completed: ${report.completedDays} / 7"
        completedDaysProgress.max = 7
        completedDaysProgress.progress = report.completedDays

        weeklyCaloriesText.text = "Total Calories: ${report.totalCalories} kcal"
        weeklyProteinText.text = "Total Protein: ${report.totalProtein} g"
        weeklyAverageText.text = "Average: ${report.averageCalories} kcal/day, ${report.averageProtein} g/day"
        loggedDaysText.text = "Days with logs: ${report.daysLogged}"
    }

    private fun refreshWeightSection() {
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val start = Calendar.getInstance().apply {
            timeInMillis = end
            add(Calendar.DAY_OF_YEAR, -29)
        }.timeInMillis

        val entries = planRepository.getWeightLogEntriesBetween(start, end)
        weightGraph.setData(entries)

        val latestEntry = planRepository.getLatestWeightLogEntry()
        if (latestEntry == null) {
            latestWeightText.text = "Latest Weight: --"
        } else {
            latestWeightText.text = "Latest Weight: ${"%.1f".format(latestEntry.weightKg)} kg on ${dateFormatter.format(latestEntry.date)}"
        }
    }
}
