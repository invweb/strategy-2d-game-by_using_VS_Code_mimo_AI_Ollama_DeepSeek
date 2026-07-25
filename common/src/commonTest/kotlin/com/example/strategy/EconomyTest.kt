package com.example.strategy

import com.example.strategy.model.*
import com.example.strategy.logic.*
import kotlin.test.*

class EconomyTest {

    private fun createPlayer(
        id: Int = 1,
        techs: TechState = TechState()
    ) = Player(id = id, name = "Player$id", color = "blue", resources = Resources(), techs = techs)

    private fun createRegion(
        id: Int = 1,
        ownerId: Int? = 1,
        terrain: TerrainType = TerrainType.PLAINS,
        population: Int = 20,
        buildings: List<Building> = emptyList()
    ) = Region(
        id = id, name = "Region$id", terrain = terrain,
        tileX = id, tileY = 0, ownerId = ownerId,
        population = population, buildings = buildings
    )

    @Test
    fun testIncomePlains() {
        val player = createPlayer()
        val regions = listOf(createRegion(terrain = TerrainType.PLAINS, population = 20))
        val map = GameMap(width = 1, height = 1, regions = regions)
        val income = Economy.calculateIncome(player, map)
        assertEquals(3, income.food)
        assertEquals(0, income.wood)
        assertEquals(0, income.stone)
        assertEquals(2, income.gold) // 20 / 10
    }

    @Test
    fun testIncomeForest() {
        val player = createPlayer()
        val regions = listOf(createRegion(terrain = TerrainType.FOREST, population = 30))
        val map = GameMap(width = 1, height = 1, regions = regions)
        val income = Economy.calculateIncome(player, map)
        assertEquals(1, income.food)
        assertEquals(3, income.wood)
        assertEquals(3, income.gold) // 30 / 10
    }

    @Test
    fun testIncomeMountain() {
        val player = createPlayer()
        val regions = listOf(createRegion(terrain = TerrainType.MOUNTAIN, population = 15))
        val map = GameMap(width = 1, height = 1, regions = regions)
        val income = Economy.calculateIncome(player, map)
        assertEquals(1, income.food)
        assertEquals(2, income.stone)
        assertEquals(1, income.iron)
        assertEquals(1, income.gold) // 15 / 10
    }

    @Test
    fun testIncomeHills() {
        val player = createPlayer()
        val regions = listOf(createRegion(terrain = TerrainType.HILLS, population = 10))
        val map = GameMap(width = 1, height = 1, regions = regions)
        val income = Economy.calculateIncome(player, map)
        assertEquals(2, income.food)
        assertEquals(2, income.stone)
        assertEquals(1, income.gold) // 10 / 10
    }

    @Test
    fun testIncomeWithFarm() {
        val player = createPlayer()
        val regions = listOf(createRegion(
            terrain = TerrainType.PLAINS,
            population = 20,
            buildings = listOf(Building(BuildingType.FARM))
        ))
        val map = GameMap(width = 1, height = 1, regions = regions)
        val income = Economy.calculateIncome(player, map)
        assertEquals(5, income.food) // 3 base + 2 from farm
    }

    @Test
    fun testIncomeWithLumberMill() {
        val player = createPlayer()
        val regions = listOf(createRegion(
            terrain = TerrainType.FOREST,
            population = 20,
            buildings = listOf(Building(BuildingType.LUMBER_MILL))
        ))
        val map = GameMap(width = 1, height = 1, regions = regions)
        val income = Economy.calculateIncome(player, map)
        assertEquals(5, income.wood) // 3 base + 2 from lumber mill
    }

    @Test
    fun testIncomeWithMarket() {
        val player = createPlayer()
        val regions = listOf(createRegion(
            terrain = TerrainType.PLAINS,
            population = 20,
            buildings = listOf(Building(BuildingType.MARKET))
        ))
        val map = GameMap(width = 1, height = 1, regions = regions)
        val income = Economy.calculateIncome(player, map)
        assertEquals(5, income.gold) // 2 base + 3 from market
    }

    @Test
    fun testIncomeAgricultureBonus() {
        val player = createPlayer(techs = TechState(researched = listOf(TechType.AGRICULTURE)))
        val regions = listOf(createRegion(terrain = TerrainType.PLAINS, population = 20))
        val map = GameMap(width = 1, height = 1, regions = regions)
        val income = Economy.calculateIncome(player, map)
        assertEquals(4, income.food) // 3 * 1.5 = 4.5 -> 4
    }

    @Test
    fun testIncomeMultipleRegions() {
        val player = createPlayer()
        val regions = listOf(
            createRegion(1, 1, TerrainType.PLAINS, 20),
            createRegion(2, 1, TerrainType.FOREST, 30),
            createRegion(3, 1, TerrainType.MOUNTAIN, 15)
        )
        val map = GameMap(width = 3, height = 1, regions = regions)
        val income = Economy.calculateIncome(player, map)
        assertEquals(5, income.food) // 3 + 1 + 1
        assertEquals(3, income.wood)
        assertEquals(2, income.stone)
        assertEquals(1, income.iron)
        assertEquals(6, income.gold) // 2 + 3 + 1
    }

    @Test
    fun testIncomeOnlyOwnedRegions() {
        val player = createPlayer(id = 1)
        val regions = listOf(
            createRegion(1, 1, TerrainType.PLAINS, 20),
            createRegion(2, 2, TerrainType.FOREST, 30)
        )
        val map = GameMap(width = 2, height = 1, regions = regions)
        val income = Economy.calculateIncome(player, map)
        assertEquals(3, income.food) // only player 1's region
        assertEquals(0, income.wood)
    }

    @Test
    fun testUpkeepCost() {
        val player = createPlayer()
        val regions = listOf(
            createRegion(1, 1, population = 20),
            createRegion(2, 1, population = 30)
        )
        val map = GameMap(width = 2, height = 1, regions = regions)
        val upkeep = Economy.upkeepCost(player, map)
        assertEquals(10, upkeep.food) // (20 + 30) / 5
    }

    @Test
    fun testUpkeepCostSingleRegion() {
        val player = createPlayer()
        val regions = listOf(createRegion(1, 1, population = 15))
        val map = GameMap(width = 1, height = 1, regions = regions)
        val upkeep = Economy.upkeepCost(player, map)
        assertEquals(3, upkeep.food) // 15 / 5
    }
}
