package com.example.strategy.desktop

import com.example.strategy.model.GameState
import com.example.strategy.platform.GameFactory
import kotlin.test.*

class SaveManagerTest {

    private val testState = GameFactory.createDefaultGameState()

    @Test
    fun saveAndLoad() {
        val name = "test_save_${System.currentTimeMillis()}"
        assertTrue(SaveManager.save(testState, name))
        val loaded = SaveManager.load(name)
        assertNotNull(loaded)
        assertEquals(testState.turn, loaded.turn)
        assertEquals(testState.currentPlayerId, loaded.currentPlayerId)
        SaveManager.deleteSave(name)
    }

    @Test
    fun loadNonExistentReturnsNull() {
        val loaded = SaveManager.load("nonexistent_${System.currentTimeMillis()}")
        assertNull(loaded)
    }

    @Test
    fun deleteSave() {
        val name = "test_delete_${System.currentTimeMillis()}"
        SaveManager.save(testState, name)
        assertTrue(SaveManager.deleteSave(name))
        assertNull(SaveManager.load(name))
    }

    @Test
    fun deleteNonExistentReturnsFalse() {
        assertFalse(SaveManager.deleteSave("nonexistent_${System.currentTimeMillis()}"))
    }

    @Test
    fun listSavesAfterSave() {
        val name = "test_list_${System.currentTimeMillis()}"
        SaveManager.save(testState, name)
        val saves = SaveManager.listSaves()
        assertTrue(saves.contains(name))
        SaveManager.deleteSave(name)
    }

    @Test
    fun saveSanitizesName() {
        val name = "test/special:chars${System.currentTimeMillis()}"
        assertTrue(SaveManager.save(testState, name))
        val saves = SaveManager.listSaves()
        assertTrue(saves.any { it.contains("test_special_chars") })
        SaveManager.listSaves().filter { it.startsWith("test_special_chars") }.forEach { SaveManager.deleteSave(it) }
    }
}
