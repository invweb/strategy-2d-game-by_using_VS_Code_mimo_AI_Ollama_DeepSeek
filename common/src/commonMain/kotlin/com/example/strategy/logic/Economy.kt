package com.example.strategy.logic

import com.example.strategy.model.*

// Simple economy — calculates resource income per turn for a player
object Economy {

    fun calculateIncome(player: Player, gameMap: GameMap): Resources {
        val ownedRegions = gameMap.regions.filter { it.ownerId == player.id }
        var food = 0
        var wood = 0
        var stone = 0
        var iron = 0
        var gold = 0

        for (region in ownedRegions) {
            val pop = region.population
            food += when (region.terrain) {
                TerrainType.PLAINS -> 3
                TerrainType.FOREST -> 1
                TerrainType.HILLS -> 2
                TerrainType.MOUNTAIN -> 1
                TerrainType.WATER -> 0
            }
            wood += if (region.terrain == TerrainType.FOREST) 3 else 0
            stone += if (region.terrain == TerrainType.MOUNTAIN || region.terrain == TerrainType.HILLS) 2 else 0
            iron += if (region.terrain == TerrainType.MOUNTAIN) 1 else 0
            gold += pop / 10

            for (building in region.buildings) {
                when (building.type) {
                    BuildingType.FARM -> food += 2 * building.level
                    BuildingType.LUMBER_MILL -> wood += 2 * building.level
                    BuildingType.QUARRY -> stone += 2 * building.level
                    BuildingType.MINE -> iron += 2 * building.level
                    BuildingType.MARKET -> gold += 3 * building.level
                    BuildingType.BARRACKS -> {} // no resource bonus
                    BuildingType.WALL -> {}    // no resource bonus
                }
            }
        }
        return Resources(food, wood, stone, iron, gold)
    }

    fun applyTurnIncome(gameState: GameState): GameState {
        val updatedPlayers = gameState.players.map { player ->
            val income = calculateIncome(player, gameState.map)
            player.copy(resources = player.resources + income)
        }
        return gameState.copy(players = updatedPlayers)
    }

    fun upkeepCost(player: Player, gameMap: GameMap): Resources {
        val ownedRegions = gameMap.regions.filter { it.ownerId == player.id }
        val totalPopulation = ownedRegions.sumOf { it.population }
        val foodUpkeep = totalPopulation / 5
        return Resources(food = foodUpkeep)
    }
}
