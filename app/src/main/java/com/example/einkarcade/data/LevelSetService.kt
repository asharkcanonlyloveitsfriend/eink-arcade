package com.example.einkarcade.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LevelSetService(
    context: Context,
) {
    private val contentResolver = context.contentResolver
    private val repository = LevelsRepository(context)

    suspend fun import(uri: Uri): Int =
        withContext(Dispatchers.IO) {
            val displayName =
                contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    cursor.takeIf { it.moveToFirst() }
                        ?.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }

            val parse =
                when {
                    displayName?.endsWith(".slc", ignoreCase = true) == true ->
                        SlcLevelSetParser::parse
                    displayName?.endsWith(".txt", ignoreCase = true) == true ||
                        displayName?.endsWith(".sok", ignoreCase = true) == true ->
                        TextLevelSetParser::parse
                    else -> throw IllegalArgumentException("Unsupported level-set file: $displayName")
                }

            contentResolver.openInputStream(uri)?.use { input ->
                val parsedLevelSet = parse(input)
                val normalizedLevels = parsedLevelSet.levels.map(LevelNormalizer::normalize).toTypedArray()
                repository.importLevelSet(parsedLevelSet.title, normalizedLevels)
            } ?: throw IllegalArgumentException("Unable to read $displayName")
        }

    suspend fun rename(
        levelSetId: Int,
        title: String,
    ) =
        withContext(Dispatchers.IO) {
            repository.renameLevelSet(levelSetId, title)
        }

    suspend fun delete(levelSetId: Int) =
        withContext(Dispatchers.IO) {
            repository.deleteLevelSet(levelSetId)
        }
}
