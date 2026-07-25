package com.example.strategy

import com.example.strategy.model.*
import kotlin.test.*

class ResourcesTest {

    @Test
    fun testPlus() {
        val a = Resources(food = 10, wood = 5)
        val b = Resources(food = 3, wood = 7, gold = 2)
        val result = a + b
        assertEquals(13, result.food)
        assertEquals(12, result.wood)
        assertEquals(2, result.gold)
        assertEquals(0, result.stone)
    }

    @Test
    fun testMinus() {
        val a = Resources(food = 10, wood = 5, gold = 8)
        val b = Resources(food = 3, wood = 2, gold = 1)
        val result = a - b
        assertEquals(7, result.food)
        assertEquals(3, result.wood)
        assertEquals(7, result.gold)
    }

    @Test
    fun testCanAffordExact() {
        val player = Resources(food = 10, gold = 5)
        val cost = Resources(food = 10, gold = 5)
        assertTrue(player.canAfford(cost))
    }

    @Test
    fun testCanAffordMore() {
        val player = Resources(food = 20, gold = 10)
        val cost = Resources(food = 10, gold = 5)
        assertTrue(player.canAfford(cost))
    }

    @Test
    fun testCanAffordInsufficient() {
        val player = Resources(food = 5, gold = 3)
        val cost = Resources(food = 10, gold = 5)
        assertFalse(player.canAfford(cost))
    }

    @Test
    fun testCanAffordZeroCost() {
        val player = Resources(food = 0, gold = 0)
        val cost = Resources()
        assertTrue(player.canAfford(cost))
    }

    @Test
    fun testDefaults() {
        val r = Resources()
        assertEquals(0, r.food)
        assertEquals(0, r.wood)
        assertEquals(0, r.stone)
        assertEquals(0, r.iron)
        assertEquals(0, r.gold)
    }
}
