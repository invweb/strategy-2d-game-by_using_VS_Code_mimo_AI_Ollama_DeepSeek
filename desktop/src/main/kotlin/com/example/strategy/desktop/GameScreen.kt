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

    private lateinit var infoLabel: Label
    private lateinit var statusLabel: Label
    private lateinit var skin: Skin
    private val actionButtons = mutableListOf<TextButton>()
    private val buildButtons = mutableListOf<TextButton>()

    private val tileTextures = mutableMapOf<TileKey, TextureRegion>()
    private val buildingIcons = mutableMapOf<BuildingType, TextureRegion>()
    private data class TileKey(val terrain: TerrainType, val ownerId: Int?)

    override fun show() {
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

        // All input in one processor: map clicks + camera pan + zoom
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
                        updateInfoLabel()
                    }
                }
                return false // don't consume — let Stage handle UI buttons too
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

        // Stage handles UI buttons; mapInput handles map clicks, pan, zoom
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
                        // Rolling hills — two overlapping mounds
                        pix.setColor(0.52f, 0.48f, 0.3f, 1f)
                        pix.fillCircle(40, 100, 36)
                        pix.fillCircle(88, 108, 32)
                        pix.fillCircle(20, 112, 26)
                        // Lighter tops
                        pix.setColor(0.6f, 0.55f, 0.38f, 1f)
                        pix.fillCircle(40, 90, 24)
                        pix.fillCircle(88, 98, 20)
                        pix.fillCircle(20, 102, 18)
                        // Grass highlights
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

    // Building icons — sprites drawn in center of tile
    private fun generateBuildingIcons() {
        val s = 48
        fun makeIcon(draw: (Pixmap) -> Unit): TextureRegion {
            val p = Pixmap(s, s, Pixmap.Format.RGBA8888)
            draw(p)
            val t = Texture(p); p.dispose()
            return TextureRegion(t)
        }

        // FARM — green field with yellow wheat
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

        // LUMBER_MILL — tree + saw
        buildingIcons[BuildingType.LUMBER_MILL] = makeIcon { p ->
            // Tree trunk
            p.setColor(0.45f, 0.3f, 0.1f, 1f)
            p.fillRectangle(18, 16, 8, 22)
            // Foliage
            p.setColor(0.15f, 0.45f, 0.15f, 1f)
            p.fillCircle(22, 12, 14)
            // Saw blade
            p.setColor(0.75f, 0.75f, 0.75f, 1f)
            p.fillCircle(36, 30, 8)
            p.setColor(0.9f, 0.9f, 0.9f, 1f)
            p.fillCircle(36, 30, 4)
        }

        // QUARRY — stone blocks
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

        // MINE — cave entrance + pickaxe
        buildingIcons[BuildingType.MINE] = makeIcon { p ->
            // Cave
            p.setColor(0.35f, 0.3f, 0.25f, 1f)
            p.fillRectangle(4, 12, 40, 28)
            p.setColor(0.15f, 0.12f, 0.1f, 1f)
            p.fillCircle(24, 28, 12)
            // Support beam
            p.setColor(0.5f, 0.35f, 0.15f, 1f)
            p.fillRectangle(10, 8, 4, 32)
            p.fillRectangle(34, 8, 4, 32)
            p.fillRectangle(10, 8, 28, 4)
            // Rail tracks
            p.setColor(0.6f, 0.6f, 0.6f, 1f)
            p.fillRectangle(14, 36, 20, 2)
        }

        // MARKET — stall with goods
        buildingIcons[BuildingType.MARKET] = makeIcon { p ->
            // Stall roof
            p.setColor(0.8f, 0.2f, 0.2f, 1f)
            p.fillTriangle(6, 8, 24, 2, 42, 8)
            // Posts
            p.setColor(0.45f, 0.3f, 0.1f, 1f)
            p.fillRectangle(10, 8, 3, 28)
            p.fillRectangle(35, 8, 3, 28)
            // Counter
            p.setColor(0.55f, 0.4f, 0.2f, 1f)
            p.fillRectangle(8, 20, 32, 4)
            // Goods (gold)
            p.setColor(0.95f, 0.8f, 0.1f, 1f)
            p.fillCircle(16, 18, 4)
            p.fillCircle(24, 16, 4)
            p.fillCircle(32, 18, 4)
        }

        // BARRACKS — shield + sword
        buildingIcons[BuildingType.BARRACKS] = makeIcon { p ->
            // Shield
            p.setColor(0.7f, 0.15f, 0.15f, 1f)
            p.fillCircle(18, 24, 14)
            p.setColor(0.9f, 0.85f, 0.1f, 1f)
            p.fillCircle(18, 24, 8)
            // Sword
            p.setColor(0.75f, 0.75f, 0.8f, 1f)
            p.fillRectangle(30, 4, 4, 36)
            p.fillRectangle(26, 6, 12, 4)
            p.setColor(0.5f, 0.35f, 0.1f, 1f)
            p.fillRectangle(30, 36, 4, 8)
        }

        // WALL — stone fortification
        buildingIcons[BuildingType.WALL] = makeIcon { p ->
            p.setColor(0.55f, 0.52f, 0.48f, 1f)
            p.fillRectangle(2, 20, 44, 20)
            // Battlements
            p.fillRectangle(2, 12, 8, 10)
            p.fillRectangle(16, 12, 8, 10)
            p.fillRectangle(30, 12, 8, 10)
            // Mortar lines
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
        topPanel.add(infoLabel).row()
        topPanel.add(statusLabel).row()
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

        val buildFarmBtn = btn("Farm (10F 5W)", "BUILD_FARM"); buildButtons.add(buildFarmBtn)
        val buildLumberBtn = btn("Lumber Mill (15W)", "BUILD_LUMBER_MILL"); buildButtons.add(buildLumberBtn)
        val buildBarracksBtn = btn("Barracks (15W 10S 10G)", "BUILD_BARRACKS"); buildButtons.add(buildBarracksBtn)
        val buildMineBtn = btn("Mine (5W 15S 5I)", "BUILD_MINE"); buildButtons.add(buildMineBtn)

        panel.add(buildFarmBtn).fillX()
        panel.add(buildLumberBtn).fillX()
        panel.add(buildBarracksBtn).fillX().row()
        panel.add(buildMineBtn).fillX()
        panel.add(btn("Recruit (10F 5G)", "RECRUIT")).fillX()
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
        root.add(panel).right().bottom().pad(10f)
        stage.addActor(root)
    }

    private fun updateInfoLabel() {
        val r = selectedRegion
        val player = state.currentPlayer()
        val isMyTurn = state.currentPlayerId == 0

        // Territory stats
        val myTerritories = state.map.regions.count { it.ownerId == 0 }
        val enemyTerritories = state.map.regions.count { it.ownerId == 1 }
        val myPop = state.map.regions.filter { it.ownerId == 0 }.sumOf { it.population }
        val enemyPop = state.map.regions.filter { it.ownerId == 1 }.sumOf { it.population }

        // Income per turn
        val income = com.example.strategy.logic.Economy.calculateIncome(player!!, state.map)
        val upkeep = com.example.strategy.logic.Economy.upkeepCost(player, state.map)

        if (r == null) {
            infoLabel.setText("Click a region to select")
        } else {
            val owner = when (r.ownerId) { 0 -> "Yours"; 1 -> "Enemy"; else -> "Neutral" }
            val buildings = if (r.buildings.isEmpty()) "No buildings" else r.buildings.joinToString { it.type.name }
            val attack = r.population
            val defense = r.population + r.buildings.count { it.type == com.example.strategy.model.BuildingType.WALL } * 5
            infoLabel.setText(
                "${r.name} | ${r.terrain} | $owner\n" +
                "Population: ${r.population} (Attack: $attack, Defense: $defense)\n" +
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
                isMyTurn -> "YOUR TURN — choose an action"
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
    }

    private fun handleAction(actionType: String) {
        try {
            if (actionType == "END_TURN") {
                state = com.example.strategy.logic.TurnManager.endTurn(state)
                game.gameState = state; selectedRegion = null; actionUsedThisTurn = false
                attackMode = false; attackSourceId = -1
                moveMode = false; moveSourceId = -1
                runAITurns()
                updateInfoLabel()
                return
            }
            if (actionUsedThisTurn) {
                Gdx.app.log("GameScreen", "Already used action this turn!")
                return
            }
            val region = selectedRegion
            if (region == null) {
                Gdx.app.log("GameScreen", "No region selected!")
                return
            }
            // Block build if region already has a building
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
                "DEVELOP" -> com.example.strategy.logic.ActionQueue.GameAction(state.currentPlayerId, com.example.strategy.logic.ActionQueue.ActionType.DEVELOP, region.id)
                else -> return
            }
            com.example.strategy.logic.ActionQueue.enqueue(action)
            state = com.example.strategy.logic.ActionQueue.processAll(state)
            game.gameState = state
            selectedRegion = state.map.getRegionById(region.id)
            actionUsedThisTurn = true
            updateInfoLabel()
            Gdx.app.log("GameScreen", "Action done: $actionType on ${region.name}")
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Action error: ${e.message}")
        }
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

            batch.begin()
            for (region in state.map.regions) {
                val key = TileKey(region.terrain, region.ownerId)
                val tr = tileTextures[key] ?: continue
                val x = region.tileX * tileSize
                val y = (state.map.height - 1 - region.tileY) * tileSize
                batch.draw(tr, x, y, tileSize, tileSize)

                if (region.terrain != TerrainType.WATER) {
                    // Population number — top-right corner of tile
                    val popText = "${region.population}"
                    val popColor = when {
                        region.ownerId == 0 -> Color(0.5f, 0.8f, 1f, 0.9f)
                        region.ownerId == 1 -> Color(1f, 0.5f, 0.5f, 0.9f)
                        else -> Color.WHITE
                    }
                    // Draw population label
                    game.font.color = Color.BLACK
                    game.font.draw(batch, popText, x + tileSize - 28f, y + tileSize - 6f)
                    game.font.color = popColor
                    game.font.draw(batch, popText, x + tileSize - 30f, y + tileSize - 4f)

                    // Building icons — small row at bottom of tile
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
