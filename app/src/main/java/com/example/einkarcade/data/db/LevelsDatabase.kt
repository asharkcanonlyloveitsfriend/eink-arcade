package com.example.einkarcade.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LevelSetEntity::class,
        LevelEntity::class,
        PuzzleEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class LevelsDatabase : RoomDatabase() {
    abstract fun levelsDao(): LevelsDao

    companion object {
        private fun migrationToVersion6(
            fromVersion: Int,
            hasUserSolution: Boolean,
        ): Migration =
            object : Migration(fromVersion, 6) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        """
                        CREATE TABLE puzzles_new (
                            id INTEGER NOT NULL PRIMARY KEY,
                            grid TEXT NOT NULL,
                            last_completed_at TEXT,
                            user_solution TEXT
                        )
                        """.trimIndent(),
                    )
                    database.execSQL(
                        """
                        CREATE TABLE levels_backup (
                            id INTEGER NOT NULL PRIMARY KEY,
                            title TEXT NOT NULL,
                            level_set_id INTEGER NOT NULL,
                            puzzle_id INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    database.execSQL(
                        if (hasUserSolution) {
                            """
                            INSERT INTO puzzles_new (id, grid, last_completed_at, user_solution)
                            SELECT id, grid, last_completed_at, user_solution FROM puzzles
                            """.trimIndent()
                        } else {
                            """
                            INSERT INTO puzzles_new (id, grid, last_completed_at, user_solution)
                            SELECT id, grid, last_completed_at, NULL FROM puzzles
                            """.trimIndent()
                        },
                    )
                    database.execSQL(
                        """
                        INSERT INTO levels_backup (id, title, level_set_id, puzzle_id)
                        SELECT id, title, level_set_id, puzzle_id FROM levels
                        """.trimIndent(),
                    )
                    database.execSQL("DROP TABLE levels")
                    database.execSQL("DROP TABLE puzzles")
                    database.execSQL("ALTER TABLE puzzles_new RENAME TO puzzles")
                    database.execSQL(
                        """
                        CREATE TABLE levels (
                            id INTEGER NOT NULL PRIMARY KEY,
                            title TEXT NOT NULL,
                            level_set_id INTEGER NOT NULL,
                            puzzle_id INTEGER NOT NULL,
                            FOREIGN KEY (level_set_id) REFERENCES level_sets(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY (puzzle_id) REFERENCES puzzles(id) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    database.execSQL(
                        """
                        INSERT INTO levels (id, title, level_set_id, puzzle_id)
                        SELECT id, title, level_set_id, puzzle_id FROM levels_backup
                        """.trimIndent(),
                    )
                    database.execSQL("DROP TABLE levels_backup")
                    database.execSQL("CREATE INDEX index_levels_level_set_id ON levels (level_set_id)")
                    database.execSQL("CREATE INDEX index_levels_puzzle_id ON levels (puzzle_id)")
                }
            }

        private val MIGRATION_1_6 = migrationToVersion6(fromVersion = 1, hasUserSolution = false)
        private val MIGRATION_2_6 = migrationToVersion6(fromVersion = 2, hasUserSolution = false)
        private val MIGRATION_3_6 = migrationToVersion6(fromVersion = 3, hasUserSolution = true)
        private val MIGRATION_4_6 = migrationToVersion6(fromVersion = 4, hasUserSolution = true)
        private val MIGRATION_5_6 = migrationToVersion6(fromVersion = 5, hasUserSolution = true)

        @Volatile
        private var instance: LevelsDatabase? = null

        fun getInstance(context: Context): LevelsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        LevelsDatabase::class.java,
                        "einkarcade.db",
                    ).allowMainThreadQueries()
                    .addMigrations(
                        MIGRATION_1_6,
                        MIGRATION_2_6,
                        MIGRATION_3_6,
                        MIGRATION_4_6,
                        MIGRATION_5_6,
                    )
                    .build()
                    .also { instance = it }
            }
    }
}
