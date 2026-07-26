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
                TerrainType.DESERT -> 1
                TerrainType.SWAMP -> 1
                TerrainType.SNOW -> 0
            }
            wood += when (region.terrain) {
                TerrainType.FOREST -> 3
                TerrainType.SWAMP -> 2
                else -> 0
            }
            stone += if (region.terrain == TerrainType.MOUNTAIN || region.terrain == TerrainType.HILLS) 2 else 0
            iron += when (region.terrain) {
                TerrainType.MOUNTAIN -> 1
                TerrainType.SNOW -> 1
                else -> 0
            }
            gold += when (region.terrain) {
                TerrainType.DESERT -> (pop / 8) + 1
                else -> pop / 10
            }

            for (building in region.buildings) {
                when (building.type) {
                    BuildingType.FARM -> food += 2 * building.level
                    BuildingType.LUMBER_MILL -> wood += 2 * building.level
                    BuildingType.QUARRY -> stone += 2 * building.level
                    BuildingType.MINE -> iron += 2 * building.level
                    BuildingType.MARKET -> gold += 3 * building.level
                    BuildingType.BARRACKS -> {}
                    BuildingType.WALL -> {}
                }
            }
        }

        if (player.techs.isResearched(TechType.AGRICULTURE)) {
            food = (food * 1.5).toInt()
        }

        return Resources(food, wood, stone, iron, gold)
    }

    fun applyTurnIncome(gameState: GameState): GameState {
        val bonuses = DifficultyBonuses.forDifficulty(gameState.difficulty)
        val updatedPlayers = gameState.players.map { player ->
            val income = calculateIncome(player, gameState.map)
            val adjustedIncome = if (!player.isHuman) {
                Resources(
                    food = (income.food * bonuses.resourceMultiplier).toInt(),
                    wood = (income.wood * bonuses.resourceMultiplier).toInt(),
                    stone = (income.stone * bonuses.resourceMultiplier).toInt(),
                    iron = (income.iron * bonuses.resourceMultiplier).toInt(),
                    gold = (income.gold * bonuses.resourceMultiplier).toInt()
                )
            } else income
            player.copy(resources = player.resources + adjustedIncome)
        }
        return gameState.copy(players = updatedPlayers)
    }

    fun upkeepCost(player: Player, gameMap: GameMap): Resources {
        val ownedRegions = gameMap.regions.filter { it.ownerId == player.id }
        val totalPopulation = ownedRegions.sumOf { it.population }
        val foodUpkeep = totalPopulation / 5
        val extraUpkeep: Int = ownedRegions.sumOf { region: Region ->
            when (region.terrain) {
                TerrainType.SNOW -> 1
                TerrainType.SWAMP -> 1
                else -> 0
            }.toLong()
        }.toInt()
        return Resources(food = foodUpkeep + extraUpkeep)
    }
}
