package com.example.strategy.model

import kotlinx.serialization.Serializable

@Serializable
enum class UnitType {
    INFANTRY,
    CAVALRY,
    SIEGE
}

@Serializable
data class Unit(
    val type: UnitType,
    val count: Int = 0
) {
    val attackPower: Int get() = when (type) {
        UnitType.INFANTRY -> 1
        UnitType.CAVALRY -> 2
        UnitType.SIEGE -> 3
    }
    val defensePower: Int get() = when (type) {
        UnitType.INFANTRY -> 1
        UnitType.CAVALRY -> 1
        UnitType.SIEGE -> 0
    }
    val hp: Int get() = when (type) {
        UnitType.INFANTRY -> 10
        UnitType.CAVALRY -> 15
        UnitType.SIEGE -> 5
    }
}

@Serializable
data class UnitStack(
    val units: List<Unit> = emptyList()
) {
    val totalPopulation: Int get() = units.sumOf { it.count }

    fun add(type: UnitType, count: Int): UnitStack {
        val existing = units.find { it.type == type }
        val updated = if (existing != null) {
            units.map { if (it.type == type) it.copy(count = it.count + count) else it }
        } else {
            units + Unit(type, count)
        }
        return copy(units = updated.filter { it.count > 0 })
    }

    fun remove(type: UnitType, count: Int): UnitStack {
        val updated = units.mapNotNull {
            if (it.type == type) {
                val newCount = it.count - count
                if (newCount > 0) it.copy(count = newCount) else null
            } else it
        }
        return copy(units = updated)
    }

    fun totalAttack(): Int = units.sumOf { it.count * it.attackPower }
    fun totalDefense(): Int = units.sumOf { it.count * it.defensePower }

    fun split(half: Boolean): Pair<UnitStack, UnitStack> {
        val a = mutableListOf<Unit>()
        val b = mutableListOf<Unit>()
        for (u in units) {
            val halfCount = u.count / 2
            val remainder = u.count - halfCount
            if (half) {
                a.add(u.copy(count = halfCount))
                b.add(u.copy(count = remainder))
            } else {
                a.add(u.copy(count = halfCount))
                b.add(u.copy(count = remainder))
            }
        }
        return UnitStack(a.filter { it.count > 0 }) to UnitStack(b.filter { it.count > 0 })
    }
}

val RECRUIT_COSTS = mapOf(
    UnitType.INFANTRY to Resources(food = 5, gold = 3),
    UnitType.CAVALRY to Resources(food = 10, gold = 8, wood = 5),
    UnitType.SIEGE to Resources(wood = 15, iron = 10, gold = 10)
)
