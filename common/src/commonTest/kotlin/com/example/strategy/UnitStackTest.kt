package com.example.strategy

import com.example.strategy.model.*
import kotlin.test.*

class UnitStackTest {

    @Test
    fun testAddNewUnitType() {
        val stack = UnitStack()
        val updated = stack.add(UnitType.INFANTRY, 5)
        assertEquals(1, updated.units.size)
        assertEquals(UnitType.INFANTRY, updated.units[0].type)
        assertEquals(5, updated.units[0].count)
    }

    @Test
    fun testAddExistingUnitType() {
        val stack = UnitStack(units = listOf(Unit(UnitType.INFANTRY, 3)))
        val updated = stack.add(UnitType.INFANTRY, 5)
        assertEquals(1, updated.units.size)
        assertEquals(8, updated.units[0].count)
    }

    @Test
    fun testAddMultipleTypes() {
        val stack = UnitStack()
            .add(UnitType.INFANTRY, 5)
            .add(UnitType.CAVALRY, 3)
        assertEquals(2, stack.units.size)
    }

    @Test
    fun testRemoveUnit() {
        val stack = UnitStack(units = listOf(Unit(UnitType.INFANTRY, 10)))
        val updated = stack.remove(UnitType.INFANTRY, 3)
        assertEquals(1, updated.units.size)
        assertEquals(7, updated.units[0].count)
    }

    @Test
    fun testRemoveAllUnits() {
        val stack = UnitStack(units = listOf(Unit(UnitType.INFANTRY, 5)))
        val updated = stack.remove(UnitType.INFANTRY, 5)
        assertTrue(updated.units.isEmpty())
    }

    @Test
    fun testRemoveMoreThanAvailable() {
        val stack = UnitStack(units = listOf(Unit(UnitType.INFANTRY, 3)))
        val updated = stack.remove(UnitType.INFANTRY, 5)
        assertTrue(updated.units.isEmpty())
    }

    @Test
    fun testTotalAttack() {
        val stack = UnitStack(
            units = listOf(
                Unit(UnitType.INFANTRY, 5),  // 5 * 1 = 5
                Unit(UnitType.CAVALRY, 3),    // 3 * 2 = 6
                Unit(UnitType.SIEGE, 2)       // 2 * 3 = 6
            )
        )
        assertEquals(17, stack.totalAttack())
    }

    @Test
    fun testTotalDefense() {
        val stack = UnitStack(
            units = listOf(
                Unit(UnitType.INFANTRY, 5),  // 5 * 1 = 5
                Unit(UnitType.CAVALRY, 3),    // 3 * 1 = 3
                Unit(UnitType.SIEGE, 2)       // 2 * 0 = 0
            )
        )
        assertEquals(8, stack.totalDefense())
    }

    @Test
    fun testTotalPopulation() {
        val stack = UnitStack(
            units = listOf(
                Unit(UnitType.INFANTRY, 5),
                Unit(UnitType.CAVALRY, 3),
                Unit(UnitType.SIEGE, 2)
            )
        )
        assertEquals(10, stack.totalPopulation)
    }

    @Test
    fun testSplitEvenCount() {
        val stack = UnitStack(units = listOf(Unit(UnitType.INFANTRY, 10)))
        val (a, b) = stack.split(half = true)
        assertEquals(5, a.units[0].count)
        assertEquals(5, b.units[0].count)
    }

    @Test
    fun testSplitOddCount() {
        val stack = UnitStack(units = listOf(Unit(UnitType.INFANTRY, 7)))
        val (a, b) = stack.split(half = true)
        assertEquals(3, a.units[0].count)
        assertEquals(4, b.units[0].count)
    }

    @Test
    fun testSplitMultipleTypes() {
        val stack = UnitStack(
            units = listOf(
                Unit(UnitType.INFANTRY, 10),
                Unit(UnitType.CAVALRY, 6)
            )
        )
        val (a, b) = stack.split(half = true)
        assertEquals(2, a.units.size)
        assertEquals(2, b.units.size)
        assertEquals(5, a.units.find { it.type == UnitType.INFANTRY }?.count)
        assertEquals(3, a.units.find { it.type == UnitType.CAVALRY }?.count)
    }
}
