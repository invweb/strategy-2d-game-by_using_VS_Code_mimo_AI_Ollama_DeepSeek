package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.strategy.model.*
import com.example.strategy.logic.ActionQueue
import com.example.strategy.logic.TurnManager

class NetworkGameScreen(
    private val game: StrategyGame,
    private val networkClient: NetworkClient,
    private val myPlayerId: Int
) : ScreenAdapter() {

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
    private var gameOver = false

    private val tileSize = 128f
    private val animManager = AnimationManager()
    private var soundManager: SoundManager? = null
    private var animTime = 0f
    private var aiPending = false
    private var alive = false

    private lateinit var mapRenderer: MapRenderer
    private lateinit var gameInput: GameInput

    private lateinit var infoLabel: Label
    private lateinit var statusLabel: Label
    private lateinit var diplomacyLabel: Label
    private lateinit var statsLabel: Label
    private lateinit var skin: Skin
    private var showStats = false
    private val actionButtons = mutableListOf<TextButton>()
    private val buildButtons = mutableListOf<TextButton>()
    private val diploButtons = mutableListOf<TextButton>()
    private val techButtons = mutableListOf<TextButton>()

    override fun show() {
        alive = true
        soundManager?.dispose()
        soundManager = SoundManager()
        skin = createSkin()
        stage = Stage(ScreenViewport())

        mapRenderer = MapRenderer(batch, shapeRenderer, tileSize, game)
        mapRenderer.generateAll()

        gameInput = GameInput(
            game, camera, stage, tileSize,
            stateProvider = { state },
            stateSetter = { state = it; game.gameState = it; sendStateUpdate() },
            selectedRegionProvider = { selectedRegion },
            selectedRegionSetter = { selectedRegion = it },
            selectedRegionsProvider = { selectedRegions },
            actionUsedThisTurnProvider = { actionUsedThisTurn },
            actionUsedThisTurnSetter = { actionUsedThisTurn = it },
            attackModeProvider = { attackMode },
            attackModeSetter = { attackMode = it },
            attackSourceIdProvider = { attackSourceId },
            attackSourceIdSetter = { attackSourceId = it },
            moveModeProvider = { moveMode },
            moveModeSetter = { moveMode = it },
            moveSourceIdProvider = { moveSourceId },
            moveSourceIdSetter = { moveSourceId = it },
            aiPendingProvider = { aiPending },
            updateInfoLabel = { updateInfoLabel() },
            infoLabelSetter = { infoLabel.setText(it) },
            statusLabelSetter = { statusLabel.setText(it) },
            statusLabelColorSetter = { statusLabel.color = it },
            soundPlayer = { soundManager?.play(it) },
            animProvider = { animManager },
            mapRenderer = mapRenderer
        )

        buildUI()
        gameInput.setup()

        val mapPixelW = state.map.width * tileSize
        val mapPixelH = state.map.height * tileSize
        camera.position.set(mapPixelW / 2f, mapPixelH / 2f, 0f)
        camera.zoom = 1.2f
        camera.viewportWidth = Gdx.graphics.width.toFloat()
        camera.viewportHeight = Gdx.graphics.height.toFloat()
        camera.update()

        // Setup network message handler
        networkClient.onMessage = { msg -> handleNetworkMessage(msg) }

        updateInfoLabel()
    }

    private fun handleNetworkMessage(message: NetworkClient.ServerMessage) {
        when (message) {
            is NetworkClient.ServerMessage.TurnUpdate -> {
                state = TurnManager.startTurn(state)
                game.gameState = state
                actionUsedThisTurn = false
                updateInfoLabel()
            }
            is NetworkClient.ServerMessage.ActionApplied -> {
                state = game.gameState
                updateInfoLabel()
            }
            is NetworkClient.ServerMessage.Error -> {
                statusLabel.setText("${Locale.ERROR} ${message.message}")
                statusLabel.color = Color.RED
            }
            is NetworkClient.ServerMessage.OpponentDisconnected -> {
                statusLabel.setText(Locale.OPPONENT_DISCONNECTED)
                statusLabel.color = Color.ORANGE
            }
            is NetworkClient.ServerMessage.GameStarted -> {
                state = TurnManager.startTurn(state)
                game.gameState = state
                updateInfoLabel()
            }
            else -> {}
        }
    }

    private fun sendStateUpdate() {
        if (networkClient.connected) {
            networkClient.sendGameState(state, "EndTurn")
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

        panel.add(btn(Locale.BUILD_FARM, Actions.BUILD_FARM)).fillX()
        panel.add(btn(Locale.BUILD_LUMBER, Actions.BUILD_LUMBER_MILL)).fillX()
        panel.add(btn(Locale.BUILD_BARRACKS_COST, Actions.BUILD_BARRACKS)).fillX().row()
        panel.add(btn(Locale.BUILD_MINE_COST, Actions.BUILD_MINE)).fillX()
        panel.add(btn(Locale.RECRUIT_COST, Actions.RECRUIT)).fillX()
        panel.add(btn(Locale.RECRUIT_INFANTRY_COST, Actions.RECRUIT_INFANTRY)).fillX().row()
        panel.add(btn(Locale.RECRUIT_CAVALRY_COST, Actions.RECRUIT_CAVALRY)).fillX().row()
        panel.add(btn(Locale.RECRUIT_SIEGE_COST, Actions.RECRUIT_SIEGE)).fillX()
        panel.add(btn(Locale.DEVELOP_COST, Actions.DEVELOP)).fillX()
        panel.add(btn(Locale.MOVE_BTN, Actions.MOVE)).fillX().row()
        panel.add(btn(Locale.ATTACK_BTN, Actions.ATTACK)).fillX()

        val endBtn = TextButton(Locale.END_TURN, skin)
        endBtn.label.setFontScale(0.75f); endBtn.label.color = Color.GOLD
        endBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { handleAction(Actions.END_TURN) }
        })
        panel.add(endBtn).fillX().padLeft(10f)

        val backBtn = TextButton(Locale.MENU, skin)
        backBtn.label.setFontScale(0.75f); backBtn.label.color = Color.LIGHT_GRAY
        backBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                networkClient.disconnect()
                game.setScreen(LobbyScreen(game))
            }
        })
        panel.add(backBtn).fillX().padLeft(10f)

        root.add().expandY()
        root.add(panel).right().bottom().pad(10f)
        stage.addActor(root)
    }

    private fun updateInfoLabel() {
        val r = selectedRegion
        val player = state.currentPlayer()
        val isMyTurn = state.currentPlayerId == myPlayerId

        val myTerritories = state.map.regions.count { it.ownerId == myPlayerId }
        val enemyTerritories = state.map.regions.count { it.ownerId != myPlayerId && it.terrain != TerrainType.WATER }

        if (r == null) {
            infoLabel.setText(Locale.CLICK_REGION)
        } else if (!state.fog.isExplored(myPlayerId, r.id)) {
            infoLabel.setText(Locale.UNKNOWN_TERRITORY)
        } else {
            val owner = when (r.ownerId) { myPlayerId -> Locale.YOURS; null -> Locale.NEUTRAL; else -> Locale.ENEMY }
            val buildings = if (r.buildings.isEmpty()) Locale.NO_BUILDINGS else r.buildings.joinToString { it.type.name }
            val attack = r.population + r.units.totalAttack()
            val defense = r.population + r.units.totalDefense() + r.buildings.count { it.type == BuildingType.WALL } * 5
            val unitInfo = if (r.units.units.isEmpty()) Locale.NO_UNITS else r.units.units.joinToString { "${it.count} ${it.type.name.lowercase()}" }
            infoLabel.setText("${r.name} | ${r.terrain} | $owner\n${Locale.POPULATION}: ${r.population} (${Locale.ATTACK}: $attack, ${Locale.DEFENSE}: $defense)\n${Locale.UNITS}: $unitInfo\n${Locale.BUILDINGS}: $buildings")
        }

        statusLabel.setText(
            "${Locale.TURN} ${state.turn} | My ID: $myPlayerId\n" +
            "${Locale.TERRITORIES}: You $myTerritories vs Enemy $enemyTerritories\n" +
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
    }

    private fun handleAction(actionType: String) {
        try {
            if (actionType == Actions.END_TURN) {
                state = TurnManager.endTurn(state)
                game.gameState = state; selectedRegion = null; selectedRegions.clear(); actionUsedThisTurn = false
                attackMode = false; attackSourceId = -1; moveMode = false; moveSourceId = -1
                soundManager?.play(SoundManager.SoundType.END_TURN)
                sendStateUpdate()
                updateInfoLabel()
                return
            }
            if (actionUsedThisTurn || gameOver) return
            if (actionType.startsWith("DIPLO_")) { handleDiploAction(actionType); return }
            if (actionType.startsWith(Actions.RESEARCH + ":")) {
                val action = ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.RESEARCH, 0, actionType.removePrefix(Actions.RESEARCH + ":"))
                ActionQueue.DEFAULT.enqueue(action); state = ActionQueue.DEFAULT.processAll(state)
                game.gameState = state; actionUsedThisTurn = true
                soundManager?.play(SoundManager.SoundType.RESEARCH)
                networkClient.sendGameState(state, "ActionApplied")
                updateInfoLabel()
                return
            }
            val region = selectedRegion
            if (region == null) return
            if (actionType == Actions.ATTACK) {
                if (region.ownerId != state.currentPlayerId) return
                attackMode = true; attackSourceId = region.id
                infoLabel.setText("${Locale.ATTACK_MODE} ${region.name}")
                return
            }
            if (actionType == Actions.MOVE) {
                if (region.ownerId != state.currentPlayerId) return
                moveMode = true; moveSourceId = region.id
                infoLabel.setText("${Locale.MOVE_MODE} ${region.name}")
                return
            }
            val action = when (actionType) {
                Actions.BUILD_FARM -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.BUILD, region.id, "FARM")
                Actions.BUILD_LUMBER_MILL -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.BUILD, region.id, "LUMBER_MILL")
                Actions.BUILD_BARRACKS -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.BUILD, region.id, "BARRACKS")
                Actions.BUILD_MINE -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.BUILD, region.id, "MINE")
                Actions.RECRUIT -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.RECRUIT, region.id)
                Actions.RECRUIT_INFANTRY -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.RECRUIT_INFANTRY, region.id)
                Actions.RECRUIT_CAVALRY -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.RECRUIT_CAVALRY, region.id)
                Actions.RECRUIT_SIEGE -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.RECRUIT_SIEGE, region.id)
                Actions.DEVELOP -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.DEVELOP, region.id)
                else -> return
            }
        ActionQueue.DEFAULT.enqueue(action); state = ActionQueue.DEFAULT.processAll(state)
            game.gameState = state; selectedRegion = state.map.getRegionById(region.id); actionUsedThisTurn = true
            networkClient.sendGameState(state, "ActionApplied")
            updateInfoLabel()
        } catch (e: Exception) {
            Gdx.app.error("NetworkGameScreen", "Action error: ${e.message}")
        }
    }

    private fun handleDiploAction(actionType: String) {
        if (actionUsedThisTurn) return
        val action = when (actionType) {
            Actions.DIPLO_ALLIANCE -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.PROPOSE_ALLIANCE, 1)
            Actions.DIPLO_BREAK -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.BREAK_ALLIANCE, 1)
            Actions.DIPLO_TRADE -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.PROPOSE_TRADE, 1)
            Actions.DIPLO_CANCEL_TRADE -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.CANCEL_TRADE, 1)
            else -> return
        }
        ActionQueue.DEFAULT.enqueue(action); state = ActionQueue.DEFAULT.processAll(state)
        game.gameState = state; actionUsedThisTurn = true
        soundManager?.play(SoundManager.SoundType.ALLIANCE)
        networkClient.sendGameState(state, "ActionApplied")
        updateInfoLabel()
    }

    override fun render(delta: Float) {
        try {
            Gdx.gl.glClearColor(0.1f, 0.15f, 0.3f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            camera.update()
            batch.projectionMatrix = camera.combined
            animTime += delta

            mapRenderer.drawTiles(state, animTime, actionUsedThisTurn, selectedRegions)
            mapRenderer.drawSelectionBox(gameInput.isBoxSelecting, gameInput.boxStartScreenX, gameInput.boxStartScreenY)

            animManager.update(delta)
            if (animManager.hasAnimations()) {
                shapeRenderer.projectionMatrix = camera.combined
                animManager.render(shapeRenderer, tileSize)
            }

            stage.act(delta); stage.draw()

            game.batch.begin()
            val player = state.currentPlayer()
            val resText = "Food: ${player?.resources?.food ?: 0}   Wood: ${player?.resources?.wood ?: 0}   Stone: ${player?.resources?.stone ?: 0}   Gold: ${player?.resources?.gold ?: 0}   Iron: ${player?.resources?.iron ?: 0}"
            val resLayout = GlyphLayout(game.font, resText)
            game.font.color = Color.WHITE
            game.font.draw(game.batch, resText, Gdx.graphics.width - resLayout.width - 12f, Gdx.graphics.height - 12f)
            game.batch.end()
        } catch (e: Exception) {
            Gdx.app.error("NetworkGameScreen", "Render error: ${e.message}", e)
        }
    }

    override fun hide() { alive = false }
    override fun resize(width: Int, height: Int) { camera.viewportWidth = width.toFloat(); camera.viewportHeight = height.toFloat(); camera.update(); stage.viewport.update(width, height, true) }
    override fun dispose() { batch.dispose(); shapeRenderer.dispose(); soundManager?.dispose(); mapRenderer.dispose(); stage.dispose(); skin.dispose() }

    private fun createSkin(): Skin = SkinFactory.createSkin()
}
