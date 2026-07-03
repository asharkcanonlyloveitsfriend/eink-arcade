package com.example.einkarcade.session

import com.example.einkarcade.catalog.LevelCatalog
import com.example.einkarcade.catalog.LevelSetSummary
import com.example.einkarcade.catalog.LevelSummary
import com.example.einkarcade.sokoban.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelPreferenceServiceTest {
    @Test
    fun togglesPreferencesAndPersistsThem() {
        val catalog = FakeCatalog()
        val service = LevelPreferenceService(catalog)
        val level = Level.fromAscii("Test", "@ $.", 7)

        assertEquals(1, service.toggleLike(level))
        assertEquals(7 to 1, catalog.rating)
        assertEquals(0, service.toggleLike(level))
        assertEquals(-1, service.toggleDislike(level))
        assertTrue(service.toggleStar(level))
        assertEquals(7 to true, catalog.starred)
    }

    private class FakeCatalog : LevelCatalog {
        var rating: Pair<Int, Int>? = null
        var starred: Pair<Int, Boolean>? = null
        override fun getSetSummaries(): List<LevelSetSummary> = emptyList()
        override fun getLevelSummaries(setId: Int): List<LevelSummary> = emptyList()
        override fun setRating(puzzleId: Int, rating: Int) { this.rating = puzzleId to rating }
        override fun setStarred(puzzleId: Int, isStarred: Boolean) { starred = puzzleId to isStarred }
    }
}
