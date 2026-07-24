package com.example.strategy.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val turn: Int = 1,
    val currentPlayerId: Int = 0,
    val players: List<Player>,
    val map: GameMap,
    val actionsLog: List<String> = emptyList()
) {
    fun currentPlayer(): Player? = players.find { it.id == currentPlayerId }
}
