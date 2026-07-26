# Strategy — Rundenbasiertes 2D-Strategiespiel

## Beschreibung

Ein Prototyp eines rundenbasierten 2D-Strategiespiels, inspiriert vom Spiel "Imperialism 2", gebaut mit **libGDX + Kotlin + Kotlin Multiplatform**. Läuft im Desktop-Modus auf macOS. Unterstützt **3 Sprachen**: Englisch, Russisch, Deutsch.

## Screenshots

### Hauptmenü
![Hauptmenü](screenshots/menu.png)

### Einstellungen
![Einstellungen](screenshots/settings.png)

### Spiel
![Spielfenster](screenshots/game_window.png)

### Lade-Dialog
![Lade-Dialog](screenshots/load_dialog.png)

### Speicher-Dialog
![Speicher-Dialog](screenshots/save_dialog.png)

## Technologien

- **Kotlin 2.0.21** + Kotlin Multiplatform
- **libGDX 1.12.1** — Rendering, Eingabe, UI, FreeType-Schriften
- **kotlinx.serialization** — JSON-Serialisierung
- **Ollama** — lokaler AI-Server für den Gegner
- **LM Studio** — alternativer lokaler AI-Server (OpenAI-kompatibles API)
- **DeepSeek R1 (7B)** — Reasoning-Modell für KI-Entscheidungen

## Funktionen

- **Hexagonales Raster** mit Geländetypen: Ebene, Wald, Hügel, Berge, Wasser
- **Einheitentypen**: Infanterie, Kavallerie, Belagerung — auf der Karte mit Icons dargestellt
- **Gebäude**: Farm, Sägewerk, Kaserne, Mine, Markt, Mauer — jedes bietet Boni
- **Speicher/Lade-System**: Benannte Speicherstände mit Dialog-UI, Löschen mit Bestätigung
- **Box-Auswahl**: Klicken und ziehen, um mehrere Regionen gleichzeitig auszuwählen
- **Lokalisierung**: Englisch, Russisch, Deutsch — umschaltbar in den Einstellungen
- **KI-Gegner**: Nutzt Ollama (DeepSeek R1) mit automatischem Fallback auf regelbasierte KI
- **Rundenbasierter Kampf**: Angriff, Verteidigung, Rekrutierung von Einheiten, Territorien entwickeln
- **Diplomatie**: Bündnis, Handelsrouten mit dem Feind
- **Technologiebaum**: Landwirtschaft, Eisenverarbeitung, Maurerkunst, Befestigung, Reitkunst, Belagerungstechnik
- **Asynchrone KI**: KI-Züge laufen im Hintergrundthread — keine UI-Einfrierungen

## Projektstruktur

```
Strategy/
├── common/                          # Reine Kotlin-Logik
│   └── src/commonMain/kotlin/com/example/strategy/
│       ├── model/
│       │   ├── Region.kt            # Region, Gebäude, Geländetypen
│       │   ├── GameMap.kt           # Karte, Nachbarschaftssuche
│       │   ├── Player.kt            # Spieler, Ressourcen
│       │   ├── Resources.kt         # Ressourcentypen, Arithmetik
│       │   ├── Unit.kt              # Einheitentypen, Kampfstatistiken
│       │   ├── Diplomacy.kt         # Diplomatiestatus
│       │   ├── TechTree.kt          # Technologiebaum
│       │   └── GameState.kt         # Übergeordneter Zustand
│       ├── logic/
│       │   ├── Economy.kt           # Einkommens/Unterhaltsberechnung
│       │   ├── ActionQueue.kt       # Aktionswarteschlange
│       │   ├── GameRules.kt         # Bauen, rekrutieren, angreifen, bewegen
│       │   ├── TurnManager.kt       # Zugablauf
│       │   ├── DiplomacyManager.kt  # Diplomatische Aktionen
│       │   └── RandomEvents.kt      # Zufallsereignisse pro Zug
│       ├── pathfinding/
│       │   └── AStar.kt             # Reiner Kotlin A*
│       ├── ai/
│       │   ├── OllamaAI.kt          # KI-Engine (Ollama + LM Studio)
│       │   └── AISettings.kt        # KI-Backend-Konfiguration
│       └── platform/
│           ├── Platform.kt          # Plattformabstraktion
│           ├── HttpClient.kt        # HTTP-Schnittstelle
│           └── GameFactory.kt       # Weltgenerierung
└── desktop/                         # libGDX Desktop
    ├── build.gradle.kts
    └── src/main/kotlin/com/example/strategy/desktop/
        ├── DesktopLauncher.kt       # Einstiegspunkt + Crash-Logging
        ├── StrategyGame.kt          # Spielklasse
        ├── GameScreen.kt            # Hauptspielbildschirm + UI
        ├── MenuScreen.kt            # Hauptmenü + Einstellungen
        ├── Locale.kt                # Lokalisierung (EN/RU/DE)
        ├── SaveManager.kt           # Benanntes Speicher/Lade-System
        ├── AnimationManager.kt      # Angriffs/Bewegungsanimationen
        ├── SoundManager.kt          # Prozedurische Soundeffekte

        ├── MiniMap.kt               # Minikarten-Rendering
        └── TilesetGenerator.kt      # Laufzeit-Tileset-Generierung
```

## Spielmechaniken

| Mechanik | Beschreibung |
|----------|-------------|
| **Ressourcen** | Nahrung, Holz, Stein, Eisen, Gold — aus Territorien gewonnen |
| **Einheiten** | Infanterie (ANG 1/VER 1), Kavallerie (ANG 2/VER 1), Belagerung (ANG 3/VER 0) |
| **Bauen** | Farm, Sägewerk, Kaserne, Mine, Markt, Mauer |
| **Rekrutieren** | Infanterie: 5N+3G, Kavallerie: 10N+8G+5H, Belagerung: 15H+10E+10G (benötigt Kaserne) |
| **Entwickeln** | +3 Bevölkerung für 10G |
| **Angriff** | Angriffsstärke = Bevölkerung + Einheitenangriff. Verteidigung = feindliche Bevölkerung + Einheitenverteidigung + Mauerbonus |
| **Bewegen** | Hälfte der Truppen zwischen eigenen Regionen übertragen |
| **Wirtschaft** | Territoriumseinkommen abzüglich Unterhalt (Bevölkerung/5 Nahrung) |
| **Züge** | Spieler wechseln sich ab, KI läuft asynchron im Hintergrund |
| **Ollama KI** | DeepSeek R1 analysiert Spielzustand und trifft Entscheidungen |
| **Diplomatie** | Bündnis, Handelsrouten, Bündnis auflösen |
| **Technologien** | 6 Technologiebaum-Elemente mit Voraussetzungen |

## Ressourcen nach Geländetyp

| Gelände | Nahrung | Holz | Stein | Eisen | Gold |
|---------|---------|------|-------|-------|------|
| Ebene | +3 | 0 | 0 | 0 | Bev/10 |
| Wald | +1 | +3 | 0 | 0 | Bev/10 |
| Hügel | +2 | 0 | +2 | 0 | Bev/10 |
| Berge | +1 | 0 | +2 | +1 | Bev/10 |

## Gebäude

| Gebäude | Kosten | Bonus |
|---------|--------|-------|
| Farm | 10N + 5H | +2 Nahrung/Zug |
| Sägewerk | 15H + 5G | +2 Holz/Zug |
| Kaserne | 15H + 10S + 10G | Ermöglicht Rekrutierung |
| Mine | 5H + 15S + 5E | +2 Eisen/Zug |
| Markt | 10H + 5S + 15G | +3 Gold/Zug |
| Mauer | 20S + 5E | +5 Verteidigung pro Mauer |

## Steuerung

| Aktion | Steuerung |
|--------|-----------|
| Region auswählen | Linksklick auf Karte |
| Mehrere Regionen auswählen | Linksklick + Ziehen (Box-Auswahl) |
| Kamera bewegen | Rechtsklick + Ziehen |
| Zoom | Mausrad |
| Bauen | Region auswählen → Aktionsbutton |
| Rekrutieren | Region mit Kaserne auswählen → REKRUTIEREN-Button |
| Angriff | Eigene Region auswählen → ANGREIFEN → feindliche Region anklicken |
| Truppen bewegen | Eigene Region auswählen → BEWEGEN → eigene andere Region anklicken |
| Zug beenden | ZUG BEENDEN |
| Spiel speichern | SPEICHERN → Name eingeben → SPEICHERN |
| Spiel laden | LADEN → Speicherstand aus Liste auswählen |

## Lokalisierung

Öffnen Sie **EINSTELLUNGEN** im Hauptmenü zum Wechseln zwischen:
- English (EN)
- Русский (RU)
- Deutsch (DE)

Die Sprache wird automatisch gespeichert.

## KI

Das Spiel unterstützt **3 KI-Backends** — wählen Sie in **EINSTELLUNGEN**:

### None (Eingebaut)
Regelbasierte KI, keine externen Abhängigkeiten erforderlich. Funktioniert sofort.

### Ollama
Lokaler AI-Server mit dem Modell deepseek-r1:7b.

**Einrichtung:**
1. Ollama installieren: `curl -fsSL https://ollama.ai/install.sh | sh`
2. Modell herunterladen: `ollama pull deepseek-r1:7b`
3. Ollama starten: `ollama serve`

### LM Studio
Alternativer lokaler AI-Server mit OpenAI-kompatiblem API.

**Einrichtung:**
1. [LM Studio](https://lmstudio.ai) herunterladen und installieren
2. Ein Modell laden (z.B. DeepSeek, Llama, Mistral)
3. Lokalen Server starten (Standard: `http://localhost:1234`)
4. URL und Modellname in **EINSTELLUNGEN** einstellen

### Wie es funktioniert

Wenn Sie **ZUG BEENDEN** drücken:
1. Ihre ausstehende Aktion (Bauen/Rekrutieren/Angriff/Bewegen) wird auf den Spielzustand angewendet
2. Der Zug wechselt zum KI-Spieler
3. `TurnManager.startTurn()` wendet Einkommen, Unterhalt, Kriegsnebel und Zufallsereignisse für die KI an
4. `OllamaAI.decide()` wird in einem **Hintergrundthread** aufgerufen (nicht-blockierend):
   - Prüft `AISettings.backend` um den KI-Engine zu bestimmen
   - Formt einen Prompt mit KI-Ressourcen, Territorien, Feindpositionen, Diplomatiestatus
   - **Ollama**: sendet HTTP POST an `{url}/api/generate`
   - **LM Studio**: sendet HTTP POST an `{url}/v1/chat/completions` (OpenAI-kompatibles Format)
   - Das Modell analysiert die Situation und gibt eine Aktion zurück (z.B. `BUILD_FARM:3`, `RECRUIT:7`)
5. Die KI-Aktion wird über `Gdx.app.postRunnable` zurück zum GL-Thread geleitet
6. `applyAIAction()` führt die Aktion aus, dann `TurnManager.endTurn()` wechselt zurück zum Spieler
7. Wenn die KI-Antwort zu lange dauert oder der Server nicht erreichbar ist, läuft sofort die **Fallback-KI** (regelbasiert, kein HTTP nötig)

### Fallback (Kein Server erforderlich)

**Das Spiel funktioniert vollständig ohne Ollama oder LM Studio.** Wenn das ausgewählte Backend nicht verfügbar ist, verwendet die KI eine regelbasierte Strategie:

1. Kaserne bauen, wenn Feind in der Nähe und bezahlbar
2. Mauern bauen, wenn Kaserne vorhanden und Feind in der Nähe
3. Farm auf leeren Territorien bauen (wenn bezahlbar)
4. Mine auf Bergen bauen (wenn bezahlbar)
5. Truppen rekrutieren, wenn Kaserne vorhanden (wenn bezahlbar)
6. Regionen entwickeln, wenn Gold vorhanden (wenn bezahlbar)
7. Handel vorschlagen, wenn neutral und wohlhabend

## Spielführer

- [English](HOW_TO_PLAY_EN.md)
- [Русский](HOW_TO_PLAY_RU.md)
- [Deutsch](HOW_TO_PLAY_DE.md)

## Bekannte Probleme

- Ollama/LM Studio-Aufrufe können die UI kurzzeitig einfrieren, wenn der Server langsam antwortet (durch asynchrone KI reduziert, aber Timeout kann immer noch zu kurzen Pausen führen)

## Änderungsprotokoll

### v0.2.0 (Juli 2026)
- KI-Backend-Auswahl: Ollama, LM Studio oder Fallback
- Lokalisierung: Englisch, Russisch, Deutsch
- Einheiten-Icons auf der Karte (Infanterie, Kavallerie, Belagerung)
- Box-Auswahl für mehrere Regionen
- Benannte Speicherstände mit Löschbestätigung
- Zoom-Buttons (+/-) mit Mausrad-Hinweis
- Tutorial-Hinweis beim ersten Spielstart
- Crash-Logging in Datei
- GitHub Actions CI
- Spielführer in 3 Sprachen

### v0.1.0 (Juli 2026)
- Erstveröffentlichung mit Kernspielspiel
- Ollama KI mit Fallback-Strategie
- Diplomatie, Technologiebaum, Kriegsnebel
- Animationen, Sounds, Minikarte
- Kartenkonfiguration, Schwierigkeitsgrade

## Starten

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew desktop:run
```

## Letztes Update

Juli 2026
