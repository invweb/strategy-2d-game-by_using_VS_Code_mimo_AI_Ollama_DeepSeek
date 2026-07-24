package com.example.strategy.serialization

import com.example.strategy.model.GameState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// JSON serialization via kotlinx.serialization
object GameStateSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun serialize(state: GameState): String = json.encodeToString(state)

    fun deserialize(jsonString: String): GameState = json.decodeFromString(jsonString)
}
