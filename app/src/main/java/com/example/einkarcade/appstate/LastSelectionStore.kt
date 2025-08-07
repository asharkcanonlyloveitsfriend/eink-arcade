package com.example.einkarcade.appstate

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class LastSelectionStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "eink_arcade_prefs"
        private const val KEY_SET_ID = "current_set_id"
        private const val KEY_LEVEL_NAME = "current_level_name"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(setId: String, levelName: String) {
        prefs.edit {
            putString(KEY_SET_ID, setId)
                .putString(KEY_LEVEL_NAME, levelName)
        }
    }

    fun load(): Pair<String, String>? {
        val savedSetId = prefs.getString(KEY_SET_ID, null)
        val savedLevelName = prefs.getString(KEY_LEVEL_NAME, null)
        if (savedSetId == null || savedLevelName == null) return null
        return savedSetId to savedLevelName
    }
}
