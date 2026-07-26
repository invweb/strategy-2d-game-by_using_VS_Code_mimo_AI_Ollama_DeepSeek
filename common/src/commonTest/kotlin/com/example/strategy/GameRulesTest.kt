package com.example.strategy

import com.example.strategy.model.*
import com.example.strategy.logic.*
import kotlin.test.*

class GameRulesTest {

    private fun createPlayer(
        id: Int = 1,
        resources: Resources = Resources(food = 100, wood = 100, stone = 100, iron = 100, gold = 200),
        techs: TechState = TechState()
    ) = Player(id = id, name = "Player$id", color = "blue", resources = resources, techs = techs)

    private fun createRegion(
        id: Int = 1,
        ownerId: Int? = 1,
        terrain: TerrainType = TerrainType.PLAINS,
        population: Int = 20,
        buildings: List<Building> = emptyList(),
        units: UnitStack = UnitStack()
    ) = Region(
        id = id, name = "Region$id", terrain = terrain,
        tileX = id, tileY = 0, ownerId = ownerId,
        population = population, buildings = buildings, units = units
    )

    private fun createGameState(
        players: List<Player> = listOf(createPlayer(1), createPlayer(2)),
        regions: List<Region> = listOf(createRegion(1, 1), createRegion(2, 2))
    ): GameState {
        val map = GameMap(width = 2, height = 1, regions = regions)
        return GameState(players = players, map = map, currentPlayerId = 1)
    }

    @Test
    fun testCanBuildSuccess() {
        val player = createPlayer()
        val region = createRegion()
        assertTrue(GameRules.canBuild(player, region, BuildingType.FARM))
    }

    @Test
    fun testCanBuildNotOwner() {
        val player = createPlayer(id = 1)
        val region = createRegion(ownerId = 2)
        assertFalse(GameRules.canBuild(player, region, BuildingType.FARM))
    }

    @Test
    fun testCanBuildAlreadyHasBuilding() {
        val player = createPlayer()
        val region = createRegion(buildings = listOf(Building(BuildingType.FARM)))
        assertFalse(GameRules.canBuild(player, region, BuildingType.FARM))
    }

    @Test
    fun testCanBuildInsufficientResources() {
        val player = createPlayer(resources = Resources(food = 0, wood = 0))
        val region = createRegion()
        assertFalse(GameRules.canBuild(player, region, BuildingType.FARM))
    }

    @Test
    fun testProcessBuildSuccess() {
        val state = createGameState()
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.BUILD,
            targetRegionId = 1,
            param = "FARM"
        )
        val newState = GameRules.processBuild(state, action)
        val region = newState.map.getRegionById(1)!!
        assertEquals(1, region.buildings.size)
        assertEquals(BuildingType.FARM, region.buildings[0].type)
    }

    @Test
    fun testProcessBuildDeductsResources() {
        val state = createGameState()
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.BUILD,
            targetRegionId = 1,
            param = "FARM"
        )
        val newState = GameRules.processBuild(state, action)
        val player = newState.players.find { it.id == 1 }!!
        assertEquals(90, player.resources.food)
        assertEquals(95, player.resources.wood)
    }

    @Test
    fun testProcessRecruitSuccess() {
        val region = createRegion(buildings = listOf(Building(BuildingType.BARRACKS)))
        val state = createGameState(regions = listOf(region, createRegion(2, 2)))
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.RECRUIT,
            targetRegionId = 1
        )
        val newState = GameRules.processRecruit(state, action)
        val updatedRegion = newState.map.getRegionById(1)!!
        assertEquals(25, updatedRegion.population)
        assertEquals(5, updatedRegion.units.totalPopulation)
    }

    @Test
    fun testProcessRecruitNoBarracks() {
        val state = createGameState()
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.RECRUIT,
            targetRegionId = 1
        )
        val newState = GameRules.processRecruit(state, action)
        val updatedRegion = newState.map.getRegionById(1)!!
        assertEquals(20, updatedRegion.population)
    }

    @Test
    fun testProcessAttackWin() {
        val source = createRegion(1, 1, population = 50, units = UnitStack(units = listOf(Unit(UnitType.INFANTRY, 10))))
        val target = createRegion(2, 2, population = 5, units = UnitStack())
        val state = createGameState(regions = listOf(source, target))
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.ATTACK,
            targetRegionId = 2,
            param = "1"
        )
        val newState = GameRules.processAttack(state, action)
        val conquered = newState.map.getRegionById(2)!!
        assertEquals(1, conquered.ownerId)
    }

    @Test
    fun testProcessAttackLose() {
        val source = createRegion(1, 1, population = 3, units = UnitStack())
        val target = createRegion(2, 2, population = 50, units = UnitStack(units = listOf(Unit(UnitType.INFANTRY, 10))))
        val state = createGameState(regions = listOf(source, target))
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.ATTACK,
            targetRegionId = 2,
            param = "1"
        )
        val newState = GameRules.processAttack(state, action)
        val stillEnemy = newState.map.getRegionById(2)!!
        assertEquals(2, stillEnemy.ownerId)
    }

    @Test
    fun testProcessMoveSuccess() {
        val source = createRegion(1, 1, population = 20)
        val dest = createRegion(2, 1, population = 10)
        val state = createGameState(regions = listOf(source, dest))
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.MOVE_TROOPS,
            targetRegionId = 1,
            param = "2"
        )
        val newState = GameRules.processMove(state, action)
        assertEquals(10, newState.map.getRegionById(1)!!.population)
        assertEquals(20, newState.map.getRegionById(2)!!.population)
    }

    @Test
    fun testProcessMoveDifferentOwners() {
        val source = createRegion(1, 1, population = 20)
        val dest = createRegion(2, 2, population = 10)
        val state = createGameState(regions = listOf(source, dest))
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.MOVE_TROOPS,
            targetRegionId = 1,
            param = "2"
        )
        val newState = GameRules.processMove(state, action)
        assertEquals(20, newState.map.getRegionById(1)!!.population)
        assertEquals(10, newState.map.getRegionById(2)!!.population)
    }

    @Test
    fun testProcessDevelopSuccess() {
        val state = createGameState()
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.DEVELOP,
            targetRegionId = 1
        )
        val newState = GameRules.processDevelop(state, action)
        assertEquals(23, newState.map.getRegionById(1)!!.population)
        assertEquals(190, newState.players.find { it.id == 1 }!!.resources.gold)
    }

    @Test
    fun testProcessDevelopInsufficientGold() {
        val player = createPlayer(resources = Resources(gold = 5))
        val state = createGameState(players = listOf(player, createPlayer(2)))
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.DEVELOP,
            targetRegionId = 1
        )
        val newState = GameRules.processDevelop(state, action)
        assertEquals(20, newState.map.getRegionById(1)!!.population)
    }

    @Test
    fun testProcessResearchSuccess() {
        val state = createGameState()
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.RESEARCH,
            targetRegionId = 1,
            param = "AGRICULTURE"
        )
        val newState = GameRules.processResearch(state, action)
        val player = newState.players.find { it.id == 1 }!!
        assertFalse(player.techs.isResearched(TechType.AGRICULTURE))
        assertEquals(TechType.AGRICULTURE, player.techs.researching)
        assertTrue(player.techs.turnsLeft > 0)
    }

    @Test
    fun testProcessResearchAlreadyResearched() {
        val player = createPlayer(techs = TechState(researched = listOf(TechType.AGRICULTURE)))
        val state = createGameState(players = listOf(player, createPlayer(2)))
        val action = ActionQueue.GameAction(
            playerId = 1,
            type = ActionQueue.ActionType.RESEARCH,
            targetRegionId = 1,
            param = "AGRICULTURE"
        )
        val newState = GameRules.processResearch(state, action)
        val updatedPlayer = newState.players.find { it.id == 1 }!!
        assertEquals(1, updatedPlayer.techs.researched.size)
    }
}
