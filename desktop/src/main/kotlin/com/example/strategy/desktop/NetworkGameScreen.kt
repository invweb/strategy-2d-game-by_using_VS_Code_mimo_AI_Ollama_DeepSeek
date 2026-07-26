package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
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
    private lateinit var gameUI: GameUI
    private lateinit var skin: Skin

    override fun show() {
        alive = true
        soundManager?.dispose()
        soundManager = SoundManager()
        skin = SkinFactory.createSkin()
        stage = Stage(ScreenViewport())

        mapRenderer = MapRenderer(batch, shapeRenderer, tileSize, game)
        mapRenderer.generateAll()

        gameUI = GameUI(
            stage = stage,
            skin = skin,
            stateProvider = { state },
            selectedRegionProvider = { selectedRegion },
            selectedRegionsProvider = { selectedRegions },
            actionUsedThisTurnProvider = { actionUsedThisTurn },
            aiPendingProvider = { aiPending },
            gameOverProvider = { gameOver },
            actionHandler = { handleAction(it) },
            undoHandler = { },
            menuHandler = { networkClient.disconnect(); game.setScreen(LobbyScreen(game)) },
            camera = camera,
            soundPlayer = { soundManager?.play(it) },
            stateSetter = { state = it; game.gameState = it; sendStateUpdate() },
            resetMode = { resetMode() },
            infoLabelRef = { gameUI.infoLabel },
            statusLabelRef = { gameUI.statusLabel }
        )
        gameUI.build()

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
            updateInfoLabel = { gameUI.updateInfoLabel() },
            infoLabelSetter = { gameUI.infoLabel.setText(it) },
            statusLabelSetter = { gameUI.statusLabel.setText(it) },
            statusLabelColorSetter = { gameUI.statusLabel.color = it },
            soundPlayer = { soundManager?.play(it) },
            animProvider = { animManager },
            mapRenderer = mapRenderer
        )
        gameInput.setup()

        val mapPixelW = state.map.width * tileSize
        val mapPixelH = state.map.height * tileSize
        camera.position.set(mapPixelW / 2f, mapPixelH / 2f, 0f)
        camera.zoom = 1.2f
        camera.viewportWidth = Gdx.graphics.width.toFloat()
        camera.viewportHeight = Gdx.graphics.height.toFloat()
        camera.update()

        networkClient.onMessage = { msg -> handleNetworkMessage(msg) }
        gameUI.updateInfoLabel()
    }

    private fun resetMode() {
        selectedRegion = null; selectedRegions.clear()
        actionUsedThisTurn = false
        attackMode = false; attackSourceId = -1
        moveMode = false; moveSourceId = -1
        gameInput.clearReachable()
        gameUI.updateInfoLabel()
    }

    private fun handleNetworkMessage(message: NetworkClient.ServerMessage) {
        when (message) {
            is NetworkClient.ServerMessage.TurnUpdate -> {
                val turnResult = TurnManager.startTurn(state)
                state = turnResult.state
                game.gameState = state
                actionUsedThisTurn = false
                gameUI.updateInfoLabel()
            }
            is NetworkClient.ServerMessage.ActionApplied -> {
                state = game.gameState
                gameUI.updateInfoLabel()
            }
            is NetworkClient.ServerMessage.Error -> {
                gameUI.statusLabel.setText("${Locale.ERROR} ${message.message}")
                gameUI.statusLabel.color = Color.RED
            }
            is NetworkClient.ServerMessage.OpponentDisconnected -> {
                gameUI.statusLabel.setText(Locale.OPPONENT_DISCONNECTED)
                gameUI.statusLabel.color = Color.ORANGE
            }
            is NetworkClient.ServerMessage.GameStarted -> {
                val turnResult = TurnManager.startTurn(state)
                state = turnResult.state
                game.gameState = state
                gameUI.updateInfoLabel()
            }
            else -> {}
        }
    }

    private fun sendStateUpdate() {
        if (networkClient.connected) {
            networkClient.sendGameState(state, "EndTurn")
        }
    }

    private fun handleAction(actionType: String) {
        try {
            if (actionType == Actions.END_TURN) {
                state = TurnManager.endTurn(state)
                game.gameState = state; selectedRegion = null; selectedRegions.clear(); actionUsedThisTurn = false
                attackMode = false; attackSourceId = -1; moveMode = false; moveSourceId = -1
                gameInput.clearReachable()
                soundManager?.play(SoundManager.SoundType.END_TURN)
                sendStateUpdate()
                gameUI.updateInfoLabel()
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
                gameUI.updateInfoLabel()
                return
            }
            val region = selectedRegion
            if (region == null) return
            if (actionType == Actions.ATTACK) {
                if (region.ownerId != state.currentPlayerId) return
                attackMode = true; attackSourceId = region.id
                gameUI.infoLabel.setText("${Locale.ATTACK_MODE} ${region.name}")
                return
            }
            if (actionType == Actions.MOVE) {
                if (region.ownerId != state.currentPlayerId) return
                moveMode = true; moveSourceId = region.id
                gameInput.calculateReachable(region.id)
                gameUI.infoLabel.setText("${Locale.MOVE_MODE} ${region.name}")
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
            gameUI.updateInfoLabel()
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
        gameUI.updateInfoLabel()
    }

    override fun render(delta: Float) {
        try {
            Gdx.gl.glClearColor(0.1f, 0.15f, 0.3f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            camera.update()
            batch.projectionMatrix = camera.combined
            animTime += delta
            gameUI.update(delta)
            gameUI.updateEvent(delta)

            mapRenderer.drawTiles(state, animTime, actionUsedThisTurn, selectedRegions, gameInput.reachableRegions)
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
}
