package com.example.bfit

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bfit.database.PlanRepository
import com.example.bfit.databinding.ActivityPlannerBinding
import java.io.Serializable
import java.util.Calendar

class PlannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlannerBinding
    private lateinit var planAdapter: PlanAdapter
    private val planByDate = mutableMapOf<Long, List<PlanListItem>>()
    private lateinit var planRepository: PlanRepository
    private var selectedDate: Long = 0
    private val completedDates = mutableSetOf<Long>()
    private var exerciseOnlyMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        planRepository = PlanRepository(this)

        // Back button
        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val planResult = getSerializable(intent, "plan", PlanResult::class.java)
        exerciseOnlyMode = intent.getBooleanExtra("openExerciseOnly", false)

        if (exerciseOnlyMode) {
            binding.plannerTitleText.text = getString(R.string.exercise_focus_title)
        }

        if (planResult == null) {
            Toast.makeText(this, "Error: Plan data is missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val today = Calendar.getInstance()
        for (i in 0 until 30) {
            val date = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            date.set(Calendar.HOUR_OF_DAY, 0)
            date.set(Calendar.MINUTE, 0)
            date.set(Calendar.SECOND, 0)
            date.set(Calendar.MILLISECOND, 0)
            val dayInMillis = date.timeInMillis

            val dayOfWeek = ((date.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1).toString() // Correctly map to the meal plan's day keys
            val mealPlanForDay = planResult.mealPlan[dayOfWeek]

            if (mealPlanForDay != null && mealPlanForDay.size >= 3) {
                val planItems = mutableListOf<PlanListItem>()

                val (breakfastText, breakfastCalories, breakfastProtein) = mealPlanForDay[0]
                val (lunchText, lunchCalories, lunchProtein) = mealPlanForDay[1]
                val (dinnerText, dinnerCalories, dinnerProtein) = mealPlanForDay[2]

                planItems.add(PlanListItem.Header("Breakfast"))
                planItems.add(PlanListItem.PlanItem(id = "${dayInMillis}-FOOD-$breakfastText", type = ItemType.FOOD, text = "$breakfastText ($breakfastCalories kcal, $breakfastProtein g protein)"))
                planItems.add(PlanListItem.Header("Lunch"))
                planItems.add(PlanListItem.PlanItem(id = "${dayInMillis}-FOOD-$lunchText", type = ItemType.FOOD, text = "$lunchText ($lunchCalories kcal, $lunchProtein g protein)"))
                planItems.add(PlanListItem.Header("Dinner"))
                planItems.add(PlanListItem.PlanItem(id = "${dayInMillis}-FOOD-$dinnerText", type = ItemType.FOOD, text = "$dinnerText ($dinnerCalories kcal, $dinnerProtein g protein)"))

                if (planResult.exercises.isNotEmpty()) {
                    planItems.add(PlanListItem.Header("Exercise"))
                    planItems.addAll(planResult.exercises.split("\n").filter { it.isNotBlank() }
                        .map { exerciseText ->
                            PlanListItem.PlanItem(id = "${dayInMillis}-EXERCISE-$exerciseText", type = ItemType.EXERCISE, text = exerciseText)
                        })
                }
                planByDate[dayInMillis] = planItems
            }

            if (planRepository.isDayComplete(dayInMillis)) {
                completedDates.add(dayInMillis)
            }
        }

        selectedDate = intent.getLongExtra("selectedDate", System.currentTimeMillis())
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selectedDate = calendar.timeInMillis
        binding.calendarView.date = selectedDate
        updatePlanForDate(selectedDate)
        updateStreak()

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val c = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedDate = c.timeInMillis
            updatePlanForDate(selectedDate)
        }

        binding.markDayCompleteBtn.setOnClickListener {
            planRepository.markDayAsComplete(selectedDate)
            completedDates.add(selectedDate)
            val planItems = planByDate.getOrDefault(selectedDate, emptyList())
            for (item in planItems) {
                if (item is PlanListItem.PlanItem) {
                    val textParts = item.text.split(" ")
                    val calories = textParts.findLast { it.contains("kcal") }?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                    val protein = textParts.findLast { it.contains("g") }?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                    onPlanItemCompleted(item, true, calories, protein)
                }
            }
            updatePlanForDate(selectedDate)
            updateStreak()
            Toast.makeText(this, "Day marked as complete! 🎉", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun updateStreak() {
        val streak = planRepository.getStreak()
        binding.streakText.text = "🔥 $streak Day Streak"
    }

    private fun onPlanItemCompleted(item: PlanListItem.PlanItem, isCompleted: Boolean, calories: Int, protein: Int) {
        planRepository.markPlanItemAsComplete(item.id, isCompleted, calories, protein)
    }

    private fun updatePlanForDate(date: Long) {
        val generatedPlan = planByDate.getOrDefault(date, emptyList()).toMutableList()

        if (exerciseOnlyMode) {
            val exerciseOnly = mutableListOf<PlanListItem>()
            var include = false
            for (item in generatedPlan) {
                when (item) {
                    is PlanListItem.Header -> {
                        include = item.title == "Exercise"
                        if (include) {
                            exerciseOnly.add(item)
                        }
                    }
                    is PlanListItem.PlanItem -> if (include) {
                        exerciseOnly.add(item)
                    }
                }
            }
            generatedPlan.clear()
            generatedPlan.addAll(exerciseOnly)
        }

        val extraItems = planRepository.getExtraMealItems(date)
        if (extraItems.isNotEmpty()) {
            val exerciseIndex = generatedPlan.indexOfFirst { it is PlanListItem.Header && it.title == "Exercise" }
            if (exerciseIndex != -1) {
                generatedPlan.add(exerciseIndex, PlanListItem.Header("Extras"))
                generatedPlan.addAll(exerciseIndex + 1, extraItems.map {
                    PlanListItem.PlanItem(id = it.id, type = ItemType.FOOD, text = "${it.text} (${it.calories} kcal, ${it.protein} g protein)", isCompleted = planRepository.isPlanItemComplete(it.id))
                })
            } else {
                generatedPlan.add(PlanListItem.Header("Extras"))
                generatedPlan.addAll(extraItems.map {
                    PlanListItem.PlanItem(id = it.id, type = ItemType.FOOD, text = "${it.text} (${it.calories} kcal, ${it.protein} g protein)", isCompleted = planRepository.isPlanItemComplete(it.id))
                })
            }
        }

        // Show/hide empty state
        val emptyStateText = findViewById<TextView>(R.id.emptyStateText)
        if (generatedPlan.isEmpty()) {
            emptyStateText?.visibility = View.VISIBLE
            emptyStateText?.text = if (exerciseOnlyMode) {
                getString(R.string.no_exercise_for_date)
            } else {
                getString(R.string.no_plan_for_date)
            }
            binding.planRecyclerView.visibility = View.GONE
            binding.markDayCompleteBtn.isEnabled = false
        } else {
            emptyStateText?.visibility = View.GONE
            binding.planRecyclerView.visibility = View.VISIBLE
            binding.markDayCompleteBtn.isEnabled = true
        }

        val finalPlanItems = generatedPlan.map { item ->
            if (item is PlanListItem.PlanItem) {
                item.isCompleted = planRepository.isPlanItemComplete(item.id)
            }
            item
        }

        planAdapter = PlanAdapter(finalPlanItems) { item, isCompleted ->
            val textParts = item.text.split(" ")
            val calories = textParts.findLast { it.contains("kcal") }?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val protein = textParts.findLast { it.contains("g") }?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            onPlanItemCompleted(item, isCompleted, calories, protein)
        }
        binding.planRecyclerView.adapter = planAdapter
    }

    @Suppress("DEPRECATION")
    private fun <T : Serializable?> getSerializable(intent: android.content.Intent, key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(key, clazz)
        } else {
            intent.getSerializableExtra(key) as? T
        }
    }
}
