package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Stage
import com.example.strategy.model.*
import com.example.strategy.logic.ActionQueue

class GameInput(
    private val game: StrategyGame,
    private val camera: OrthographicCamera,
    private val stage: Stage,
    private val tileSize: Float,
    private val holder: GameStateHolder,
    private val stateSetter: (GameState) -> Unit,
    private val updateInfoLabel: () -> Unit,
    private val soundPlayer: (SoundManager.SoundType) -> Unit,
    private val animProvider: () -> AnimationManager,
    private val mapRenderer: MapRenderer,
    private val onAttackMode: ((String) -> Unit)? = null,
    private val onMoveMode: ((String) -> Unit)? = null
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
    var reachableRegions = setOf<Region>()
        private set

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
                val region = holder.state.map.getRegionAt(tileX, tileY)

                if (holder.attackMode && region != null && region.ownerId != holder.state.currentPlayerId && region.terrain != TerrainType.WATER) {
                    val sourceRegion = holder.state.map.getRegionById(holder.attackSourceId)
                    if (sourceRegion != null) {
                        val fromX = sourceRegion.tileX * tileSize + tileSize / 2f
                        val fromY = (holder.state.map.height - 1 - sourceRegion.tileY) * tileSize + tileSize / 2f
                        val toX = region.tileX * tileSize + tileSize / 2f
                        val toY = (holder.state.map.height - 1 - region.tileY) * tileSize + tileSize / 2f
                        animProvider().addMove(fromX, fromY, toX, toY)
                        animProvider().addAttack(toX, toY)
                        animProvider().addDamage(toX, toY, 0)
                        soundPlayer(SoundManager.SoundType.ATTACK)
                    }
                    val action = ActionQueue.GameAction(holder.state.currentPlayerId, ActionQueue.ActionType.ATTACK, region.id, holder.attackSourceId.toString())
                    ActionQueue.DEFAULT.enqueue(action)
                    stateSetter(ActionQueue.DEFAULT.processAll(holder.state))
                    holder.attackMode = false; holder.attackSourceId = -1
                    holder.actionUsedThisTurn = true
                    holder.selectedRegion = holder.state.map.getRegionById(region.id)
                    updateInfoLabel()
                    return true
                }

                if (holder.moveMode && region != null && region.ownerId == holder.state.currentPlayerId && region.id != holder.moveSourceId && region.terrain != TerrainType.WATER && reachableRegions.contains(region)) {
                    soundPlayer(SoundManager.SoundType.MOVE)
                    val action = ActionQueue.GameAction(holder.state.currentPlayerId, ActionQueue.ActionType.MOVE_TROOPS, holder.moveSourceId, region.id.toString())
                    ActionQueue.DEFAULT.enqueue(action)
                    stateSetter(ActionQueue.DEFAULT.processAll(holder.state))
                    holder.moveMode = false; holder.moveSourceId = -1; clearReachable()
                    holder.actionUsedThisTurn = true
                    holder.selectedRegion = holder.state.map.getRegionById(region.id)
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
                    val region = holder.state.map.getRegionAt(tileX, tileY)
                    holder.selectedRegions.clear()
                    if (region != null && region.terrain != TerrainType.WATER) {
                        holder.selectedRegion = region
                        holder.selectedRegions.add(region)
                        soundPlayer(SoundManager.SoundType.SELECT)
                    } else {
                        holder.selectedRegion = null
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
                    holder.selectedRegion = null
                    holder.selectedRegions.clear()
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

    fun calculateReachable(sourceId: Int) {
        val player = holder.state.currentPlayer()
        val hasHorseback = player?.techs?.isResearched(TechType.HORSEBACK) == true
        val maxCost = if (hasHorseback) 10 else 8
        reachableRegions = com.example.strategy.pathfinding.AStar.findReachableRegions(holder.state.map, sourceId, maxCost)
    }

    fun clearReachable() {
        reachableRegions = emptySet()
    }

    private fun selectRegionsInBox() {
        val sx1 = minOf(boxStartScreenX.toFloat(), Gdx.input.x.toFloat())
        val sy1 = minOf(boxStartScreenY.toFloat(), Gdx.input.y.toFloat())
        val sx2 = maxOf(boxStartScreenX.toFloat(), Gdx.input.x.toFloat())
        val sy2 = maxOf(boxStartScreenY.toFloat(), Gdx.input.y.toFloat())
        holder.selectedRegions.clear()
        holder.selectedRegion = null
        val tempVec = com.badlogic.gdx.math.Vector3()
        for (region in holder.state.map.regions) {
            val wx = region.tileX * tileSize + tileSize / 2f
            val wy = (holder.state.map.height - 1 - region.tileY) * tileSize + tileSize / 2f
            tempVec.set(wx, wy, 0f)
            camera.project(tempVec)
            val screenCX = tempVec.x
            val screenCY = Gdx.graphics.height - tempVec.y
            if (screenCX in sx1..sx2 && screenCY in sy1..sy2 && region.terrain != TerrainType.WATER) {
                holder.selectedRegions.add(region)
            }
        }
        if (holder.selectedRegions.isNotEmpty()) holder.selectedRegion = holder.selectedRegions.first()
        soundPlayer(SoundManager.SoundType.SELECT)
        updateInfoLabel()
    }
}
