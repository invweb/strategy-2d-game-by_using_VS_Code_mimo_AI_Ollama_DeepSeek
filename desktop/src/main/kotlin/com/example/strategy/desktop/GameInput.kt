package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Stage
import com.example.strategy.model.*
import com.example.strategy.logic.ActionQueue
import com.example.strategy.logic.TurnManager

class GameInput(
    private val game: StrategyGame,
    private val camera: OrthographicCamera,
    private val stage: Stage,
    private val tileSize: Float,
    private val stateProvider: () -> GameState,
    private val stateSetter: (GameState) -> Unit,
    private val selectedRegionProvider: () -> Region?,
    private val selectedRegionSetter: (Region?) -> Unit,
    private val selectedRegionsProvider: () -> MutableList<Region>,
    private val actionUsedThisTurnProvider: () -> Boolean,
    private val actionUsedThisTurnSetter: (Boolean) -> Unit,
    private val attackModeProvider: () -> Boolean,
    private val attackModeSetter: (Boolean) -> Unit,
    private val attackSourceIdProvider: () -> Int,
    private val attackSourceIdSetter: (Int) -> Unit,
    private val moveModeProvider: () -> Boolean,
    private val moveModeSetter: (Boolean) -> Unit,
    private val moveSourceIdProvider: () -> Int,
    private val moveSourceIdSetter: (Int) -> Unit,
    private val aiPendingProvider: () -> Boolean,
    private val updateInfoLabel: () -> Unit,
    private val infoLabelSetter: (String) -> Unit,
    private val statusLabelSetter: (String) -> Unit,
    private val statusLabelColorSetter: (com.badlogic.gdx.graphics.Color) -> Unit,
    private val soundPlayer: (SoundManager.SoundType) -> Unit,
    private val animProvider: () -> AnimationManager,
    private val mapRenderer: MapRenderer
) {
    private var lastPanX = 0
    private var lastPanY = 0
    var isPanning = false
        private set
    var isBoxSelecting = false
        private set
    var boxStartScreenX = 0
        private set
    var boxStartScreenY = 0
        private set
    private val minZoom = 0.3f
    private val maxZoom = 3.0f

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
                val region = stateProvider().map.getRegionAt(tileX, tileY)

                if (attackModeProvider() && region != null && region.ownerId != stateProvider().currentPlayerId && region.terrain != TerrainType.WATER) {
                    val sourceRegion = stateProvider().map.getRegionById(attackSourceIdProvider())
                    if (sourceRegion != null) {
                        val fromX = sourceRegion.tileX * tileSize + tileSize / 2f
                        val fromY = (stateProvider().map.height - 1 - sourceRegion.tileY) * tileSize + tileSize / 2f
                        val toX = region.tileX * tileSize + tileSize / 2f
                        val toY = (stateProvider().map.height - 1 - region.tileY) * tileSize + tileSize / 2f
                        animProvider().addMove(fromX, fromY, toX, toY)
                        animProvider().addAttack(toX, toY)
                        animProvider().addDamage(toX, toY, 0)
                        soundPlayer(SoundManager.SoundType.ATTACK)
                    }
                    val action = ActionQueue.GameAction(stateProvider().currentPlayerId, ActionQueue.ActionType.ATTACK, region.id, attackSourceIdProvider().toString())
                    ActionQueue.enqueue(action)
                    stateSetter(ActionQueue.processAll(stateProvider()))
                    attackModeSetter(false); attackSourceIdSetter(-1)
                    actionUsedThisTurnSetter(true)
                    selectedRegionSetter(stateProvider().map.getRegionById(region.id))
                    updateInfoLabel()
                    return true
                }

                if (moveModeProvider() && region != null && region.ownerId == stateProvider().currentPlayerId && region.id != moveSourceIdProvider() && region.terrain != TerrainType.WATER) {
                    val action = ActionQueue.GameAction(stateProvider().currentPlayerId, ActionQueue.ActionType.MOVE_TROOPS, moveSourceIdProvider(), region.id.toString())
                    ActionQueue.enqueue(action)
                    stateSetter(ActionQueue.processAll(stateProvider()))
                    moveModeSetter(false); moveSourceIdSetter(-1)
                    actionUsedThisTurnSetter(true)
                    selectedRegionSetter(stateProvider().map.getRegionById(region.id))
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
                    val region = stateProvider().map.getRegionAt(tileX, tileY)
                    selectedRegionsProvider().clear()
                    if (region != null && region.terrain != TerrainType.WATER) {
                        selectedRegionSetter(region)
                        selectedRegionsProvider().add(region)
                        soundPlayer(SoundManager.SoundType.SELECT)
                    } else {
                        selectedRegionSetter(null)
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
                    selectedRegionSetter(null)
                    selectedRegionsProvider().clear()
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

    fun setup() {
        Gdx.input.inputProcessor = InputMultiplexer(stage, mapInput)
    }

    fun cancelPanning() {
        isPanning = false
    }

    private fun selectRegionsInBox() {
        val sx1 = minOf(boxStartScreenX.toFloat(), Gdx.input.x.toFloat())
        val sy1 = minOf(boxStartScreenY.toFloat(), Gdx.input.y.toFloat())
        val sx2 = maxOf(boxStartScreenX.toFloat(), Gdx.input.x.toFloat())
        val sy2 = maxOf(boxStartScreenY.toFloat(), Gdx.input.y.toFloat())
        selectedRegionsProvider().clear()
        selectedRegionSetter(null)
        val tempVec = com.badlogic.gdx.math.Vector3()
        for (region in stateProvider().map.regions) {
            val wx = region.tileX * tileSize + tileSize / 2f
            val wy = (stateProvider().map.height - 1 - region.tileY) * tileSize + tileSize / 2f
            tempVec.set(wx, wy, 0f)
            camera.project(tempVec)
            val screenCX = tempVec.x
            val screenCY = Gdx.graphics.height - tempVec.y
            if (screenCX in sx1..sx2 && screenCY in sy1..sy2 && region.terrain != TerrainType.WATER) {
                selectedRegionsProvider().add(region)
            }
        }
        if (selectedRegionsProvider().isNotEmpty()) selectedRegionSetter(selectedRegionsProvider().first())
        soundPlayer(SoundManager.SoundType.SELECT)
        updateInfoLabel()
    }
}
