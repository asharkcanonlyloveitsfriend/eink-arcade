package com.example.einkarcade.data

import android.content.Context
import android.util.Log
import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.storage.JsonStore
import org.json.JSONArray
import org.json.JSONObject

// Repository for loading/saving level sets.
class LevelsRepository(context: Context) {
    private val jsonStore = JsonStore(context)

    fun loadSets(): List<LevelSet>? {
        val jsonText = jsonStore.readText() ?: return null
        val root = JSONObject(jsonText)
        val setsArr = root.getJSONArray("sets")
        val out = mutableListOf<LevelSet>()
        for (i in 0 until setsArr.length()) {
            val setObj = setsArr.getJSONObject(i)
            val setId = setObj.getString("id")
            val setName = setObj.getString("name")

            val levelsArr = setObj.getJSONArray("levels")
            val levels = mutableListOf<Level>()
            for (j in 0 until levelsArr.length()) {
                val lvl = levelsArr.getJSONObject(j)
                val name = lvl.getString("name")
                val ascii = lvl.getString("ascii")
                val level = Level.fromAscii(name, ascii)
                level.setRating(lvl.optInt("rating", 0))
                level.setCompletedAt(lvl.optLong("completedAt", 0L))
                levels.add(level)
            }
            out.add(LevelSet(id = setId, name = setName, levels = levels))
        }
        return out
    }

    // Build full JSON from the in-memory model.
    fun saveAllFromSets(sets: List<LevelSet>): Boolean {
        return try {
            val outRoot = JSONObject()
            val outSets = JSONArray()
            for (set in sets) {
                if (set.levels.isEmpty()) continue
                val outSet = JSONObject()
                outSet.put("id", set.id)
                outSet.put("name", set.name)

                val outLevels = JSONArray()
                set.levels.forEach { lvl ->
                    val obj = JSONObject()
                    obj.put("name", lvl.name)
                    obj.put("ascii", lvl.ascii)
                    obj.put("rating", lvl.rating)
                    obj.put("completedAt", lvl.completedAt)
                    outLevels.put(obj)
                }
                outSet.put("levels", outLevels)
                outSets.put(outSet)
            }
            outRoot.put("sets", outSets)
            jsonStore.writeText(outRoot.toString())
        } catch (t: Throwable) {
            Log.e("LevelsRepository", "saveAllFromSets failed", t)
            false
        }
    }
}
