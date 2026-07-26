package com.example.strategy

import com.example.strategy.model.*
import com.example.strategy.platform.GameFactory
import kotlin.test.*

class GameFactoryTest {

    @Test
    fun createDefaultGameStateHasTwoPlayers() {
        val state = GameFactory.createDefaultGameState()
        assertEquals(2, state.players.size)
    }

    @Test
    fun createDefaultGameStateHasCorrectPlayerIds() {
        val state = GameFactory.createDefaultGameState()
        assertEquals(0, state.players[0].id)
        assertEquals(1, state.players[1].id)
    }

    @Test
    fun createDefaultGameStateHumanPlayerIsFirst() {
        val state = GameFactory.createDefaultGameState()
        assertTrue(state.players[0].isHuman)
        assertFalse(state.players[1].isHuman)
    }

    @Test
    fun createDefaultGameStateTurnIsOne() {
        val state = GameFactory.createDefaultGameState()
        assertEquals(1, state.turn)
    }

    @Test
    fun createDefaultGameStateCurrentPlayerIsZero() {
        val state = GameFactory.createDefaultGameState()
        assertEquals(0, state.currentPlayerId)
    }

    @Test
    fun createSmallMapHasCorrectDimensions() {
        val state = GameFactory.createGameState(GameFactory.MapSize.SMALL, GameFactory.TerrainStyle.BALANCED)
        assertEquals(8, state.map.width)
        assertEquals(6, state.map.height)
    }

    @Test
    fun createMediumMapHasCorrectDimensions() {
        val state = GameFactory.createGameState(GameFactory.MapSize.MEDIUM, GameFactory.TerrainStyle.BALANCED)
        assertEquals(12, state.map.width)
        assertEquals(8, state.map.height)
    }

    @Test
    fun createLargeMapHasCorrectDimensions() {
        val state = GameFactory.createGameState(GameFactory.MapSize.LARGE, GameFactory.TerrainStyle.BALANCED)
        assertEquals(16, state.map.width)
        assertEquals(10, state.map.height)
    }

    @Test
    fun mapHasWaterBorder() {
        val state = GameFactory.createDefaultGameState()
        for (region in state.map.regions) {
            if (region.tileX == 0 || region.tileY == 0 ||
                region.tileX == state.map.width - 1 || region.tileY == state.map.height - 1) {
                assertEquals(TerrainType.WATER, region.terrain)
            }
        }
    }

    @Test
    fun mapHasTwoOwners() {
        val state = GameFactory.createDefaultGameState()
        val owners = state.map.regions.map { it.ownerId }.toSet()
        assertTrue(owners.contains(0))
        assertTrue(owners.contains(1))
        assertTrue(owners.contains(null))
    }

    @Test
    fun eachTerrainStyleProducesValidMap() {
        for (style in GameFactory.TerrainStyle.entries) {
            val state = GameFactory.createGameState(GameFactory.MapSize.MEDIUM, style)
            assertTrue(state.map.regions.isNotEmpty())
            val terrainTypes = state.map.regions.map { it.terrain }.toSet()
            assertTrue(terrainTypes.contains(TerrainType.WATER))
            assertTrue(terrainTypes.size >= 2)
        }
    }

    @Test
    fun capitalRegionHasStartingBuildings() {
        val state = GameFactory.createDefaultGameState()
        val capital = state.map.getRegionAt(1, 1)
        if (capital != null && capital.terrain == TerrainType.PLAINS) {
            assertTrue(capital.buildings.any { it.type == BuildingType.FARM })
            assertTrue(capital.buildings.any { it.type == BuildingType.BARRACKS })
            assertTrue(capital.buildings.any { it.type == BuildingType.MARKET })
        }
    }
}
