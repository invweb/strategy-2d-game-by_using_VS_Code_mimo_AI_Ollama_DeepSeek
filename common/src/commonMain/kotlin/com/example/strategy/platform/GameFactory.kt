package com.example.strategy.platform

import com.example.strategy.model.*
import com.example.strategy.serialization.GameStateSerializer
import kotlin.random.Random

// Default game state factory — creates initial game world
object GameFactory {

    private val terrainSeed = Random.nextLong()
    private val perlin = PerlinNoise(terrainSeed)

    enum class MapSize(val w: Int, val h: Int) {
        SMALL(8, 6),
        MEDIUM(12, 8),
        LARGE(16, 10)
    }

    enum class TerrainStyle {
        BALANCED,
        FOREST_HEAVY,
        MOUNTAINOUS,
        PLAINS_DOMINANT
    }

    fun createDefaultGameState(): GameState = createGameState(MapSize.MEDIUM, TerrainStyle.BALANCED, Difficulty.NORMAL)

    fun createGameState(mapSize: MapSize, terrainStyle: TerrainStyle, difficulty: Difficulty = Difficulty.NORMAL): GameState {
        val players = listOf(
            Player(id = 0, name = "Player", color = "BLUE", isHuman = true),
            Player(id = 1, name = "AI Empire", color = "RED", isHuman = false)
        )

        val w = mapSize.w
        val h = mapSize.h
        val regions = mutableListOf<Region>()
        var id = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val terrain = when {
                    x == 0 || y == 0 || x == w - 1 || y == h - 1 -> TerrainType.WATER
                    else -> generateTerrain(x, y, terrainStyle, w, h)
                }

                val owner = when {
                    terrain == TerrainType.WATER -> null
                    x < w / 2 -> 0
                    else -> 1
                }
                val pop = if (terrain == TerrainType.WATER) 0 else (5..15).random()

                val buildings = mutableListOf<Building>()
                if (owner == 0 && x == 1 && y == 1 && terrain == TerrainType.PLAINS) {
                    buildings.add(Building(BuildingType.FARM))
                    buildings.add(Building(BuildingType.BARRACKS))
                    buildings.add(Building(BuildingType.MARKET))
                }
                if (owner == 1 && x == w - 2 && y == h - 2 && terrain == TerrainType.PLAINS) {
                    buildings.add(Building(BuildingType.FARM))
                    buildings.add(Building(BuildingType.BARRACKS))
                    buildings.add(Building(BuildingType.MARKET))
                }

                regions.add(
                    Region(
                        id = id,
                        name = "Region $id",
                        terrain = terrain,
                        tileX = x,
                        tileY = y,
                        ownerId = owner,
                        population = pop,
                        buildings = buildings
                    )
                )
                id++
            }
        }

        return GameState(
            turn = 1,
            currentPlayerId = 0,
            players = players,
            map = GameMap(width = w, height = h, regions = regions),
            difficulty = difficulty
        )
    }

    private fun generateTerrain(x: Int, y: Int, style: TerrainStyle, w: Int, h: Int): TerrainType {
        val freq = 6.0 / minOf(w, h)
        val nx = x.toDouble() * freq
        val ny = y.toDouble() * freq
        val elevation = perlin.octaveNoise(nx, ny, 4, 0.5)
        val moisture = perlin.octaveNoise(nx + 50, ny + 50, 3, 0.5)

        val e = (elevation + 1) / 2.0
        val m = (moisture + 1) / 2.0

        return when (style) {
            TerrainStyle.BALANCED -> when {
                e < 0.25 -> TerrainType.WATER
                e < 0.35 -> TerrainType.SWAMP
                e < 0.50 -> when {
                    m > 0.65 -> TerrainType.FOREST
                    m < 0.30 -> TerrainType.DESERT
                    else -> TerrainType.PLAINS
                }
                e < 0.65 -> when {
                    m > 0.60 -> TerrainType.FOREST
                    else -> TerrainType.HILLS
                }
                e < 0.80 -> TerrainType.MOUNTAIN
                else -> TerrainType.SNOW
            }
            TerrainStyle.FOREST_HEAVY -> when {
                e < 0.20 -> TerrainType.WATER
                e < 0.30 -> TerrainType.SWAMP
                e < 0.55 -> when {
                    m > 0.40 -> TerrainType.FOREST
                    else -> TerrainType.PLAINS
                }
                e < 0.70 -> when {
                    m > 0.35 -> TerrainType.FOREST
                    else -> TerrainType.HILLS
                }
                e < 0.85 -> TerrainType.MOUNTAIN
                else -> TerrainType.SNOW
            }
            TerrainStyle.MOUNTAINOUS -> when {
                e < 0.20 -> TerrainType.WATER
                e < 0.30 -> TerrainType.SWAMP
                e < 0.45 -> when {
                    m > 0.60 -> TerrainType.FOREST
                    m < 0.35 -> TerrainType.DESERT
                    else -> TerrainType.PLAINS
                }
                e < 0.60 -> TerrainType.HILLS
                e < 0.80 -> TerrainType.MOUNTAIN
                else -> TerrainType.SNOW
            }
            TerrainStyle.PLAINS_DOMINANT -> when {
                e < 0.25 -> TerrainType.WATER
                e < 0.35 -> TerrainType.SWAMP
                e < 0.60 -> when {
                    m > 0.75 -> TerrainType.FOREST
                    m < 0.25 -> TerrainType.DESERT
                    else -> TerrainType.PLAINS
                }
                e < 0.75 -> TerrainType.HILLS
                e < 0.85 -> TerrainType.MOUNTAIN
                else -> TerrainType.SNOW
            }
        }
    }

    fun saveGame(state: GameState) {
        val json = GameStateSerializer.serialize(state)
        PlatformProvider.platform.writeTextFile("savegame.json", json)
    }

    fun loadGame(): GameState? {
        val json = PlatformProvider.platform.readTextFile("savegame.json") ?: return null
        return GameStateSerializer.deserialize(json)
    }
}
