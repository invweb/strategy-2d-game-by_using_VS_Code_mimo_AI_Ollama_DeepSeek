package com.example.strategy

import com.example.strategy.model.*
import com.example.strategy.logic.*
import com.example.strategy.ai.OllamaAI
import kotlin.test.*

class ImprovementsTest {

    private fun createPlayer(
        id: Int = 0,
        resources: Resources = Resources(food = 100, wood = 100, stone = 100, iron = 100, gold = 200),
        isHuman: Boolean = false
    ) = Player(id = id, name = "Player$id", color = "blue", resources = resources, isHuman = isHuman)

    private fun createRegion(
        id: Int,
        ownerId: Int? = 0,
        terrain: TerrainType = TerrainType.PLAINS,
        population: Int = 20,
        buildings: List<Building> = emptyList(),
        units: UnitStack = UnitStack(),
        tileX: Int = id % 4,
        tileY: Int = id / 4
    ) = Region(
        id = id, name = "Region$id", terrain = terrain,
        tileX = tileX, tileY = tileY, ownerId = ownerId,
        population = population, buildings = buildings, units = units
    )

    @Test
    fun testGameMapIndexedGetRegionById() {
        val regions = (0..9).map { createRegion(it) }
        val map = GameMap(width = 4, height = 3, regions = regions)
        for (i in 0..9) {
            val region = map.getRegionById(i)
            assertNotNull(region)
            assertEquals(i, region.id)
        }
        assertNull(map.getRegionById(99))
    }

    @Test
    fun testGameMapGetNeighbors() {
        val regions = listOf(
            createRegion(0, tileX = 0, tileY = 0),
            createRegion(1, tileX = 1, tileY = 0),
            createRegion(2, tileX = 0, tileY = 1),
            createRegion(3, tileX = 1, tileY = 1)
        )
        val map = GameMap(width = 2, height = 2, regions = regions)
        val neighbors = map.getNeighbors(regions[0])
        assertTrue(neighbors.any { it.id == 1 })
        assertTrue(neighbors.any { it.id == 2 })
        assertTrue(neighbors.any { it.id == 3 })
    }

    @Test
    fun testGameMapReplaceRegion() {
        val regions = listOf(createRegion(0), createRegion(1))
        val map = GameMap(width = 2, height = 1, regions = regions)
        val updated = regions[0].copy(population = 50)
        val newMap = map.replaceRegion(updated)
        assertEquals(50, newMap.getRegionById(0)!!.population)
        assertEquals(20, newMap.getRegionById(1)!!.population)
    }

    @Test
    fun testAIFallbackProducesAction() {
        val player = createPlayer(0, Resources(food = 50, wood = 50, stone = 50, iron = 20, gold = 100))
        val aiPlayer = createPlayer(1)
        val regions = listOf(
            createRegion(0, ownerId = 0, terrain = TerrainType.PLAINS),
            createRegion(1, ownerId = 1, terrain = TerrainType.PLAINS)
        )
        val map = GameMap(width = 2, height = 1, regions = regions)
        val state = GameState(
            players = listOf(player, aiPlayer),
            map = map,
            currentPlayerId = 1
        )
        val action = OllamaAI.decide(state)
        assertNotNull(action)
    }

    @Test
    fun testAIFallbackReturnsNullForHuman() {
        val player = createPlayer(0, isHuman = true)
        val aiPlayer = createPlayer(1)
        val regions = listOf(createRegion(0, ownerId = 0), createRegion(1, ownerId = 1))
        val map = GameMap(width = 2, height = 1, regions = regions)
        val state = GameState(
            players = listOf(player, aiPlayer),
            map = map,
            currentPlayerId = 0
        )
        val action = OllamaAI.decide(state)
        assertNull(action)
    }

    @Test
    fun testUnitStackSplitBalanced() {
        val stack = UnitStack(units = listOf(
            Unit(UnitType.INFANTRY, 10),
            Unit(UnitType.CAVALRY, 6)
        ))
        val (a, b) = stack.split(half = true)
        assertEquals(5, a.units.find { it.type == UnitType.INFANTRY }?.count)
        assertEquals(5, b.units.find { it.type == UnitType.INFANTRY }?.count)
        assertEquals(3, a.units.find { it.type == UnitType.CAVALRY }?.count)
        assertEquals(3, b.units.find { it.type == UnitType.CAVALRY }?.count)
    }

    @Test
    fun testUnitStackSplitOdd() {
        val stack = UnitStack(units = listOf(Unit(UnitType.INFANTRY, 7)))
        val (a, b) = stack.split(half = true)
        assertEquals(3, a.units.find { it.type == UnitType.INFANTRY }?.count)
        assertEquals(4, b.units.find { it.type == UnitType.INFANTRY }?.count)
    }

    @Test
    fun testDiplomacyManagerAlliance() {
        val player0 = createPlayer(0)
        val player1 = createPlayer(1)
        val regions = listOf(createRegion(0, ownerId = 0), createRegion(1, ownerId = 1))
        val map = GameMap(width = 2, height = 1, regions = regions)
        val state = GameState(
            players = listOf(player0, player1),
            map = map,
            currentPlayerId = 0
        )
        val result = DiplomacyManager.proposeAlliance(state, 0, 1)
        assertTrue(result.diplomacy.isAllied(0, 1))
    }

    @Test
    fun testDiplomacyManagerTrade() {
        val player0 = createPlayer(0)
        val player1 = createPlayer(1)
        val regions = listOf(createRegion(0, ownerId = 0), createRegion(1, ownerId = 1))
        val map = GameMap(width = 2, height = 1, regions = regions)
        val state = GameState(
            players = listOf(player0, player1),
            map = map,
            currentPlayerId = 0
        )
        val result = DiplomacyManager.proposeTrade(state, 0, 1)
        assertTrue(result.diplomacy.isTradePartner(0, 1))
    }

    @Test
    fun testDiplomacyManagerBreakAlliance() {
        val player0 = createPlayer(0)
        val player1 = createPlayer(1)
        val regions = listOf(createRegion(0, ownerId = 0), createRegion(1, ownerId = 1))
        val map = GameMap(width = 2, height = 1, regions = regions)
        var state = GameState(
            players = listOf(player0, player1),
            map = map,
            currentPlayerId = 0
        )
        state = DiplomacyManager.proposeAlliance(state, 0, 1)
        assertTrue(state.diplomacy.isAllied(0, 1))
        state = DiplomacyManager.breakAlliance(state, 0, 1)
        assertFalse(state.diplomacy.isAllied(0, 1))
    }

    @Test
    fun testResearchPrerequisites() {
        val player = createPlayer(0)
        assertTrue(player.techs.canResearch(TechType.AGRICULTURE))
        assertFalse(player.techs.canResearch(TechType.IRON_WORKING))

        val playerWithAgri = player.copy(techs = TechState(researched = listOf(TechType.AGRICULTURE)))
        assertTrue(playerWithAgri.techs.canResearch(TechType.IRON_WORKING))
        assertTrue(playerWithAgri.techs.canResearch(TechType.MASONRY))
        assertFalse(playerWithAgri.techs.canResearch(TechType.SIEGE_ENGINEERING))
    }

    @Test
    fun testTradeIncomeBetweenAllies() {
        val player0 = createPlayer(0, Resources(food = 50, wood = 50, stone = 50, iron = 50, gold = 50))
        val player1 = createPlayer(1, Resources(food = 100, wood = 100, stone = 100, iron = 100, gold = 100))
        val region0 = createRegion(0, ownerId = 0, terrain = TerrainType.PLAINS)
        val region1 = createRegion(1, ownerId = 1, terrain = TerrainType.FOREST)
        val map = GameMap(width = 2, height = 1, regions = listOf(region0, region1))

        var state = GameState(players = listOf(player0, player1), map = map, currentPlayerId = 0)
        state = DiplomacyManager.proposeAlliance(state, 0, 1)
        state = DiplomacyManager.proposeTrade(state, 0, 1)
        assertTrue(state.diplomacy.isTradePartner(0, 1))
        state = DiplomacyManager.applyTradeIncome(state)

        val p0After = state.players.find { it.id == 0 }!!
        assertTrue(p0After.resources.food >= 50)
    }

    @Test
    fun testEconomyIncomeWithAgriculture() {
        val player = createPlayer(0, Resources(food = 50))
        val region = createRegion(0, ownerId = 0, terrain = TerrainType.PLAINS)
        val map = GameMap(width = 1, height = 1, regions = listOf(region))

        val income = Economy.calculateIncome(player, map)
        val playerWithTech = player.copy(techs = TechState(researched = listOf(TechType.AGRICULTURE)))
        val incomeWithTech = Economy.calculateIncome(playerWithTech, map)
        assertTrue(incomeWithTech.food > income.food)
    }

    @Test
    fun testProcessAttackWithUnits() {
        val source = createRegion(0, ownerId = 0, population = 30,
            units = UnitStack(units = listOf(Unit(UnitType.CAVALRY, 5))))
        val target = createRegion(1, ownerId = 1, population = 10)
        val players = listOf(createPlayer(0), createPlayer(1))
        val map = GameMap(width = 2, height = 1, regions = listOf(source, target))
        val state = GameState(players = players, map = map, currentPlayerId = 0)

        val action = ActionQueue.GameAction(0, ActionQueue.ActionType.ATTACK, 1, "0")
        val result = GameRules.processAttack(state, action)
        assertEquals(0, result.map.getRegionById(1)!!.ownerId)
    }

    @Test
    fun testProcessAttackWithWalls() {
        val source = createRegion(0, ownerId = 0, population = 30)
        val target = createRegion(1, ownerId = 1, population = 20,
            buildings = listOf(Building(BuildingType.WALL), Building(BuildingType.WALL)))
        val players = listOf(createPlayer(0), createPlayer(1))
        val map = GameMap(width = 2, height = 1, regions = listOf(source, target))
        val state = GameState(players = players, map = map, currentPlayerId = 0)

        val action = ActionQueue.GameAction(0, ActionQueue.ActionType.ATTACK, 1, "0")
        val result = GameRules.processAttack(state, action)
        assertEquals(1, result.map.getRegionById(1)!!.ownerId)
    }

    @Test
    fun testFogStateExploresOwnedAndNeighbors() {
        val regions = listOf(
            createRegion(0, tileX = 0, tileY = 0),
            createRegion(1, tileX = 1, tileY = 0),
            createRegion(2, tileX = 0, tileY = 1),
            createRegion(3, tileX = 1, tileY = 1)
        )
        val map = GameMap(width = 2, height = 2, regions = regions)
        val fog = FogState()
        val explored = fog.exploreAllOwned(0, map)

        assertTrue(explored.isExplored(0, 0))
        assertTrue(explored.isExplored(0, 1))
        assertTrue(explored.isExplored(0, 2))
        assertTrue(explored.isExplored(0, 3))
    }

    @Test
    fun testDifficultyBonuses() {
        val easy = DifficultyBonuses.forDifficulty(Difficulty.EASY)
        val normal = DifficultyBonuses.forDifficulty(Difficulty.NORMAL)
        val hard = DifficultyBonuses.forDifficulty(Difficulty.HARD)

        assertTrue(easy.resourceMultiplier < normal.resourceMultiplier)
        assertTrue(hard.resourceMultiplier > normal.resourceMultiplier)
        assertTrue(hard.combatBonus > normal.combatBonus)
    }

    @Test
    fun testProcessRecruitUnitTypes() {
        val region = createRegion(0, ownerId = 0, buildings = listOf(Building(BuildingType.BARRACKS)))
        val player = createPlayer(0, Resources(food = 100, wood = 100, stone = 100, iron = 100, gold = 200))
        val players = listOf(player, createPlayer(1))
        val map = GameMap(width = 1, height = 1, regions = listOf(region))
        val state = GameState(players = players, map = map, currentPlayerId = 0)

        val cavalryAction = ActionQueue.GameAction(0, ActionQueue.ActionType.RECRUIT_CAVALRY, 0)
        val result = GameRules.processRecruitUnit(state, cavalryAction, UnitType.CAVALRY)
        assertEquals(3, result.map.getRegionById(0)!!.units.units.find { it.type == UnitType.CAVALRY }?.count)
    }
}
