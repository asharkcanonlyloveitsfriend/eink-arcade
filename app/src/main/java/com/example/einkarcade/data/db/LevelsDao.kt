package com.example.einkarcade.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LevelsDao {
    @Query("SELECT COUNT(*) FROM level_sets")
    fun countLevelSets(): Int

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM level_sets")
    fun nextLevelSetId(): Int

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM levels")
    fun nextLevelId(): Int

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM puzzles")
    fun nextPuzzleId(): Int

    @Transaction
    @Query("SELECT * FROM level_sets ORDER BY LOWER(title) ASC")
    fun getAllLevelSetsWithLevels(): List<LevelSetWithLevels>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLevelSets(levelSets: List<LevelSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLevels(levels: List<LevelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPuzzles(puzzles: List<PuzzleEntity>)

    @Query("UPDATE level_sets SET title = :title WHERE id = :levelSetId")
    fun renameLevelSet(
        levelSetId: Int,
        title: String,
    ): Int

    @Query("DELETE FROM puzzles WHERE id IN (SELECT puzzle_id FROM levels WHERE level_set_id = :levelSetId)")
    fun deletePuzzlesForLevelSet(levelSetId: Int)

    @Query("DELETE FROM level_sets WHERE id = :levelSetId")
    fun deleteLevelSet(levelSetId: Int): Int

    @Query(
        "UPDATE puzzles SET last_completed_at = :lastCompletedAt, user_solution = :userSolution WHERE id = :puzzleId",
    )
    fun updatePuzzleCompletion(
        puzzleId: Int,
        lastCompletedAt: String?,
        userSolution: String?,
    )

    @Query("SELECT user_solution FROM puzzles WHERE id = :puzzleId")
    fun getUserSolution(puzzleId: Int): String?
}
