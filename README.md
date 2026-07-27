# Strategy — 2D Turn-Based Strategy Game

## Description

A turn-based 2D strategy game inspired by "Imperialism 2", built on **libGDX + Kotlin + Kotlin Multiplatform**. Runs on macOS desktop. Supports **3 languages**: English, Russian, German. Features multiplayer via WebSocket server.

## Screenshots

### Main Menu
![Main Menu](screenshots/menu.png)

### Settings
![Settings](screenshots/settings.png)

### Game
![Game Window](screenshots/game_window.png)

### Load Dialog
![Load Dialog](screenshots/load_dialog.png)

### Save Dialog
![Save Dialog](screenshots/save_dialog.png)

## Technologies

- **Kotlin 2.0.21** + Kotlin Multiplatform
- **libGDX 1.12.1** — rendering, input, UI, FreeType fonts
- **kotlinx.serialization** — JSON serialization
- **Ktor 3.0.3** — WebSocket multiplayer server
- **Ollama** — local AI server for opponent intelligence
- **LM Studio** — alternative local AI server (OpenAI-compatible API)

## Features

- **8 terrain types**: Plains, Forest, Hills, Mountain, Water, Desert, Swamp, Snow — each with unique resource bonuses
- **Procedural map generation** via Perlin noise — natural landscapes with elevation and moisture
- **Unit types**: Infantry, Cavalry, Siege — visualized on the map with icons
- **Buildings**: Farm, Lumber Mill, Barracks, Mine, Market, Wall — each provides bonuses
- **Movement range highlighting** — reachable tiles pulse blue when moving troops
- **Undo stack** — multiple action reversals per turn
- **Save/Load system** — named saves with validation, delete with confirmation
- **Box selection** — click-drag to select multiple regions at once
- **Localization** — English, Russian, German — switchable from Settings
- **AI opponent** — uses Ollama (DeepSeek R1) with automatic fallback to rule-based AI
- **Turn-based combat** — attack, defend, recruit units, develop territories
- **Diplomacy** — alliance, trade routes with enemy
- **Technology tree** — Agriculture, Iron Working, Masonry, Fortification, Horseback, Siege Engineering
- **Asynchronous AI** — AI turns run in background thread — no UI freezes
- **Procedural sounds** — varied waveforms (sine, square, sawtooth), chords, arpeggios, sweeps
- **Multiplayer** — WebSocket-based two-player online game

## Project Structure

```
Strategy/
├── common/                          # Pure Kotlin logic
│   └── src/commonMain/kotlin/com/example/strategy/
│       ├── model/
│       │   ├── Region.kt            # Region, buildings, terrain types
│       │   ├── GameMap.kt           # Map with O(1) lookup indexes
│       │   ├── Player.kt            # Player, resources
│       │   ├── Resources.kt         # Resource types, arithmetic
│       │   ├── Unit.kt              # Unit types, combat stats
│       │   ├── Diplomacy.kt         # Diplomacy state
│       │   ├── TechTree.kt          # Technology tree
│       │   ├── FogState.kt          # Fog of war
│       │   ├── Difficulty.kt        # Difficulty levels
│       │   └── GameState.kt         # Top-level state
│       ├── logic/
│       │   ├── Economy.kt           # Income/upkeep calculations
│       │   ├── ActionQueue.kt       # Injectable action queue
│       │   ├── GameRules.kt         # Build, recruit, attack, move
│       │   ├── TurnManager.kt       # Turn flow
│       │   ├── DiplomacyManager.kt  # Diplomacy actions
│       │   └── RandomEvents.kt      # Random events per turn
│       ├── pathfinding/
│       │   └── AStar.kt             # A* pathfinding + reachability
│       ├── ai/
│       │   ├── OllamaAI.kt          # AI engine (Ollama + LM Studio)
│       │   └── AISettings.kt        # AI backend configuration
│       ├── serialization/
│       │   └── GameStateSerializer.kt
│       └── platform/
│           ├── Platform.kt          # Platform abstraction
│           ├── HttpClient.kt        # HTTP interface
│           ├── GameFactory.kt       # World generation (Perlin noise)
│           └── PerlinNoise.kt       # Perlin noise implementation
├── desktop/                         # libGDX Desktop
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/example/strategy/desktop/
│       ├── DesktopLauncher.kt       # Entry point + crash logging
│       ├── StrategyGame.kt          # Game class
│       ├── GameScreen.kt            # Main game screen + logic
│       ├── GameUI.kt                # UI construction + dialogs
│       ├── GameInput.kt             # Input handling + box selection
│       ├── MapRenderer.kt           # Procedural terrain tiles + icons
│       ├── MenuScreen.kt            # Main menu + settings
│       ├── Locale.kt                # Localization (EN/RU/DE)
│       ├── Actions.kt               # Action name constants
│       ├── SaveManager.kt           # Named save/load with validation
│       ├── SkinFactory.kt           # Shared UI skin creation
│       ├── AnimationManager.kt      # Attack/move animations
│       ├── SoundManager.kt          # Procedural sound effects (11 types)
│       ├── TilesetGenerator.kt      # Runtime tileset generation
│       ├── NetworkClient.kt         # WebSocket client
│       ├── NetworkGameScreen.kt     # Network game screen
│       ├── LobbyScreen.kt           # Multiplayer lobby
│       ├── DesktopPlatform.kt       # JVM file I/O
│       └── DesktopHttpClient.kt     # JVM HTTP client
├── server/                          # Ktor WebSocket multiplayer server
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/example/strategy/server/
│       ├── Server.kt                # Ktor main, WebSocket endpoint
│       ├── Protocol.kt              # Message encoding/decoding
│       └── GameRoom.kt              # Room management, AI fallback
├── tools/
│   └── generate_tileset.py          # Tileset generation script
├── .github/workflows/
│   ├── ci.yml                       # Tests + build on push/PR
│   └── release.yml                  # Auto-publish on tags
└── releases/                        # Ready-to-run builds
```

## Game Mechanics

| Mechanic | Description |
|----------|-------------|
| **Resources** | Food, Wood, Stone, Iron, Gold — harvested from territories |
| **Units** | Infantry (ATK 1/DEF 1), Cavalry (ATK 2/DEF 1), Siege (ATK 3/DEF 0) |
| **Building** | Farm, Lumber Mill, Barracks, Mine, Market, Wall |
| **Recruiting** | Infantry: 5F+3G, Cavalry: 10F+8G+5W, Siege: 15W+10I+10G (requires Barracks) |
| **Development** | +3 population for 10G |
| **Attack** | Attack strength = population + unit attack. Defense = population + unit defense + Wall bonus |
| **Move** | Transfer troops between your regions (shows reachable range) |
| **Undo** | Multiple action reversals per turn (stack-based) |
| **Economy** | Territory income minus upkeep (population/5 food + terrain extras) |
| **Turns** | Players alternate, AI runs asynchronously in background |
| **Diplomacy** | Alliance, trade routes, break alliance |
| **Technology** | 6 tech tree items with prerequisites |
| **Random Events** | Plague, harvest bonus, revolt, gold discovery, trade caravan |

## Resources by Terrain

| Terrain | Food | Wood | Stone | Iron | Gold | Upkeep |
|---------|------|------|-------|------|------|--------|
| Plains | +3 | 0 | 0 | 0 | pop/10 | 0 |
| Forest | +1 | +3 | 0 | 0 | pop/10 | 0 |
| Hills | +2 | 0 | +2 | 0 | pop/10 | 0 |
| Mountain | +1 | 0 | +2 | +1 | pop/10 | 0 |
| Desert | +1 | 0 | 0 | 0 | pop/8+1 | 0 |
| Swamp | +1 | +2 | 0 | 0 | pop/10 | +1 |
| Snow | 0 | 0 | 0 | +1 | pop/10 | +1 |
| Water | 0 | 0 | 0 | 0 | 0 | 0 |

## Buildings

| Building | Cost | Bonus |
|----------|------|-------|
| Farm | 10F + 5W | +2 Food/turn |
| Lumber Mill | 15W | +2 Wood/turn |
| Barracks | 15W + 10S + 10G | Enables recruiting |
| Mine | 5W + 15S + 5I | +2 Iron/turn |
| Market | 10W + 5S + 15G | +3 Gold/turn |
| Wall | 20S + 5I | +5 Defense per wall |

## Controls

| Action | How |
|--------|-----|
| Select region | Left-click on map |
| Select multiple regions | Left-click + drag (box selection) |
| Pan camera | Right-click + drag |
| Zoom | Scroll wheel or +/- buttons |
| Build | Select region → action button |
| Recruit | Select region with Barracks → RECRUIT button |
| Attack | Select your region → ATTACK → click enemy region |
| Move troops | Select your region → MOVE → click destination (reachable tiles highlighted) |
| Undo | UNDO button (multiple per turn) |
| End turn | END TURN |
| Save game | SAVE → enter name → SAVE |
| Load game | LOAD → select save from list |

## Localization

Open **SETTINGS** in the main menu to switch between:
- English (EN)
- Русский (RU)
- Deutsch (DE)

Language preference is saved automatically.

## AI

The game supports **3 AI backends** — select in **SETTINGS**:

### None (Fallback)
Rule-based AI, no external dependencies required. Works immediately.

### Ollama
Local AI server running deepseek-r1:7b.

**Setup:**
1. Install Ollama: `curl -fsSL https://ollama.ai/install.sh | sh`
2. Pull model: `ollama pull deepseek-r1:7b`
3. Start Ollama: `ollama serve`

### LM Studio
Alternative local AI server with OpenAI-compatible API.

**Setup:**
1. Download and install [LM Studio](https://lmstudio.ai)
2. Load a model (e.g., DeepSeek, Llama, Mistral)
3. Start the local server (default: `http://localhost:1234`)
4. Set URL and model name in **SETTINGS**

### How it works

When you press **END TURN**:
1. Your pending action (build/recruit/attack/move) is applied to the game state
2. Turn switches to the AI player
3. `TurnManager.startTurn()` applies income, upkeep, fog of war, and random events for the AI
4. `OllamaAI.decide()` is called in a **background thread** (non-blocking):
   - Checks `AISettings.backend` to determine which AI engine to use
   - Formats a prompt with AI's resources, territories, enemy positions, diplomacy status
   - **Ollama**: sends HTTP POST to `{url}/api/generate`
   - **LM Studio**: sends HTTP POST to `{url}/v1/chat/completions` (OpenAI-compatible format)
   - Model analyzes the situation and returns one action (e.g., `BUILD_FARM:3`, `RECRUIT:7`)
5. The AI action is dispatched back to the GL thread via `Gdx.app.postRunnable`
6. `applyAIAction()` executes the action, then `TurnManager.endTurn()` switches back to the player
7. If the AI response takes too long or the server is unreachable, **fallback AI** runs immediately (rule-based, no HTTP needed)

## Multiplayer

The game supports **two-player online mode** via WebSocket.

### Server
```bash
./gradlew :server:run    # Starts on port 8080, WebSocket at /game
```

### Client
1. Open **MULTIPLAYER** from the main menu
2. Enter server URL and your name
3. **Create Room** — get a room ID to share
4. **Join Room** — enter room ID to connect
5. Game starts when both players connect

### How it works
- Server is authoritative — validates all actions, manages turns
- JSON protocol over WebSocket (create room, game action, end turn, chat)
- If second player disconnects, AI fallback takes over
- Server reuses all common module logic (ActionQueue, TurnManager, OllamaAI)

## Game Guides

- [English](HOW_TO_PLAY_EN.md)
- [Русский](HOW_TO_PLAY_RU.md)
- [Deutsch](HOW_TO_PLAY_DE.md)

## Testing

```bash
# Run all tests (common + desktop + server)
./gradlew :common:jvmTest :desktop:test :server:test
```

### Test Coverage
- **GameRules**: Build, recruit, attack, move, develop, research
- **Economy**: Income per terrain, building bonuses, agriculture tech, upkeep
- **UnitStack**: Add, remove, split, attack/defense totals
- **Resources**: Arithmetic, canAfford
- **TurnManager**: Start/end turn, income, history snapshots
- **AStar**: Pathfinding, terrain costs, water blocking
- **DiplomacyManager**: Alliance, trade, break, turn increments
- **RandomEvents**: All 5 event types, apply effects
- **GameFactory**: Map generation, terrain styles, starting buildings
- **SaveManager**: Save, load, delete, validation
- **Locale**: Language switching, all translations
- **Protocol**: Message encoding/decoding for multiplayer

## Changelog

### v0.4.0 (July 2026)
- New tileset extraction tool: `tools/extract_tileset.py` for converting external tilesets
- Support for larger external tilesets (64px tiles, 160+ tiles per image)
- Improved terrain tile analysis with color-based classification
- Backup build files added for safe build configuration changes
- Tileset: 8 terrain types (48×384 px) - Plains, Forest, Mountain, Hills, Water, Desert, Swamp, Snow
- Updated project configuration with test framework (JUnit Jupiter)
- Plan file integration for gigacode session tracking

### v0.3.0 (July 2026)
- Procedural terrain tiles with distinct patterns per biome
- Movement range highlighting with A* flood fill
- Undo stack for multiple action reversals
- Terrain bonuses: Desert (+gold), Swamp (+wood +upkeep), Snow (+iron +upkeep)
- Enhanced sound system: chords, arpeggios, sweeps, 11 sound types
- Save file validation on load
- Perlin noise map generation (replaced modular arithmetic)
- ActionQueue refactored to injectable class (supports isolated instances)
- Removed dead HexGridRenderer code
- CI/CD: all module tests run on push/PR
- Added 54 tests (26 desktop/server + 28 common)
- Updated README with multiplayer, terrain table, controls

### v0.2.0 (July 2026)
- AI backend selection: Ollama, LM Studio, or Fallback
- Localization: English, Russian, German
- Unit icons on map (Infantry, Cavalry, Siege)
- Box selection for multiple regions
- Named save/load with delete confirmation
- Zoom buttons (+/-) with scroll wheel hint
- Tutorial hint on first game start
- Crash logging to file
- GitHub Actions CI
- Game guides in 3 languages

### v0.1.0 (July 2026)
- Initial release with core gameplay
- Ollama AI with fallback strategy
- Diplomacy, tech tree, fog of war
- Animations, sounds, minimap
- Map configuration, difficulty levels

## Prerequisites

Make sure you have Java 17 or higher installed:

```bash
## Running

### Prerequisites

Make sure you have Java 17 or higher installed:

```bash
# macOS (Homebrew)
brew install openjdk@17

# Linux (Debian/Ubuntu)
sudo apt install openjdk-17-jdk

# Windows
choco install openjdk
```

### Build and Run

```bash
# Desktop
./gradlew desktop:run

# Server (for multiplayer)
./gradlew :server:run

# All modules (common tests, desktop build, server build)
./gradlew build
```

## License

MIT

## Last Updated

July 2026
