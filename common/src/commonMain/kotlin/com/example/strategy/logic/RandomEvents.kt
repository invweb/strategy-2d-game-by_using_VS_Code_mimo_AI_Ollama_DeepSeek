package com.example.strategy.logic

import com.example.strategy.model.*
import kotlin.random.Random

object RandomEvents {

    enum class EventType {
        BONUS_HARVEST,
        PLAGUE,
        REVOLT,
        GOLD_DISCOVERY,
        TRADE_CARAVAN
    }

    data class Event(
        val type: EventType,
        val regionId: Int,
        val description: String
    )

    fun generateEvent(gameState: GameState): Event? {
        if (Random.nextFloat() > 0.3f) return null

        val player = gameState.currentPlayer() ?: return null
        val ownedRegions = gameState.map.regions.filter { it.ownerId == player.id && it.terrain != TerrainType.WATER }
        if (ownedRegions.isEmpty()) return null

        val region = ownedRegions.random()
        val eventType = EventType.entries.random()

        return when (eventType) {
            EventType.BONUS_HARVEST -> Event(
                eventType, region.id,
                "Bountiful harvest in ${region.name}! +15 Food"
            )
            EventType.PLAGUE -> Event(
                eventType, region.id,
                "Plague strikes ${region.name}! -5 Population"
            )
            EventType.REVOLT -> Event(
                eventType, region.id,
                "Revolt in ${region.name}! -3 Population"
            )
            EventType.GOLD_DISCOVERY -> Event(
                eventType, region.id,
                "Gold discovered in ${region.name}! +20 Gold"
            )
            EventType.TRADE_CARAVAN -> Event(
                eventType, region.id,
                "Trade caravan visits ${region.name}! +10 Gold, +5 Food"
            )
        }
    }

    fun applyEvent(gameState: GameState, event: Event): GameState {
        val region = gameState.map.getRegionById(event.regionId) ?: return gameState
        val player = gameState.currentPlayer() ?: return gameState

        return when (event.type) {
            EventType.BONUS_HARVEST -> gameState.copy(
                players = gameState.players.map {
                    if (it.id == player.id) it.copy(resources = it.resources + Resources(food = 15)) else it
                },
                actionsLog = gameState.actionsLog + event.description
            )
            EventType.PLAGUE -> gameState.copy(
                map = gameState.map.replaceRegion(region.copy(population = (region.population - 5).coerceAtLeast(1))),
                actionsLog = gameState.actionsLog + event.description
            )
            EventType.REVOLT -> gameState.copy(
                map = gameState.map.replaceRegion(region.copy(population = (region.population - 3).coerceAtLeast(1))),
                actionsLog = gameState.actionsLog + event.description
            )
            EventType.GOLD_DISCOVERY -> gameState.copy(
                players = gameState.players.map {
                    if (it.id == player.id) it.copy(resources = it.resources + Resources(gold = 20)) else it
                },
                actionsLog = gameState.actionsLog + event.description
            )
            EventType.TRADE_CARAVAN -> gameState.copy(
                players = gameState.players.map {
                    if (it.id == player.id) it.copy(resources = it.resources + Resources(gold = 10, food = 5)) else it
                },
                actionsLog = gameState.actionsLog + event.description
            )
        }
    }
}
