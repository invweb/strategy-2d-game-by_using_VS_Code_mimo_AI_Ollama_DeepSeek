package com.example.strategy.platform

import com.example.strategy.model.*
import com.example.strategy.serialization.GameStateSerializer

// Default game state factory — creates initial game world
object GameFactory {

    fun createDefaultGameState(): GameState {
        val players = listOf(
            Player(id = 0, name = "Player", color = "BLUE", isHuman = true),
            Player(id = 1, name = "AI Empire", color = "RED", isHuman = false)
        )

        val regions = mutableListOf<Region>()
        var id = 0
        for (y in 0 until 8) {
            for (x in 0 until 10) {
                val terrain = when {
                    x == 0 || y == 0 || x == 9 || y == 7 -> TerrainType.WATER
                    (x + y) % 5 == 0 -> TerrainType.MOUNTAIN
                    (x * y) % 7 == 0 -> TerrainType.FOREST
                    (x + y) % 3 == 0 -> TerrainType.HILLS
                    else -> TerrainType.PLAINS
                }

                val owner = when {
                    terrain == TerrainType.WATER -> null
                    x < 5 -> 0
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
            map = GameMap(width = 10, height = 8, regions = regions)
        )
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
