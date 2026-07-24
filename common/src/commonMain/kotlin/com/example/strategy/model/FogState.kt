package com.example.strategy.model

import kotlinx.serialization.Serializable

@Serializable
data class FogState(
    val exploredBy: Map<Int, Set<Int>> = emptyMap()
) {
    fun isExplored(playerId: Int, regionId: Int): Boolean {
        return regionId in (exploredBy[playerId] ?: emptySet())
    }

    fun explore(playerId: Int, regionId: Int, gameMap: GameMap): FogState {
        val current = exploredBy[playerId] ?: emptySet()
        val region = gameMap.getRegionById(regionId) ?: return this
        val neighbors = gameMap.getNeighbors(region).map { it.id }.toSet()
        val newExplored = current + regionId + neighbors
        return copy(exploredBy = exploredBy + (playerId to newExplored))
    }

    fun exploreAllOwned(playerId: Int, gameMap: GameMap): FogState {
        var state = this
        for (region in gameMap.regions.filter { it.ownerId == playerId }) {
            state = state.explore(playerId, region.id, gameMap)
        }
        return state
    }
}
