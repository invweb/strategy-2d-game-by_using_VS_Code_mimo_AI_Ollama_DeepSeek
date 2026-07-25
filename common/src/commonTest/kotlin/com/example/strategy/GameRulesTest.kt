package com.example.strategy.logic

import com.example.strategy.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRulesTest {

    private fun createTestPlayer(id: Int = 0, resources: Resources = Resources(food = 100, wood = 100, stone = 100, iron = 100, gold = 100)): Player {
        return Player(id = id, name = "Player$id", color = "blue", resources = resources, isHuman = id == 0)
    }

    private fun createTestRegion(id: Int = 0, ownerId: Int? = 0, buildings: List<Building> = emptyList(), population: Int = 10): Region {
        return Region(id = id, name = "Region$id", terrain = TerrainType.PLAINS, tileX = 0, tileY = 0, ownerId = ownerId, buildings = buildings, population = population)
    }

    private fun createTestGameState(vararg players: Player): GameState {
        val regions = listOf(
            createTestRegion(0, 0),
            createTestRegion(1, 1),
            createTestRegion(2, 0),
            createTestRegion(3, null)
        )
        return GameState(players = players.toList(), map = GameMap(width = 4, height = 1, regions = regions.toList()))
    }

    @Test
    fun `canBuild returns true when region has no buildings and player can afford`() {
        val player = createTestPlayer()
        val region = createTestRegion()
        assertTrue(GameRules.canBuild(player, region, BuildingType.FARM))
    }

    @Test
    fun `canBuild returns false when region already has building`() {
        val player = createTestPlayer()
        val region = createTestRegion(buildings = listOf(Building(BuildingType.FARM)))
        assertFalse(GameRules.canBuild(player, region, BuildingType.FARM))
    }

    @Test
    fun `canBuild returns false when player cannot afford`() {
        val player = createTestPlayer(resources = Resources(food = 0, wood = 0, stone = 0, iron = 0, gold = 0))
        val region = createTestRegion()
        assertFalse(GameRules.canBuild(player, region, BuildingType.FARM))
    }

    @Test
    fun `canBuild returns false when region is not owned by player`() {
        val player = createTestPlayer(id = 0)
        val region = createTestRegion(ownerId = 1)
        assertFalse(GameRules.canBuild(player, region, BuildingType.FARM))
    }

    @Test
    fun `processBuild deducts resources and adds building`() {
        val player = createTestPlayer()
        val region = createTestRegion()
        val state = createTestGameState(player)

        val action = ActionQueue.GameAction(0, ActionQueue.ActionType.BUILD, 0, "FARM")
        val newState = GameRules.processBuild(state, action)

        val updatedPlayer = newState.players.find { it.id == 0 }!!
        val updatedRegion = newState.map.getRegionById(0)!!

        assertEquals(90, updatedPlayer.resources.food)
        assertEquals(95, updatedPlayer.resources.wood)
        assertTrue(updatedRegion.buildings.any { it.type == BuildingType.FARM })
    }

    @Test
    fun `processRecruit adds units when barracks exists`() {
        val player = createTestPlayer()
        val region = createTestRegion(buildings = listOf(Building(BuildingType.BARRACKS)))
        val regions = listOf(region, createTestRegion(1, 1), createTestRegion(2, 0), createTestRegion(3, null))
        val state = GameState(players = listOf(player), map = GameMap(width = 4, height = 1, regions = regions))

        val action = ActionQueue.GameAction(0, ActionQueue.ActionType.RECRUIT, 0)
        val newState = GameRules.processRecruit(state, action)

        val updatedRegion = newState.map.getRegionById(0)!!
        assertEquals(15, updatedRegion.population)
        assertTrue(updatedRegion.units.units.any { it.type == UnitType.INFANTRY && it.count == 5 })
    }

    @Test
    fun `processRecruit does nothing without barracks`() {
        val player = createTestPlayer()
        val region = createTestRegion()
        val regions = listOf(region, createTestRegion(1, 1), createTestRegion(2, 0), createTestRegion(3, null))
        val state = GameState(players = listOf(player), map = GameMap(width = 4, height = 1, regions = regions))

        val action = ActionQueue.GameAction(0, ActionQueue.ActionType.RECRUIT, 0)
        val newState = GameRules.processRecruit(state, action)

        val updatedRegion = newState.map.getRegionById(0)!!
        assertEquals(10, updatedRegion.population)
    }

    @Test
    fun `processDevelop adds population and deducts gold`() {
        val player = createTestPlayer()
        val state = createTestGameState(player)

        val action = ActionQueue.GameAction(0, ActionQueue.ActionType.DEVELOP, 0)
        val newState = GameRules.processDevelop(state, action)

        val updatedPlayer = newState.players.find { it.id == 0 }!!
        val updatedRegion = newState.map.getRegionById(0)!!

        assertEquals(90, updatedPlayer.resources.gold)
        assertEquals(13, updatedRegion.population)
    }
}
