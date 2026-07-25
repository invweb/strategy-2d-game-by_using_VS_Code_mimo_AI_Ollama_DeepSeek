package com.example.strategy.logic

import com.example.strategy.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TurnManagerTest {

    private fun createTestGameState(): GameState {
        val players = listOf(
            Player(id = 0, name = "Human", color = "blue", resources = Resources(food = 50, wood = 30, stone = 20, gold = 100), isHuman = true),
            Player(id = 1, name = "AI", color = "red", resources = Resources(food = 50, wood = 30, stone = 20, gold = 100), isHuman = false)
        )
        val regions = listOf(
            Region(id = 0, name = "R0", terrain = TerrainType.PLAINS, tileX = 0, tileY = 0, ownerId = 0, population = 10),
            Region(id = 1, name = "R1", terrain = TerrainType.FOREST, tileX = 1, tileY = 0, ownerId = 1, population = 10)
        )
        return GameState(players = players, map = GameMap(width = 2, height = 1, regions = regions))
    }

    @Test
    fun `startTurn applies income to current player`() {
        val state = createTestGameState()
        val newState = TurnManager.startTurn(state)

        val player = newState.players.find { it.id == 0 }!!
        assertTrue(player.resources.food > 50) // income added
    }

    @Test
    fun `endTurn switches to next player`() {
        val state = createTestGameState()
        val newState = TurnManager.endTurn(state)

        assertEquals(1, newState.currentPlayerId)
    }

    @Test
    fun `endTurn increments turn when wrapping`() {
        val state = createTestGameState()
        var s = state
        s = TurnManager.endTurn(s) // player 0 -> 1
        s = TurnManager.endTurn(s) // player 1 -> 0, turn should increment
        assertEquals(2, s.turn)
    }

    @Test
    fun `endTurn records history snapshot`() {
        val state = createTestGameState()
        val newState = TurnManager.endTurn(state)

        assertTrue(newState.history.isNotEmpty())
    }
}
