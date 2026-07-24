package com.example.strategy.logic

import com.example.strategy.model.*

// Business rules — build, recruit, attack, move, develop
object GameRules {

    private val BUILD_COSTS = mapOf(
        BuildingType.FARM to Resources(food = 10, wood = 5),
        BuildingType.LUMBER_MILL to Resources(wood = 15, gold = 5),
        BuildingType.QUARRY to Resources(wood = 10, stone = 10),
        BuildingType.MINE to Resources(wood = 5, stone = 15, iron = 5),
        BuildingType.MARKET to Resources(wood = 10, stone = 5, gold = 15),
        BuildingType.BARRACKS to Resources(wood = 15, stone = 10, gold = 10),
        BuildingType.WALL to Resources(stone = 20, iron = 5)
    )

    fun canBuild(player: Player, region: Region, type: BuildingType): Boolean {
        val cost = BUILD_COSTS[type] ?: return false
        return region.ownerId == player.id && region.buildings.isEmpty() && player.resources.canAfford(cost)
    }

    fun processBuild(gameState: GameState, action: ActionQueue.GameAction): GameState {
        val player = gameState.players.find { it.id == action.playerId } ?: return gameState
        val region = gameState.map.getRegionById(action.targetRegionId) ?: return gameState
        val buildingType = try {
            BuildingType.valueOf(action.param)
        } catch (_: Exception) {
            return gameState
        }

        if (!canBuild(player, region, buildingType)) return gameState

        val cost = BUILD_COSTS[buildingType] ?: return gameState
        val updatedPlayer = player.copy(resources = player.resources - cost)
        val updatedRegion = region.copy(buildings = region.buildings + Building(buildingType))

        return gameState.copy(
            players = gameState.players.map { if (it.id == player.id) updatedPlayer else it },
            map = gameState.map.replaceRegion(updatedRegion),
            actionsLog = gameState.actionsLog + "${player.name} built ${buildingType.name} in ${region.name}"
        )
    }

    fun processRecruit(gameState: GameState, action: ActionQueue.GameAction): GameState {
        val player = gameState.players.find { it.id == action.playerId } ?: return gameState
        val region = gameState.map.getRegionById(action.targetRegionId) ?: return gameState

        if (region.ownerId != player.id) return gameState
        val hasBarracks = region.buildings.any { it.type == BuildingType.BARRACKS }
        if (!hasBarracks) return gameState

        val cost = Resources(food = 10, gold = 5)
        if (!player.resources.canAfford(cost)) return gameState

        val updatedPlayer = player.copy(resources = player.resources - cost)
        val updatedRegion = region.copy(population = region.population + 5)

        return gameState.copy(
            players = gameState.players.map { if (it.id == player.id) updatedPlayer else it },
            map = gameState.map.replaceRegion(updatedRegion),
            actionsLog = gameState.actionsLog + "${player.name} recruited troops in ${region.name}"
        )
    }

    fun processAttack(gameState: GameState, action: ActionQueue.GameAction): GameState {
        val player = gameState.players.find { it.id == action.playerId } ?: return gameState
        val targetRegion = gameState.map.getRegionById(action.targetRegionId) ?: return gameState
        val sourceRegionId = action.param.toIntOrNull() ?: return gameState
        val sourceRegion = gameState.map.getRegionById(sourceRegionId) ?: return gameState

        if (sourceRegion.ownerId != player.id) return gameState
        if (sourceRegion.ownerId == targetRegion.ownerId) return gameState

        val attackStrength = sourceRegion.population
        val defenseStrength = targetRegion.population
        val wallBonus = targetRegion.buildings.count { it.type == BuildingType.WALL } * 5

        if (attackStrength > defenseStrength + wallBonus) {
            val updatedSource = sourceRegion.copy(population = sourceRegion.population / 2)
            val updatedTarget = targetRegion.copy(
                ownerId = player.id,
                population = (attackStrength - defenseStrength - wallBonus).coerceAtLeast(1),
                buildings = targetRegion.buildings.filter { it.type != BuildingType.WALL }
            )
            return gameState.copy(
                map = gameState.map.replaceRegion(updatedSource).replaceRegion(updatedTarget),
                actionsLog = gameState.actionsLog + "${player.name} conquered ${targetRegion.name}"
            )
        }

        val updatedSource = sourceRegion.copy(population = sourceRegion.population / 3)
        return gameState.copy(
            map = gameState.map.replaceRegion(updatedSource),
            actionsLog = gameState.actionsLog + "${player.name} failed to conquer ${targetRegion.name}"
        )
    }

    fun processMove(gameState: GameState, action: ActionQueue.GameAction): GameState {
        val player = gameState.players.find { it.id == action.playerId } ?: return gameState
        val source = gameState.map.getRegionById(action.targetRegionId) ?: return gameState
        val destId = action.param.toIntOrNull() ?: return gameState
        val dest = gameState.map.getRegionById(destId) ?: return gameState

        if (source.ownerId != player.id || dest.ownerId != player.id) return gameState
        if (source.id == dest.id) return gameState

        val troops = source.population / 2

        val updatedSource = source.copy(population = source.population - troops)
        val updatedDest = dest.copy(population = dest.population + troops)

        return gameState.copy(
            map = gameState.map.replaceRegion(updatedSource).replaceRegion(updatedDest),
            actionsLog = gameState.actionsLog + "${player.name} moved $troops troops from ${source.name} to ${dest.name}"
        )
    }

    fun processDevelop(gameState: GameState, action: ActionQueue.GameAction): GameState {
        val player = gameState.players.find { it.id == action.playerId } ?: return gameState
        val region = gameState.map.getRegionById(action.targetRegionId) ?: return gameState

        if (region.ownerId != player.id) return gameState

        val cost = Resources(gold = 10)
        if (!player.resources.canAfford(cost)) return gameState

        val updatedPlayer = player.copy(resources = player.resources - cost)
        val updatedRegion = region.copy(population = region.population + 3)

        return gameState.copy(
            players = gameState.players.map { if (it.id == player.id) updatedPlayer else it },
            map = gameState.map.replaceRegion(updatedRegion),
            actionsLog = gameState.actionsLog + "${player.name} developed ${region.name}"
        )
    }
}
