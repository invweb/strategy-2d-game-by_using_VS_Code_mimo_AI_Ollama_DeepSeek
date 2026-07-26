package com.example.strategy

import com.example.strategy.logic.DiplomacyManager
import com.example.strategy.model.*
import com.example.strategy.platform.GameFactory
import kotlin.test.*

class DiplomacyManagerTest {

    private val testState = GameFactory.createDefaultGameState()

    @Test
    fun proposeAllianceCreatesAlliance() {
        val result = DiplomacyManager.proposeAlliance(testState, 0, 1)
        val relation = result.diplomacy.getRelation(0, 1)
        assertEquals(DiplomacyStatus.ALLIED, relation.status)
    }

    @Test
    fun proposeAllianceWithSelfDoesNothing() {
        val result = DiplomacyManager.proposeAlliance(testState, 0, 0)
        val relation = result.diplomacy.getRelation(0, 1)
        assertEquals(DiplomacyStatus.NEUTRAL, relation.status)
    }

    @Test
    fun proposeAllianceWithNonexistentPlayerDoesNothing() {
        val result = DiplomacyManager.proposeAlliance(testState, 0, 999)
        assertEquals(testState.turn, result.turn)
    }

    @Test
    fun proposeAllianceAlreadyAlliedDoesNothing() {
        val allied = DiplomacyManager.proposeAlliance(testState, 0, 1)
        val result = DiplomacyManager.proposeAlliance(allied, 0, 1)
        val relation = result.diplomacy.getRelation(0, 1)
        assertEquals(DiplomacyStatus.ALLIED, relation.status)
    }

    @Test
    fun breakAllianceSetsNeutral() {
        val allied = DiplomacyManager.proposeAlliance(testState, 0, 1)
        val result = DiplomacyManager.breakAlliance(allied, 0, 1)
        val relation = result.diplomacy.getRelation(0, 1)
        assertEquals(DiplomacyStatus.NEUTRAL, relation.status)
        assertFalse(relation.tradeActive)
    }

    @Test
    fun breakAllianceWhenNeutralDoesNothing() {
        val result = DiplomacyManager.breakAlliance(testState, 0, 1)
        val relation = result.diplomacy.getRelation(0, 1)
        assertEquals(DiplomacyStatus.NEUTRAL, relation.status)
    }

    @Test
    fun proposeTradeSetsTradeActive() {
        val result = DiplomacyManager.proposeTrade(testState, 0, 1)
        val relation = result.diplomacy.getRelation(0, 1)
        assertTrue(relation.tradeActive)
    }

    @Test
    fun proposeTradeWithSelfDoesNothing() {
        val result = DiplomacyManager.proposeTrade(testState, 0, 0)
        assertEquals(testState.turn, result.turn)
    }

    @Test
    fun cancelTradeSetsTradeInactive() {
        val withTrade = DiplomacyManager.proposeTrade(testState, 0, 1)
        val result = DiplomacyManager.cancelTrade(withTrade, 0, 1)
        val relation = result.diplomacy.getRelation(0, 1)
        assertFalse(relation.tradeActive)
    }

    @Test
    fun cancelTradeWhenNoTradeDoesNothing() {
        val result = DiplomacyManager.cancelTrade(testState, 0, 1)
        assertEquals(testState.turn, result.turn)
    }

    @Test
    fun incrementAllianceTurnsIncreasesAlliedTurns() {
        val allied = DiplomacyManager.proposeAlliance(testState, 0, 1)
        val result = DiplomacyManager.incrementAllianceTurns(allied)
        val relation = result.diplomacy.getRelation(0, 1)
        assertEquals(1, relation.turnsAllied)
    }

    @Test
    fun incrementAllianceTurnsDoesNotAffectNeutral() {
        val result = DiplomacyManager.incrementAllianceTurns(testState)
        val relation = result.diplomacy.getRelation(0, 1)
        assertEquals(0, relation.turnsAllied)
    }

    @Test
    fun allianceAndTradeWorkTogether() {
        var state = DiplomacyManager.proposeAlliance(testState, 0, 1)
        state = DiplomacyManager.proposeTrade(state, 0, 1)
        val relation = state.diplomacy.getRelation(0, 1)
        assertEquals(DiplomacyStatus.ALLIED, relation.status)
        assertTrue(relation.tradeActive)
    }

    @Test
    fun breakingAllianceCancelsTrade() {
        var state = DiplomacyManager.proposeAlliance(testState, 0, 1)
        state = DiplomacyManager.proposeTrade(state, 0, 1)
        state = DiplomacyManager.breakAlliance(state, 0, 1)
        val relation = state.diplomacy.getRelation(0, 1)
        assertFalse(relation.tradeActive)
    }
}
