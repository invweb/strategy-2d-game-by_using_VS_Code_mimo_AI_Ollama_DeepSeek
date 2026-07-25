# So spielt man — Strategie

## Ziel
Erobere feindliche Gebiete, indem du Armeen aufbaust, Einheiten rekrutierst und deinen Gegner angreifst.

## Schnellstart
1. **NEUES SPIEL** — starte ein neues Spiel mit Standardeinstellungen
2. **Linksklick** auf eine Region zum Auswählen
3. **Baue** eine Struktur in deiner Region (Farm, Kaserne usw.)
4. **Rekrutiere** Einheiten in einer Region mit Kaserne
5. **Greife** eine feindliche Region aus deiner Region mit Truppen an
6. **ENDE ZUG** — KI macht ihren Zug

## Ressourcen
| Ressource | Beschaffung |
|-----------|------------|
| Nahrung | Ebene (+3), Wald (+1), Hügel (+2), Berge (+1), Farm (+2/Zug) |
| Holz | Wald (+3), Sägewerk (+2/Zug) |
| Stein | Hügel (+2), Berge (+2), Steinbruch (+2/Zug) |
| Eisen | Berge (+1), Mine (+2/Zug) |
| Gold | Bevölkerung/10 pro Region, Markt (+3/Zug) |

## Gebäude
| Gebäude | Kosten | Effekt |
|---------|--------|--------|
| Farm | 10N + 5H | +2 Nahrung/Zug |
| Sägewerk | 15H + 5G | +2 Holz/Zug |
| Kaserne | 15H + 10S + 10G | Ermöglicht Einheitenrekrutierung |
| Mine | 5H + 15S + 5E | +2 Eisen/Zug |
| Markt | 10H + 5S + 15G | +3 Gold/Zug |
| Mauer | 20S + 5E | +5 Verteidigung pro Mauer |

## Einheiten
| Einheit | Kosten | Angriff | Verteidigung | LP |
|---------|--------|---------|-------------|-----|
| Infanterie | 5N + 3G | 1 | 1 | 10 |
| Kavallerie | 10N + 8G + 5H | 2 | 1 | 15 |
| Belagerung | 15H + 10E + 10G | 3 | 0 | 5 |

**Rekrutierung** erfordert eine Kaserne in der Region. Jede Rekrutierung erhöht Bevölkerung + Einheiten.

## Kampfsystem
- **Angriffskraft** = deine Bevölkerung + Einheiten-Bonus
- **Verteidigungskraft** = feindliche Bevölkerung + Einheiten-Verteidigung + Mauer-Bonus (+5 pro Mauer)
- **Sieg**: Gebiet erobert, du verlierst die Hälfte deiner Truppen
- **Niederlage**: du verlierst 2/3 deiner Truppen

## Tipps
- Baue früh eine Kaserne, um Einheiten zu rekrutieren
- Mauern schützen gegen Angriffe (+5 Verteidigung pro Stück)
- Halte die Nahrungsproduktion hoch — Bevölkerung verbraucht Nahrung (Bevölkerung / 5 pro Zug)
- Nutze Kavallerie für starke Angriffe, Infanterie für Verteidigung
- Belagerungseinheiten helfen gegen befestigte (Mauer-geschützte) Stellungen

## Diplomatie
- **Allianz**: Schließe eine Allianz mit dem Feind
- **Handel**: Richte eine Handelsroute für zusätzliche Ressourcen ein
- **Abbruch: Brich eine bestehende Allianz

## Technologiebaum
Erforsche Technologien für Bonusse:
- **Landwirtschaft**: +50% Nahrungsproduktion
- **Eisenverarbeitung**: +2 Angriffskraft
- **Maurerkunst**: Ermöglicht Mauerbau
- **Befestigung**: +10 Verteidigung pro Mauer
- **Pferdezucht**: +2 Bewegungsreichweite
- **Belagerungstechnik**: +50% Angriff gegen Mauern

## Steuerung
| Aktion | Taste |
|--------|-------|
| Region auswählen | Linksklick |
| Bereich auswählen | Linksklick + Ziehen |
| Kamera verschieben | Rechtsklick + Ziehen |
| Zoom | Mausrad oder +/- Buttons |
| Bauen | Region auswählen → Aktionsbutton |
| Angriff | Eigene Region → ANGREIFEN → Feind klicken |
| Truppen verschieben | Eigene Region → BEWEGEN → eigene Region klicken |
| Zug beenden | ENDE ZUG |
| Spiel speichern | SPEICHERN → Name eingeben → SPEICHERN |
| Spiel laden | LADEN → aus Liste wählen |

## KI
Das Spiel verwendet einen KI-Gegner. Wähle die KI-Engine in den **EINSTELLUNGEN**:
- **Ollama** — erfordert installiertes Ollama mit einem Modell (z.B. deepseek-r1:7b)
- **LM Studio** — erfordert laufendes LM Studio mit geladenem Modell
- **None** — eingebaute Regeln, keine externen Abhängigkeiten nötig
