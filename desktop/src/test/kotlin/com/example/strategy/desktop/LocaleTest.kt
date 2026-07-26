package com.example.strategy.desktop

import kotlin.test.*

class LocaleTest {

    @Test
    fun defaultLanguageIsEnglish() {
        Locale.resetToDefault()
        assertEquals(Locale.Lang.EN, Locale.get())
    }

    @Test
    fun switchToRussian() {
        Locale.set(Locale.Lang.RU)
        assertEquals(Locale.Lang.RU, Locale.get())
        assertEquals("СТРАТЕГИЯ", Locale.MAIN_TITLE)
        Locale.resetToDefault()
    }

    @Test
    fun switchToGerman() {
        Locale.set(Locale.Lang.DE)
        assertEquals(Locale.Lang.DE, Locale.get())
        assertEquals("STRATEGIE", Locale.MAIN_TITLE)
        Locale.resetToDefault()
    }

    @Test
    fun allStringsHaveTranslations() {
        for (lang in Locale.Lang.entries) {
            Locale.set(lang)
            assertNotNull(Locale.MAIN_TITLE)
            assertNotNull(Locale.NEW_GAME)
            assertNotNull(Locale.LOAD_GAME)
            assertNotNull(Locale.QUIT)
            assertNotNull(Locale.SETTINGS)
            assertNotNull(Locale.END_TURN)
            assertNotNull(Locale.MENU)
            assertNotNull(Locale.SAVE)
            assertNotNull(Locale.LOAD)
            assertNotNull(Locale.TUTORIAL_HINT)
            assertNotNull(Locale.VICTORY)
            assertNotNull(Locale.DEFEAT)
            assertNotNull(Locale.DIPLO_ALLIANCE)
            assertNotNull(Locale.DIPLO_BREAK)
            assertNotNull(Locale.DIPLO_TRADE)
            assertNotNull(Locale.DIPLO_CANCEL_TRADE)
        }
        Locale.resetToDefault()
    }

    @Test
    fun difficultyNameMapping() {
        Locale.set(Locale.Lang.RU)
        assertEquals("Лёгкий", Locale.difficultyName("EASY"))
        assertEquals("Нормальный", Locale.difficultyName("NORMAL"))
        assertEquals("Сложный", Locale.difficultyName("HARD"))
        assertEquals("UNKNOWN", Locale.difficultyName("UNKNOWN"))
        Locale.resetToDefault()
    }

    @Test
    fun mapSizeNameMapping() {
        Locale.set(Locale.Lang.DE)
        assertEquals("Klein", Locale.mapSizeName("SMALL"))
        assertEquals("Mittel", Locale.mapSizeName("MEDIUM"))
        assertEquals("Groß", Locale.mapSizeName("LARGE"))
        Locale.resetToDefault()
    }
}
