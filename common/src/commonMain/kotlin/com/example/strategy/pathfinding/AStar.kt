package com.example.strategy.pathfinding

import com.example.strategy.model.GameMap
import com.example.strategy.model.Region
import com.example.strategy.model.TerrainType
import kotlin.math.abs

// Simple A* pathfinding — works on GameMap regions, pure Kotlin
object AStar {

    data class PathNode(
        val region: Region,
        val gCost: Int,
        val fCost: Int,
        val parent: PathNode? = null
    ) : Comparable<PathNode> {
        override fun compareTo(other: PathNode): Int = fCost.compareTo(other.fCost)
    }

    private fun heuristic(a: Region, b: Region): Int =
        abs(a.tileX - b.tileX) + abs(a.tileY - b.tileY)

    private fun terrainCost(terrain: TerrainType): Int = when (terrain) {
        TerrainType.PLAINS -> 1
        TerrainType.FOREST -> 3
        TerrainType.HILLS -> 2
        TerrainType.MOUNTAIN -> 4
        TerrainType.WATER -> Int.MAX_VALUE // impassable
        TerrainType.DESERT -> 2
        TerrainType.SWAMP -> 3
        TerrainType.SNOW -> 2
    }

    fun findPath(gameMap: GameMap, startId: Int, endId: Int): List<Region> {
        val start = gameMap.getRegionById(startId) ?: return emptyList()
        val end = gameMap.getRegionById(endId) ?: return emptyList()

        if (startId == endId) return listOf(start)

        val openSet = mutableListOf(PathNode(start, 0, heuristic(start, start)))
        val closedSet = mutableSetOf<Int>()
        val nodeMap = mutableMapOf<Int, PathNode>()

        nodeMap[start.id] = openSet.first()

        while (openSet.isNotEmpty()) {
            val current = openSet.removeFirst()

            if (current.region.id == end.id) {
                return reconstructPath(current)
            }

            closedSet.add(current.region.id)

            for (neighbor in gameMap.getNeighbors(current.region)) {
                if (neighbor.id in closedSet) continue
                if (neighbor.ownerId != start.ownerId && neighbor.id != end.id) continue

                val moveCost = terrainCost(neighbor.terrain)
                if (moveCost == Int.MAX_VALUE) continue

                val tentativeG = current.gCost + moveCost
                val existing = nodeMap[neighbor.id]

                if (existing == null || tentativeG < existing.gCost) {
                    val node = PathNode(neighbor, tentativeG, tentativeG + heuristic(neighbor, end), current)
                    nodeMap[neighbor.id] = node
                    openSet.add(node)
                    openSet.sort()
                }
            }
        }

        return emptyList()
    }

    private fun reconstructPath(node: PathNode): List<Region> {
        val path = mutableListOf<Region>()
        var current: PathNode? = node
        while (current != null) {
            path.add(0, current.region)
            current = current.parent
        }
        return path
    }
}
