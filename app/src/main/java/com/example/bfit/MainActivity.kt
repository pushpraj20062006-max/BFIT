package com.example.bfit

import android.animation.LayoutTransition
import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.bfit.database.ExtraMealItem
import com.example.bfit.database.FirestoreRepository
import com.example.bfit.database.PlanRepository
import com.example.bfit.network.RetrofitInstance
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.Calendar
import java.util.Locale

data class PlanResult(
    val category: String,
    val calories: Int,
    val totalProtein: Int,
    val mealPlan: Map<String, List<Triple<String, Int, Int>>>,
    val exercises: String
) : Serializable

class MainActivity : AppCompatActivity() {

    private var isCm = true
    private var currentPlan: PlanResult? = null
    private var currentBmi: Float = 0f
    private var currentGoal: String = ""
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private lateinit var planRepository: PlanRepository
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var auth: FirebaseAuth

    private lateinit var inputForm: LinearLayout
    private lateinit var dashboardView: LinearLayout
    private lateinit var streakText: TextView

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val barcode = it.data?.getStringExtra("barcode")
            if (barcode != null) {
                fetchFoodData(barcode)
            }
        }
    }

    private fun fetchFoodData(barcode: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getFoodData(barcode)
                val product = response.product
                if (product != null) {
                    val productName = product.productName ?: "Unknown Product"
                    val calories = product.nutriments?.energyKcal100g ?: 0.0
                    val protein = product.nutriments?.proteins_100g ?: 0.0
                    val carbs = product.nutriments?.carbohydrates_100g ?: 0.0
                    val fats = product.nutriments?.fat_100g ?: 0.0
                    showFoodInfoDialog(productName, calories, protein, carbs, fats)
                } else {
                    Toast.makeText(this@MainActivity, "Product not found", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error fetching data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showFoodInfoDialog(productName: String, calories: Double, protein: Double, carbs: Double, fats: Double) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_food_info, null)
        val productNameText = dialogView.findViewById<TextView>(R.id.productNameText)
        val caloriesText = dialogView.findViewById<TextView>(R.id.caloriesText)
        val proteinText = dialogView.findViewById<TextView>(R.id.proteinText)
        val carbsText = dialogView.findViewById<TextView>(R.id.carbsText)
        val fatsText = dialogView.findViewById<TextView>(R.id.fatsText)
        val proteinProgress = dialogView.findViewById<LinearProgressIndicator>(R.id.proteinProgress)
        val carbsProgress = dialogView.findViewById<LinearProgressIndicator>(R.id.carbsProgress)
        val fatsProgress = dialogView.findViewById<LinearProgressIndicator>(R.id.fatsProgress)

        productNameText.text = productName
        caloriesText.text = "${calories.toInt()} kcal per 100g"
        proteinText.text = "${protein.toInt()}g"
        carbsText.text = "${carbs.toInt()}g"
        fatsText.text = "${fats.toInt()}g"

        // Calculate progress (as percentage of 100g maxes)
        val totalMacros = protein + carbs + fats
        if (totalMacros > 0) {
            proteinProgress.progress = ((protein / totalMacros) * 100).toInt()
            carbsProgress.progress = ((carbs / totalMacros) * 100).toInt()
            fatsProgress.progress = ((fats / totalMacros) * 100).toInt()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Add to Daily Log") { _, _ ->
                showDatePicker(productName, calories.toInt(), protein.toInt(), carbs.toInt(), fats.toInt())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker(productName: String, calories: Int, protein: Int, carbs: Int = 0, fats: Int = 0) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, {
            _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay)
            selectedDate.set(Calendar.HOUR_OF_DAY, 0)
            selectedDate.set(Calendar.MINUTE, 0)
            selectedDate.set(Calendar.SECOND, 0)
            selectedDate.set(Calendar.MILLISECOND, 0)
            addExtraMealItem(productName, calories, protein, selectedDate.timeInMillis, carbs, fats)
        }, year, month, day).show()
    }

    private fun addExtraMealItem(name: String, calories: Int, protein: Int, date: Long, carbs: Int = 0, fats: Int = 0) {
        val extraMealItem = ExtraMealItem(
            id = "${date}-FOOD-$name",
            date = date,
            text = name,
            calories = calories,
            protein = protein
        )
        planRepository.addExtraMealItem(extraMealItem)
        planRepository.addCaloriesToDailyLog(date, calories, protein)

        // Also sync to Firestore
        lifecycleScope.launch {
            firestoreRepository.addFoodToLog(date, name, calories, protein, carbs, fats)
        }

        navigateToPlanner(date)
    }

    private fun navigateToPlanner(date: Long) {
        val intent = Intent(this, PlannerActivity::class.java)
        intent.putExtra("plan", currentPlan)
        intent.putExtra("selectedDate", date)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        firestoreRepository = FirestoreRepository()
        sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE)
        planRepository = PlanRepository(this)

        // Check if user is authenticated or in demo mode
        val isDemoMode = sharedPreferences.getBoolean(LoginActivity.DEMO_MODE_KEY, false)
        if (auth.currentUser == null && !isDemoMode) {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
            return
        }

        inputForm = findViewById(R.id.inputForm)
        dashboardView = findViewById(R.id.dashboardView)
        streakText = findViewById(R.id.streakText)

        dashboardView.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val ageInput = inputForm.findViewById<EditText>(R.id.ageInput)
        val heightInput = inputForm.findViewById<TextInputEditText>(R.id.heightInput)
        val heightInputLayout = inputForm.findViewById<TextInputLayout>(R.id.heightInputLayout)
        val weightInput = inputForm.findViewById<EditText>(R.id.weightInput)
        val genderRadioGroup = inputForm.findViewById<RadioGroup>(R.id.genderRadioGroup)
        val calcBtn = inputForm.findViewById<Button>(R.id.calcBtn)
        val scanBtn = findViewById<Button>(R.id.scanBtn)
        val fabChat = findViewById<FloatingActionButton>(R.id.fabChat)
        val dietaryPreference = inputForm.findViewById<AutoCompleteTextView>(R.id.dietaryPreference)
        val bodyGoal = inputForm.findViewById<AutoCompleteTextView>(R.id.bodyGoal)
        val allergiesInput = inputForm.findViewById<TextInputEditText>(R.id.allergiesInput)
        val lactoseIntolerantSwitch = inputForm.findViewById<SwitchMaterial>(R.id.lactoseIntolerantSwitch)

        val bodyGoalItems = arrayOf("Bulk", "Lean", "Maintain")
        val bodyGoalAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bodyGoalItems)
        bodyGoal.setAdapter(bodyGoalAdapter)

        val dietItems = arrayOf("Veg", "Non-Veg", "Keto", "Vegan", "Paleo", "High-Protein")
        val dietAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dietItems)
        dietaryPreference.setAdapter(dietAdapter)

        heightInputLayout.setEndIconOnClickListener {
            isCm = !isCm
            val currentHeight = heightInput.text.toString().toFloatOrNull()
            if (currentHeight != null) {
                if (isCm) {
                    heightInputLayout.hint = getString(R.string.height_cm)
                    heightInput.setText(String.format(Locale.getDefault(), "%.0f", currentHeight * 30.48))
                } else {
                    heightInputLayout.hint = getString(R.string.height_ft)
                    heightInput.setText(String.format(Locale.getDefault(), "%.2f", currentHeight / 30.48))
                }
            }
        }

        calcBtn.setOnClickListener {
            val age = ageInput.text.toString()
            var height = heightInput.text.toString()
            val weight = weightInput.text.toString()
            val selectedGender = if (genderRadioGroup.checkedRadioButtonId == R.id.maleRadio) "Male" else "Female"
            val selectedDiet = dietaryPreference.text.toString()
            val goal = bodyGoal.text.toString()
            val allergies = allergiesInput.text.toString()
            val isLactoseIntolerant = lactoseIntolerantSwitch.isChecked

            if (age.isEmpty() || height.isEmpty() || weight.isEmpty() || goal.isEmpty() || selectedDiet.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isCm) {
                height = (height.toFloat() * 30.48).toString()
            }

            val h = height.toFloat() / 100
            val w = weight.toFloat()

            val bmi = w / (h * h)
            currentBmi = bmi
            currentGoal = goal
            currentPlan = getPlan(bmi, selectedGender, selectedDiet, goal, allergies.split(",").map { it.trim().lowercase() }, isLactoseIntolerant)
            saveUserData(age, height, weight, selectedGender, selectedDiet, goal, allergies, isLactoseIntolerant, currentPlan!!)

            // Sync profile and plan to Firestore
            lifecycleScope.launch {
                firestoreRepository.saveUserProfile(mapOf(
                    "height" to height.toFloat(),
                    "weight" to w,
                    "age" to age.toInt(),
                    "gender" to selectedGender,
                    "diet" to selectedDiet,
                    "healthGoal" to goal,
                    "allergies" to allergies,
                    "isLactoseIntolerant" to isLactoseIntolerant
                ))

                firestoreRepository.saveWorkoutPlan(mapOf(
                    "category" to currentPlan!!.category,
                    "calories" to currentPlan!!.calories,
                    "totalProtein" to currentPlan!!.totalProtein,
                    "exercises" to currentPlan!!.exercises,
                    "mealPlan" to gson.toJson(currentPlan!!.mealPlan)
                ))
            }

            showDashboard()
        }

        scanBtn.setOnClickListener {
            launchScanner()
        }

        // Dashboard scan button
        val scanDashBtn = dashboardView.findViewById<Button>(R.id.scanDashBtn)
        scanDashBtn.setOnClickListener {
            launchScanner()
        }

        val viewPlanBtn = dashboardView.findViewById<Button>(R.id.viewPlanBtn)
        viewPlanBtn.setOnClickListener {
            val intent = Intent(this, PlannerActivity::class.java)
            intent.putExtra("plan", currentPlan)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val storeBtn = dashboardView.findViewById<Button>(R.id.storeBtn)
        storeBtn.setOnClickListener {
            val intent = Intent(this, StoreActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        fabChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Logout button - Firebase sign out
        val logoutBtn = dashboardView.findViewById<Button>(R.id.logoutBtn)
        logoutBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    auth.signOut()
                    clearUserData()
                    startActivity(Intent(this, LoginActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Profile button
        val profileBtn = dashboardView.findViewById<Button>(R.id.profileBtn)
        profileBtn.setOnClickListener {
            showProfileDialog()
        }

        loadUserData()
    }

    private fun launchScanner() {
        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun showProfileDialog() {
        val user = auth.currentUser
        val isDemoMode = sharedPreferences.getBoolean(LoginActivity.DEMO_MODE_KEY, false)

        // Handle demo mode where there is no Firebase user
        if (user == null && !isDemoMode) return

        if (isDemoMode && user == null) {
            // Demo mode — show local data
            val height = sharedPreferences.getString("height", "Not set") ?: "Not set"
            val weight = sharedPreferences.getString("weight", "Not set") ?: "Not set"
            val goal = sharedPreferences.getString("goal", "Not set") ?: "Not set"
            val diet = sharedPreferences.getString("diet", "Not set") ?: "Not set"

            val message = """
                👤 Mode: Demo
                📏 Height: ${height}cm
                ⚖️ Weight: ${weight}kg
                🎯 Goal: $goal
                🥗 Diet: $diet
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Your Profile")
                .setMessage(message)
                .setPositiveButton("Edit") { _, _ ->
                    clearUserData()
                    currentPlan = null
                    showInputForm()
                }
                .setNegativeButton("Close", null)
                .show()
            return
        }

        lifecycleScope.launch {
            val profile = firestoreRepository.getUserProfile()
            val email = user?.email ?: "N/A"
            val displayName = profile?.get("displayName") as? String ?: user?.displayName ?: "N/A"
            val height = profile?.get("height")
            val weight = profile?.get("weight")
            val goal = profile?.get("healthGoal") as? String ?: "N/A"
            val diet = profile?.get("diet") as? String ?: "N/A"

            val heightStr = when (height) {
                is Number -> "${height.toFloat()} cm"
                else -> "Not set"
            }
            val weightStr = when (weight) {
                is Number -> "${weight.toFloat()} kg"
                else -> "Not set"
            }

            val message = """
                📧 Email: $email
                👤 Name: $displayName
                📏 Height: $heightStr
                ⚖️ Weight: $weightStr
                🎯 Goal: $goal
                🥗 Diet: $diet
            """.trimIndent()

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Your Profile")
                .setMessage(message)
                .setPositiveButton("Edit") { _, _ ->
                    clearUserData()
                    currentPlan = null
                    showInputForm()
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentPlan != null) {
            showDashboard()
        }
    }

    private fun saveUserData(age: String, height: String, weight: String, gender: String, diet: String, goal: String, allergies: String, isLactoseIntolerant: Boolean, plan: PlanResult) {
        sharedPreferences.edit {
            putString("age", age)
            putString("height", height)
            putString("weight", weight)
            putString("gender", gender)
            putString("diet", diet)
            putString("goal", goal)
            putString("allergies", allergies)
            putBoolean("isLactoseIntolerant", isLactoseIntolerant)
            putString("plan", gson.toJson(plan))
            putFloat("bmi", currentBmi)
            putString("currentGoal", currentGoal)
        }
    }

    private fun loadUserData() {
        val planJson = sharedPreferences.getString("plan", null)
        if (planJson != null) {
            val type = object : TypeToken<PlanResult>() {}.type
            currentPlan = gson.fromJson(planJson, type)
            currentBmi = sharedPreferences.getFloat("bmi", 0f)
            currentGoal = sharedPreferences.getString("currentGoal", "") ?: ""
            showDashboard()
        } else {
            showInputForm()
        }
    }

    private fun clearUserData() {
        sharedPreferences.edit { clear() }
        currentPlan = null
        currentBmi = 0f
        currentGoal = ""
    }

    private fun showDashboard() {
        inputForm.visibility = View.GONE
        dashboardView.visibility = View.VISIBLE

        // Show welcome message with user name
        val welcomeText = dashboardView.findViewById<TextView>(R.id.welcomeText)
        val isDemoMode = sharedPreferences.getBoolean(LoginActivity.DEMO_MODE_KEY, false)
        val displayName = if (isDemoMode) {
            "Athlete"
        } else {
            auth.currentUser?.displayName
                ?: auth.currentUser?.email?.substringBefore("@")
                ?: "Athlete"
        }
        welcomeText?.text = "Welcome, $displayName!"

        // Show BMI card
        val bmiCard = dashboardView.findViewById<MaterialCardView>(R.id.bmiCard)
        val bmiValueText = dashboardView.findViewById<TextView>(R.id.bmiValueText)
        val bmiCategoryText = dashboardView.findViewById<TextView>(R.id.bmiCategoryText)
        val bmiGoalText = dashboardView.findViewById<TextView>(R.id.bmiGoalText)

        if (currentBmi > 0) {
            bmiCard.visibility = View.VISIBLE
            bmiValueText.text = String.format(Locale.getDefault(), "BMI: %.1f", currentBmi)
            bmiCategoryText.text = currentPlan?.category ?: ""
            bmiGoalText.text = if (currentGoal.isNotEmpty()) "Goal: $currentGoal" else ""
        } else {
            bmiCard.visibility = View.GONE
        }

        val dailyPlanRecyclerView = dashboardView.findViewById<RecyclerView>(R.id.dailyPlanRecyclerView)

        currentPlan?.let { plan ->
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            val dayStart = today.timeInMillis
            val dayOfWeek = ((today.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1).toString()
            val mealPlanForToday = plan.mealPlan[dayOfWeek]
            val dailyPlanItems = mutableListOf<PlanListItem>()

            if (mealPlanForToday != null && mealPlanForToday.size >= 3) {
                val (breakfastText, breakfastCalories, breakfastProtein) = mealPlanForToday[0]
                val (lunchText, lunchCalories, lunchProtein) = mealPlanForToday[1]
                val (dinnerText, dinnerCalories, dinnerProtein) = mealPlanForToday[2]

                dailyPlanItems.add(PlanListItem.Header("Breakfast"))
                dailyPlanItems.add(PlanListItem.PlanItem(id = "$dayStart-FOOD-$breakfastText", type = ItemType.FOOD, text = "$breakfastText ($breakfastCalories kcal, $breakfastProtein g protein)"))
                dailyPlanItems.add(PlanListItem.Header("Lunch"))
                dailyPlanItems.add(PlanListItem.PlanItem(id = "$dayStart-FOOD-$lunchText", type = ItemType.FOOD, text = "$lunchText ($lunchCalories kcal, $lunchProtein g protein)"))
                dailyPlanItems.add(PlanListItem.Header("Dinner"))
                dailyPlanItems.add(PlanListItem.PlanItem(id = "$dayStart-FOOD-$dinnerText", type = ItemType.FOOD, text = "$dinnerText ($dinnerCalories kcal, $dinnerProtein g protein)"))
            }

            val extraItems = planRepository.getExtraMealItems(dayStart)
            if (extraItems.isNotEmpty()) {
                dailyPlanItems.add(PlanListItem.Header("Extras"))
                dailyPlanItems.addAll(extraItems.map {
                    PlanListItem.PlanItem(id = it.id, type = ItemType.FOOD, text = "${it.text} (${it.calories} kcal, ${it.protein} g protein)", isCompleted = planRepository.isPlanItemComplete(it.id))
                })
            }

            if (plan.exercises.isNotEmpty()) {
                dailyPlanItems.add(PlanListItem.Header("Exercise"))
                dailyPlanItems.addAll(plan.exercises.split("\n").filter { it.isNotBlank() }
                    .map { exerciseText ->
                        PlanListItem.PlanItem(id = "$dayStart-EXERCISE-$exerciseText", type = ItemType.EXERCISE, text = exerciseText)
                    })
            }

            var totalCalories = 0
            var totalProtein = 0
            val finalPlanItems = dailyPlanItems.map { item ->
                if (item is PlanListItem.PlanItem) {
                    item.isCompleted = planRepository.isPlanItemComplete(item.id)
                    if (item.isCompleted) {
                        val kcalRegex = "(\\d+)\\s*kcal".toRegex()
                        val proteinRegex = "(\\d+)\\s*g".toRegex()
                        totalCalories += kcalRegex.find(item.text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        totalProtein += proteinRegex.find(item.text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }
                }
                item
            }
            planRepository.updateDailyLog(dayStart, totalCalories, totalProtein)

            // Sync daily log to Firestore
            lifecycleScope.launch {
                firestoreRepository.saveDailyLog(dayStart, totalCalories, totalProtein)
            }

            updateDashboardCalories()
            updateStreak()

            dailyPlanRecyclerView.adapter = PlanAdapter(finalPlanItems) { item, isCompleted ->
                val kcalRegex = "(\\d+)\\s*kcal".toRegex()
                val proteinRegex = "(\\d+)\\s*g".toRegex()
                val calories = kcalRegex.find(item.text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val protein = proteinRegex.find(item.text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                planRepository.markPlanItemAsComplete(item.id, isCompleted, calories, protein)
                if(isCompleted) {
                    planRepository.markDayAsComplete(dayStart)
                }
                updateDashboardCalories()
                updateStreak()

                // Sync to Firestore
                lifecycleScope.launch {
                    val log = planRepository.getDailyLog(dayStart)
                    firestoreRepository.saveDailyLog(
                        dayStart,
                        log?.totalCalories ?: 0,
                        log?.totalProtein ?: 0
                    )
                }
            }
        }
    }

    private fun updateDashboardCalories() {
        val dailyCaloriesText = dashboardView.findViewById<TextView>(R.id.dailyCaloriesText)
        val calorieProgressIndicator = dashboardView.findViewById<CircularProgressIndicator>(R.id.calorieProgressIndicator)
        val calorieProgressText = dashboardView.findViewById<TextView>(R.id.calorieProgressText)
        val proteinProgressIndicator = dashboardView.findViewById<LinearProgressIndicator>(R.id.proteinProgressIndicator)
        val proteinProgressText = dashboardView.findViewById<TextView>(R.id.proteinProgressText)

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val dailyLog = planRepository.getDailyLog(today.timeInMillis)

        val caloriesConsumed = dailyLog?.totalCalories ?: 0
        val totalCalories = currentPlan?.calories ?: 0
        val proteinConsumed = dailyLog?.totalProtein ?: 0
        val totalProtein = currentPlan?.totalProtein ?: 0

        dailyCaloriesText.text = getString(R.string.calories_consumed_format, caloriesConsumed, totalCalories)
        calorieProgressText.text = getString(R.string.calorie_progress_format, caloriesConsumed)

        if (totalCalories > 0) {
            calorieProgressIndicator.progress = ((caloriesConsumed * 100) / totalCalories).coerceAtMost(100)
        }

        proteinProgressText.text = getString(R.string.protein_consumed_format, proteinConsumed, totalProtein)
        if (totalProtein > 0) {
            proteinProgressIndicator.progress = ((proteinConsumed * 100) / totalProtein).coerceAtMost(100)
        }
    }

    private fun updateStreak() {
        val streak = planRepository.getStreak()
        streakText.text = getString(R.string.streak_format, streak)
    }

    private fun showInputForm() {
        inputForm.visibility = View.VISIBLE
        dashboardView.visibility = View.GONE
    }

    private fun getPlan(bmi: Float, gender: String, diet: String, goal: String, allergies: List<String>, isLactoseIntolerant: Boolean): PlanResult {
        val resources = resources
        val (baseCalories, mealArrays) = when {
            bmi < 18.5 -> (if (gender == "Male") 3000 else 2600) to getMealArraysForDiet(diet, "underweight")
            bmi in 18.5..24.9 -> (if (gender == "Male") 2500 else 2100) to getMealArraysForDiet(diet, "normal_weight")
            bmi in 25.0..29.9 -> (if (gender == "Male") 2000 else 1700) to getMealArraysForDiet(diet, "overweight")
            else -> (if (gender == "Male") 1700 else 1400) to getMealArraysForDiet(diet, "obese")
        }

        val (breakfastArray, lunchArray, dinnerArray) = mealArrays

        val breakfastOptions = resources.getStringArray(breakfastArray).toList()
        val lunchOptions = resources.getStringArray(lunchArray).toList()
        val dinnerOptions = resources.getStringArray(dinnerArray).toList()

        val finalCalories = when (goal) {
            "Bulk" -> baseCalories + 500
            "Lean" -> baseCalories - 500
            else -> baseCalories
        }

        val filter = { list: List<String> ->
            var filtered = list
            if (isLactoseIntolerant) {
                filtered = filtered.filterNot { it.contains("Milk", ignoreCase = true) || it.contains("Yogurt", ignoreCase = true) || it.contains("Paneer", ignoreCase = true) || it.contains("Cheese", ignoreCase = true) }
            }
            allergies.forEach { allergy ->
                if (allergy.isNotBlank()) {
                    filtered = filtered.filterNot { it.contains(allergy, ignoreCase = true) }
                }
            }
            filtered
        }

        val filteredBreakfasts = filter(breakfastOptions).shuffled()
        val filteredLunches = filter(lunchOptions).shuffled()
        val filteredDinners = filter(dinnerOptions).shuffled()

        val mealPlan = (1..7).associate { day ->
            day.toString() to listOf(
                filteredBreakfasts.getOrElse(day - 1) { filteredBreakfasts.random() }.split("|").let { Triple(it[0], it[2].toInt(), it[3].toInt()) },
                filteredLunches.getOrElse(day - 1) { filteredLunches.random() }.split("|").let { Triple(it[0], it[2].toInt(), it[3].toInt()) },
                filteredDinners.getOrElse(day - 1) { filteredDinners.random() }.split("|").let { Triple(it[0], it[2].toInt(), it[3].toInt()) }
            )
        }

        val totalProtein = mealPlan.values.flatten().sumOf { it.third }

        val category = when {
            bmi < 18.5 -> "Underweight"
            bmi in 18.5..24.9 -> "Normal Weight"
            bmi in 25.0..29.9 -> "Overweight"
            else -> "Obese"
        }

        val exerciseOptionsArray = when (goal) {
            "Bulk" -> R.array.bulk_exercises
            "Lean" -> R.array.lean_exercises
            else -> R.array.maintain_exercises
        }
        val exercises = resources.getStringArray(exerciseOptionsArray).toList()

        return PlanResult(category, finalCalories, totalProtein, mealPlan, exercises.joinToString("\n") { "- $it" })
    }

    private fun getMealArraysForDiet(diet: String, weightCategory: String): Triple<Int, Int, Int> {
        return when (diet) {
            "Veg" -> when (weightCategory) {
                "underweight" -> Triple(R.array.underweight_veg_breakfast, R.array.underweight_veg_lunch, R.array.underweight_veg_dinner)
                "normal_weight" -> Triple(R.array.normal_weight_veg_breakfast, R.array.normal_weight_veg_lunch, R.array.normal_weight_veg_dinner)
                "overweight" -> Triple(R.array.overweight_veg_breakfast, R.array.overweight_veg_lunch, R.array.overweight_veg_dinner)
                else -> Triple(R.array.obese_veg_breakfast, R.array.obese_veg_lunch, R.array.obese_veg_dinner)
            }
            "Non-Veg" -> when (weightCategory) {
                "underweight" -> Triple(R.array.underweight_non_veg_breakfast, R.array.underweight_non_veg_lunch, R.array.underweight_non_veg_dinner)
                "normal_weight" -> Triple(R.array.normal_weight_non_veg_breakfast, R.array.normal_weight_non_veg_lunch, R.array.normal_weight_non_veg_dinner)
                "overweight" -> Triple(R.array.overweight_non_veg_breakfast, R.array.overweight_non_veg_lunch, R.array.overweight_non_veg_dinner)
                else -> Triple(R.array.obese_non_veg_breakfast, R.array.obese_non_veg_lunch, R.array.obese_non_veg_dinner)
            }
            "Keto" -> when (weightCategory) {
                "underweight" -> Triple(R.array.underweight_keto_breakfast, R.array.underweight_keto_lunch, R.array.underweight_keto_dinner)
                "normal_weight" -> Triple(R.array.normal_weight_keto_breakfast, R.array.normal_weight_keto_lunch, R.array.normal_weight_keto_dinner)
                "overweight" -> Triple(R.array.overweight_keto_breakfast, R.array.overweight_keto_lunch, R.array.overweight_keto_dinner)
                else -> Triple(R.array.obese_keto_breakfast, R.array.obese_keto_lunch, R.array.obese_keto_dinner)
            }
            "Vegan" -> when (weightCategory) {
                "underweight" -> Triple(R.array.underweight_vegan_breakfast, R.array.underweight_vegan_lunch, R.array.underweight_vegan_dinner)
                "normal_weight" -> Triple(R.array.normal_weight_vegan_breakfast, R.array.normal_weight_vegan_lunch, R.array.normal_weight_vegan_dinner)
                "overweight" -> Triple(R.array.overweight_vegan_breakfast, R.array.overweight_vegan_lunch, R.array.overweight_vegan_dinner)
                else -> Triple(R.array.obese_vegan_breakfast, R.array.obese_vegan_lunch, R.array.obese_vegan_dinner)
            }
            "Paleo" -> when (weightCategory) {
                "underweight" -> Triple(R.array.underweight_paleo_breakfast, R.array.underweight_paleo_lunch, R.array.underweight_paleo_dinner)
                "normal_weight" -> Triple(R.array.normal_weight_paleo_breakfast, R.array.normal_weight_paleo_lunch, R.array.normal_weight_paleo_dinner)
                "overweight" -> Triple(R.array.overweight_paleo_breakfast, R.array.overweight_paleo_lunch, R.array.overweight_paleo_dinner)
                else -> Triple(R.array.obese_paleo_breakfast, R.array.obese_paleo_lunch, R.array.obese_paleo_dinner)
            }
            "High-Protein" -> when (weightCategory) {
                "underweight" -> Triple(R.array.underweight_high_protein_breakfast, R.array.underweight_high_protein_lunch, R.array.underweight_high_protein_dinner)
                "normal_weight" -> Triple(R.array.normal_weight_high_protein_breakfast, R.array.normal_weight_high_protein_lunch, R.array.normal_weight_high_protein_dinner)
                "overweight" -> Triple(R.array.overweight_high_protein_breakfast, R.array.overweight_high_protein_lunch, R.array.overweight_high_protein_dinner)
                else -> Triple(R.array.obese_high_protein_breakfast, R.array.obese_high_protein_lunch, R.array.obese_high_protein_dinner)
            }
            else -> Triple(R.array.normal_weight_veg_breakfast, R.array.normal_weight_veg_lunch, R.array.normal_weight_veg_dinner)
        }
    }
}
