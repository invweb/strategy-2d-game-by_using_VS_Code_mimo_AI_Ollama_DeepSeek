package com.example.strategy.logic

import com.example.strategy.model.*

/**
 * Core game rules engine. Processes player actions and applies game logic.
 *
 * All methods are pure functions: they take a [GameState] and return a new [GameState]
 * with the action applied. No side effects.
 *
 * Action types:
 * - [processBuild] — construct a building in a region
 * - [processRecruit] — recruit a unit (requires Barracks)
 * - [processRecruitUnit] — recruit a specific unit type (Infantry/Cavalry/Siege)
 * - [processAttack] — attack an enemy region
 * - [processMove] — move troops between owned regions
 * - [processDevelop] — increase population (costs gold)
 * - [processResearch] — start researching a technology (multi-turn)
 * - [processUpgradeBuilding] — upgrade a building (max level 3)
 */
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

    private val BUILD_TERRAIN = mapOf(
        BuildingType.FARM to setOf(TerrainType.PLAINS, TerrainType.HILLS),
        BuildingType.LUMBER_MILL to setOf(TerrainType.FOREST),
        BuildingType.QUARRY to setOf(TerrainType.MOUNTAIN, TerrainType.HILLS),
        BuildingType.MINE to setOf(TerrainType.MOUNTAIN),
        BuildingType.MARKET to setOf(TerrainType.PLAINS, TerrainType.HILLS, TerrainType.FOREST),
        BuildingType.BARRACKS to setOf(TerrainType.PLAINS, TerrainType.HILLS, TerrainType.FOREST),
        BuildingType.WALL to setOf(TerrainType.PLAINS, TerrainType.MOUNTAIN, TerrainType.HILLS)
    )

    fun canBuild(player: Player, region: Region, type: BuildingType): Boolean {
        val cost = BUILD_COSTS[type] ?: return false
        val allowedTerrains = BUILD_TERRAIN[type]
        if (allowedTerrains != null && region.terrain !in allowedTerrains) return false
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
        val updatedRegion = region.copy(
            population = region.population + 5,
            units = region.units.add(UnitType.INFANTRY, 5)
        )

        return gameState.copy(
            players = gameState.players.map { if (it.id == player.id) updatedPlayer else it },
            map = gameState.map.replaceRegion(updatedRegion),
            actionsLog = gameState.actionsLog + "${player.name} recruited troops in ${region.name}"
        )
    }

    fun processRecruitUnit(gameState: GameState, action: ActionQueue.GameAction, unitType: UnitType): GameState {
        val player = gameState.players.find { it.id == action.playerId } ?: return gameState
        val region = gameState.map.getRegionById(action.targetRegionId) ?: return gameState

        if (region.ownerId != player.id) return gameState
        val hasBarracks = region.buildings.any { it.type == BuildingType.BARRACKS }
        if (!hasBarracks) return gameState

        val cost = RECRUIT_COSTS[unitType] ?: return gameState
        if (!player.resources.canAfford(cost)) return gameState

        val count = when (unitType) {
            UnitType.INFANTRY -> 5
            UnitType.CAVALRY -> 3
            UnitType.SIEGE -> 1
        }

        val updatedPlayer = player.copy(resources = player.resources - cost)
        val updatedRegion = region.copy(
            population = region.population + count,
            units = region.units.add(unitType, count)
        )

        return gameState.copy(
            players = gameState.players.map { if (it.id == player.id) updatedPlayer else it },
            map = gameState.map.replaceRegion(updatedRegion),
            actionsLog = gameState.actionsLog + "${player.name} recruited ${count} ${unitType.name.lowercase()} in ${region.name}"
        )
    }

    fun processAttack(gameState: GameState, action: ActionQueue.GameAction): GameState {
        val player = gameState.players.find { it.id == action.playerId } ?: return gameState
        val targetRegion = gameState.map.getRegionById(action.targetRegionId) ?: return gameState
        val sourceRegionId = action.param.toIntOrNull() ?: return gameState
        val sourceRegion = gameState.map.getRegionById(sourceRegionId) ?: return gameState

        if (sourceRegion.ownerId != player.id) return gameState
        if (sourceRegion.ownerId == targetRegion.ownerId) return gameState

        var attackStrength = sourceRegion.population + sourceRegion.units.totalAttack()
        if (player.techs.isResearched(TechType.IRON_WORKING)) attackStrength += 2

        var defenseStrength = targetRegion.population + targetRegion.units.totalDefense()
        val wallCount = targetRegion.buildings.count { it.type == BuildingType.WALL }
        var wallBonus = wallCount * 5
        val defender = gameState.players.find { it.id == targetRegion.ownerId }
        if (defender?.techs?.isResearched(TechType.FORTIFICATION) == true) wallBonus = wallCount * 10

        var attackPower = attackStrength
        val siegeCount = sourceRegion.units.units.find { it.type == UnitType.SIEGE }?.count ?: 0
        if (player.techs.isResearched(TechType.SIEGE_ENGINEERING) && wallCount > 0) {
            attackPower = (attackStrength * 1.5).toInt()
        } else if (siegeCount > 0 && wallCount > 0) {
            attackPower = attackStrength + siegeCount * 3
        }

        if (attackPower > defenseStrength + wallBonus) {
            val (keptUnits, movedUnits) = sourceRegion.units.split(half = true)
            val updatedSource = sourceRegion.copy(
                population = sourceRegion.population / 2,
                units = keptUnits
            )
            val updatedTarget = targetRegion.copy(
                ownerId = player.id,
                population = (attackStrength - defenseStrength - wallBonus).coerceAtLeast(1),
                buildings = targetRegion.buildings.filter { it.type != BuildingType.WALL },
                units = movedUnits
            )
            return gameState.copy(
                map = gameState.map.replaceRegion(updatedSource).replaceRegion(updatedTarget),
                actionsLog = gameState.actionsLog + "${player.name} conquered ${targetRegion.name}"
            )
        }

        val updatedSource = sourceRegion.copy(
            population = sourceRegion.population / 3,
            units = sourceRegion.units.remove(UnitType.INFANTRY, sourceRegion.units.units.find { it.type == UnitType.INFANTRY }?.count?.div(3) ?: 0)
        )
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

    fun processResearch(gameState: GameState, action: ActionQueue.GameAction): GameState {
        val player = gameState.players.find { it.id == action.playerId } ?: return gameState
        val techType = try {
            TechType.valueOf(action.param)
        } catch (_: Exception) {
            return gameState
        }

        if (!player.techs.canResearch(techType)) return gameState

        val tech = TECH_TREE.find { it.type == techType } ?: return gameState
        if (!player.resources.canAfford(tech.cost)) return gameState

        val updatedPlayer = player.copy(
            resources = player.resources - tech.cost,
            techs = player.techs.startResearch(techType)
        )

        return gameState.copy(
            players = gameState.players.map { if (it.id == player.id) updatedPlayer else it },
            actionsLog = gameState.actionsLog + "${player.name} started researching ${tech.name} (${tech.turnsRequired} turns)"
        )
    }

    fun processUpgradeBuilding(gameState: GameState, action: ActionQueue.GameAction): GameState {
        val player = gameState.players.find { it.id == action.playerId } ?: return gameState
        val region = gameState.map.getRegionById(action.targetRegionId) ?: return gameState

        if (region.ownerId != player.id) return gameState

        val buildingType = try {
            BuildingType.valueOf(action.param)
        } catch (_: Exception) {
            return gameState
        }

        val building = region.buildings.find { it.type == buildingType } ?: return gameState
        if (building.level >= 3) return gameState

        val cost = Resources(
            food = 15 * building.level,
            wood = 10 * building.level,
            stone = 10 * building.level,
            gold = 15 * building.level
        )
        if (!player.resources.canAfford(cost)) return gameState

        val updatedRegion = region.copy(
            buildings = region.buildings.map {
                if (it.type == buildingType) it.copy(level = it.level + 1) else it
            }
        )

        return gameState.copy(
            map = gameState.map.replaceRegion(updatedRegion),
            players = gameState.players.map {
                if (it.id == player.id) it.copy(resources = it.resources - cost) else it
            },
            actionsLog = gameState.actionsLog + "${player.name} upgraded ${buildingType.name} in ${region.name} to level ${building.level + 1}"
        )
    }
}
