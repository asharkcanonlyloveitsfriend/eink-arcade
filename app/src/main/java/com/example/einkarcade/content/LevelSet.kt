package com.example.einkarcade.content

import com.example.einkarcade.sokoban.Level

data class LevelSet(
    val id: String,
    val name: String,
    val levels: List<Level>
)
