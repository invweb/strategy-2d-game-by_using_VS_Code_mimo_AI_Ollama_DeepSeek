package com.example.strategy.desktop

import com.example.strategy.model.GameState
import kotlinx.serialization.json.Json
import java.io.File

object SaveManager {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun saveDir(): File {
        val home = System.getProperty("user.home")
        return File(home, ".strategy_saves")
    }

    fun save(state: GameState, name: String): Boolean {
        return try {
            val dir = saveDir()
            if (!dir.exists()) dir.mkdirs()
            val safeName = name.replace(Regex("[^a-zA-Z0-9_\\- ]"), "_").trim()
            val file = File(dir, "$safeName.json")
            val data = json.encodeToString(GameState.serializer(), state)
            file.writeText(data)
            println("[SaveManager] Saved to ${file.absolutePath}")
            true
        } catch (e: Exception) {
            println("[SaveManager] Save failed: ${e.message}")
            false
        }
    }

    fun load(name: String): GameState? {
        return try {
            val file = File(saveDir(), "$name.json")
            if (!file.exists()) {
                println("[SaveManager] No save file: ${file.absolutePath}")
                return null
            }
            val data = file.readText()
            json.decodeFromString(GameState.serializer(), data)
        } catch (e: Exception) {
            println("[SaveManager] Load failed: ${e.message}")
            null
        }
    }

    fun listSaves(): List<String> {
        val dir = saveDir()
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    fun deleteSave(name: String): Boolean {
        return try {
            File(saveDir(), "$name.json").delete()
        } catch (e: Exception) {
            false
        }
    }
}
