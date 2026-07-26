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
            if (data.isBlank()) {
                println("[SaveManager] Save file is empty: $name")
                return null
            }
            val state = json.decodeFromString(GameState.serializer(), data)
            if (!validate(state)) {
                println("[SaveManager] Save file validation failed: $name")
                return null
            }
            state
        } catch (e: Exception) {
            println("[SaveManager] Load failed: ${e.message}")
            null
        }
    }

    private fun validate(state: GameState): Boolean {
        if (state.turn < 1) return false
        if (state.players.isEmpty()) return false
        if (state.map.regions.isEmpty()) return false
        if (state.currentPlayerId !in state.players.map { it.id }) return false
        for (region in state.map.regions) {
            if (region.id < 0) return false
            if (region.terrain == com.example.strategy.model.TerrainType.WATER && region.population > 0) return false
            if (region.ownerId != null && region.ownerId !in state.players.map { it.id }) return false
        }
        return true
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
