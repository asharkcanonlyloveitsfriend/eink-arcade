package com.example.einkarcade.session

import com.example.einkarcade.sokoban.GameEngine
import com.example.einkarcade.sokoban.Level

class GameSession(
    val level: Level,
) {
    var engine: GameEngine = GameEngine(level)
        private set

    fun restart() {
        engine = GameEngine(level)
    }
}
