package com.example.einkarcade.storage

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

// JSON store for levels.txt in Downloads/EinkArcade.
class JsonStore(private val context: Context) {
    private val cr get() = context.contentResolver
    private val projection = arrayOf(
        MediaStore.Downloads._ID,
        MediaStore.Downloads.DISPLAY_NAME,
        MediaStore.Downloads.RELATIVE_PATH
    )
    private var cachedUri: Uri? = null

    private fun findUri(): Uri? {
        val selection = "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
        val args = arrayOf(LEVELS_JSON_NAME, LEVELS_DIR_RELATIVE_PATH)
        return cr.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idCol)
                Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
            } else null
        }
    }

    private fun resolveUri(): Uri? {
        cachedUri?.let { return it }
        val uri = findUri()
        cachedUri = uri
        return uri
    }

    fun readText(): String? {
        val uri = resolveUri() ?: return null
        return cr.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }

    fun writeText(text: String): Boolean {
        val uri = resolveUri() ?: return false
        return try {
            cr.openOutputStream(uri, "w")?.use { os ->
                os.write(text.toByteArray())
                os.flush()
            }
            true
        } catch (t: Throwable) {
            Log.e("JsonStore", "write failed", t)
            false
        }
    }
}
