package com.example.strategy.logic

import com.example.strategy.model.GameState
import com.example.strategy.model.UnitType

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
        DEVELOP,
        PROPOSE_ALLIANCE,
        BREAK_ALLIANCE,
        PROPOSE_TRADE,
        CANCEL_TRADE,
        RESEARCH,
        RECRUIT_INFANTRY,
        RECRUIT_CAVALRY,
        RECRUIT_SIEGE
    }

    private val pendingActions: MutableList<GameAction> = mutableListOf()

    @Synchronized
    fun enqueue(action: GameAction) {
        pendingActions.add(action)
    }

    @Synchronized
    fun processAll(gameState: GameState): GameState {
        val actions = pendingActions.toList()
        pendingActions.clear()
        var state = gameState
        for (action in actions) {
            state = when (action.type) {
                ActionType.BUILD -> GameRules.processBuild(state, action)
                ActionType.RECRUIT -> GameRules.processRecruit(state, action)
                ActionType.ATTACK -> GameRules.processAttack(state, action)
                ActionType.MOVE_TROOPS -> GameRules.processMove(state, action)
                ActionType.DEVELOP -> GameRules.processDevelop(state, action)
                ActionType.PROPOSE_ALLIANCE -> DiplomacyManager.proposeAlliance(state, action.playerId, action.targetRegionId)
                ActionType.BREAK_ALLIANCE -> DiplomacyManager.breakAlliance(state, action.playerId, action.targetRegionId)
                ActionType.PROPOSE_TRADE -> DiplomacyManager.proposeTrade(state, action.playerId, action.targetRegionId)
                ActionType.CANCEL_TRADE -> DiplomacyManager.cancelTrade(state, action.playerId, action.targetRegionId)
                ActionType.RESEARCH -> GameRules.processResearch(state, action)
                ActionType.RECRUIT_INFANTRY -> GameRules.processRecruitUnit(state, action, UnitType.INFANTRY)
                ActionType.RECRUIT_CAVALRY -> GameRules.processRecruitUnit(state, action, UnitType.CAVALRY)
                ActionType.RECRUIT_SIEGE -> GameRules.processRecruitUnit(state, action, UnitType.SIEGE)
            }
        }
        return state
    }

    @Synchronized
    fun clear() {
        pendingActions.clear()
    }
}
