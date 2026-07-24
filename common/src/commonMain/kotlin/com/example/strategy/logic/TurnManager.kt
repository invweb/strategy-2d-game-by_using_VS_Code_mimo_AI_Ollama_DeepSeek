package com.example.strategy.logic

import com.example.strategy.model.GameState

// Turn manager — orchestrates turn flow
object TurnManager {

    fun startTurn(gameState: GameState): GameState {
        val updated = Economy.applyTurnIncome(gameState)
        val player = updated.currentPlayer() ?: return updated
        val upkeep = Economy.upkeepCost(player, updated.map)
        val afterUpkeep = updated.copy(
            players = updated.players.map {
                if (it.id == player.id) it.copy(resources = it.resources - upkeep) else it
            }
        )
        return afterUpkeep
    }

    fun endTurn(gameState: GameState): GameState {
        val processed = ActionQueue.processAll(gameState)
        val currentIndex = processed.players.indexOfFirst { it.id == processed.currentPlayerId }
        val wrapped = currentIndex + 1 >= processed.players.size
        val nextPlayerId = if (wrapped) processed.players.first().id
                           else processed.players[currentIndex + 1].id
        val newTurn = if (wrapped) processed.turn + 1 else processed.turn
        return processed.copy(currentPlayerId = nextPlayerId, turn = newTurn)
    }

    fun advanceFullTurn(gameState: GameState): GameState {
        var state = gameState
        for (player in gameState.players) {
            state = state.copy(currentPlayerId = player.id)
            state = startTurn(state)
            state = endTurn(state)
        }
        return state.copy(turn = state.turn + 1)
    }
}
