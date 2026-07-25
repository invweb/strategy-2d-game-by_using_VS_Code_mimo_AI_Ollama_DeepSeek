package com.example.strategy.logic

import com.example.strategy.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class EconomyTest {

    @Test
    fun `calculateIncome from plains gives 3 food`() {
        val player = Player(id = 0, name = "P0", color = "blue")
        val regions = listOf(
            Region(id = 0, name = "R0", terrain = TerrainType.PLAINS, tileX = 0, tileY = 0, ownerId = 0)
        )
        val map = GameMap(width = 1, height = 1, regions = regions)

        val income = Economy.calculateIncome(player, map)

        assertEquals(3, income.food)
        assertEquals(0, income.wood)
        assertEquals(0, income.stone)
        assertEquals(0, income.iron)
    }

    @Test
    fun `calculateIncome from forest gives 1 food and 3 wood`() {
        val player = Player(id = 0, name = "P0", color = "blue")
        val regions = listOf(
            Region(id = 0, name = "R0", terrain = TerrainType.FOREST, tileX = 0, tileY = 0, ownerId = 0, population = 10)
        )
        val map = GameMap(width = 1, height = 1, regions = regions)

        val income = Economy.calculateIncome(player, map)

        assertEquals(1, income.food)
        assertEquals(3, income.wood)
        assertEquals(1, income.gold) // pop/10 = 10/10 = 1
    }

    @Test
    fun `calculateIncome from mountain gives iron`() {
        val player = Player(id = 0, name = "P0", color = "blue")
        val regions = listOf(
            Region(id = 0, name = "R0", terrain = TerrainType.MOUNTAIN, tileX = 0, tileY = 0, ownerId = 0, population = 20)
        )
        val map = GameMap(width = 1, height = 1, regions = regions)

        val income = Economy.calculateIncome(player, map)

        assertEquals(1, income.iron)
        assertEquals(2, income.gold) // pop/10 = 20/10 = 2
    }

    @Test
    fun `calculateIncome with farm adds bonus`() {
        val player = Player(id = 0, name = "P0", color = "blue")
        val regions = listOf(
            Region(id = 0, name = "R0", terrain = TerrainType.PLAINS, tileX = 0, tileY = 0, ownerId = 0,
                buildings = listOf(Building(BuildingType.FARM)))
        )
        val map = GameMap(width = 1, height = 1, regions = regions)

        val income = Economy.calculateIncome(player, map)

        assertEquals(5, income.food) // 3 base + 2 farm bonus
    }

    @Test
    fun `upkeepCost scales with population`() {
        val player = Player(id = 0, name = "P0", color = "blue")
        val regions = listOf(
            Region(id = 0, name = "R0", terrain = TerrainType.PLAINS, tileX = 0, tileY = 0, ownerId = 0, population = 25)
        )
        val map = GameMap(width = 1, height = 1, regions = regions)

        val upkeep = Economy.upkeepCost(player, map)

        assertEquals(5, upkeep.food) // 25/5 = 5
    }

    @Test
    fun `calculateIncome sums multiple regions`() {
        val player = Player(id = 0, name = "P0", color = "blue")
        val regions = listOf(
            Region(id = 0, name = "R0", terrain = TerrainType.PLAINS, tileX = 0, tileY = 0, ownerId = 0),
            Region(id = 1, name = "R1", terrain = TerrainType.FOREST, tileX = 1, tileY = 0, ownerId = 0, population = 10)
        )
        val map = GameMap(width = 2, height = 1, regions = regions)

        val income = Economy.calculateIncome(player, map)

        assertEquals(4, income.food) // 3 plains + 1 forest
        assertEquals(3, income.wood) // 3 forest
    }
}
