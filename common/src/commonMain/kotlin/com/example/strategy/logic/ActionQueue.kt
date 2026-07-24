package com.example.strategy.logic

import com.example.strategy.model.GameState

// Action queue — buffers player actions during a turn
object ActionQueue {

    data class GameAction(
        val playerId: Int,
        val type: ActionType,
        val targetRegionId: Int,
        val param: String = ""
    )

    enum class ActionType {
        BUILD,
        RECRUIT,
        ATTACK,
        MOVE_TROOPS,
        DEVELOP
    }

    private val pendingActions: MutableList<GameAction> = mutableListOf()

    fun enqueue(action: GameAction) {
        pendingActions.add(action)
    }

    fun processAll(gameState: GameState): GameState {
        var state = gameState
        for (action in pendingActions) {
            state = when (action.type) {
                ActionType.BUILD -> GameRules.processBuild(state, action)
                ActionType.RECRUIT -> GameRules.processRecruit(state, action)
                ActionType.ATTACK -> GameRules.processAttack(state, action)
                ActionType.MOVE_TROOPS -> GameRules.processMove(state, action)
                ActionType.DEVELOP -> GameRules.processDevelop(state, action)
            }
        }
        pendingActions.clear()
        return state
    }

    fun clear() {
        pendingActions.clear()
    }
}
