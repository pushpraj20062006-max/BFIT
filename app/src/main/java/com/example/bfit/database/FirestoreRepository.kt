package com.example.bfit.database

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * FirestoreRepository handles all cloud data operations.
 * It works alongside Room (local cache) to provide a single source of truth architecture.
 * - Room: local cache for offline support
 * - Firestore: persistent cloud storage for cross-device sync
 */
class FirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    companion object {
        private const val TAG = "FirestoreRepository"
        private const val USERS_COLLECTION = "users"
        private const val WORKOUT_PLANS_COLLECTION = "workout_plans"
        private const val NUTRITION_LOGS_COLLECTION = "nutrition_logs"
        private const val CHAT_HISTORY_COLLECTION = "chat_history"
        private const val DAILY_LOGS_COLLECTION = "daily_logs"
        private const val SUPPLEMENTS_COLLECTION = "supplements"
        private const val PURCHASES_COLLECTION = "purchases"
    }

    // ==================== USER PROFILE ====================

    suspend fun saveUserProfile(profileData: Map<String, Any>) {
        val uid = currentUserId ?: return
        try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .set(profileData, SetOptions.merge())
                .await()
            Log.d(TAG, "User profile saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile", e)
        }
    }

    suspend fun getUserProfile(): Map<String, Any>? {
        val uid = currentUserId ?: return null
        return try {
            val doc = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
            doc.data
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile", e)
            null
        }
    }

    // ==================== WORKOUT PLANS ====================

    suspend fun saveWorkoutPlan(planData: Map<String, Any>) {
        val uid = currentUserId ?: return
        try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(WORKOUT_PLANS_COLLECTION)
                .document("current_plan")
                .set(planData)
                .await()
            Log.d(TAG, "Workout plan saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving workout plan", e)
        }
    }

    suspend fun getWorkoutPlan(): Map<String, Any>? {
        val uid = currentUserId ?: return null
        return try {
            val doc = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(WORKOUT_PLANS_COLLECTION)
                .document("current_plan")
                .get()
                .await()
            doc.data
        } catch (e: Exception) {
            Log.e(TAG, "Error getting workout plan", e)
            null
        }
    }

    // ==================== NUTRITION / DAILY LOGS ====================

    suspend fun saveDailyLog(date: Long, calories: Int, protein: Int, carbs: Int = 0, fats: Int = 0) {
        val uid = currentUserId ?: return
        try {
            val logData = hashMapOf(
                "date" to date,
                "totalCalories" to calories,
                "totalProtein" to protein,
                "totalCarbs" to carbs,
                "totalFats" to fats,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(DAILY_LOGS_COLLECTION)
                .document(date.toString())
                .set(logData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving daily log", e)
        }
    }

    suspend fun getDailyLog(date: Long): Map<String, Any>? {
        val uid = currentUserId ?: return null
        return try {
            val doc = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(DAILY_LOGS_COLLECTION)
                .document(date.toString())
                .get()
                .await()
            doc.data
        } catch (e: Exception) {
            Log.e(TAG, "Error getting daily log", e)
            null
        }
    }

    suspend fun addFoodToLog(date: Long, foodName: String, calories: Int, protein: Int, carbs: Int = 0, fats: Int = 0) {
        val uid = currentUserId ?: return
        try {
            val foodData = hashMapOf(
                "name" to foodName,
                "calories" to calories,
                "protein" to protein,
                "carbs" to carbs,
                "fats" to fats,
                "date" to date,
                "addedAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(NUTRITION_LOGS_COLLECTION)
                .add(foodData)
                .await()

            // Also update the daily totals
            val currentLog = getDailyLog(date)
            val currentCalories = (currentLog?.get("totalCalories") as? Long)?.toInt() ?: 0
            val currentProtein = (currentLog?.get("totalProtein") as? Long)?.toInt() ?: 0
            val currentCarbs = (currentLog?.get("totalCarbs") as? Long)?.toInt() ?: 0
            val currentFats = (currentLog?.get("totalFats") as? Long)?.toInt() ?: 0

            saveDailyLog(
                date,
                currentCalories + calories,
                currentProtein + protein,
                currentCarbs + carbs,
                currentFats + fats
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error adding food to log", e)
        }
    }

    // ==================== CHAT HISTORY ====================

    suspend fun saveChatMessage(message: String, isUser: Boolean, sessionId: String) {
        val uid = currentUserId ?: return
        try {
            val chatData = hashMapOf(
                "message" to message,
                "isUser" to isUser,
                "sessionId" to sessionId,
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(CHAT_HISTORY_COLLECTION)
                .add(chatData)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving chat message", e)
        }
    }

    suspend fun getChatHistory(sessionId: String? = null): List<Map<String, Any>> {
        val uid = currentUserId ?: return emptyList()
        return try {
            val query = if (sessionId != null) {
                firestore.collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(CHAT_HISTORY_COLLECTION)
                    .whereEqualTo("sessionId", sessionId)
                    .orderBy("timestamp")
            } else {
                firestore.collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(CHAT_HISTORY_COLLECTION)
                    .orderBy("timestamp")
                    .limit(100)
            }
            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat history", e)
            emptyList()
        }
    }

    suspend fun getChatSessions(): List<Map<String, Any>> {
        val uid = currentUserId ?: return emptyList()
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(CHAT_HISTORY_COLLECTION)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            // Group by sessionId and get latest message per session
            val sessions = snapshot.documents
                .mapNotNull { it.data }
                .groupBy { it["sessionId"] as? String ?: "" }
                .map { (sessionId, messages) ->
                    val firstUserMsg = messages.firstOrNull { it["isUser"] == true }
                    hashMapOf<String, Any>(
                        "sessionId" to sessionId,
                        "preview" to (firstUserMsg?.get("message") as? String ?: "Chat session"),
                        "messageCount" to messages.size,
                        "timestamp" to (messages.first()["timestamp"] ?: 0L)
                    )
                }
            sessions
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat sessions", e)
            emptyList()
        }
    }

    // ==================== SUPPLEMENTS / STORE ====================

    suspend fun getSupplements(): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection(SUPPLEMENTS_COLLECTION)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.plus("id" to doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting supplements", e)
            emptyList()
        }
    }

    suspend fun recordPurchase(supplementName: String, supplementId: String, price: Double) {
        val uid = currentUserId ?: return
        try {
            val purchaseData = hashMapOf(
                "supplementName" to supplementName,
                "supplementId" to supplementId,
                "price" to price,
                "status" to "completed",
                "purchasedAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(PURCHASES_COLLECTION)
                .add(purchaseData)
                .await()
            Log.d(TAG, "Purchase recorded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording purchase", e)
        }
    }

    suspend fun getPurchaseHistory(): List<Map<String, Any>> {
        val uid = currentUserId ?: return emptyList()
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(PURCHASES_COLLECTION)
                .orderBy("purchasedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting purchase history", e)
            emptyList()
        }
    }
}
