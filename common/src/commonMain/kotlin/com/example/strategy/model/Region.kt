package com.example.strategy.model

import kotlinx.serialization.Serializable

@Serializable
enum class TerrainType {
    PLAINS,
    FOREST,
    MOUNTAIN,
    HILLS,
    WATER
}

@Serializable
data class Region(
    val id: Int,
    val name: String,
    val terrain: TerrainType,
    val tileX: Int,
    val tileY: Int,
    val ownerId: Int? = null,
    val population: Int = 10,
    val resources: Resources = Resources(food = 5),
    val buildings: List<Building> = emptyList()
)

@Serializable
enum class BuildingType {
    FARM,
    LUMBER_MILL,
    QUARRY,
    MINE,
    MARKET,
    BARRACKS,
    WALL
}

@Serializable
data class Building(
    val type: BuildingType,
    val level: Int = 1
)
