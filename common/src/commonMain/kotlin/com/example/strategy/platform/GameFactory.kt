package com.example.strategy.platform

import com.example.strategy.model.*
import com.example.strategy.serialization.GameStateSerializer

// Default game state factory — creates initial game world
object GameFactory {

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
                    else -> generateTerrain(x, y, terrainStyle)
                }

                val owner = when {
                    terrain == TerrainType.WATER -> null
                    x < w / 2 -> 0
                    else -> 1
                }
                val pop = if (terrain == TerrainType.WATER) 0 else (5..15).random()

                regions.add(
                    Region(
                        id = id,
                        name = "Region $id",
                        terrain = terrain,
                        tileX = x,
                        tileY = y,
                        ownerId = owner,
                        population = pop
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

    private fun generateTerrain(x: Int, y: Int, style: TerrainStyle): TerrainType {
        val noise = (x * 7 + y * 13 + x * y * 3) % 10
        return when (style) {
            TerrainStyle.BALANCED -> when {
                noise % 5 == 0 -> TerrainType.MOUNTAIN
                noise % 4 == 0 -> TerrainType.FOREST
                noise % 3 == 0 -> TerrainType.HILLS
                else -> TerrainType.PLAINS
            }
            TerrainStyle.FOREST_HEAVY -> when {
                noise % 6 == 0 -> TerrainType.MOUNTAIN
                noise < 5 -> TerrainType.FOREST
                noise % 3 == 0 -> TerrainType.HILLS
                else -> TerrainType.PLAINS
            }
            TerrainStyle.MOUNTAINOUS -> when {
                noise < 4 -> TerrainType.MOUNTAIN
                noise % 3 == 0 -> TerrainType.HILLS
                noise % 4 == 0 -> TerrainType.FOREST
                else -> TerrainType.PLAINS
            }
            TerrainStyle.PLAINS_DOMINANT -> when {
                noise % 7 == 0 -> TerrainType.MOUNTAIN
                noise % 5 == 0 -> TerrainType.FOREST
                noise % 4 == 0 -> TerrainType.HILLS
                else -> TerrainType.PLAINS
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
