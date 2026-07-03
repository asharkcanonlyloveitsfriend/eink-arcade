package com.example.einkarcade.appstate

interface SelectionStore {
    fun save(
        setId: Int,
        puzzleId: Int,
    )

    fun load(): Pair<Int, Int>
}
