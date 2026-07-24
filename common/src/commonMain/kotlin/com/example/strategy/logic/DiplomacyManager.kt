package com.example.strategy.logic

import com.example.strategy.model.*

object DiplomacyManager {

    fun proposeAlliance(gameState: GameState, playerId: Int, targetId: Int): GameState {
        if (playerId == targetId) return gameState
        val target = gameState.players.find { it.id == targetId } ?: return gameState
        if (target.isHuman) return gameState

        val current = gameState.diplomacy.getRelation(playerId, targetId)
        if (current.status == DiplomacyStatus.ALLIED) return gameState

        val updated = current.copy(status = DiplomacyStatus.ALLIED, turnsAllied = 0)
        val newDiplo = gameState.diplomacy.updateRelation(updated)
        return gameState.copy(
            diplomacy = newDiplo,
            actionsLog = gameState.actionsLog + "${gameState.currentPlayer()?.name} formed alliance with ${target.name}"
        )
    }

    fun breakAlliance(gameState: GameState, playerId: Int, targetId: Int): GameState {
        val current = gameState.diplomacy.getRelation(playerId, targetId)
        if (current.status != DiplomacyStatus.ALLIED) return gameState

        val updated = current.copy(status = DiplomacyStatus.NEUTRAL, tradeActive = false, turnsAllied = 0)
        val newDiplo = gameState.diplomacy.updateRelation(updated)
        val target = gameState.players.find { it.id == targetId }
        return gameState.copy(
            diplomacy = newDiplo,
            actionsLog = gameState.actionsLog + "${gameState.currentPlayer()?.name} broke alliance with ${target?.name}"
        )
    }

    fun proposeTrade(gameState: GameState, playerId: Int, targetId: Int): GameState {
        if (playerId == targetId) return gameState
        val current = gameState.diplomacy.getRelation(playerId, targetId)
        val target = gameState.players.find { it.id == targetId } ?: return gameState
        if (target.isHuman) return gameState

        val updated = current.copy(tradeActive = true)
        val newDiplo = gameState.diplomacy.updateRelation(updated)
        return gameState.copy(
            diplomacy = newDiplo,
            actionsLog = gameState.actionsLog + "${gameState.currentPlayer()?.name} established trade with ${target.name}"
        )
    }

    fun cancelTrade(gameState: GameState, playerId: Int, targetId: Int): GameState {
        val current = gameState.diplomacy.getRelation(playerId, targetId)
        if (!current.tradeActive) return gameState

        val updated = current.copy(tradeActive = false)
        val newDiplo = gameState.diplomacy.updateRelation(updated)
        val target = gameState.players.find { it.id == targetId }
        return gameState.copy(
            diplomacy = newDiplo,
            actionsLog = gameState.actionsLog + "${gameState.currentPlayer()?.name} cancelled trade with ${target?.name}"
        )
    }

    fun applyTradeIncome(gameState: GameState): GameState {
        var state = gameState
        for (player in gameState.players) {
            val allies = gameState.diplomacy.getAlliesOf(player.id)
            if (allies.isEmpty()) continue

            var tradeBonus = Resources()
            for (allyId in allies) {
                if (!gameState.diplomacy.isTradePartner(player.id, allyId)) continue
                val ally = gameState.players.find { it.id == allyId } ?: continue
                val allyIncome = Economy.calculateIncome(ally, gameState.map)
                tradeBonus = tradeBonus + Resources(
                    food = allyIncome.food / 4,
                    wood = allyIncome.wood / 4,
                    stone = allyIncome.stone / 4,
                    iron = allyIncome.iron / 4,
                    gold = allyIncome.gold / 4
                )
            }

            if (tradeBonus != Resources()) {
                state = state.copy(
                    players = state.players.map {
                        if (it.id == player.id) it.copy(resources = it.resources + tradeBonus) else it
                    }
                )
            }
        }
        return state
    }

    fun incrementAllianceTurns(gameState: GameState): GameState {
        return gameState.copy(
            diplomacy = DiplomacyState(
                relations = gameState.diplomacy.relations.map {
                    if (it.status == DiplomacyStatus.ALLIED) it.copy(turnsAllied = it.turnsAllied + 1) else it
                }
            )
        )
    }
}
