# How to Play — Strategy

## Goal
Conquer enemy territories by building armies, recruiting units, and attacking your opponent.

## Quick Start
1. **NEW GAME** — start a new game with default settings
2. **Left-click** a region to select it
3. **BUILD** a structure on your region (Farm, Barracks, etc.)
4. **RECRUIT** units at a region with Barracks
5. **ATTACK** an enemy region from your region with troops
6. **END TURN** — AI takes its turn

## Resources
| Resource | How to get |
|----------|-----------|
| Food | Plains (+3), Forest (+1), Hills (+2), Mountains (+1), Farm (+2/turn) |
| Wood | Forest (+3), Lumber Mill (+2/turn) |
| Stone | Hills (+2), Mountains (+2), Quarry (+2/turn) |
| Iron | Mountains (+1), Mine (+2/turn) |
| Gold | Population/10 per region, Market (+3/turn) |

## Buildings
| Building | Cost | Effect |
|----------|------|--------|
| Farm | 10F + 5W | +2 Food/turn |
| Lumber Mill | 15W + 5G | +2 Wood/turn |
| Barracks | 15W + 10S + 10G | Enables unit recruiting |
| Mine | 5W + 15S + 5I | +2 Iron/turn |
| Market | 10W + 5S + 15G | +3 Gold/turn |
| Wall | 20S + 5I | +5 Defense per wall |

## Units
| Unit | Cost | Attack | Defense | HP |
|------|------|--------|---------|-----|
| Infantry | 5F + 3G | 1 | 1 | 10 |
| Cavalry | 10F + 8G + 5W | 2 | 1 | 15 |
| Siege | 15W + 10I + 10G | 3 | 0 | 5 |

**Recruiting** requires Barracks in the region. Each recruit adds population + units.

## Combat
- **Attack strength** = your population + unit attack bonuses
- **Defense strength** = enemy population + unit defense + Wall bonus (+5 per Wall)
- **Victory**: enemy territory captured, you lose half your troops
- **Defeat**: you lose 2/3 of your troops

## Tips
- Build Barracks early to start recruiting units
- Walls defend against attacks (+5 defense each)
- Keep food production high — population consumes food (population / 5 per turn)
- Use Cavalry for strong attacks, Infantry for defense
- Siege units help against fortified (Walled) positions

## Diplomacy
- **Alliance**: Form an alliance with the enemy
- **Trade**: Establish a trade route for extra resources
- **Break**: Break an existing alliance

## Technology Tree
Research technologies to gain bonuses:
- **Agriculture**: +50% food production
- **Iron Working**: +2 attack strength
- **Masonry**: Enables Wall construction
- **Fortification**: +10 defense per Wall
- **Horseback Riding**: +2 movement range
- **Siege Engineering**: +50% attack vs Walls

## Controls
| Action | Control |
|--------|---------|
| Select region | Left-click |
| Box select | Left-click + drag |
| Pan camera | Right-click + drag |
| Zoom | Scroll wheel or +/- buttons |
| Build | Select region → action button |
| Attack | Select your region → ATTACK → click enemy |
| Move troops | Select your region → MOVE → click your region |
| End turn | END TURN button |
| Save game | SAVE → enter name → SAVE |
| Load game | LOAD → select from list |

## AI
The game uses an AI opponent that plays against you. You can choose the AI engine in **SETTINGS**:
- **Ollama** — requires Ollama with a model installed (e.g., deepseek-r1:7b)
- **LM Studio** — requires LM Studio running with a loaded model
- **None** — rule-based fallback AI, no external dependencies needed
