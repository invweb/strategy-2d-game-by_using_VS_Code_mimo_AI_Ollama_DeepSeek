package com.example.strategy.desktop

import java.io.File

object Locale {
    enum class Lang(val displayName: String) { EN("English"), RU("Русский"), DE("Deutsch") }

    private var current: Lang = Lang.EN

    fun set(lang: Lang) {
        current = lang
        save()
    }

    fun get(): Lang = current

    private val prefsFile: File
        get() = File(System.getProperty("user.home"), ".strategy_prefs")

    fun load() {
        try {
            val f = prefsFile
            if (f.exists()) {
                val langName = f.readText().trim()
                current = Lang.entries.find { it.name == langName } ?: Lang.EN
            }
        } catch (_: Exception) {}
    }

    private fun save() {
        try {
            val f = prefsFile
            f.writeText(current.name)
        } catch (_: Exception) {}
    }

    // ===== UI Strings =====

    val MAIN_TITLE get() = when(current) {
        Lang.RU -> "СТРАТЕГИЯ"
        Lang.DE -> "STRATEGIE"
        Lang.EN -> "STRATEGY"
    }

    val SUBTITLE get() = when(current) {
        Lang.RU -> "Пошаговая 2D-стратегия"
        Lang.DE -> "Rundenbasierte 2D-Strategie"
        Lang.EN -> "2D Turn-Based Strategy"
    }

    val NEW_GAME get() = when(current) {
        Lang.RU -> "НОВАЯ ИГРА"
        Lang.DE -> "NEUES SPIEL"
        Lang.EN -> "NEW GAME"
    }

    val LOAD_GAME get() = when(current) {
        Lang.RU -> "ЗАГРУЗИТЬ ИГРУ"
        Lang.DE -> "SPIEL LADEN"
        Lang.EN -> "LOAD GAME"
    }

    val QUIT get() = when(current) {
        Lang.RU -> "ВЫХОД"
        Lang.DE -> "BEENDEN"
        Lang.EN -> "QUIT"
    }

    val SETTINGS get() = when(current) {
        Lang.RU -> "НАСТРОЙКИ"
        Lang.DE -> "EINSTELLUNGEN"
        Lang.EN -> "SETTINGS"
    }

    val MAP_SIZE get() = when(current) {
        Lang.RU -> "Размер карты:"
        Lang.DE -> "Kartengröße:"
        Lang.EN -> "Map Size:"
    }

    val TERRAIN get() = when(current) {
        Lang.RU -> "Рельеф:"
        Lang.DE -> "Gelände:"
        Lang.EN -> "Terrain:"
    }

    val DIFFICULTY get() = when(current) {
        Lang.RU -> "Сложность:"
        Lang.DE -> "Schwierigkeit:"
        Lang.EN -> "Difficulty:"
    }

    val LANGUAGE get() = when(current) {
        Lang.RU -> "Язык:"
        Lang.DE -> "Sprache:"
        Lang.EN -> "Language:"
    }

    val FOOTER get() = when(current) {
        Lang.RU -> "AI на Ollama  |  Kotlin + libGDX"
        Lang.DE -> "KI mit Ollama  |  Kotlin + libGDX"
        Lang.EN -> "Ollama AI Powered  |  Kotlin + libGDX"
    }

    val CLOSE get() = when(current) {
        Lang.RU -> "ЗАКРЫТЬ"
        Lang.DE -> "SCHLIEßEN"
        Lang.EN -> "CLOSE"
    }

    val OK get() = when(current) {
        Lang.RU -> "ОК"
        Lang.DE -> "OK"
        Lang.EN -> "OK"
    }

    val CANCEL get() = when(current) {
        Lang.RU -> "ОТМЕНА"
        Lang.DE -> "ABBRECHEN"
        Lang.EN -> "CANCEL"
    }

    val SAVE get() = when(current) {
        Lang.RU -> "СОХРАНИТЬ"
        Lang.DE -> "SPEICHERN"
        Lang.EN -> "SAVE"
    }

    val LOAD get() = when(current) {
        Lang.RU -> "ЗАГРУЗИТЬ"
        Lang.DE -> "LADEN"
        Lang.EN -> "LOAD"
    }

    val DELETE get() = when(current) {
        Lang.RU -> "УДАЛИТЬ"
        Lang.DE -> "LÖSCHEN"
        Lang.EN -> "DELETE"
    }

    val NO_SAVES get() = when(current) {
        Lang.RU -> "Сохранения не найдены."
        Lang.DE -> "Keine Spielstände gefunden."
        Lang.EN -> "No saved games found."
    }

    val SELECT_SAVE get() = when(current) {
        Lang.RU -> "Выберите сохранение:"
        Lang.DE -> "Spielstand wählen:"
        Lang.EN -> "Select a save:"
    }

    val CONFIRM_DELETE get() = when(current) {
        Lang.RU -> "Удалить"
        Lang.DE -> "Löschen"
        Lang.EN -> "Delete"
    }

    val QUESTIONMARK get() = when(current) {
        Lang.RU -> "?"
        Lang.DE -> "?"
        Lang.EN -> "?"
    }

    // ===== GameScreen =====

    val CLICK_REGION get() = when(current) {
        Lang.RU -> "Нажмите на область для выбора"
        Lang.DE -> "Klicken Sie auf eine Region"
        Lang.EN -> "Click a region to select"
    }

    val UNKNOWN_TERRITORY get() = when(current) {
        Lang.RU -> "Неизвестная территория — исследуйте"
        Lang.DE -> "Unbekanntes Gebiet — erforschen"
        Lang.EN -> "Unknown territory — explore to reveal"
    }

    val YOURS get() = when(current) {
        Lang.RU -> "Ваша"
        Lang.DE -> "Euer"
        Lang.EN -> "Yours"
    }

    val ENEMY get() = when(current) {
        Lang.RU -> "Вражеская"
        Lang.DE -> "Feindlich"
        Lang.EN -> "Enemy"
    }

    val NEUTRAL get() = when(current) {
        Lang.RU -> "Нейтральная"
        Lang.DE -> "Neutral"
        Lang.EN -> "Neutral"
    }

    val NO_BUILDINGS get() = when(current) {
        Lang.RU -> "Нет зданий"
        Lang.DE -> "Keine Gebäude"
        Lang.EN -> "No buildings"
    }

    val NO_UNITS get() = when(current) {
        Lang.RU -> "Нет юнитов"
        Lang.DE -> "Keine Einheiten"
        Lang.EN -> "No units"
    }

    val SELECTED get() = when(current) {
        Lang.RU -> "Выбрано"
        Lang.DE -> "Ausgewählt"
        Lang.EN -> "Selected"
    }

    val TOTAL_POP get() = when(current) {
        Lang.RU -> "Население"
        Lang.DE -> "Bevölkerung"
        Lang.EN -> "Total Pop"
    }

    val ATTACK get() = when(current) {
        Lang.RU -> "Атака"
        Lang.DE -> "Angriff"
        Lang.EN -> "Attack"
    }

    val DEFENSE get() = when(current) {
        Lang.RU -> "Защита"
        Lang.DE -> "Verteidigung"
        Lang.EN -> "Defense"
    }

    val POPULATION get() = when(current) {
        Lang.RU -> "Население"
        Lang.DE -> "Bevölkerung"
        Lang.EN -> "Population"
    }

    val BUILDINGS get() = when(current) {
        Lang.RU -> "Здания"
        Lang.DE -> "Gebäude"
        Lang.EN -> "Buildings"
    }

    val UNITS get() = when(current) {
        Lang.RU -> "Юниты"
        Lang.DE -> "Einheiten"
        Lang.EN -> "Units"
    }

    val TURN get() = when(current) {
        Lang.RU -> "Ход"
        Lang.DE -> "Runde"
        Lang.EN -> "Turn"
    }

    val TERRITORIES get() = when(current) {
        Lang.RU -> "Территории"
        Lang.DE -> "Gebiete"
        Lang.EN -> "Territories"
    }

    val INCOME get() = when(current) {
        Lang.RU -> "Доход"
        Lang.DE -> "Einkommen"
        Lang.EN -> "Income"
    }

    val UPKEEP get() = when(current) {
        Lang.RU -> "Содержание"
        Lang.DE -> "Unterhalt"
        Lang.EN -> "Upkeep"
    }

    val ACTION_USED get() = when(current) {
        Lang.RU -> "ДЕЙСТВИЕ ИСПОЛЬЗОВАНО — нажмите КОНЕЦ ХОДА"
        Lang.DE -> "AKTION GENUTZT — ENDE ZIEHEN drücken"
        Lang.EN -> "ACTION USED — click END TURN"
    }

    val YOUR_TURN get() = when(current) {
        Lang.RU -> "ВАШ ХОД — выберите одно действие"
        Lang.DE -> "IHRE RUNDE — wählen Sie eine Aktion"
        Lang.EN -> "YOUR TURN — choose one action"
    }

    val WAITING get() = when(current) {
        Lang.RU -> "Ожидание..."
        Lang.DE -> "Warten..."
        Lang.EN -> "Waiting..."
    }

    val END_TURN get() = when(current) {
        Lang.RU -> "КОНЕЦ ХОДА"
        Lang.DE -> "ENDE ZUG"
        Lang.EN -> "END TURN"
    }

    val MENU get() = when(current) {
        Lang.RU -> "МЕНЮ"
        Lang.DE -> "MENÜ"
        Lang.EN -> "MENU"
    }

    val STATS get() = when(current) {
        Lang.RU -> "СТАТИСТИКА"
        Lang.DE -> "STATISTIK"
        Lang.EN -> "STATS"
    }

    val DIPLOMACY get() = when(current) {
        Lang.RU -> "Дипломатия:"
        Lang.DE -> "Diplomatie:"
        Lang.EN -> "Diplomacy:"
    }

    val TECHS get() = when(current) {
        Lang.RU -> "Технологии"
        Lang.DE -> "Technologien"
        Lang.EN -> "Technologies"
    }

    // MapSize
    val SMALL get() = when(current) {
        Lang.RU -> "Маленький"; Lang.DE -> "Klein"; Lang.EN -> "Small"
    }
    val MEDIUM get() = when(current) {
        Lang.RU -> "Средний"; Lang.DE -> "Mittel"; Lang.EN -> "Medium"
    }
    val LARGE get() = when(current) {
        Lang.RU -> "Большой"; Lang.DE -> "Groß"; Lang.EN -> "Large"
    }

    // Terrain
    val BALANCED get() = when(current) {
        Lang.RU -> "Сбалансированный"; Lang.DE -> "Ausgeglichen"; Lang.EN -> "Balanced"
    }
    val CONTINENTAL get() = when(current) {
        Lang.RU -> "Континентальный"; Lang.DE -> "Kontinental"; Lang.EN -> "Continental"
    }
    val ISLANDS get() = when(current) {
        Lang.RU -> "Острова"; Lang.DE -> "Inseln"; Lang.EN -> "Islands"
    }

    // Difficulty
    val EASY get() = when(current) {
        Lang.RU -> "Лёгкий"; Lang.DE -> "Leicht"; Lang.EN -> "Easy"
    }
    val NORMAL get() = when(current) {
        Lang.RU -> "Нормальный"; Lang.DE -> "Normal"; Lang.EN -> "Normal"
    }
    val HARD get() = when(current) {
        Lang.RU -> "Сложный"; Lang.DE -> "Schwer"; Lang.EN -> "Hard"
    }

    fun difficultyName(d: String): String = when(d) {
        "EASY" -> EASY; "NORMAL" -> NORMAL; "HARD" -> HARD; else -> d
    }

    fun mapSizeName(s: String): String = when(s) {
        "SMALL" -> SMALL; "MEDIUM" -> MEDIUM; "LARGE" -> LARGE; else -> s
    }

    fun terrainName(t: String): String = when(t) {
        "BALANCED" -> BALANCED; "CONTINENTAL" -> CONTINENTAL; "ISLANDS" -> ISLANDS; else -> t
    }
}
