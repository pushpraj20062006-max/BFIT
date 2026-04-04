package com.example.bfit

import android.app.Application
import com.example.bfit.database.AppDatabase
import com.google.firebase.FirebaseApp
import com.clerk.android.Clerk

class BFITApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase (for Firestore backend)
        FirebaseApp.initializeApp(this)
        
        // Initialize Clerk (Simplified Authentication)
        // No complex JSON files—just one line to get started!
        Clerk.initialize(this, BuildConfig.CLERK_PUBLISHABLE_KEY)
    }
}
