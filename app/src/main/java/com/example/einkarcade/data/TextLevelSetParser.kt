package com.example.einkarcade.data

import java.io.InputStream

object TextLevelSetParser {
    private const val MINIMUM_LEVEL_ROWS = 3
    private const val SOKOBAN_CHARACTERS = "# .*$@+_"
    private val TITLE_LABELS = listOf("Set:", "Title:")

    fun parse(input: InputStream): ParsedLevelSet {
        val lines = input.bufferedReader().use { it.readLines() }
        return ParsedLevelSet(title = extractTitle(lines), levels = parseLevels(lines))
    }

    private fun extractTitle(lines: List<String>): String =
        lines.firstNotNullOfOrNull { line ->
            TITLE_LABELS.firstNotNullOfOrNull { label ->
                line.takeIf { it.startsWith(label) }?.removePrefix(label)?.trim()?.takeIf(String::isNotEmpty)
            }
        } ?: throw IllegalArgumentException("Text file does not contain a Set: or Title: label.")

    private fun parseLevels(lines: List<String>): Array<String> {
        val levels = mutableListOf<String>()
        val currentLevel = mutableListOf<String>()

        fun finishLevel() {
            if (currentLevel.size >= MINIMUM_LEVEL_ROWS) {
                levels.add(currentLevel.joinToString("\n"))
            }
            currentLevel.clear()
        }

        lines.forEach { line ->
            if (line.isSokobanRow()) {
                currentLevel.add(line)
            } else {
                finishLevel()
            }
        }
        finishLevel()

        return levels.toTypedArray()
    }

    private fun String.isSokobanRow(): Boolean =
        isNotBlank() && all { character -> character in SOKOBAN_CHARACTERS }

}
