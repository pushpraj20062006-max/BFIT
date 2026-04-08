package com.example.bfit.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for BFIT application.
 * Stores plan item completions, daily nutrition logs,
 * extra meal items, and weight entries.
 *
 * Version History:
 * v3 → v4: Added protein column to extra_meal_items
 * v4 → v5: Added weight_entries table for weight tracking
 */
@Database(
    entities = [
        PlanItemCompletion::class,
        DailyLog::class,
        ExtraMealItem::class,
        WeightEntry::class
    ],
    version = 5,
    exportSchema = false
)
abstract class PlanDatabase : RoomDatabase() {

    abstract fun planDao(): PlanDao

    companion object {
        @Volatile
        private var INSTANCE: PlanDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE extra_meal_items ADD COLUMN protein INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS weight_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        weight REAL NOT NULL,
                        bmi REAL NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): PlanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlanDatabase::class.java,
                    "plan_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
