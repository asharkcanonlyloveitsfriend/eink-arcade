package com.example.einkarcade.data

import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position

interface LevelDataSource {
    fun loadSets(): List<LevelSet>?

    fun recordCompletion(
        level: Level,
        solutionHistory: List<List<Position>>,
    ): String

    fun syncWithServer()
}
