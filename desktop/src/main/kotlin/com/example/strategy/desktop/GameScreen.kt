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
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Window
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
    private val selectedRegions = mutableListOf<Region>()
    private var state = game.gameState
    private var actionUsedThisTurn = false
    private var attackMode = false
    private var attackSourceId = -1
    private var moveMode = false
    private var moveSourceId = -1

    private var lastPanX = 0
    private var lastPanY = 0
    private var isPanning = false
    private var isBoxSelecting = false
    private var boxStartScreenX = 0
    private var boxStartScreenY = 0
    private val minZoom = 0.3f
    private val maxZoom = 3.0f
    private val tileSize = 128f
    private val animManager = AnimationManager()
    private var zoomHintTimer = 0f
    private var zoomHintLabel: Label? = null
    private var soundManager: SoundManager? = null
    private var animTime = 0f
    private var aiPending = false
    private var alive = false
    private var tutorialTimer = 8f
    private var tutorialLabel: Label? = null

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
    private val unitIcons = mutableMapOf<UnitType, TextureRegion>()
    private data class TileKey(val terrain: TerrainType, val ownerId: Int?)

    override fun show() {
        alive = true
        soundManager?.dispose()
        soundManager = SoundManager()
        skin = createSkin()
        stage = Stage(ScreenViewport())

        generateTileTextures()
        generateBuildingIcons()
        generateUnitIcons()
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
                    boxStartScreenX = screenX
                    boxStartScreenY = screenY
                    isBoxSelecting = false

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
                    return true
                }
                return false
            }
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (isPanning) { isPanning = false; return true }
                if (button == 0) {
                    if (isBoxSelecting) {
                        selectRegionsInBox()
                        isBoxSelecting = false
                        return true
                    } else {
                        val worldCoords = camera.unproject(
                            com.badlogic.gdx.math.Vector3(screenX.toFloat(), (Gdx.graphics.height - screenY).toFloat(), 0f)
                        )
                        val tileX = (worldCoords.x / tileSize).toInt()
                        val tileY = (worldCoords.y / tileSize).toInt()
                        val region = state.map.getRegionAt(tileX, tileY)
                        selectedRegions.clear()
                        if (region != null && region.terrain != TerrainType.WATER) {
                            selectedRegion = region
                            selectedRegions.add(region)
                            soundManager?.play(SoundManager.SoundType.SELECT)
                        } else {
                            selectedRegion = null
                        }
                        updateInfoLabel()
                        return true
                    }
                }
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
                if (Gdx.input.isButtonPressed(0)) {
                    val dx = (screenX - boxStartScreenX).toFloat()
                    val dy = (screenY - boxStartScreenY).toFloat()
                    if (!isBoxSelecting && (dx * dx + dy * dy) > 25f) {
                        isBoxSelecting = true
                        selectedRegion = null
                        selectedRegions.clear()
                    }
                    if (isBoxSelecting) return true
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

        tutorialTimer = 8f
        tutorialLabel = Label(Locale.TUTORIAL_HINT, skin)
        tutorialLabel!!.color = Color(1f, 1f, 0.5f, 0.9f)
        tutorialLabel!!.setFontScale(0.9f)
        tutorialLabel!!.setWrap(true)
        tutorialLabel!!.setSize(500f, 60f)
        tutorialLabel!!.setPosition(Gdx.graphics.width / 2f - 250f, Gdx.graphics.height - 50f)
        stage.addActor(tutorialLabel)
    }

    private fun selectRegionsInBox() {
        val sx1 = minOf(boxStartScreenX.toFloat(), Gdx.input.x.toFloat())
        val sy1 = minOf(boxStartScreenY.toFloat(), Gdx.input.y.toFloat())
        val sx2 = maxOf(boxStartScreenX.toFloat(), Gdx.input.x.toFloat())
        val sy2 = maxOf(boxStartScreenY.toFloat(), Gdx.input.y.toFloat())
        selectedRegions.clear()
        selectedRegion = null
        val tempVec = com.badlogic.gdx.math.Vector3()
        for (region in state.map.regions) {
            val wx = region.tileX * tileSize + tileSize / 2f
            val wy = (state.map.height - 1 - region.tileY) * tileSize + tileSize / 2f
            tempVec.set(wx, wy, 0f)
            camera.project(tempVec)
            val screenCX = tempVec.x
            val screenCY = Gdx.graphics.height - tempVec.y
            if (screenCX in sx1..sx2 && screenCY in sy1..sy2 && region.terrain != TerrainType.WATER) {
                selectedRegions.add(region)
            }
        }
        if (selectedRegions.isNotEmpty()) selectedRegion = selectedRegions.first()
        soundManager?.play(SoundManager.SoundType.SELECT)
        updateInfoLabel()
    }

    private fun generateUnitIcons() {
        val s = 48
        fun makeIcon(draw: (Pixmap) -> Unit): TextureRegion {
            val p = Pixmap(s, s, Pixmap.Format.RGBA8888)
            draw(p)
            val t = Texture(p); p.dispose()
            return TextureRegion(t)
        }
        unitIcons[UnitType.INFANTRY] = makeIcon { p ->
            p.setColor(0.2f, 0.5f, 0.2f, 1f)
            p.fillCircle(24, 12, 8)
            p.fillRectangle(20, 20, 8, 16)
            p.fillRectangle(14, 24, 6, 4)
            p.fillRectangle(28, 24, 6, 4)
            p.fillRectangle(20, 36, 4, 8)
            p.fillRectangle(26, 36, 4, 8)
        }
        unitIcons[UnitType.CAVALRY] = makeIcon { p ->
            p.setColor(0.55f, 0.35f, 0.15f, 1f)
            p.fillCircle(16, 20, 12)
            p.fillRectangle(8, 20, 24, 10)
            p.fillRectangle(6, 30, 6, 12)
            p.fillRectangle(16, 30, 6, 12)
            p.fillRectangle(26, 30, 6, 12)
            p.setColor(0.3f, 0.6f, 0.3f, 1f)
            p.fillCircle(32, 16, 8)
        }
        unitIcons[UnitType.SIEGE] = makeIcon { p ->
            p.setColor(0.45f, 0.3f, 0.1f, 1f)
            p.fillRectangle(8, 28, 32, 8)
            p.fillRectangle(10, 20, 4, 12)
            p.fillRectangle(34, 20, 4, 12)
            p.fillRectangle(8, 16, 32, 4)
            p.setColor(0.7f, 0.7f, 0.7f, 1f)
            p.fillCircle(24, 10, 6)
        }
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
                        pix.fillCircle(40, 100, 36); pix.fillCircle(88, 108, 32); pix.fillCircle(20, 112, 26)
                        pix.setColor(0.6f, 0.55f, 0.38f, 1f)
                        pix.fillCircle(40, 90, 24); pix.fillCircle(88, 98, 20); pix.fillCircle(20, 102, 18)
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
            p.setColor(0.3f, 0.55f, 0.2f, 1f); p.fillRectangle(4, 14, 40, 20)
            p.setColor(0.9f, 0.85f, 0.15f, 1f)
            for (i in 0..4) { val bx = 8 + i * 8; p.fillRectangle(bx, 8, 2, 12); p.fillCircle(bx, 8, 3) }
            p.setColor(0.6f, 0.45f, 0.2f, 1f); p.fillRectangle(2, 34, 44, 4)
        }
        buildingIcons[BuildingType.LUMBER_MILL] = makeIcon { p ->
            p.setColor(0.45f, 0.3f, 0.1f, 1f); p.fillRectangle(18, 16, 8, 22)
            p.setColor(0.15f, 0.45f, 0.15f, 1f); p.fillCircle(22, 12, 14)
            p.setColor(0.75f, 0.75f, 0.75f, 1f); p.fillCircle(36, 30, 8)
        }
        buildingIcons[BuildingType.BARRACKS] = makeIcon { p ->
            p.setColor(0.7f, 0.15f, 0.15f, 1f); p.fillCircle(18, 24, 14)
            p.setColor(0.9f, 0.85f, 0.1f, 1f); p.fillCircle(18, 24, 8)
            p.setColor(0.75f, 0.75f, 0.8f, 1f); p.fillRectangle(30, 4, 4, 36)
        }
        buildingIcons[BuildingType.MINE] = makeIcon { p ->
            p.setColor(0.35f, 0.3f, 0.25f, 1f); p.fillRectangle(4, 12, 40, 28)
            p.setColor(0.15f, 0.12f, 0.1f, 1f); p.fillCircle(24, 28, 12)
            p.setColor(0.5f, 0.35f, 0.15f, 1f); p.fillRectangle(10, 8, 4, 32); p.fillRectangle(34, 8, 4, 32)
        }
        buildingIcons[BuildingType.WALL] = makeIcon { p ->
            p.setColor(0.55f, 0.52f, 0.48f, 1f); p.fillRectangle(2, 20, 44, 20)
            p.setColor(0.4f, 0.38f, 0.35f, 1f); p.fillRectangle(12, 20, 2, 20); p.fillRectangle(24, 20, 2, 20); p.fillRectangle(36, 20, 2, 20)
        }
        buildingIcons[BuildingType.QUARRY] = makeIcon { p ->
            p.setColor(0.6f, 0.58f, 0.55f, 1f); p.fillRectangle(6, 18, 18, 14); p.fillRectangle(26, 22, 16, 10)
        }
        buildingIcons[BuildingType.MARKET] = makeIcon { p ->
            p.setColor(0.8f, 0.2f, 0.2f, 1f); p.fillTriangle(6, 8, 24, 2, 42, 8)
            p.setColor(0.45f, 0.3f, 0.1f, 1f); p.fillRectangle(10, 8, 3, 28); p.fillRectangle(35, 8, 3, 28)
        }
    }

    private fun buildUI() {
        val root = Table(skin).apply { setFillParent(true) }

        val topPanel = Table(skin).apply { left().top().pad(10f); defaults().left().padBottom(2f) }
        infoLabel = Label(Locale.CLICK_REGION, skin)
        statusLabel = Label("", skin)
        statsLabel = Label("", skin)
        statsLabel.color = Color.LIGHT_GRAY
        topPanel.add(infoLabel).row()
        topPanel.add(statusLabel).row()
        topPanel.add(statsLabel).row()
        root.add(topPanel).left().top().expandX().colspan(2).row()

        val panel = Table(skin).apply { right().bottom().pad(10f); defaults().pad(3f).right() }

        fun btn(text: String, action: String): TextButton {
            val b = TextButton(text, skin)
            b.label.setFontScale(0.75f)
            b.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) { handleAction(action) }
            })
            actionButtons.add(b)
            return b
        }

        fun diploBtn(text: String, action: String): TextButton {
            val b = TextButton(text, skin)
            b.label.setFontScale(0.65f)
            b.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) { handleAction(action) }
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
            override fun clicked(event: InputEvent?, x: Float, y: Float) { handleAction("END_TURN") }
        })
        panel.add(endBtn).fillX().padLeft(10f)

        val menuBtn = TextButton("MENU", skin)
        menuBtn.label.setFontScale(0.75f); menuBtn.label.color = Color.LIGHT_GRAY
        menuBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { game.setScreen(MenuScreen(game)) }
        })
        panel.add(menuBtn).fillX().padLeft(10f)

        val quitBtn = TextButton(Locale.QUIT, skin)
        quitBtn.label.setFontScale(0.75f); quitBtn.label.color = Color.RED
        quitBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val win = Window("", skin)
                win.isModal = true; win.isMovable = true; win.pad(16f)
                win.add(Label(Locale.SAVE_QUESTION, skin)).colspan(2).row()
                val saveBtn = TextButton(Locale.SAVE, skin)
                saveBtn.label.setFontScale(0.9f); saveBtn.color = Color(0.3f, 0.6f, 0.3f, 1f)
                saveBtn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        win.remove()
                        showSaveDialog()
                        game.setScreen(MenuScreen(game))
                    }
                })
                val noSaveBtn = TextButton(Locale.NO_SAVE, skin)
                noSaveBtn.label.setFontScale(0.9f)
                noSaveBtn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        win.remove()
                        game.setScreen(MenuScreen(game))
                    }
                })
                val cancelBtn = TextButton(Locale.CANCEL, skin)
                cancelBtn.label.setFontScale(0.9f)
                cancelBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { win.remove() } })
                win.add(saveBtn).width(120f).padRight(8f)
                win.add(noSaveBtn).width(120f).padRight(8f)
                win.add(cancelBtn).width(120f)
                win.pack()
                win.setPosition(Gdx.graphics.width / 2f - win.width / 2f, Gdx.graphics.height / 2f - win.height / 2f)
                stage.addActor(win)
            }
        })
        panel.add(quitBtn).fillX().padLeft(10f)

        val saveBtn = TextButton("SAVE", skin)
        saveBtn.label.setFontScale(0.7f); saveBtn.label.color = Color.CYAN
        saveBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { showSaveDialog() }
        })
        panel.add(saveBtn).fillX().padLeft(10f)

        val loadBtn = TextButton("LOAD", skin)
        loadBtn.label.setFontScale(0.7f); loadBtn.label.color = Color.CYAN
        loadBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { showLoadDialog() }
        })
        panel.add(loadBtn).fillX().padLeft(20f)

        val diploPanel = Table(skin).apply { right().top().pad(10f); defaults().pad(2f) }
        diplomacyLabel = Label(Locale.DIPLOMACY, skin)
        diplomacyLabel.color = Color.CYAN
        diploPanel.add(diplomacyLabel).colspan(2).row()
        diploPanel.add(diploBtn("Alliance", "DIPLO_ALLIANCE")).fillX()
        diploPanel.add(diploBtn("Break", "DIPLO_BREAK")).fillX().row()
        diploPanel.add(diploBtn("Trade", "DIPLO_TRADE")).fillX()
        diploPanel.add(diploBtn("Cancel", "DIPLO_CANCEL_TRADE")).fillX().row()

        val techPanel = Table(skin).apply { left().bottom().pad(10f); defaults().pad(2f) }
        techLabel = Label(Locale.TECHS, skin)
        techLabel.color = Color.YELLOW
        techPanel.add(techLabel).colspan(2).row()
        for (tech in com.example.strategy.model.TECH_TREE) {
            val b = TextButton(tech.name.take(8), skin)
            b.label.setFontScale(0.55f)
            b.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) { handleAction("RESEARCH:${tech.type.name}") }
            })
            techButtons.add(b)
            techPanel.add(b).fillX().colspan(2).row()
        }

        val zoomPanel = Table(skin).apply { left().bottom().pad(10f); defaults().pad(3f) }
        zoomHintLabel = Label("", skin)
        zoomHintLabel!!.color = Color.LIGHT_GRAY
        zoomHintLabel!!.setFontScale(0.7f)
        zoomPanel.add(zoomHintLabel).colspan(2).row()

        val zoomBgPix = Pixmap(16, 16, Pixmap.Format.RGBA8888).apply { setColor(1f, 1f, 0.4f, 0.35f); fill() }
        val zoomBgTex = Texture(zoomBgPix); zoomBgPix.dispose()
        val zoomUp = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(zoomBgTex, 4, 4, 4, 4))
        val zoomDownPix = Pixmap(16, 16, Pixmap.Format.RGBA8888).apply { setColor(1f, 1f, 0.4f, 0.55f); fill() }
        val zoomDownTex = Texture(zoomDownPix); zoomDownPix.dispose()
        val zoomDown = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(zoomDownTex, 4, 4, 4, 4))
        val zoomBtnStyle = TextButton.TextButtonStyle().apply {
            font = skin.getFont("default-font")
            fontColor = Color.WHITE
            up = zoomUp
            down = zoomDown
        }
        skin.add("zoom-btn", zoomBtnStyle)

        val zoomInBtn = TextButton("+", zoomBtnStyle)
        zoomInBtn.label.setFontScale(1.2f)
        zoomInBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                camera.zoom = (camera.zoom - 0.1f).coerceIn(minZoom, maxZoom)
                camera.update()
                zoomHintLabel?.setText(Locale.ZOOM_HINT)
                zoomHintTimer = 2f
            }
        })
        val zoomOutBtn = TextButton("-", zoomBtnStyle)
        zoomOutBtn.label.setFontScale(1.2f)
        zoomOutBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                camera.zoom = (camera.zoom + 0.1f).coerceIn(minZoom, maxZoom)
                camera.update()
                zoomHintLabel?.setText(Locale.ZOOM_HINT)
                zoomHintTimer = 2f
            }
        })
        zoomPanel.add(zoomInBtn).width(50f)
        zoomPanel.add(zoomOutBtn).width(50f)

        root.add().expandY()
        root.add(diploPanel).right().top().pad(10f).padTop(40f).row()
        root.add(zoomPanel).left().bottom().pad(10f)
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
        diplomacyLabel.setText("${Locale.DIPLOMACY} $diploStatus${if (diplo.tradeActive) " + Trade" else ""}")

        if (selectedRegions.size > 1) {
            val totalPop = selectedRegions.sumOf { it.population }
            val totalAttack = selectedRegions.sumOf { it.population + it.units.totalAttack() }
            val totalDefense = selectedRegions.sumOf { it.population + it.units.totalDefense() + it.buildings.count { b -> b.type == BuildingType.WALL } * 5 }
            val names = selectedRegions.joinToString { it.name }
            infoLabel.setText("${Locale.SELECTED} ${selectedRegions.size} ${Locale.TERRITORIES.lowercase()}: $names\n${Locale.TOTAL_POP}: $totalPop | ${Locale.ATTACK}: $totalAttack | ${Locale.DEFENSE}: $totalDefense")
        } else if (r == null) {
            infoLabel.setText(Locale.CLICK_REGION)
        } else if (!state.fog.isExplored(0, r.id)) {
            infoLabel.setText(Locale.UNKNOWN_TERRITORY)
        } else {
            val owner = when (r.ownerId) { 0 -> Locale.YOURS; 1 -> Locale.ENEMY; else -> Locale.NEUTRAL }
            val buildings = if (r.buildings.isEmpty()) Locale.NO_BUILDINGS else r.buildings.joinToString { it.type.name }
            val attack = r.population + r.units.totalAttack()
            val defense = r.population + r.units.totalDefense() + r.buildings.count { it.type == BuildingType.WALL } * 5
            val unitInfo = if (r.units.units.isEmpty()) Locale.NO_UNITS else r.units.units.joinToString { "${it.count} ${it.type.name.lowercase()}" }
            infoLabel.setText("${r.name} | ${r.terrain} | $owner\n${Locale.POPULATION}: ${r.population} (${Locale.ATTACK}: $attack, ${Locale.DEFENSE}: $defense)\n${Locale.UNITS}: $unitInfo\n${Locale.BUILDINGS}: $buildings")
        }

        val aiInfo = when (com.example.strategy.ai.AISettings.backend) {
            com.example.strategy.ai.AISettings.Backend.NONE -> "AI: Fallback"
            com.example.strategy.ai.AISettings.Backend.OLLAMA -> "AI: Ollama (${com.example.strategy.ai.AISettings.ollamaModel})"
            com.example.strategy.ai.AISettings.Backend.LM_STUDIO -> "AI: LM Studio (${com.example.strategy.ai.AISettings.lmStudioModel.ifEmpty { "default" }})"
        }
        statusLabel.setText(
            "${Locale.TURN} ${state.turn} | ${player?.name ?: "?"}\n" +
            "${Locale.TERRITORIES}: ${Locale.YOURS} $myTerritories vs $enemyTerritories\n" +
            "${Locale.POPULATION}: ${Locale.YOURS} $myPop vs $enemyPop\n" +
            "${Locale.INCOME}: +${income.food}F +${income.wood}W +${income.stone}S +${income.gold}G | ${Locale.UPKEEP}: -${upkeep.food}F\n" +
            "$aiInfo\n" +
            when {
                actionUsedThisTurn -> Locale.ACTION_USED
                isMyTurn -> Locale.YOUR_TURN
                else -> Locale.WAITING
            }
        )
        statusLabel.color = when {
            actionUsedThisTurn -> Color.ORANGE
            isMyTurn -> Color.GREEN
            else -> Color.GRAY
        }

        val dimmed = actionUsedThisTurn || !isMyTurn
        for (b in actionButtons) b.color.a = if (dimmed) 0.3f else 1f
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
        if (!showStats) { statsLabel.setText(""); return }
        val history = state.history
        if (history.isEmpty()) { statsLabel.setText("No history yet"); return }
        statsLabel.setText("--- Stats ---\n" + history.takeLast(5).joinToString("\n") { h ->
            "T${h.turn}: ${h.territories} terr, ${h.population} pop, F${h.resources.food} W${h.resources.wood} S${h.resources.stone} G${h.resources.gold}"
        })
    }

    private fun showSaveDialog() {
        val win = Window(Locale.SAVE, skin)
        win.isModal = true; win.isMovable = true; win.pad(16f); win.defaults().pad(5f)
        win.add(Label(Locale.SELECT_SAVE, skin)).colspan(2).row()
        val nameField = TextField("save_${state.turn}", skin)
        win.add(nameField).colspan(2).width(250f).padBottom(8f).row()
        val saveBtn = TextButton(Locale.SAVE, skin)
        saveBtn.label.setFontScale(0.9f)
        saveBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val name = nameField.text.trim()
                if (name.isEmpty()) return
                if (SaveManager.save(state, name)) { statusLabel.setText("Saved: $name"); statusLabel.color = Color.CYAN }
                else { statusLabel.setText("Save FAILED!"); statusLabel.color = Color.RED }
                win.remove()
            }
        })
        win.add(saveBtn).width(120f)
        val cancelBtn = TextButton(Locale.CANCEL, skin)
        cancelBtn.label.setFontScale(0.9f)
        cancelBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { win.remove() } })
        win.add(cancelBtn).width(120f)
        win.pack()
        win.setPosition(Gdx.graphics.width / 2f - win.width / 2f, Gdx.graphics.height / 2f - win.height / 2f)
        stage.addActor(win)
    }

    private fun showLoadDialog() {
        val saves = SaveManager.listSaves()
        if (saves.isEmpty()) { statusLabel.setText(Locale.NO_SAVES); statusLabel.color = Color.RED; return }

        var win: Window? = null

        fun rebuildList(savesList: List<String>) {
            win?.remove()
            if (savesList.isEmpty()) return

            val w = Window(Locale.LOAD_GAME, skin)
            w.isModal = true; w.isMovable = true; w.pad(16f)
            w.add(Label(Locale.SELECT_SAVE, skin)).colspan(3).row()

            val listTable = Table(skin)
            for (saveName in savesList) {
                val nameBtn = TextButton(saveName, skin)
                nameBtn.label.setFontScale(0.8f)
                nameBtn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        val loaded = SaveManager.load(saveName)
                        if (loaded != null) {
                            state = loaded; game.gameState = state; selectedRegion = null; selectedRegions.clear()
                            actionUsedThisTurn = false; attackMode = false; attackSourceId = -1; moveMode = false; moveSourceId = -1
                            statusLabel.setText("Loaded: $saveName"); statusLabel.color = Color.CYAN
                            updateInfoLabel(); w.remove()
                        } else { statusLabel.setText("Load FAILED!"); statusLabel.color = Color.RED }
                    }
                })

                val trashRegion = makeTrashIcon()
                val delStyle = com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle()
                delStyle.imageUp = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(trashRegion.texture, 0, 0, 0, 0))
                delStyle.imageDown = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(trashRegion.texture, 0, 0, 0, 0))
                val delBtn = com.badlogic.gdx.scenes.scene2d.ui.ImageButton(delStyle)
                delBtn.image.setScale(1.2f)
                delBtn.color = Color(0.8f, 0.25f, 0.25f, 1f)
                delBtn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        val confirm = Window(Locale.CONFIRM_DELETE, skin)
                        confirm.isModal = true; confirm.isMovable = true; confirm.pad(16f)
                        confirm.add(Label("${Locale.CONFIRM_DELETE} \"$saveName\"?", skin)).row()
                        val yesBtn = TextButton(Locale.DELETE, skin)
                        yesBtn.label.setFontScale(0.9f); yesBtn.color = Color(0.8f, 0.2f, 0.2f, 1f)
                        yesBtn.addListener(object : ClickListener() {
                            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                                SaveManager.deleteSave(saveName)
                                confirm.remove()
                                val remaining = SaveManager.listSaves()
                                if (remaining.isEmpty()) { w.remove(); statusLabel.setText("All saves deleted"); statusLabel.color = Color.ORANGE }
                                else rebuildList(remaining)
                            }
                        })
                        val noBtn = TextButton(Locale.CANCEL, skin)
                        noBtn.label.setFontScale(0.9f)
                        noBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { confirm.remove() } })
                        confirm.add(yesBtn).width(100f).padRight(10f)
                        confirm.add(noBtn).width(100f)
                        confirm.pack()
                        confirm.setPosition(Gdx.graphics.width / 2f - confirm.width / 2f, Gdx.graphics.height / 2f - confirm.height / 2f)
                        stage.addActor(confirm)
                    }
                })

                listTable.add(nameBtn).width(200f).fillX().padRight(8f)
                listTable.add(delBtn).width(40f)
                listTable.row()
            }

            val scrollPane = com.badlogic.gdx.scenes.scene2d.ui.ScrollPane(listTable, skin)
            w.add(scrollPane).colspan(3).width(260f).height(180f).padBottom(8f).row()

            val closeBtn = TextButton(Locale.CLOSE, skin)
            closeBtn.label.setFontScale(0.9f)
            closeBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { w.remove() } })
            w.add(closeBtn).width(120f)

            w.pack()
            w.setPosition(Gdx.graphics.width / 2f - w.width / 2f, Gdx.graphics.height / 2f - w.height / 2f)
            stage.addActor(w)
            win = w
        }

        rebuildList(saves)
    }

    private fun makeTrashIcon(): TextureRegion {
        val p = Pixmap(24, 24, Pixmap.Format.RGBA8888)
        p.setColor(Color(0.9f, 0.2f, 0.2f, 1f))
        p.fillRectangle(5, 4, 14, 3)
        p.fillRectangle(7, 7, 2, 14)
        p.fillRectangle(11, 7, 2, 14)
        p.fillRectangle(15, 7, 2, 14)
        p.fillRectangle(3, 18, 18, 3)
        p.fillRectangle(9, 1, 6, 4)
        val t = Texture(p); p.dispose()
        return TextureRegion(t)
    }

    private fun handleAction(actionType: String) {
        try {
            if (actionType == "END_TURN") {
                state = com.example.strategy.logic.TurnManager.endTurn(state)
                game.gameState = state; selectedRegion = null; selectedRegions.clear(); actionUsedThisTurn = false
                attackMode = false; attackSourceId = -1; moveMode = false; moveSourceId = -1
                soundManager?.play(SoundManager.SoundType.END_TURN)
                runAITurns(); updateInfoLabel()
                return
            }
            if (actionUsedThisTurn || aiPending) return
            if (actionType.startsWith("DIPLO_")) { handleDiploAction(actionType); return }
            if (actionType.startsWith("RESEARCH:")) {
                val action = com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.RESEARCH, 0, actionType.removePrefix("RESEARCH:"))
                com.example.strategy.logic.ActionQueue.enqueue(action)
                state = com.example.strategy.logic.ActionQueue.processAll(state)
                game.gameState = state; actionUsedThisTurn = true
                soundManager?.play(SoundManager.SoundType.RESEARCH); updateInfoLabel()
                return
            }
            val region = selectedRegion
            if (region == null) return
            if (actionType == "ATTACK") {
                if (region.ownerId != state.currentPlayerId) return
                attackMode = true; attackSourceId = region.id
                infoLabel.setText("ATTACK MODE: Click enemy region to attack from ${region.name}")
                return
            }
            if (actionType == "MOVE") {
                if (region.ownerId != state.currentPlayerId) return
                moveMode = true; moveSourceId = region.id
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
            game.gameState = state; selectedRegion = state.map.getRegionById(region.id); actionUsedThisTurn = true
            updateInfoLabel()
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Action error: ${e.message}")
        }
    }

    private fun handleDiploAction(actionType: String) {
        if (actionUsedThisTurn) return
        val action = when (actionType) {
            "DIPLO_ALLIANCE" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.PROPOSE_ALLIANCE, 1)
            "DIPLO_BREAK" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.BREAK_ALLIANCE, 1)
            "DIPLO_TRADE" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.PROPOSE_TRADE, 1)
            "DIPLO_CANCEL_TRADE" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.CANCEL_TRADE, 1)
            else -> return
        }
        com.example.strategy.logic.ActionQueue.enqueue(action)
        state = com.example.strategy.logic.ActionQueue.processAll(state)
        game.gameState = state; actionUsedThisTurn = true
        soundManager?.play(SoundManager.SoundType.ALLIANCE); updateInfoLabel()
    }

    private fun runAITurns() {
        if (state.currentPlayerId == 0) return
        aiPending = true
        state = com.example.strategy.logic.TurnManager.startTurn(state)
        game.gameState = state
        Thread {
            try {
                val aiAction = com.example.strategy.ai.OllamaAI.decide(state)
                Gdx.app.postRunnable { if (alive) applyAIAction(aiAction) }
            } catch (e: Exception) {
                Gdx.app.postRunnable { if (alive) applyAIAction(null) }
            }
        }.start()
    }

    private fun applyAIAction(aiAction: com.example.strategy.ai.OllamaAI.AIAction?) {
        if (aiAction != null) {
            val action = com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, aiAction.actionType, aiAction.targetRegionId, aiAction.param)
            com.example.strategy.logic.ActionQueue.enqueue(action)
            state = com.example.strategy.logic.ActionQueue.processAll(state)
        }
        state = com.example.strategy.logic.TurnManager.endTurn(state)
        game.gameState = state; aiPending = false; updateInfoLabel()
    }

    override fun render(delta: Float) {
        try {
            Gdx.gl.glClearColor(0.1f, 0.15f, 0.3f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            if (isPanning) {
                if (!Gdx.input.isButtonPressed(1) && !Gdx.input.isButtonPressed(2)) isPanning = false
                else {
                    camera.translate((lastPanX - Gdx.input.x) * 1.5f * camera.zoom, (Gdx.input.y - lastPanY) * 1.5f * camera.zoom)
                    lastPanX = Gdx.input.x; lastPanY = Gdx.input.y
                }
            }

            camera.update()
            batch.projectionMatrix = camera.combined
            animTime += delta
            if (zoomHintTimer > 0f) {
                zoomHintTimer -= delta
                if (zoomHintTimer <= 0f) { zoomHintLabel?.setText(""); zoomHintTimer = 0f }
            }
            if (tutorialTimer > 0f) {
                tutorialTimer -= delta
                if (tutorialTimer <= 0f) { tutorialLabel?.remove(); tutorialTimer = 0f }
            }

            batch.begin()
            for (region in state.map.regions) {
                val explored = state.fog.isExplored(0, region.id)
                if (!explored) {
                    val x = region.tileX * tileSize
                    val y = (state.map.height - 1 - region.tileY) * tileSize
                    batch.setColor(0.1f, 0.1f, 0.15f, 1f)
                    batch.draw(tileTextures[TileKey(region.terrain, null)], x, y, tileSize, tileSize)
                    batch.setColor(Color.WHITE); continue
                }
                val key = TileKey(region.terrain, region.ownerId)
                val tr = tileTextures[key] ?: continue
                val x = region.tileX * tileSize
                val y = (state.map.height - 1 - region.tileY) * tileSize
                batch.draw(tr, x, y, tileSize, tileSize)

                if (region.terrain != TerrainType.WATER) {
                    val popText = "${region.population}"
                    val popColor = when { region.ownerId == 0 -> Color(0.5f, 0.8f, 1f, 0.9f); region.ownerId == 1 -> Color(1f, 0.5f, 0.5f, 0.9f); else -> Color.WHITE }
                    game.font.color = Color.BLACK; game.font.draw(batch, popText, x + tileSize - 28f, y + tileSize - 6f)
                    game.font.color = popColor; game.font.draw(batch, popText, x + tileSize - 30f, y + tileSize - 4f)

                    if (region.buildings.isNotEmpty()) {
                        val iconSize = tileSize * 0.28f; val gap = 2f
                        val totalW = region.buildings.size * iconSize + (region.buildings.size - 1) * gap
                        var sx = x + (tileSize - totalW) / 2f
                        for (building in region.buildings) { buildingIcons[building.type]?.let { batch.draw(it, sx, y + 4f, iconSize, iconSize) }; sx += iconSize + gap }
                    }
                    if (region.units.units.isNotEmpty()) {
                        val iconSize = tileSize * 0.22f; val gap = 2f
                        val unitList = region.units.units.filter { it.count > 0 }
                        val totalW = unitList.size * (iconSize + 8f) + (unitList.size - 1) * gap
                        var sx = x + (tileSize - totalW) / 2f
                        for (unit in unitList) {
                            unitIcons[unit.type]?.let { batch.draw(it, sx, y + 4f, iconSize, iconSize) }
                            val ct = "${unit.count}"
                            game.font.color = Color.BLACK; game.font.draw(batch, ct, sx + iconSize - 2f, y + 4f + iconSize - 2f)
                            game.font.color = Color.WHITE; game.font.draw(batch, ct, sx + iconSize - 4f, y + 4f + iconSize - 4f)
                            sx += iconSize + 8f + gap
                        }
                    }
                    if (region.ownerId == 0 && !actionUsedThisTurn) {
                        if (region.buildings.isEmpty() || region.buildings.any { it.type == BuildingType.BARRACKS }) {
                            val pulse = (kotlin.math.sin(animTime * 3f) * 0.2f + 0.35f)
                            batch.setColor(0.2f, 1f, 0.2f, pulse)
                            batch.draw(tileTextures[TileKey(region.terrain, region.ownerId)], x, y, tileSize, tileSize)
                            batch.setColor(Color.WHITE)
                        }
                    }
                }
            }
            selectedRegions.forEach { r ->
                val x = r.tileX * tileSize; val y = (state.map.height - 1 - r.tileY) * tileSize
                batch.setColor(1f, 1f, 0f, 0.85f)
                batch.draw(tileTextures[TileKey(r.terrain, r.ownerId)], x, y, tileSize, tileSize)
                batch.setColor(Color.WHITE)
            }
            batch.end()

            shapeRenderer.projectionMatrix = camera.combined
            shapeRenderer.color = Color.YELLOW
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            selectedRegions.forEach { r ->
                val x = r.tileX * tileSize; val y = (state.map.height - 1 - r.tileY) * tileSize
                shapeRenderer.rect(x + 1f, y + 1f, tileSize - 2f, tileSize - 2f)
            }
            shapeRenderer.end()

            if (isBoxSelecting) {
                val sx1 = minOf(boxStartScreenX.toFloat(), Gdx.input.x.toFloat())
                val sy1 = minOf((Gdx.graphics.height - boxStartScreenY).toFloat(), (Gdx.graphics.height - Gdx.input.y).toFloat())
                val sx2 = maxOf(boxStartScreenX.toFloat(), Gdx.input.x.toFloat())
                val sy2 = maxOf((Gdx.graphics.height - boxStartScreenY).toFloat(), (Gdx.graphics.height - Gdx.input.y).toFloat())
                val sw = sx2 - sx1; val sh = sy2 - sy1
                if (sw > 0f && sh > 0f) {
                    val screenProj = com.badlogic.gdx.math.Matrix4().apply { setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()) }
                    shapeRenderer.projectionMatrix = screenProj
                    shapeRenderer.color = Color(0.5f, 1f, 0.5f, 0.3f)
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                    shapeRenderer.rect(sx1, sy1, sw, sh)
                    shapeRenderer.end()
                    shapeRenderer.color = Color(0.5f, 1f, 0.5f, 1f)
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                    shapeRenderer.rect(sx1, sy1, sw, sh)
                    shapeRenderer.end()
                }
            }

            animManager.update(delta)
            if (animManager.hasAnimations()) { shapeRenderer.projectionMatrix = camera.combined; animManager.render(shapeRenderer, tileSize) }

            stage.act(delta); stage.draw()

            game.batch.begin()
            val player = state.currentPlayer()
            val resText = "Food: ${player?.resources?.food ?: 0}   Wood: ${player?.resources?.wood ?: 0}   Stone: ${player?.resources?.stone ?: 0}   Gold: ${player?.resources?.gold ?: 0}   Iron: ${player?.resources?.iron ?: 0}"
            val resLayout = GlyphLayout(game.font, resText)
            game.font.color = Color.WHITE
            game.font.draw(game.batch, resText, Gdx.graphics.width - resLayout.width - 12f, Gdx.graphics.height - 12f)
            game.batch.end()
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Render error: ${e.message}", e)
        }
    }

    override fun hide() { alive = false }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat(); camera.viewportHeight = height.toFloat(); camera.update()
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        batch.dispose(); shapeRenderer.dispose(); soundManager?.dispose()
        tileTextures.values.forEach { it.texture.dispose() }
        buildingIcons.values.forEach { it.texture.dispose() }
        unitIcons.values.forEach { it.texture.dispose() }
        stage.dispose(); skin.dispose()
    }

    private fun generateFont(): BitmapFont {
        val fontPaths = arrayOf(
            "/System/Library/Fonts/Supplemental/Arial.ttf",
            "/System/Library/Fonts/Helvetica.ttc",
            "/Library/Fonts/Arial.ttf"
        )
        var generator: com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator? = null
        for (path in fontPaths) {
            if (java.io.File(path).exists()) {
                generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(Gdx.files.absolute(path))
                break
            }
        }
        if (generator == null) {
            generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(Gdx.files.absolute(fontPaths[0]))
        }
        val params = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter()
        params.size = 16
        params.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        params.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        params.characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,*':?!@#$%&()-+=/<>" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "äöüÄÖÜß «»—…"
        val font = generator.generateFont(params)
        generator.dispose()
        return font
    }

    private fun createSkin(): Skin {
        val s = Skin()
        val font = generateFont()
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

        val windowBgPix = Pixmap(32, 32, Pixmap.Format.RGBA8888).apply { setColor(0.08f, 0.09f, 0.14f, 1f); fill() }
        val windowBgTex = Texture(windowBgPix); windowBgPix.dispose()
        val windowBg = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(windowBgTex, 4, 4, 4, 4))
        val bf = font
        s.add("default", Window.WindowStyle(bf, Color.CYAN, windowBg))
        s.add("default", TextField.TextFieldStyle().apply {
            this.font = bf; fontColor = Color.WHITE; background = windowBg
            cursor = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1))
            selection = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1))
        })
        val listSelTex = Texture(Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(0.3f, 0.5f, 0.7f, 1f); fill() })
        val listStyle = com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle()
        listStyle.javaClass.getDeclaredField("font").apply { isAccessible = true }.set(listStyle, bf)
        listStyle.selection = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(listSelTex, 0, 0, 0, 0))
        listStyle.fontColorSelected = Color.WHITE; listStyle.fontColorUnselected = Color.LIGHT_GRAY
        s.add("default", listStyle)
        s.add("default", com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle().apply {
            background = windowBg
            vScroll = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1))
            hScroll = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1))
            vScrollKnob = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(downTex, 1, 1, 1, 1))
            hScrollKnob = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(downTex, 1, 1, 1, 1))
        })

        return s
    }
}
