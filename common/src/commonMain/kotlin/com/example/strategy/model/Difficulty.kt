package com.example.strategy.model

import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty(val displayName: String) {
    EASY("Easy"),
    NORMAL("Normal"),
    HARD("Hard")
}

@Serializable
data class DifficultyBonuses(
    val resourceMultiplier: Float = 1f,
    val combatBonus: Int = 0,
    val extraActions: Int = 0
) {
    companion object {
        fun forDifficulty(difficulty: Difficulty): DifficultyBonuses = when (difficulty) {
            Difficulty.EASY -> DifficultyBonuses(resourceMultiplier = 0.8f, combatBonus = -1, extraActions = 0)
            Difficulty.NORMAL -> DifficultyBonuses(resourceMultiplier = 1f, combatBonus = 0, extraActions = 0)
            Difficulty.HARD -> DifficultyBonuses(resourceMultiplier = 1.3f, combatBonus = 2, extraActions = 1)
        }
    }
}
