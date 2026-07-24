package com.example.strategy.model

import kotlinx.serialization.Serializable

@Serializable
enum class Resource {
    FOOD,
    WOOD,
    STONE,
    IRON,
    GOLD
}

@Serializable
data class Resources(
    val food: Int = 0,
    val wood: Int = 0,
    val stone: Int = 0,
    val iron: Int = 0,
    val gold: Int = 0
) {
    operator fun plus(other: Resources): Resources = Resources(
        food = food + other.food,
        wood = wood + other.wood,
        stone = stone + other.stone,
        iron = other.iron + iron,
        gold = gold + other.gold
    )

    operator fun minus(other: Resources): Resources = Resources(
        food = food - other.food,
        wood = wood - other.wood,
        stone = stone - other.stone,
        iron = iron - other.iron,
        gold = gold - other.gold
    )

    fun canAfford(cost: Resources): Boolean =
        food >= cost.food && wood >= cost.wood &&
                stone >= cost.stone && iron >= cost.iron &&
                gold >= cost.gold
}
