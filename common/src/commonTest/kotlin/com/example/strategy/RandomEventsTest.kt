package com.example.strategy

import com.example.strategy.logic.RandomEvents
import com.example.strategy.model.*
import com.example.strategy.platform.GameFactory
import kotlin.test.*

class RandomEventsTest {

    private val testState = GameFactory.createDefaultGameState()

    @Test
    fun generateEventReturnsNullOrEvent() {
        val event = RandomEvents.generateEvent(testState)
        if (event != null) {
            assertNotNull(event.type)
            assertTrue(event.regionId >= 0)
            assertTrue(event.description.isNotEmpty())
        }
    }

    @Test
    fun applyBonusHarvestAddsFood() {
        val player = testState.currentPlayer() ?: return
        val region = testState.map.regions.first { it.ownerId == player.id && it.terrain != TerrainType.WATER }
        val event = RandomEvents.Event(RandomEvents.EventType.BONUS_HARVEST, region.id, "test")
        val result = RandomEvents.applyEvent(testState, event)
        val updatedPlayer = result.players.first { it.id == player.id }
        assertTrue(updatedPlayer.resources.food > player.resources.food)
    }

    @Test
    fun applyPlagueReducesPopulation() {
        val player = testState.currentPlayer() ?: return
        val region = testState.map.regions.first { it.ownerId == player.id && it.terrain != TerrainType.WATER }
        val originalPop = region.population
        val event = RandomEvents.Event(RandomEvents.EventType.PLAGUE, region.id, "test")
        val result = RandomEvents.applyEvent(testState, event)
        val updatedRegion = result.map.getRegionById(region.id)!!
        assertTrue(updatedRegion.population < originalPop)
        assertTrue(updatedRegion.population >= 1)
    }

    @Test
    fun applyRevoltReducesPopulation() {
        val player = testState.currentPlayer() ?: return
        val region = testState.map.regions.first { it.ownerId == player.id && it.terrain != TerrainType.WATER }
        val originalPop = region.population
        val event = RandomEvents.Event(RandomEvents.EventType.REVOLT, region.id, "test")
        val result = RandomEvents.applyEvent(testState, event)
        val updatedRegion = result.map.getRegionById(region.id)!!
        assertTrue(updatedRegion.population < originalPop)
    }

    @Test
    fun applyGoldDiscoveryAddsGold() {
        val player = testState.currentPlayer() ?: return
        val region = testState.map.regions.first { it.ownerId == player.id && it.terrain != TerrainType.WATER }
        val event = RandomEvents.Event(RandomEvents.EventType.GOLD_DISCOVERY, region.id, "test")
        val result = RandomEvents.applyEvent(testState, event)
        val updatedPlayer = result.players.first { it.id == player.id }
        assertTrue(updatedPlayer.resources.gold > player.resources.gold)
    }

    @Test
    fun applyTradeCaravanAddsGoldAndFood() {
        val player = testState.currentPlayer() ?: return
        val region = testState.map.regions.first { it.ownerId == player.id && it.terrain != TerrainType.WATER }
        val event = RandomEvents.Event(RandomEvents.EventType.TRADE_CARAVAN, region.id, "test")
        val result = RandomEvents.applyEvent(testState, event)
        val updatedPlayer = result.players.first { it.id == player.id }
        assertTrue(updatedPlayer.resources.gold > player.resources.gold)
        assertTrue(updatedPlayer.resources.food > player.resources.food)
    }

    @Test
    fun applyEventToInvalidRegionReturnsUnchanged() {
        val event = RandomEvents.Event(RandomEvents.EventType.BONUS_HARVEST, -999, "test")
        val result = RandomEvents.applyEvent(testState, event)
        assertEquals(testState.turn, result.turn)
    }

    @Test
    fun applyEventLogsDescription() {
        val player = testState.currentPlayer() ?: return
        val region = testState.map.regions.first { it.ownerId == player.id && it.terrain != TerrainType.WATER }
        val event = RandomEvents.Event(RandomEvents.EventType.BONUS_HARVEST, region.id, "Test event description")
        val result = RandomEvents.applyEvent(testState, event)
        assertTrue(result.actionsLog.any { it.contains("Test event description") })
    }
}
