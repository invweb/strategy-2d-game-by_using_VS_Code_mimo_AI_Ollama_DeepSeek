package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.example.strategy.model.GameState
import kotlinx.serialization.json.Json
import java.io.File

object SaveManager {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private const val SAVE_DIR = "saves"
    private const val SAVE_FILE = "save.json"

    fun save(state: GameState): Boolean {
        return try {
            val dir = File(Gdx.files.local(SAVE_DIR).file().absolutePath)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, SAVE_FILE)
            val data = json.encodeToString(GameState.serializer(), state)
            file.writeText(data)
            true
        } catch (e: Exception) {
            println("[SaveManager] Save failed: ${e.message}")
            false
        }
    }

    fun load(): GameState? {
        return try {
            val dir = File(Gdx.files.local(SAVE_DIR).file().absolutePath)
            val file = File(dir, SAVE_FILE)
            if (!file.exists()) return null
            val data = file.readText()
            json.decodeFromString(GameState.serializer(), data)
        } catch (e: Exception) {
            println("[SaveManager] Load failed: ${e.message}")
            null
        }
    }

    fun hasSave(): Boolean {
        val dir = File(Gdx.files.local(SAVE_DIR).file().absolutePath)
        return File(dir, SAVE_FILE).exists()
    }

    fun deleteSave(): Boolean {
        return try {
            val dir = File(Gdx.files.local(SAVE_DIR).file().absolutePath)
            val file = File(dir, SAVE_FILE)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
}
