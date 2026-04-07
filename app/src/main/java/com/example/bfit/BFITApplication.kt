package com.example.bfit

import android.app.Application
import com.example.bfit.database.AppDatabase
import com.google.firebase.FirebaseApp

class BFITApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase (for Auth + Firestore)
        FirebaseApp.initializeApp(this)
    }
}
