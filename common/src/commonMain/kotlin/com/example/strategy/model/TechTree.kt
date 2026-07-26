package com.example.strategy.model

import kotlinx.serialization.Serializable

@Serializable
enum class TechType {
    AGRICULTURE,
    IRON_WORKING,
    FORTIFICATION,
    MASONRY,
    HORSEBACK,
    SIEGE_ENGINEERING
}

@Serializable
data class Technology(
    val type: TechType,
    val name: String,
    val cost: Resources,
    val description: String,
    val prerequisites: List<TechType> = emptyList(),
    val turnsRequired: Int = 2
)

@Serializable
data class TechState(
    val researched: List<TechType> = emptyList(),
    val researching: TechType? = null,
    val turnsLeft: Int = 0
) {
    fun isResearched(type: TechType): Boolean = type in researched
    fun canResearch(type: TechType): Boolean {
        if (type in researched) return false
        if (researching != null) return false
        val tech = TECH_TREE.find { it.type == type } ?: return false
        return tech.prerequisites.all { it in researched }
    }

    fun tickResearch(): TechState {
        if (researching == null || turnsLeft <= 0) return this
        val newTurnsLeft = turnsLeft - 1
        return if (newTurnsLeft <= 0) {
            copy(researched = researched + researching, researching = null, turnsLeft = 0)
        } else {
            copy(turnsLeft = newTurnsLeft)
        }
    }

    fun startResearch(type: TechType): TechState {
        val tech = TECH_TREE.find { it.type == type } ?: return this
        return copy(researching = type, turnsLeft = tech.turnsRequired)
    }
}

val TECH_TREE = listOf(
    Technology(TechType.AGRICULTURE, "Agriculture", Resources(food = 30, wood = 10, gold = 20), "+50% food production", emptyList()),
    Technology(TechType.IRON_WORKING, "Iron Working", Resources(iron = 15, gold = 25, stone = 10), "+2 attack strength", listOf(TechType.AGRICULTURE)),
    Technology(TechType.FORTIFICATION, "Fortification", Resources(stone = 30, iron = 10, gold = 15), "+5 defense per wall", listOf(TechType.MASONRY)),
    Technology(TechType.MASONRY, "Masonry", Resources(stone = 20, wood = 15, gold = 10), "Build walls", listOf(TechType.AGRICULTURE)),
    Technology(TechType.HORSEBACK, "Horseback Riding", Resources(food = 20, gold = 30, wood = 10), "+2 movement range", listOf(TechType.IRON_WORKING)),
    Technology(TechType.SIEGE_ENGINEERING, "Siege Engineering", Resources(wood = 25, iron = 20, stone = 15, gold = 20), "+50% attack vs walls", listOf(TechType.IRON_WORKING, TechType.MASONRY))
)
