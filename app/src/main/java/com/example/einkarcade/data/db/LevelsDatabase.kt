package com.example.einkarcade.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LevelSetEntity::class,
        LevelEntity::class,
        PuzzleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LevelsDatabase : RoomDatabase() {
    abstract fun levelsDao(): LevelsDao

    companion object {
        @Volatile
        private var instance: LevelsDatabase? = null
        fun getInstance(context: Context): LevelsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LevelsDatabase::class.java,
                    "einkarcade.db"
                )
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
