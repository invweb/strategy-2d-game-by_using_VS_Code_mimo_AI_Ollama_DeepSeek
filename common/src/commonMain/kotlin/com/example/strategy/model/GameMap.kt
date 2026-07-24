package com.example.strategy.model

import kotlinx.serialization.Serializable

@Serializable
data class GameMap(
    val width: Int,
    val height: Int,
    val regions: List<Region>
) {
    private val regionByTile: Map<Pair<Int, Int>, Region> by lazy {
        regions.associateBy { it.tileX to it.tileY }
    }

    fun getRegionAt(tileX: Int, tileY: Int): Region? =
        regionByTile[tileX to tileY]

    fun getRegionById(id: Int): Region? =
        regions.find { it.id == id }

    fun getNeighbors(region: Region): List<Region> {
        val directions = listOf(
            -1 to 0, 1 to 0, 0 to -1, 0 to 1,
            -1 to -1, -1 to 1, 1 to -1, 1 to 1
        )
        return directions.mapNotNull { (dx, dy) ->
            getRegionAt(region.tileX + dx, region.tileY + dy)
        }
    }

    fun replaceRegion(updated: Region): GameMap =
        copy(regions = regions.map { if (it.id == updated.id) updated else it })
}
