package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.strategy.model.*

class GameScreen(private val game: StrategyGame) : ScreenAdapter() {

    private lateinit var stage: Stage
    private val camera = OrthographicCamera()
    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()

    private var selectedRegion: Region? = null
    private var state = game.gameState
    private var actionUsedThisTurn = false
    private var attackMode = false
    private var attackSourceId = -1
    private var moveMode = false
    private var moveSourceId = -1

    private var lastPanX = 0
    private var lastPanY = 0
    private var isPanning = false
    private val minZoom = 0.3f
    private val maxZoom = 3.0f
    private val tileSize = 128f
    private val animManager = AnimationManager()
    private var soundManager: SoundManager? = null
    private val miniMap = MiniMap(tileSize)
    private var animTime = 0f

    private lateinit var infoLabel: Label
    private lateinit var statusLabel: Label
    private lateinit var diplomacyLabel: Label
    private lateinit var techLabel: Label
    private lateinit var statsLabel: Label
    private lateinit var skin: Skin
    private var showStats = false
    private val actionButtons = mutableListOf<TextButton>()
    private val buildButtons = mutableListOf<TextButton>()
    private val diploButtons = mutableListOf<TextButton>()
    private val techButtons = mutableListOf<TextButton>()

    private val tileTextures = mutableMapOf<TileKey, TextureRegion>()
    private val buildingIcons = mutableMapOf<BuildingType, TextureRegion>()
    private data class TileKey(val terrain: TerrainType, val ownerId: Int?)

    override fun show() {
        soundManager?.dispose()
        soundManager = SoundManager()
        skin = createSkin()
        stage = Stage(ScreenViewport())

        generateTileTextures()
        generateBuildingIcons()
        buildUI()

        val mapPixelW = state.map.width * tileSize
        val mapPixelH = state.map.height * tileSize
        camera.position.set(mapPixelW / 2f, mapPixelH / 2f, 0f)
        camera.zoom = 1.2f
        camera.viewportWidth = Gdx.graphics.width.toFloat()
        camera.viewportHeight = Gdx.graphics.height.toFloat()
        camera.update()

        val mapInput = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (button == 1 || button == 2) {
                    isPanning = true
                    lastPanX = screenX; lastPanY = screenY
                    return true
                }
                if (button == 0) {
                    val worldCoords = camera.unproject(
                        com.badlogic.gdx.math.Vector3(screenX.toFloat(), (Gdx.graphics.height - screenY).toFloat(), 0f)
                    )
                    val tileX = (worldCoords.x / tileSize).toInt()
                    val tileY = (worldCoords.y / tileSize).toInt()
                    val region = state.map.getRegionAt(tileX, tileY)

                    if (attackMode && region != null && region.ownerId != state.currentPlayerId && region.terrain != TerrainType.WATER) {
                        val sourceRegion = state.map.getRegionById(attackSourceId)
                        if (sourceRegion != null) {
                            val fromX = sourceRegion.tileX * tileSize + tileSize / 2f
                            val fromY = (state.map.height - 1 - sourceRegion.tileY) * tileSize + tileSize / 2f
                            val toX = region.tileX * tileSize + tileSize / 2f
                            val toY = (state.map.height - 1 - region.tileY) * tileSize + tileSize / 2f
                            animManager.addMove(fromX, fromY, toX, toY)
                            animManager.addAttack(toX, toY)
                            soundManager?.play(SoundManager.SoundType.ATTACK)
                        }
                        val action = com.example.strategy.logic.ActionQueue.GameAction(
                            state.currentPlayerId,
                            com.example.strategy.logic.ActionQueue.ActionType.ATTACK,
                            region.id,
                            attackSourceId.toString()
                        )
                        com.example.strategy.logic.ActionQueue.enqueue(action)
                        state = com.example.strategy.logic.ActionQueue.processAll(state)
                        game.gameState = state
                        attackMode = false; attackSourceId = -1
                        actionUsedThisTurn = true
                        selectedRegion = state.map.getRegionById(region.id)
                        updateInfoLabel()
                        return true
                    }

                    if (moveMode && region != null && region.ownerId == state.currentPlayerId && region.id != moveSourceId && region.terrain != TerrainType.WATER) {
                        val action = com.example.strategy.logic.ActionQueue.GameAction(
                            state.currentPlayerId,
                            com.example.strategy.logic.ActionQueue.ActionType.MOVE_TROOPS,
                            moveSourceId,
                            region.id.toString()
                        )
                        com.example.strategy.logic.ActionQueue.enqueue(action)
                        state = com.example.strategy.logic.ActionQueue.processAll(state)
                        game.gameState = state
                        moveMode = false; moveSourceId = -1
                        actionUsedThisTurn = true
                        selectedRegion = state.map.getRegionById(region.id)
                        updateInfoLabel()
                        return true
                    }

                    if (region != null && region.terrain != TerrainType.WATER) {
                        selectedRegion = region
                        soundManager?.play(SoundManager.SoundType.SELECT)
                        updateInfoLabel()
                    }
                }
                return false
            }
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (isPanning) { isPanning = false; return true }
                return false
            }
            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                if (isPanning) {
                    camera.translate(
                        (lastPanX - screenX) * 1.5f * camera.zoom,
                        (screenY - lastPanY) * 1.5f * camera.zoom
                    )
                    lastPanX = screenX; lastPanY = screenY
                    return true
                }
                return false
            }
            override fun scrolled(amountX: Float, amountY: Float): Boolean {
                camera.zoom = (camera.zoom + amountY * 0.05f).coerceIn(minZoom, maxZoom)
                camera.update()
                return true
            }
        }

        Gdx.input.inputProcessor = InputMultiplexer(stage, mapInput)

        state = com.example.strategy.logic.TurnManager.startTurn(state)
        game.gameState = state
        updateInfoLabel()
    }

    private fun generateTileTextures() {
        for (terrain in TerrainType.entries) {
            for (owner in listOf(null, 0, 1)) {
                val key = TileKey(terrain, owner)
                val pix = Pixmap(tileSize.toInt(), tileSize.toInt(), Pixmap.Format.RGBA8888)
                when (terrain) {
                    TerrainType.PLAINS -> pix.setColor(0.4f, 0.7f, 0.3f, 1f)
                    TerrainType.FOREST -> pix.setColor(0.15f, 0.45f, 0.15f, 1f)
                    TerrainType.MOUNTAIN -> pix.setColor(0.5f, 0.45f, 0.4f, 1f)
                    TerrainType.HILLS -> pix.setColor(0.6f, 0.55f, 0.35f, 1f)
                    TerrainType.WATER -> pix.setColor(0.2f, 0.4f, 0.8f, 1f)
                }
                pix.fill()
                if (owner != null) {
                    val bc = if (owner == 0) Color(0.3f, 0.3f, 1f, 0.6f) else Color(1f, 0.3f, 0.3f, 0.6f)
                    pix.setColor(bc)
                    val s = tileSize.toInt()
                    pix.fillRectangle(0, 0, s, 2); pix.fillRectangle(0, s - 2, s, 2)
                    pix.fillRectangle(0, 0, 2, s); pix.fillRectangle(s - 2, 0, 2, s)
                }
                when (terrain) {
                    TerrainType.FOREST -> {
                        pix.setColor(0.1f, 0.35f, 0.1f, 1f)
                        for (i in 0..2) { val tx = 6 + i * 10; pix.fillTriangle(tx, 4, tx - 5, 16, tx + 5, 16); pix.fillRectangle(tx - 1, 16, 3, 14) }
                    }
                    TerrainType.MOUNTAIN -> {
                        pix.setColor(0.6f, 0.55f, 0.5f, 1f); pix.fillTriangle(16, 4, 4, 28, 28, 28)
                        pix.setColor(0.7f, 0.7f, 0.7f, 1f); pix.fillTriangle(16, 4, 12, 12, 20, 12)
                    }
                    TerrainType.HILLS -> {
                        pix.setColor(0.52f, 0.48f, 0.3f, 1f)
                        pix.fillCircle(40, 100, 36)
                        pix.fillCircle(88, 108, 32)
                        pix.fillCircle(20, 112, 26)
                        pix.setColor(0.6f, 0.55f, 0.38f, 1f)
                        pix.fillCircle(40, 90, 24)
                        pix.fillCircle(88, 98, 20)
                        pix.fillCircle(20, 102, 18)
                        pix.setColor(0.45f, 0.55f, 0.25f, 1f)
                        pix.fillCircle(40, 82, 6)
                        pix.fillCircle(88, 92, 5)
                        pix.fillCircle(20, 96, 5)
                    }
                    TerrainType.WATER -> {
                        pix.setColor(0.3f, 0.5f, 0.9f, 1f)
                        for (i in 0..3) pix.fillCircle(4 + i * 8, tileSize.toInt() / 2 + (i % 2) * 4 - 2, 3)
                    }
                    else -> {}
                }
                val tex = Texture(pix); pix.dispose()
                tileTextures[key] = TextureRegion(tex)
            }
        }
    }

    private fun generateBuildingIcons() {
        val s = 48
        fun makeIcon(draw: (Pixmap) -> Unit): TextureRegion {
            val p = Pixmap(s, s, Pixmap.Format.RGBA8888)
            draw(p)
            val t = Texture(p); p.dispose()
            return TextureRegion(t)
        }

        buildingIcons[BuildingType.FARM] = makeIcon { p ->
            p.setColor(0.3f, 0.55f, 0.2f, 1f)
            p.fillRectangle(4, 14, 40, 20)
            p.setColor(0.9f, 0.85f, 0.15f, 1f)
            for (i in 0..4) {
                val bx = 8 + i * 8
                p.fillRectangle(bx, 8, 2, 12)
                p.fillCircle(bx, 8, 3)
            }
            p.setColor(0.6f, 0.45f, 0.2f, 1f)
            p.fillRectangle(2, 34, 44, 4)
        }

        buildingIcons[BuildingType.LUMBER_MILL] = makeIcon { p ->
            p.setColor(0.45f, 0.3f, 0.1f, 1f)
            p.fillRectangle(18, 16, 8, 22)
            p.setColor(0.15f, 0.45f, 0.15f, 1f)
            p.fillCircle(22, 12, 14)
            p.setColor(0.75f, 0.75f, 0.75f, 1f)
            p.fillCircle(36, 30, 8)
            p.setColor(0.9f, 0.9f, 0.9f, 1f)
            p.fillCircle(36, 30, 4)
        }

        buildingIcons[BuildingType.QUARRY] = makeIcon { p ->
            p.setColor(0.6f, 0.58f, 0.55f, 1f)
            p.fillRectangle(6, 18, 18, 14)
            p.fillRectangle(26, 22, 16, 10)
            p.fillRectangle(10, 6, 14, 12)
            p.setColor(0.72f, 0.7f, 0.67f, 1f)
            p.fillRectangle(8, 20, 14, 10)
            p.fillRectangle(28, 24, 12, 6)
            p.fillRectangle(12, 8, 10, 8)
        }

        buildingIcons[BuildingType.MINE] = makeIcon { p ->
            p.setColor(0.35f, 0.3f, 0.25f, 1f)
            p.fillRectangle(4, 12, 40, 28)
            p.setColor(0.15f, 0.12f, 0.1f, 1f)
            p.fillCircle(24, 28, 12)
            p.setColor(0.5f, 0.35f, 0.15f, 1f)
            p.fillRectangle(10, 8, 4, 32)
            p.fillRectangle(34, 8, 4, 32)
            p.fillRectangle(10, 8, 28, 4)
            p.setColor(0.6f, 0.6f, 0.6f, 1f)
            p.fillRectangle(14, 36, 20, 2)
        }

        buildingIcons[BuildingType.MARKET] = makeIcon { p ->
            p.setColor(0.8f, 0.2f, 0.2f, 1f)
            p.fillTriangle(6, 8, 24, 2, 42, 8)
            p.setColor(0.45f, 0.3f, 0.1f, 1f)
            p.fillRectangle(10, 8, 3, 28)
            p.fillRectangle(35, 8, 3, 28)
            p.setColor(0.55f, 0.4f, 0.2f, 1f)
            p.fillRectangle(8, 20, 32, 4)
            p.setColor(0.95f, 0.8f, 0.1f, 1f)
            p.fillCircle(16, 18, 4)
            p.fillCircle(24, 16, 4)
            p.fillCircle(32, 18, 4)
        }

        buildingIcons[BuildingType.BARRACKS] = makeIcon { p ->
            p.setColor(0.7f, 0.15f, 0.15f, 1f)
            p.fillCircle(18, 24, 14)
            p.setColor(0.9f, 0.85f, 0.1f, 1f)
            p.fillCircle(18, 24, 8)
            p.setColor(0.75f, 0.75f, 0.8f, 1f)
            p.fillRectangle(30, 4, 4, 36)
            p.fillRectangle(26, 6, 12, 4)
            p.setColor(0.5f, 0.35f, 0.1f, 1f)
            p.fillRectangle(30, 36, 4, 8)
        }

        buildingIcons[BuildingType.WALL] = makeIcon { p ->
            p.setColor(0.55f, 0.52f, 0.48f, 1f)
            p.fillRectangle(2, 20, 44, 20)
            p.fillRectangle(2, 12, 8, 10)
            p.fillRectangle(16, 12, 8, 10)
            p.fillRectangle(30, 12, 8, 10)
            p.setColor(0.4f, 0.38f, 0.35f, 1f)
            p.fillRectangle(2, 26, 44, 2)
            p.fillRectangle(12, 20, 2, 20)
            p.fillRectangle(24, 20, 2, 20)
            p.fillRectangle(36, 20, 2, 20)
        }
    }

    private fun buildUI() {
        val root = Table(skin).apply { setFillParent(true) }

        val topPanel = Table(skin).apply { left().top().pad(10f); defaults().left().padBottom(2f) }
        infoLabel = Label("Click a region to select", skin)
        statusLabel = Label("", skin)
        statsLabel = Label("", skin)
        statsLabel.color = Color.LIGHT_GRAY
        topPanel.add(infoLabel).row()
        topPanel.add(statusLabel).row()
        topPanel.add(statsLabel).row()
        root.add(topPanel).left().top().expandX()

        val panel = Table(skin).apply { right().bottom().pad(10f); defaults().pad(3f) }

        fun btn(text: String, action: String): TextButton {
            val b = TextButton(text, skin)
            b.label.setFontScale(0.75f)
            b.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    Gdx.app.log("GameScreen", "Button clicked: $action")
                    handleAction(action)
                }
            })
            actionButtons.add(b)
            return b
        }

        fun diploBtn(text: String, action: String): TextButton {
            val b = TextButton(text, skin)
            b.label.setFontScale(0.65f)
            b.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    Gdx.app.log("GameScreen", "Diplo clicked: $action")
                    handleAction(action)
                }
            })
            diploButtons.add(b)
            return b
        }

        val buildFarmBtn = btn("Farm (10F 5W)", "BUILD_FARM"); buildButtons.add(buildFarmBtn)
        val buildLumberBtn = btn("Lumber Mill (15W)", "BUILD_LUMBER_MILL"); buildButtons.add(buildLumberBtn)
        val buildBarracksBtn = btn("Barracks (15W 10S 10G)", "BUILD_BARRACKS"); buildButtons.add(buildBarracksBtn)
        val buildMineBtn = btn("Mine (5W 15S 5I)", "BUILD_MINE"); buildButtons.add(buildMineBtn)

        panel.add(buildFarmBtn).fillX()
        panel.add(buildLumberBtn).fillX()
        panel.add(buildBarracksBtn).fillX().row()
        panel.add(buildMineBtn).fillX()
        panel.add(btn("Recruit (10F 5G)", "RECRUIT")).fillX()
        panel.add(btn("Infantry (5F 3G)", "RECRUIT_INFANTRY")).fillX()
        panel.add(btn("Cavalry (10F 8G 5W)", "RECRUIT_CAVALRY")).fillX().row()
        panel.add(btn("Siege (15W 10I 10G)", "RECRUIT_SIEGE")).fillX()
        panel.add(btn("Develop (10G)", "DEVELOP")).fillX()
        panel.add(btn("MOVE", "MOVE")).fillX().row()
        panel.add(btn("ATTACK", "ATTACK")).fillX()

        val endBtn = TextButton("END TURN", skin)
        endBtn.label.setFontScale(0.75f); endBtn.label.color = Color.GOLD
        endBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Gdx.app.log("GameScreen", "END TURN clicked")
                handleAction("END_TURN")
            }
        })
        panel.add(endBtn).fillX().padLeft(10f)

        val menuBtn = TextButton("MENU", skin)
        menuBtn.label.setFontScale(0.75f); menuBtn.label.color = Color.LIGHT_GRAY
        menuBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.setScreen(MenuScreen(game))
            }
        })
        panel.add(menuBtn).fillX().padLeft(10f)

        val saveBtn = TextButton("SAVE", skin)
        saveBtn.label.setFontScale(0.7f); saveBtn.label.color = Color.CYAN
        saveBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (SaveManager.save(state)) {
                    statusLabel.setText("Game saved!")
                }
            }
        })
        panel.add(saveBtn).fillX().padLeft(10f)

        val loadBtn = TextButton("LOAD", skin)
        loadBtn.label.setFontScale(0.7f); loadBtn.label.color = Color.CYAN
        loadBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val loaded = SaveManager.load()
                if (loaded != null) {
                    state = loaded
                    game.gameState = state
                    selectedRegion = null
                    actionUsedThisTurn = false
                    attackMode = false; attackSourceId = -1
                    moveMode = false; moveSourceId = -1
                    updateInfoLabel()
                    statusLabel.setText("Game loaded!")
                } else {
                    statusLabel.setText("No save found!")
                }
            }
        })
        panel.add(loadBtn).fillX()

        val statsBtn = TextButton("STATS", skin)
        statsBtn.label.setFontScale(0.7f); statsBtn.label.color = Color.GREEN
        statsBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showStats = !showStats
                updateStatsLabel()
            }
        })
        panel.add(statsBtn).fillX()

        val diploPanel = Table(skin).apply { right().top().pad(10f); defaults().pad(2f) }
        diplomacyLabel = Label("Diplomacy", skin)
        diplomacyLabel.color = Color.CYAN
        diploPanel.add(diplomacyLabel).colspan(2).row()
        diploPanel.add(diploBtn("Alliance", "DIPLO_ALLIANCE")).fillX()
        diploPanel.add(diploBtn("Break", "DIPLO_BREAK")).fillX().row()
        diploPanel.add(diploBtn("Trade", "DIPLO_TRADE")).fillX()
        diploPanel.add(diploBtn("Cancel", "DIPLO_CANCEL_TRADE")).fillX().row()

        val techPanel = Table(skin).apply { left().top().pad(10f); defaults().pad(2f) }
        techLabel = Label("Technologies", skin)
        techLabel.color = Color.YELLOW
        techPanel.add(techLabel).colspan(2).row()
        for (tech in com.example.strategy.model.TECH_TREE) {
            val shortName = tech.name.take(8)
            val b = TextButton(shortName, skin)
            b.label.setFontScale(0.55f)
            b.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    handleAction("RESEARCH:${tech.type.name}")
                }
            })
            techButtons.add(b)
            techPanel.add(b).fillX().colspan(2).row()
        }

        root.add(techPanel).left().top().pad(10f)
        root.add(diploPanel).right().top().pad(10f)
        root.add(panel).right().bottom().pad(10f)
        stage.addActor(root)
    }

    private fun updateInfoLabel() {
        val r = selectedRegion
        val player = state.currentPlayer()
        val isMyTurn = state.currentPlayerId == 0

        val myTerritories = state.map.regions.count { it.ownerId == 0 }
        val enemyTerritories = state.map.regions.count { it.ownerId == 1 }
        val myPop = state.map.regions.filter { it.ownerId == 0 }.sumOf { it.population }
        val enemyPop = state.map.regions.filter { it.ownerId == 1 }.sumOf { it.population }

        val income = com.example.strategy.logic.Economy.calculateIncome(player!!, state.map)
        val upkeep = com.example.strategy.logic.Economy.upkeepCost(player, state.map)

        val diplo = state.diplomacy.getRelation(state.currentPlayerId, 1)
        val diploStatus = when (diplo.status) {
            DiplomacyStatus.ALLIED -> "ALLIED (${diplo.turnsAllied} turns)"
            DiplomacyStatus.TRADE_PARTNERS -> "TRADE PARTNERS"
            DiplomacyStatus.ENEMY -> "ENEMY"
            DiplomacyStatus.NEUTRAL -> "NEUTRAL"
        }
        val tradeStatus = if (diplo.tradeActive) " + Trade" else ""
        diplomacyLabel.setText("Diplomacy: $diploStatus$tradeStatus")

        if (r == null) {
            infoLabel.setText("Click a region to select")
        } else if (!state.fog.isExplored(0, r.id)) {
            infoLabel.setText("Unknown territory — explore to reveal")
        } else {
            val owner = when (r.ownerId) { 0 -> "Yours"; 1 -> "Enemy"; else -> "Neutral" }
            val buildings = if (r.buildings.isEmpty()) "No buildings" else r.buildings.joinToString { it.type.name }
            val attack = r.population + r.units.totalAttack()
            val defense = r.population + r.units.totalDefense() + r.buildings.count { it.type == BuildingType.WALL } * 5
            val unitInfo = if (r.units.units.isEmpty()) "No units" else r.units.units.joinToString { "${it.count} ${it.type.name.lowercase()}" }
            infoLabel.setText(
                "${r.name} | ${r.terrain} | $owner\n" +
                "Population: ${r.population} (Attack: $attack, Defense: $defense)\n" +
                "Units: $unitInfo\n" +
                "Buildings: $buildings"
            )
        }

        statusLabel.setText(
            "Turn ${state.turn} | ${player?.name ?: "?"}\n" +
            "Territories: You $myTerritories vs $enemyTerritories\n" +
            "Population: You $myPop vs $enemyPop\n" +
            "Income: +${income.food}F +${income.wood}W +${income.stone}S +${income.gold}G | Upkeep: -${upkeep.food}F\n" +
            when {
                actionUsedThisTurn -> "ACTION USED — click END TURN"
                isMyTurn -> "YOUR TURN — choose one action"
                else -> "Waiting..."
            }
        )
        statusLabel.color = when {
            actionUsedThisTurn -> Color.ORANGE
            isMyTurn -> Color.GREEN
            else -> Color.GRAY
        }

        val dimmed = actionUsedThisTurn || !isMyTurn
        val hasBuilding = r?.buildings?.isNotEmpty() == true
        for (b in actionButtons) {
            b.color.a = if (dimmed) 0.3f else 1f
        }
        for (b in buildButtons) {
            if (hasBuilding) b.color.a = 0.3f
        }
        for (b in diploButtons) {
            b.color.a = if (dimmed) 0.3f else 1f
        }
        for ((i, b) in techButtons.withIndex()) {
            val tech = com.example.strategy.model.TECH_TREE[i]
            val researched = state.currentPlayer()?.techs?.isResearched(tech.type) == true
            val canResearch = state.currentPlayer()?.techs?.canResearch(tech.type) == true
            when {
                researched -> { b.color.a = 1f; b.label.color = Color.GREEN }
                canResearch && !dimmed -> { b.color.a = 1f; b.label.color = Color.WHITE }
                else -> { b.color.a = 0.3f; b.label.color = Color.GRAY }
            }
        }
        updateStatsLabel()
    }

    private fun updateStatsLabel() {
        if (!showStats) {
            statsLabel.setText("")
            return
        }
        val history = state.history
        if (history.isEmpty()) {
            statsLabel.setText("No history yet")
            return
        }
        val recent = history.takeLast(5)
        val lines = recent.joinToString("\n") { h ->
            "T${h.turn}: ${h.territories} terr, ${h.population} pop, F${h.resources.food} W${h.resources.wood} S${h.resources.stone} G${h.resources.gold}"
        }
        statsLabel.setText("--- Stats ---\n$lines")
    }

    private fun handleAction(actionType: String) {
        try {
            if (actionType == "END_TURN") {
                state = com.example.strategy.logic.TurnManager.endTurn(state)
                game.gameState = state; selectedRegion = null; actionUsedThisTurn = false
                attackMode = false; attackSourceId = -1
                moveMode = false; moveSourceId = -1
                soundManager?.play(SoundManager.SoundType.END_TURN)
                runAITurns()
                updateInfoLabel()
                return
            }
            if (actionUsedThisTurn) {
                Gdx.app.log("GameScreen", "Already used action this turn!")
                return
            }
            if (actionType.startsWith("DIPLO_")) {
                handleDiploAction(actionType)
                return
            }
            if (actionType.startsWith("RESEARCH:")) {
                val techName = actionType.removePrefix("RESEARCH:")
                val action = com.example.strategy.logic.ActionQueue.GameAction(
                    state.currentPlayerId,
                    com.example.strategy.logic.ActionQueue.ActionType.RESEARCH,
                    0,
                    techName
                )
                com.example.strategy.logic.ActionQueue.enqueue(action)
                state = com.example.strategy.logic.ActionQueue.processAll(state)
                game.gameState = state
                actionUsedThisTurn = true
                soundManager?.play(SoundManager.SoundType.RESEARCH)
                updateInfoLabel()
                return
            }

            val region = selectedRegion
            if (region == null) {
                Gdx.app.log("GameScreen", "No region selected!")
                return
            }
            if (actionType.startsWith("BUILD_") && region.buildings.isNotEmpty()) {
                Gdx.app.log("GameScreen", "Region already has a building!")
                return
            }

            if (actionType == "ATTACK") {
                if (region.ownerId != state.currentPlayerId) {
                    Gdx.app.log("GameScreen", "Select YOUR region as attack source!")
                    return
                }
                attackMode = true
                attackSourceId = region.id
                infoLabel.setText("ATTACK MODE: Click enemy region to attack from ${region.name}")
                return
            }

            if (actionType == "MOVE") {
                if (region.ownerId != state.currentPlayerId) {
                    Gdx.app.log("GameScreen", "Select YOUR region as move source!")
                    return
                }
                moveMode = true
                moveSourceId = region.id
                infoLabel.setText("MOVE MODE: Click your region to move troops from ${region.name}")
                return
            }

            val action = when (actionType) {
                "BUILD_FARM" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.BUILD, region.id, "FARM")
                "BUILD_LUMBER_MILL" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.BUILD, region.id, "LUMBER_MILL")
                "BUILD_BARRACKS" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.BUILD, region.id, "BARRACKS")
                "BUILD_MINE" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.BUILD, region.id, "MINE")
                "RECRUIT" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.RECRUIT, region.id)
                "RECRUIT_INFANTRY" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.RECRUIT_INFANTRY, region.id)
                "RECRUIT_CAVALRY" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.RECRUIT_CAVALRY, region.id)
                "RECRUIT_SIEGE" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.RECRUIT_SIEGE, region.id)
                "DEVELOP" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.DEVELOP, region.id)
                else -> return
            }
            com.example.strategy.logic.ActionQueue.enqueue(action)
            state = com.example.strategy.logic.ActionQueue.processAll(state)
            game.gameState = state
            selectedRegion = state.map.getRegionById(region.id)
            actionUsedThisTurn = true

            when {
                actionType.startsWith("BUILD_") -> soundManager?.play(SoundManager.SoundType.BUILD)
                actionType.startsWith("RECRUIT") -> soundManager?.play(SoundManager.SoundType.RECRUIT)
                actionType == "DEVELOP" -> soundManager?.play(SoundManager.SoundType.RECRUIT)
            }

            updateInfoLabel()
            Gdx.app.log("GameScreen", "Action done: $actionType on ${region.name}")
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Action error: ${e.message}")
        }
    }

    private fun handleDiploAction(actionType: String) {
        if (actionUsedThisTurn) return
        val targetId = 1
        val action = when (actionType) {
            "DIPLO_ALLIANCE" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.PROPOSE_ALLIANCE, targetId)
            "DIPLO_BREAK" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.BREAK_ALLIANCE, targetId)
            "DIPLO_TRADE" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.PROPOSE_TRADE, targetId)
            "DIPLO_CANCEL_TRADE" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.CANCEL_TRADE, targetId)
            else -> return
        }
        com.example.strategy.logic.ActionQueue.enqueue(action)
        state = com.example.strategy.logic.ActionQueue.processAll(state)
        game.gameState = state
        actionUsedThisTurn = true
        soundManager?.play(SoundManager.SoundType.ALLIANCE)
        updateInfoLabel()
    }

    private fun runAITurns() {
        var maxTurns = 10
        while (state.currentPlayerId != 0 && maxTurns-- > 0) {
            state = com.example.strategy.logic.TurnManager.startTurn(state)
            val aiAction = com.example.strategy.ai.OllamaAI.decide(state)
            if (aiAction != null) {
                val action = com.example.strategy.logic.ActionQueue.GameAction(
                    state.currentPlayerId, aiAction.actionType, aiAction.targetRegionId, aiAction.param
                )
                com.example.strategy.logic.ActionQueue.enqueue(action)
                state = com.example.strategy.logic.ActionQueue.processAll(state)
                Gdx.app.log("GameScreen", "AI action: ${aiAction.actionType} on region ${aiAction.targetRegionId}")
            }
            state = com.example.strategy.logic.TurnManager.endTurn(state)
        }
        game.gameState = state
    }

    override fun render(delta: Float) {
        try {
            Gdx.gl.glClearColor(0.1f, 0.15f, 0.3f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            if (isPanning) {
                if (!Gdx.input.isButtonPressed(1) && !Gdx.input.isButtonPressed(2)) isPanning = false
                else {
                    camera.translate(
                        (lastPanX - Gdx.input.x) * 1.5f * camera.zoom,
                        (Gdx.input.y - lastPanY) * 1.5f * camera.zoom
                    )
                    lastPanX = Gdx.input.x; lastPanY = Gdx.input.y
                }
            }

            camera.update()
            batch.projectionMatrix = camera.combined
            animTime += delta

            batch.begin()
            for (region in state.map.regions) {
                val explored = state.fog.isExplored(0, region.id)
                if (!explored) {
                    val x = region.tileX * tileSize
                    val y = (state.map.height - 1 - region.tileY) * tileSize
                    batch.setColor(0.1f, 0.1f, 0.15f, 1f)
                    batch.draw(tileTextures[TileKey(region.terrain, null)], x, y, tileSize, tileSize)
                    batch.setColor(Color.WHITE)
                    continue
                }

                val key = TileKey(region.terrain, region.ownerId)
                val tr = tileTextures[key] ?: continue
                val x = region.tileX * tileSize
                val y = (state.map.height - 1 - region.tileY) * tileSize
                batch.draw(tr, x, y, tileSize, tileSize)

                if (region.terrain != TerrainType.WATER) {
                    val popText = "${region.population}"
                    val popColor = when {
                        region.ownerId == 0 -> Color(0.5f, 0.8f, 1f, 0.9f)
                        region.ownerId == 1 -> Color(1f, 0.5f, 0.5f, 0.9f)
                        else -> Color.WHITE
                    }
                    game.font.color = Color.BLACK
                    game.font.draw(batch, popText, x + tileSize - 28f, y + tileSize - 6f)
                    game.font.color = popColor
                    game.font.draw(batch, popText, x + tileSize - 30f, y + tileSize - 4f)

                    if (region.buildings.isNotEmpty()) {
                        val iconSize = tileSize * 0.28f
                        val gap = 2f
                        val totalW = region.buildings.size * iconSize + (region.buildings.size - 1) * gap
                        var startX = x + (tileSize - totalW) / 2f
                        for (building in region.buildings) {
                            val icon = buildingIcons[building.type]
                            if (icon != null) {
                                batch.draw(icon, startX, y + 4f, iconSize, iconSize)
                            }
                            startX += iconSize + gap
                        }
                    }

                    if (region.ownerId == 0 && !actionUsedThisTurn) {
                        val canBuild = region.buildings.isEmpty()
                        val hasBarracks = region.buildings.any { it.type == BuildingType.BARRACKS }
                        if (canBuild || hasBarracks) {
                            val pulse = (kotlin.math.sin(animTime * 3f) * 0.15f + 0.15f)
                            batch.setColor(0.2f, 1f, 0.2f, pulse)
                            batch.draw(tileTextures[TileKey(region.terrain, region.ownerId)], x, y, tileSize, tileSize)
                            batch.setColor(Color.WHITE)
                        }
                    }
                }
            }
            selectedRegion?.let { r ->
                val x = r.tileX * tileSize
                val y = (state.map.height - 1 - r.tileY) * tileSize
                batch.setColor(1f, 1f, 0f, 0.3f)
                batch.draw(tileTextures[TileKey(r.terrain, r.ownerId)], x, y, tileSize, tileSize)
                batch.setColor(Color.WHITE)
            }
            batch.end()

            animManager.update(delta)
            if (animManager.hasAnimations()) {
                shapeRenderer.projectionMatrix = camera.combined
                animManager.render(shapeRenderer, tileSize)
            }

            stage.act(delta)
            stage.draw()

            game.batch.begin()
            val player = state.currentPlayer()
            game.font.color = Color.WHITE
            game.font.draw(game.batch,
                "Food: ${player?.resources?.food ?: 0}   Wood: ${player?.resources?.wood ?: 0}   " +
                        "Stone: ${player?.resources?.stone ?: 0}   Gold: ${player?.resources?.gold ?: 0}   " +
                        "Iron: ${player?.resources?.iron ?: 0}",
                12f, Gdx.graphics.height - 12f)
            game.font.color = Color(0.7f, 0.7f, 0.7f, 1f)
            for ((i, msg) in state.actionsLog.takeLast(3).withIndex()) {
                game.font.draw(game.batch, msg, 12f, Gdx.graphics.height - 32f - i * 16f)
            }
            miniMap.render(game.batch, state, Gdx.graphics.width, Gdx.graphics.height)
            game.batch.end()
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Render error: ${e.message}", e)
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        soundManager?.dispose()
        miniMap.dispose()
        tileTextures.values.forEach { it.texture.dispose() }
        buildingIcons.values.forEach { it.texture.dispose() }
        stage.dispose()
        skin.dispose()
    }

    private fun createSkin(): Skin {
        val s = Skin()
        val font = BitmapFont()
        font.data.setScale(1.0f)
        s.add("default-font", font, BitmapFont::class.java)
        val upPix = Pixmap(4, 4, Pixmap.Format.RGBA8888).apply { setColor(Color(0.25f, 0.25f, 0.3f, 0.9f)); fill() }
        val downPix = Pixmap(4, 4, Pixmap.Format.RGBA8888).apply { setColor(Color(0.35f, 0.35f, 0.4f, 1f)); fill() }
        val overPix = Pixmap(4, 4, Pixmap.Format.RGBA8888).apply { setColor(Color(0.3f, 0.3f, 0.35f, 1f)); fill() }
        val upTex = Texture(upPix); upPix.dispose()
        val downTex = Texture(downPix); downPix.dispose()
        val overTex = Texture(overPix); overPix.dispose()
        s.add("default", TextButton.TextButtonStyle().apply {
            this.font = font; fontColor = Color.WHITE
            up = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 2, 2, 2, 2))
            down = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(downTex, 2, 2, 2, 2))
            over = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(overTex, 2, 2, 2, 2))
        })
        s.add("default", Label.LabelStyle(font, Color.WHITE))
        return s
    }
}
