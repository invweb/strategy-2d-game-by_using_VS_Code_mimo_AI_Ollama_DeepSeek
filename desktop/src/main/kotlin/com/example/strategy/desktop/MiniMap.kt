package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.example.strategy.model.*

class MiniMap(private val tileSize: Float) {

    private var texture: Texture? = null
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastOwnerId = ""

    fun render(batch: SpriteBatch, state: GameState, screenWidth: Int, screenHeight: Int) {
        val mapW = state.map.width
        val mapH = state.map.height

        val ownerIdHash = state.map.regions.joinToString(",") { "${it.ownerId}" }
        if (mapW != lastWidth || mapH != lastHeight || ownerIdHash != lastOwnerId) {
            lastWidth = mapW; lastHeight = mapH; lastOwnerId = ownerIdHash
            generateTexture(state)
        }

        val tex = texture ?: return
        val miniSize = 160f
        val x = screenWidth - miniSize - 10f
        val y = 10f

        batch.setColor(0f, 0f, 0f, 0.7f)
        batch.draw(tex, x - 2f, y - 2f, miniSize + 4f, miniSize + 4f)
        batch.setColor(Color.WHITE)
        batch.draw(tex, x, y, miniSize, miniSize)
    }

    private fun generateTexture(state: GameState) {
        texture?.dispose()
        val mapW = state.map.width
        val mapH = state.map.height
        val pix = Pixmap(mapW, mapH, Pixmap.Format.RGBA8888)
        pix.setColor(0.1f, 0.1f, 0.15f, 1f)
        pix.fill()

        for (region in state.map.regions) {
            val px = region.tileX
            val py = mapH - 1 - region.tileY
            if (px < 0 || px >= mapW || py < 0 || py >= mapH) continue

            val color = when {
                !state.fog.isExplored(0, region.id) -> Color(0.15f, 0.15f, 0.2f, 1f)
                region.ownerId == 0 -> Color(0.3f, 0.5f, 1f, 1f)
                region.ownerId == 1 -> Color(1f, 0.3f, 0.3f, 1f)
                else -> when (region.terrain) {
                    TerrainType.WATER -> Color(0.2f, 0.3f, 0.6f, 1f)
                    TerrainType.PLAINS -> Color(0.4f, 0.6f, 0.3f, 1f)
                    TerrainType.FOREST -> Color(0.15f, 0.4f, 0.15f, 1f)
                    TerrainType.MOUNTAIN -> Color(0.5f, 0.45f, 0.4f, 1f)
                    TerrainType.HILLS -> Color(0.55f, 0.5f, 0.35f, 1f)
                }
            }
            pix.setColor(color)
            pix.drawPixel(px, py)
        }

        texture = Texture(pix)
        pix.dispose()
    }

    fun dispose() {
        texture?.dispose()
    }
}
