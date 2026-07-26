package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.strategy.model.*
import com.example.strategy.logic.ActionQueue
import com.example.strategy.logic.Economy
import com.example.strategy.logic.TurnManager

class GameScreen(private val game: StrategyGame) : ScreenAdapter() {

    private lateinit var stage: com.badlogic.gdx.scenes.scene2d.Stage
    private val camera = OrthographicCamera()
    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()

    private var selectedRegion: Region? = null
    private val selectedRegions = mutableListOf<Region>()
    private var state = game.gameState
    private val undoStack = mutableListOf<GameState>()
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
        stage = com.badlogic.gdx.scenes.scene2d.Stage(ScreenViewport())

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
            undoHandler = { undoAction() },
            menuHandler = { game.setScreen(MenuScreen(game)) },
            camera = camera,
            soundPlayer = { soundManager?.play(it) },
            stateSetter = { state = it; game.gameState = it },
            resetMode = { resetMode() },
            infoLabelRef = { gameUI.infoLabel },
            statusLabelRef = { gameUI.statusLabel }
        )
        gameUI.build()

        gameInput = GameInput(
            game, camera, stage, tileSize,
            stateProvider = { state },
            stateSetter = { state = it; game.gameState = it },
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

        state = TurnManager.startTurn(state)
        game.gameState = state
        gameUI.updateInfoLabel()

        gameUI.showTutorialHint()
    }

    private fun resetMode() {
        selectedRegion = null; selectedRegions.clear()
        actionUsedThisTurn = false
        attackMode = false; attackSourceId = -1
        moveMode = false; moveSourceId = -1
        gameInput.clearReachable()
        gameUI.updateInfoLabel()
    }

    private fun handleAction(actionType: String) {
        try {
            if (actionType == Actions.END_TURN) {
                state = TurnManager.endTurn(state)
                game.gameState = state; selectedRegion = null; selectedRegions.clear(); actionUsedThisTurn = false
                attackMode = false; attackSourceId = -1; moveMode = false; moveSourceId = -1
                undoStack.clear(); gameInput.clearReachable()
                soundManager?.play(SoundManager.SoundType.END_TURN)
                runAITurns(); gameUI.updateInfoLabel()
                return
            }
            if (actionUsedThisTurn || aiPending || gameOver) return
            if (actionType.startsWith("DIPLO_")) { handleDiploAction(actionType); return }
            if (actionType.startsWith(Actions.RESEARCH + ":")) {
                val action = ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.RESEARCH, 0, actionType.removePrefix(Actions.RESEARCH + ":"))
                ActionQueue.DEFAULT.enqueue(action); state = ActionQueue.DEFAULT.processAll(state)
                game.gameState = state; actionUsedThisTurn = true
                soundManager?.play(SoundManager.SoundType.RESEARCH); gameUI.updateInfoLabel()
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
            undoStack.add(state)
            ActionQueue.DEFAULT.enqueue(action); state = ActionQueue.DEFAULT.processAll(state)
            game.gameState = state; selectedRegion = state.map.getRegionById(region.id); actionUsedThisTurn = true
            gameUI.updateInfoLabel()
            checkVictory()
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Action error: ${e.message}")
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
        soundManager?.play(SoundManager.SoundType.ALLIANCE); gameUI.updateInfoLabel()
        checkVictory()
    }

    private fun checkVictory() {
        val myTerritories = state.map.regions.count { it.ownerId == 0 && it.terrain != TerrainType.WATER }
        val enemyTerritories = state.map.regions.count { it.ownerId == 1 && it.terrain != TerrainType.WATER }
        if (enemyTerritories == 0 && myTerritories > 0) {
            gameOver = true
            soundManager?.play(SoundManager.SoundType.VICTORY)
            gameUI.showGameOverDialog(Locale.VICTORY)
        } else if (myTerritories == 0 && enemyTerritories > 0) {
            gameOver = true
            soundManager?.play(SoundManager.SoundType.DEFEAT)
            gameUI.showGameOverDialog(Locale.DEFEAT)
        }
    }

    private fun undoAction() {
        if (undoStack.isEmpty() || !actionUsedThisTurn) return
        state = undoStack.removeLast()
        game.gameState = state
        selectedRegion = null; selectedRegions.clear()
        actionUsedThisTurn = false
        attackMode = false; attackSourceId = -1; moveMode = false; moveSourceId = -1
        gameInput.clearReachable()
        gameUI.updateInfoLabel()
    }

    private fun runAITurns() {
        if (state.currentPlayerId == 0) return
        aiPending = true
        state = TurnManager.startTurn(state)
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
            val action = ActionQueue.GameAction(state.currentPlayerId, aiAction.actionType, aiAction.targetRegionId, aiAction.param)
            ActionQueue.DEFAULT.enqueue(action); state = ActionQueue.DEFAULT.processAll(state)
        }
        state = TurnManager.endTurn(state)
        game.gameState = state; aiPending = false; gameUI.updateInfoLabel()
    }

    override fun render(delta: Float) {
        try {
            Gdx.gl.glClearColor(0.1f, 0.15f, 0.3f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            if (gameInput.isPanning) {
                if (!Gdx.input.isButtonPressed(1) && !Gdx.input.isButtonPressed(2)) {
                    gameInput.cancelPanning()
                }
            }

            camera.update()
            batch.projectionMatrix = camera.combined
            animTime += delta
            gameUI.update(delta)
            gameUI.updateTutorial(delta)

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
            Gdx.app.error("GameScreen", "Render error: ${e.message}", e)
        }
    }

    override fun hide() { alive = false }
    override fun resize(width: Int, height: Int) { camera.viewportWidth = width.toFloat(); camera.viewportHeight = height.toFloat(); camera.update(); stage.viewport.update(width, height, true) }
    override fun dispose() { batch.dispose(); shapeRenderer.dispose(); soundManager?.dispose(); mapRenderer.dispose(); stage.dispose(); skin.dispose() }
}
