package com.example.bfit.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PlanItemCompletion::class, DailyLog::class, ExtraMealItem::class], version = 4, exportSchema = false)
abstract class PlanDatabase : RoomDatabase() {

    abstract fun planDao(): PlanDao

    companion object {
        @Volatile
        private var INSTANCE: PlanDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE extra_meal_items ADD COLUMN protein INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): PlanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlanDatabase::class.java,
                    "plan_database"
                ).addMigrations(MIGRATION_3_4).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
