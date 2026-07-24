package com.example.strategy.model

import kotlinx.serialization.Serializable

@Serializable
enum class DiplomacyStatus {
    NEUTRAL,
    ALLIED,
    TRADE_PARTNERS,
    ENEMY
}

@Serializable
data class DiplomacyRelation(
    val playerId: Int,
    val targetId: Int,
    val status: DiplomacyStatus = DiplomacyStatus.NEUTRAL,
    val tradeActive: Boolean = false,
    val turnsAllied: Int = 0
) {
    companion object {
        fun key(a: Int, b: Int): String = if (a < b) "$a-$b" else "$b-$a"
    }
}

@Serializable
data class DiplomacyState(
    val relations: List<DiplomacyRelation> = emptyList()
) {
    fun getRelation(a: Int, b: Int): DiplomacyRelation {
        val key = DiplomacyRelation.key(a, b)
        return relations.find {
            DiplomacyRelation.key(it.playerId, it.targetId) == key
        } ?: DiplomacyRelation(a, b)
    }

    fun getAlliesOf(playerId: Int): List<Int> =
        relations.filter {
            (it.playerId == playerId || it.targetId == playerId) &&
                    it.status == DiplomacyStatus.ALLIED
        }.map { if (it.playerId == playerId) it.targetId else it.playerId }

    fun isAllied(a: Int, b: Int): Boolean =
        getRelation(a, b).status == DiplomacyStatus.ALLIED

    fun isTradePartner(a: Int, b: Int): Boolean =
        getRelation(a, b).tradeActive

    fun updateRelation(relation: DiplomacyRelation): DiplomacyState {
        val key = DiplomacyRelation.key(relation.playerId, relation.targetId)
        val updated = relations.filter {
            DiplomacyRelation.key(it.playerId, it.targetId) != key
        } + relation
        return copy(relations = updated)
    }
}
