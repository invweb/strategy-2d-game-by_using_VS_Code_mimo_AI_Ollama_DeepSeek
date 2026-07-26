package com.example.strategy.desktop

import com.badlogic.gdx.graphics.Color
import com.example.strategy.model.*

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
