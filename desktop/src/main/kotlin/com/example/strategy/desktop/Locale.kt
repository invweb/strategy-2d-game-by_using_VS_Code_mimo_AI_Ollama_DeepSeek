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

    fun resetToDefault() {
        current = Lang.EN
        save()
    }

    private val prefsFile: File
        get() = File(System.getProperty("user.home"), ".strategy_prefs")

    fun load() {
        try {
            val f = prefsFile
            if (f.exists()) {
                val langName = f.readText().trim()
                current = Lang.entries.find { it.name == langName } ?: Lang.EN
            } else {
                current = Lang.EN
            }
        } catch (_: Exception) {
            current = Lang.EN
        }
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

    val SAVE_QUESTION get() = when(current) {
        Lang.RU -> "Сохранить текущую игру?"
        Lang.DE -> "Aktuelles Spiel speichern?"
        Lang.EN -> "Save current game?"
    }

    val NO_SAVE get() = when(current) {
        Lang.RU -> "НЕТ"
        Lang.DE -> "NEIN"
        Lang.EN -> "NO"
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

    val ZOOM_HINT get() = when(current) {
        Lang.RU -> "Также можно скроллом мыши"
        Lang.DE -> "Auch mit Mausrad möglich"
        Lang.EN -> "Also works with scroll wheel"
    }

    val AI_BACKEND get() = when(current) {
        Lang.RU -> "AI движок:"
        Lang.DE -> "KI-Backend:"
        Lang.EN -> "AI Backend:"
    }

    val AI_OLLAMA_URL get() = when(current) {
        Lang.RU -> "URL Ollama:"
        Lang.DE -> "Ollama URL:"
        Lang.EN -> "Ollama URL:"
    }

    val AI_OLLAMA_MODEL get() = when(current) {
        Lang.RU -> "Модель Ollama:"
        Lang.DE -> "Ollama Modell:"
        Lang.EN -> "Ollama Model:"
    }

    val AI_LMSTUDIO_URL get() = when(current) {
        Lang.RU -> "URL LM Studio:"
        Lang.DE -> "LM Studio URL:"
        Lang.EN -> "LM Studio URL:"
    }

    val AI_LMSTUDIO_MODEL get() = when(current) {
        Lang.RU -> "Модель LM Studio:"
        Lang.DE -> "LM Studio Modell:"
        Lang.EN -> "LM Studio Model:"
    }

    val TUTORIAL_HINT get() = when(current) {
        Lang.RU -> "Выберите регион кликом. Постройте здание. Вербуйте юнитов. Атакуйте врага!"
        Lang.DE -> "Klicken Sie auf eine Region. Bauen Sie ein Gebäude. Rekrutieren Sie Einheiten. Greifen Sie den Feind an!"
        Lang.EN -> "Click a region. Build a structure. Recruit units. Attack the enemy!"
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

    // GameScreen UI
    val BUILD_FARM get() = when(current) {
        Lang.RU -> "Ферма (10Е 5Д)"; Lang.DE -> "Farm (10N 5H)"; Lang.EN -> "Farm (10F 5W)"
    }
    val BUILD_LUMBER get() = when(current) {
        Lang.RU -> "Лесопилка (15Д)"; Lang.DE -> "Sägewerk (15H)"; Lang.EN -> "Lumber Mill (15W)"
    }
    val BUILD_BARRACKS_COST get() = when(current) {
        Lang.RU -> "Казарма (15Д 10К 10З)"; Lang.DE -> "Kaserne (15H 10S 10G)"; Lang.EN -> "Barracks (15W 10S 10G)"
    }
    val BUILD_MINE_COST get() = when(current) {
        Lang.RU -> "Шахта (5Д 15К 5Ж)"; Lang.DE -> "Mine (5H 15S 5E)"; Lang.EN -> "Mine (5W 15S 5I)"
    }
    val RECRUIT_COST get() = when(current) {
        Lang.RU -> "Вербовка (10Е 5З)"; Lang.DE -> "Rekrutieren (10N 5G)"; Lang.EN -> "Recruit (10F 5G)"
    }
    val RECRUIT_INFANTRY_COST get() = when(current) {
        Lang.RU -> "Пехота (5Е 3З)"; Lang.DE -> "Infanterie (5N 3G)"; Lang.EN -> "Infantry (5F 3G)"
    }
    val RECRUIT_CAVALRY_COST get() = when(current) {
        Lang.RU -> "Кавалерия (10Е 8З 5Д)"; Lang.DE -> "Kavallerie (10N 8G 5H)"; Lang.EN -> "Cavalry (10F 8G 5W)"
    }
    val RECRUIT_SIEGE_COST get() = when(current) {
        Lang.RU -> "Осада (15Д 10Ж 10З)"; Lang.DE -> "Belagerung (15H 10E 10G)"; Lang.EN -> "Siege (15W 10I 10G)"
    }
    val DEVELOP_COST get() = when(current) {
        Lang.RU -> "Развитие (10З)"; Lang.DE -> "Entwickeln (10G)"; Lang.EN -> "Develop (10G)"
    }
    val ATTACK_MODE get() = when(current) {
        Lang.RU -> "РЕЖИМ АТАКИ: нажмите на вражеский регион"; Lang.DE -> "ANGRIFFS-MODUS: Klicken Sie auf feindliche Region"; Lang.EN -> "ATTACK MODE: Click enemy region to attack from"
    }
    val MOVE_MODE get() = when(current) {
        Lang.RU -> "РЕЖИМ ПЕРЕМЕЩЕНИЯ: нажмите на ваш регион"; Lang.DE -> "BEWEGUNGS-MODUS: Klicken Sie auf Ihre Region"; Lang.EN -> "MOVE MODE: Click your region to move troops from"
    }
    val NO_HISTORY get() = when(current) {
        Lang.RU -> "Нет истории"; Lang.DE -> "Keine Historie"; Lang.EN -> "No history yet"
    }
    val STATS_HEADER get() = when(current) {
        Lang.RU -> "--- Статистика ---"; Lang.DE -> "--- Statistik ---"; Lang.EN -> "--- Stats ---"
    }
    val SAVED get() = when(current) {
        Lang.RU -> "Сохранено: "; Lang.DE -> "Gespeichert: "; Lang.EN -> "Saved: "
    }
    val SAVE_FAILED get() = when(current) {
        Lang.RU -> "Ошибка сохранения!"; Lang.DE -> "Speichern fehlgeschlagen!"; Lang.EN -> "Save FAILED!"
    }
    val LOADED get() = when(current) {
        Lang.RU -> "Загружено: "; Lang.DE -> "Geladen: "; Lang.EN -> "Loaded: "
    }
    val LOAD_FAILED get() = when(current) {
        Lang.RU -> "Ошибка загрузки!"; Lang.DE -> "Laden fehlgeschlagen!"; Lang.EN -> "Load FAILED!"
    }
    val ALL_SAVES_DELETED get() = when(current) {
        Lang.RU -> "Все сохранения удалены"; Lang.DE -> "Alle Spielstände gelöscht"; Lang.EN -> "All saves deleted"
    }

    val MOVE_BTN get() = when(current) {
        Lang.RU -> "ПЕРЕМЕСТИТЬ"; Lang.DE -> "BEWEGEN"; Lang.EN -> "MOVE"
    }
    val ATTACK_BTN get() = when(current) {
        Lang.RU -> "АТАКА"; Lang.DE -> "ANGREIFEN"; Lang.EN -> "ATTACK"
    }
    val UPGRADE_BTN get() = when(current) {
        Lang.RU -> "УЛУЧШИТЬ"; Lang.DE -> "AUSBESSERN"; Lang.EN -> "UPGRADE"
    }
    val NO_UPGRADABLE get() = when(current) {
        Lang.RU -> "Нет зданий для улучшения"; Lang.DE -> "Keine Gebäude zum Ausbessern"; Lang.EN -> "No buildings to upgrade"
    }

    val TRADE_ACTIVE get() = when(current) {
        Lang.RU -> "Торговля"; Lang.DE -> "Handel"; Lang.EN -> "Trade"
    }

    val TERR get() = when(current) {
        Lang.RU -> "терр"; Lang.DE -> "Geb"; Lang.EN -> "terr"
    }
    val POP get() = when(current) {
        Lang.RU -> "нас"; Lang.DE -> "Bev"; Lang.EN -> "pop"
    }

    val UNDO get() = when(current) {
        Lang.RU -> "ОТМЕНИТЬ"; Lang.DE -> "RÜCKGÄNGIG"; Lang.EN -> "UNDO"
    }
    val VICTORY get() = when(current) {
        Lang.RU -> "ПОБЕДА!"; Lang.DE -> "SIEG!"; Lang.EN -> "VICTORY!"
    }
    val DEFEAT get() = when(current) {
        Lang.RU -> "ПОРАЖЕНИЕ"; Lang.DE -> "NIEDERLAGE"; Lang.EN -> "DEFEAT"
    }

    val MULTIPLAYER get() = when(current) {
        Lang.RU -> "СЕТЕВАЯ ИГРА"; Lang.DE -> "NETZWERKSPIEL"; Lang.EN -> "MULTIPLAYER"
    }

    val DIPLO_ALLIANCE get() = when(current) {
        Lang.RU -> "Союз"; Lang.DE -> "Bündnis"; Lang.EN -> "Alliance"
    }
    val DIPLO_BREAK get() = when(current) {
        Lang.RU -> "Разрыв"; Lang.DE -> "Brechen"; Lang.EN -> "Break"
    }
    val DIPLO_TRADE get() = when(current) {
        Lang.RU -> "Торговля"; Lang.DE -> "Handel"; Lang.EN -> "Trade"
    }
    val DIPLO_CANCEL_TRADE get() = when(current) {
        Lang.RU -> "Отмена"; Lang.DE -> "Abbrechen"; Lang.EN -> "Cancel"
    }

    val SERVER_URL get() = when(current) {
        Lang.RU -> "URL сервера:"; Lang.DE -> "Server-URL:"; Lang.EN -> "Server URL:"
    }
    val YOUR_NAME get() = when(current) {
        Lang.RU -> "Ваше имя:"; Lang.DE -> "Ihr Name:"; Lang.EN -> "Your name:"
    }
    val CREATE_ROOM get() = when(current) {
        Lang.RU -> "СОЗДАТЬ КОМНАТУ"; Lang.DE -> "RAUM ERSTELLEN"; Lang.EN -> "CREATE ROOM"
    }
    val JOIN_ROOM get() = when(current) {
        Lang.RU -> "ВОЙТИ В КОМНАТУ"; Lang.DE -> "RAUM BEITRETEN"; Lang.EN -> "JOIN ROOM"
    }
    val BACK get() = when(current) {
        Lang.RU -> "НАЗАД"; Lang.DE -> "ZURÜCK"; Lang.EN -> "BACK"
    }
    val JOIN get() = when(current) {
        Lang.RU -> "ВОЙТИ"; Lang.DE -> "BEITRETEN"; Lang.EN -> "JOIN"
    }
    val ROOM_ID get() = when(current) {
        Lang.RU -> "ID комнаты:"; Lang.DE -> "Raum-ID:"; Lang.EN -> "Room ID:"
    }
    val CONNECTING get() = when(current) {
        Lang.RU -> "Подключение к"; Lang.DE -> "Verbindung zu"; Lang.EN -> "Connecting to"
    }
    val CONNECTED get() = when(current) {
        Lang.RU -> "Подключено! Создание комнаты..."; Lang.DE -> "Verbunden! Raum wird erstellt..."; Lang.EN -> "Connected! Creating room..."
    }
    val CONNECTION_FAILED get() = when(current) {
        Lang.RU -> "Ошибка подключения!"; Lang.DE -> "Verbindung fehlgeschlagen!"; Lang.EN -> "Connection failed!"
    }
    val WAITING_FOR_OPPONENT get() = when(current) {
        Lang.RU -> "Ожидание противника..."; Lang.DE -> "Warte auf Gegner..."; Lang.EN -> "Waiting for opponent..."
    }
    val ROOM_CREATED get() = when(current) {
        Lang.RU -> "Комната создана!"; Lang.DE -> "Raum erstellt!"; Lang.EN -> "Room created!"
    }
    val JOINING_ROOM get() = when(current) {
        Lang.RU -> "Вход в комнату..."; Lang.DE -> "Raum wird betreten..."; Lang.EN -> "Joining room..."
    }
    val JOINED_ROOM get() = when(current) {
        Lang.RU -> "Вошли в комнату"; Lang.DE -> "Raum beigetreten"; Lang.EN -> "Joined room"
    }
    val CONNECTING_TO_GAME get() = when(current) {
        Lang.RU -> "Подключение к игре..."; Lang.DE -> "Verbindung zum Spiel..."; Lang.EN -> "Connecting to game..."
    }
    val WAITING_FOR_OPPONENT_JOIN get() = when(current) {
        Lang.RU -> "Ожидание противника..."; Lang.DE -> "Warte auf Gegner..."; Lang.EN -> "Waiting for opponent to join..."
    }
    val ERROR get() = when(current) {
        Lang.RU -> "Ошибка:"; Lang.DE -> "Fehler:"; Lang.EN -> "Error:"
    }
    val OPPONENT_DISCONNECTED get() = when(current) {
        Lang.RU -> "Противник отключился!"; Lang.DE -> "Gegner getrennt!"; Lang.EN -> "Opponent disconnected!"
    }
}
