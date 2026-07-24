package com.example.strategy.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: Int,
    val name: String,
    val color: String,
    val resources: Resources = Resources(food = 50, wood = 30, stone = 20, gold = 100),
    val isHuman: Boolean = false,
    val techs: TechState = TechState()
)
