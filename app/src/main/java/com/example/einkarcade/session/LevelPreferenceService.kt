package com.example.einkarcade.session

import com.example.einkarcade.catalog.LevelCatalog
import com.example.einkarcade.sokoban.Level

class LevelPreferenceService(
    private val catalog: LevelCatalog,
) {
    fun toggleLike(level: Level): Int {
        val rating = if (level.rating == 1) 0 else 1
        catalog.setRating(level.puzzleId, rating)
        level.setRating(rating)
        return rating
    }

    fun toggleDislike(level: Level): Int {
        val rating = if (level.rating == -1) 0 else -1
        catalog.setRating(level.puzzleId, rating)
        level.setRating(rating)
        return rating
    }

    fun toggleStar(level: Level): Boolean {
        val starred = !level.isStarred
        catalog.setStarred(level.puzzleId, starred)
        level.setStarred(starred)
        return starred
    }
}
