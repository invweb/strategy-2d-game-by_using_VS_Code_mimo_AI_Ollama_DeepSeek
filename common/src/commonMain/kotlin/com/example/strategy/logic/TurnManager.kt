package com.example.strategy.logic

import com.example.strategy.model.GameState
import com.example.strategy.model.Resources
import com.example.strategy.model.TurnSnapshot

// Turn manager — orchestrates turn flow
object TurnManager {

    fun startTurn(gameState: GameState): GameState {
        var updated = Economy.applyTurnIncome(gameState)
        updated = DiplomacyManager.applyTradeIncome(updated)
        val player = updated.currentPlayer() ?: return updated
        val upkeep = Economy.upkeepCost(player, updated.map)
        val afterUpkeep = updated.copy(
            players = updated.players.map {
                if (it.id == player.id) it.copy(resources = it.resources - upkeep) else it
            }
        )
        val explored = afterUpkeep.fog.exploreAllOwned(player.id, afterUpkeep.map)
        var result = afterUpkeep.copy(fog = explored)

        val event = RandomEvents.generateEvent(result)
        if (event != null) {
            result = RandomEvents.applyEvent(result, event)
        }

        return result
    }

    fun endTurn(gameState: GameState): GameState {
        val processed = ActionQueue.DEFAULT.processAll(gameState)
        val currentIndex = processed.players.indexOfFirst { it.id == processed.currentPlayerId }
        val wrapped = currentIndex + 1 >= processed.players.size
        val nextPlayerId = if (wrapped) processed.players.first().id
                           else processed.players[currentIndex + 1].id
        val newTurn = if (wrapped) processed.turn + 1 else processed.turn
        var result = processed.copy(currentPlayerId = nextPlayerId, turn = newTurn)
        if (wrapped) {
            result = DiplomacyManager.incrementAllianceTurns(result)
        }

        val snapshot = TurnSnapshot(
            turn = result.turn,
            playerId = result.currentPlayerId,
            resources = result.currentPlayer()?.resources ?: Resources(),
            territories = result.map.regions.count { it.ownerId == result.currentPlayerId },
            population = result.map.regions.filter { it.ownerId == result.currentPlayerId }.sumOf { it.population }
        )
        return result.copy(history = result.history + snapshot)
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
