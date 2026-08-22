package com.example.einkarcade.data

import android.content.Context
import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.data.db.LevelEntity
import com.example.einkarcade.data.db.LevelSetEntity
import com.example.einkarcade.data.db.LevelsDatabase
import com.example.einkarcade.data.db.PuzzleEntity
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position
import org.json.JSONArray
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Repository for loading/saving level sets.
class LevelsRepository(
    private val context: Context,
) : LevelDataSource {
    private val database = LevelsDatabase.getInstance(context)
    private val dao = database.levelsDao()
    private val utcFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)

    override fun loadSets(): List<LevelSet>? {
        val sets = dao.getAllLevelSetsWithLevels()
        if (sets.isEmpty()) return null
        return sets.map { set ->
            val levels =
                set.levels.sortedBy { it.level.id }.map { levelWithPuzzle ->
                    val level =
                        Level.fromAscii(
                            levelWithPuzzle.level.title,
                            levelWithPuzzle.puzzle.grid,
                            levelWithPuzzle.level.puzzleId,
                        )
                    level.setRating(levelWithPuzzle.puzzle.rating)
                    level.setStarred(levelWithPuzzle.puzzle.isStarred)
                    level.setCompletedAt(levelWithPuzzle.puzzle.lastCompletedAt)
                    level
                }
            LevelSet(
                id = set.levelSet.id,
                name = set.levelSet.title,
                levels = levels,
            )
        }
    }

    fun updateRating(level: Level) {
        dao.updatePuzzleRating(level.puzzleId, level.rating)
    }

    fun importLevelSet(
        title: String,
        grids: Array<String>,
    ): Int {
        require(grids.isNotEmpty()) { "Cannot import an empty level set." }

        return database.runInTransaction<Int> {
            val levelSetId = dao.nextLevelSetId()
            val firstLevelId = dao.nextLevelId()
            val firstPuzzleId = dao.nextPuzzleId()
            val levelSet = LevelSetEntity(id = levelSetId, title = title)
            val puzzles =
                grids.mapIndexed { index, grid ->
                    PuzzleEntity(
                        id = firstPuzzleId + index,
                        grid = grid,
                        lastCompletedAt = null,
                        rating = 0,
                        isStarred = false,
                        userSolution = null,
                    )
                }
            val levels =
                grids.indices.map { index ->
                    LevelEntity(
                        id = firstLevelId + index,
                        title = "Level ${index + 1}",
                        levelSetId = levelSetId,
                        puzzleId = firstPuzzleId + index,
                    )
                }

            dao.insertLevelSets(listOf(levelSet))
            dao.insertPuzzles(puzzles)
            dao.insertLevels(levels)
            levelSetId
        }
    }

    fun renameLevelSet(
        levelSetId: Int,
        title: String,
    ) {
        require(title.isNotBlank()) { "A level set title is required." }
        check(dao.renameLevelSet(levelSetId, title.trim()) == 1) {
            "The level set no longer exists."
        }
    }

    fun deleteLevelSet(levelSetId: Int) {
        val deletedLevelSets =
            database.runInTransaction<Int> {
                dao.deletePuzzlesForLevelSet(levelSetId)
                dao.deleteLevelSet(levelSetId)
            }
        check(deletedLevelSets == 1) { "The level set no longer exists." }
    }

    fun updateStarred(level: Level) {
        dao.updatePuzzleStarred(level.puzzleId, level.isStarred)
    }

    override fun recordCompletion(
        level: Level,
        solutionHistory: List<List<com.example.einkarcade.sokoban.Position>>,
    ): String {
        val normalized = normalizeSolution(solutionHistory)
        val newPushCount = normalized.size
        val existingSolutionJson = dao.getUserSolution(level.puzzleId)
        val timestamp = utcFormatter.format(Instant.now())

        val shouldPersistSolution =
            if (existingSolutionJson == null) {
                true
            } else {
                val existingPushCount =
                    try {
                        JSONArray(existingSolutionJson).length()
                    } catch (e: Exception) {
                        // If parsing fails, treat as no existing solution
                        Int.MAX_VALUE
                    }
                newPushCount < existingPushCount
            }

        if (shouldPersistSolution) {
            val userSolutionJson =
                if (normalized.isEmpty()) {
                    null
                } else {
                    val outerArray = JSONArray()
                    for (path in normalized) {
                        val pathArray = JSONArray()
                        for (pos in path) {
                            val posArray = JSONArray()
                            posArray.put(pos.row)
                            posArray.put(pos.col)
                            pathArray.put(posArray)
                        }
                        outerArray.put(pathArray)
                    }
                    outerArray.toString()
                }
            dao.updatePuzzleCompletion(level.puzzleId, timestamp, userSolutionJson)
        } else {
            dao.updatePuzzleCompletion(level.puzzleId, timestamp, existingSolutionJson)
        }

        return timestamp
    }

    private fun normalizeSolution(history: List<List<Position>>): List<List<Position>> {
        if (history.isEmpty()) return history
        val result = mutableListOf<List<Position>>()
        for (path in history) {
            if (result.isEmpty()) {
                result.add(path)
            } else {
                val last = result.last()
                if (last.last() == path.first()) {
                    val merged = last + path.drop(1)
                    result[result.lastIndex] = merged
                } else {
                    result.add(path)
                }
            }
        }
        return result
    }

}
