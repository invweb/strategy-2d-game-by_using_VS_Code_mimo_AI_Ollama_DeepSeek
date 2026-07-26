package com.example.strategy.desktop

import com.badlogic.gdx.graphics.Color
import com.example.strategy.model.*

/**
 * Encapsulates all mutable game state shared between GameScreen, GameInput, and GameUI.
 *
 * Replaces the previous pattern of 39 getter/setter lambda parameters.
 * Provides convenience methods [resetMode] and [resetOnEndTurn] for common state transitions.
 *
 * @property state Current game state (players, map, diplomacy, etc.)
 * @property selectedRegion Currently selected region (null if none)
 * @property selectedRegions Multiple selected regions (box selection)
 * @property actionUsedThisTurn Whether the player has used their action this turn
 * @property attackMode Whether attack mode is active
 * @property attackSourceId Source region ID for attack
 * @property moveMode Whether move mode is active
 * @property moveSourceId Source region ID for move
 * @property aiPending Whether AI is thinking
 * @property gameOver Whether the game has ended
 */
class GameStateHolder(
    var state: GameState,
    var selectedRegion: Region? = null,
    val selectedRegions: MutableList<Region> = mutableListOf(),
    var actionUsedThisTurn: Boolean = false,
    var attackMode: Boolean = false,
    var attackSourceId: Int = -1,
    var moveMode: Boolean = false,
    var moveSourceId: Int = -1,
    var aiPending: Boolean = false,
    var gameOver: Boolean = false
) {
    fun resetMode() {
        selectedRegion = null
        selectedRegions.clear()
        actionUsedThisTurn = false
        attackMode = false
        attackSourceId = -1
        moveMode = false
        moveSourceId = -1
    }

    fun resetOnEndTurn() {
        selectedRegion = null
        selectedRegions.clear()
        actionUsedThisTurn = false
        attackMode = false
        attackSourceId = -1
        moveMode = false
        moveSourceId = -1
    }
}
