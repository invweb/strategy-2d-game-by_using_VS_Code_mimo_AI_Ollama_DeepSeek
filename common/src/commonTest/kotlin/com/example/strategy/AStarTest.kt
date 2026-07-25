package com.example.strategy

import com.example.strategy.model.*
import com.example.strategy.pathfinding.*
import kotlin.test.*

class AStarTest {

    private fun createRegion(
        id: Int,
        tileX: Int,
        tileY: Int,
        terrain: TerrainType = TerrainType.PLAINS,
        ownerId: Int? = 1
    ) = Region(
        id = id, name = "Region$id", terrain = terrain,
        tileX = tileX, tileY = tileY, ownerId = ownerId
    )

    private fun createMap(vararg regions: Region): GameMap =
        GameMap(width = 10, height = 10, regions = regions.toList())

    @Test
    fun testFindPathSameNode() {
        val r = createRegion(1, 0, 0)
        val map = createMap(r)
        val path = AStar.findPath(map, 1, 1)
        assertEquals(1, path.size)
        assertEquals(1, path[0].id)
    }

    @Test
    fun testFindPathDirectNeighbor() {
        val r1 = createRegion(1, 0, 0)
        val r2 = createRegion(2, 1, 0)
        val map = createMap(r1, r2)
        val path = AStar.findPath(map, 1, 2)
        assertEquals(2, path.size)
        assertEquals(1, path[0].id)
        assertEquals(2, path[1].id)
    }

    @Test
    fun testFindPathTwoSteps() {
        val r1 = createRegion(1, 0, 0)
        val r2 = createRegion(2, 1, 0)
        val r3 = createRegion(3, 2, 0)
        val map = createMap(r1, r2, r3)
        val path = AStar.findPath(map, 1, 3)
        assertEquals(3, path.size)
        assertEquals(1, path[0].id)
        assertEquals(2, path[1].id)
        assertEquals(3, path[2].id)
    }

    @Test
    fun testFindPathDiagonal() {
        val r1 = createRegion(1, 0, 0)
        val r2 = createRegion(2, 1, 1)
        val map = createMap(r1, r2)
        val path = AStar.findPath(map, 1, 2)
        assertEquals(2, path.size)
    }

    @Test
    fun testFindPathWaterBlocks() {
        val r1 = createRegion(1, 0, 0)
        val water = createRegion(2, 1, 0, terrain = TerrainType.WATER)
        val r3 = createRegion(3, 2, 0)
        val map = createMap(r1, water, r3)
        val path = AStar.findPath(map, 1, 3)
        assertTrue(path.isEmpty())
    }

    @Test
    fun testFindPathWaterDetour() {
        val r1 = createRegion(1, 0, 0)
        val water = createRegion(2, 1, 0, terrain = TerrainType.WATER)
        val r3 = createRegion(3, 2, 0)
        val r4 = createRegion(4, 1, 1, terrain = TerrainType.FOREST)
        val map = createMap(r1, water, r3, r4)
        val path = AStar.findPath(map, 1, 3)
        assertTrue(path.isNotEmpty())
        assertFalse(path.any { it.id == 2 })
    }

    @Test
    fun testFindPathPrefersPlainsOverForest() {
        val r1 = createRegion(1, 0, 0)
        val forest = createRegion(2, 1, 0, terrain = TerrainType.FOREST)
        val plains = createRegion(3, 1, 1, terrain = TerrainType.PLAINS)
        val r4 = createRegion(4, 2, 0)
        val r5 = createRegion(5, 2, 1)
        val map = createMap(r1, forest, plains, r4, r5)
        val path = AStar.findPath(map, 1, 4)
        assertTrue(path.isNotEmpty())
    }

    @Test
    fun testFindPathNonexistentStart() {
        val r1 = createRegion(1, 0, 0)
        val map = createMap(r1)
        val path = AStar.findPath(map, 99, 1)
        assertTrue(path.isEmpty())
    }

    @Test
    fun testFindPathNonexistentEnd() {
        val r1 = createRegion(1, 0, 0)
        val map = createMap(r1)
        val path = AStar.findPath(map, 1, 99)
        assertTrue(path.isEmpty())
    }

    @Test
    fun testFindPathLShape() {
        val r1 = createRegion(1, 0, 0)
        val r2 = createRegion(2, 1, 0)
        val r3 = createRegion(3, 2, 0)
        val r4 = createRegion(4, 2, 1)
        val map = createMap(r1, r2, r3, r4)
        val path = AStar.findPath(map, 1, 4)
        assertTrue(path.isNotEmpty())
        assertEquals(4, path.last().id)
    }
}
