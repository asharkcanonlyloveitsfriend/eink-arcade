package com.example.einkarcade.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

const val LEVELS_DIR_RELATIVE_PATH = "Download/EinkArcade/"
const val LEVELS_JSON_NAME = "levels.txt"
const val DEFAULT_LEVELS_ASSET = "default_levels.json"

// Ensure Downloads/EinkArcade/levels.txt exists; seed from assets if missing.
fun ensureJsonFromAssetsIfMissing(context: Context, assetPath: String = DEFAULT_LEVELS_ASSET): Uri? {
    val cr = context.contentResolver

    // Find existing file.
    val projection = arrayOf(
        MediaStore.Downloads._ID,
        MediaStore.Downloads.DISPLAY_NAME,
        MediaStore.Downloads.RELATIVE_PATH
    )
    val selection = "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
    val selectionArgs = arrayOf(LEVELS_JSON_NAME, LEVELS_DIR_RELATIVE_PATH)
    cr.query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(idCol)
            return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
        }
    }

    // Create and seed from asset.
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, LEVELS_JSON_NAME)
        put(MediaStore.Downloads.RELATIVE_PATH, LEVELS_DIR_RELATIVE_PATH)
        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
    }
    val uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
    val seed = context.assets.open(assetPath).bufferedReader().use { it.readText() }
    cr.openOutputStream(uri, "wt")?.use { os ->
        os.write(seed.toByteArray())
    }
    return uri
}
