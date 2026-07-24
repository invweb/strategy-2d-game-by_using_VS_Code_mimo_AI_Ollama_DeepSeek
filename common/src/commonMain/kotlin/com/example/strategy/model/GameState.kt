package com.example.strategy.model

import kotlinx.serialization.Serializable

@Serializable
data class TurnSnapshot(
    val turn: Int,
    val playerId: Int,
    val resources: Resources,
    val territories: Int,
    val population: Int
)

@Serializable
data class GameState(
    val turn: Int = 1,
    val currentPlayerId: Int = 0,
    val players: List<Player>,
    val map: GameMap,
    val actionsLog: List<String> = emptyList(),
    val diplomacy: DiplomacyState = DiplomacyState(),
    val fog: FogState = FogState(),
    val history: List<TurnSnapshot> = emptyList(),
    val difficulty: Difficulty = Difficulty.NORMAL
) {
    fun currentPlayer(): Player? = players.find { it.id == currentPlayerId }
}
