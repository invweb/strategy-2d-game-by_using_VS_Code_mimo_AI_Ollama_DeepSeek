package com.example.strategy.desktop

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.example.strategy.model.*
import kotlin.math.cos
import kotlin.math.sin

// Hex grid renderer — draws regions as colored hexagons based on terrain & owner
class HexGridRenderer(private val shapeRenderer: ShapeRenderer) {

    companion object {
        const val HEX_RADIUS = 32f
        const val HEX_WIDTH = HEX_RADIUS * 2f
        const val HEX_HEIGHT = HEX_RADIUS * 1.732f // sqrt(3)
    }

    fun tileToScreen(tileX: Int, tileY: Int): Pair<Float, Float> {
        val offsetX = if (tileY % 2 == 1) HEX_RADIUS else 0f
        val x = tileX * HEX_WIDTH * 0.75f + offsetX
        val y = tileY * HEX_HEIGHT * 0.5f
        return x to y
    }

    fun screenToTile(screenX: Float, screenY: Float): Pair<Int, Int> {
        var tileY = (screenY / (HEX_HEIGHT * 0.5f)).toInt()
        val offsetX = if (tileY % 2 == 1) HEX_RADIUS else 0f
        var tileX = ((screenX - offsetX) / (HEX_WIDTH * 0.75f)).toInt()
        return tileX to tileY
    }

    fun drawRegion(region: Region, selected: Boolean = false) {
        val (cx, cy) = tileToScreen(region.tileX, region.tileY)

        // Terrain color
        val terrainColor = when (region.terrain) {
            TerrainType.PLAINS -> Color(0.4f, 0.7f, 0.3f, 1f)
            TerrainType.FOREST -> Color(0.15f, 0.45f, 0.15f, 1f)
            TerrainType.MOUNTAIN -> Color(0.5f, 0.45f, 0.4f, 1f)
            TerrainType.HILLS -> Color(0.6f, 0.55f, 0.35f, 1f)
            TerrainType.WATER -> Color(0.2f, 0.4f, 0.8f, 1f)
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Base hex
        shapeRenderer.color = terrainColor
        drawHexTriangles(cx, cy, HEX_RADIUS - 1f)

        // Owner tint
        if (region.ownerId != null) {
            val ownerColor = if (region.ownerId == 0) {
                Color(0.3f, 0.3f, 1f, 0.3f)
            } else {
                Color(1f, 0.3f, 0.3f, 0.3f)
            }
            shapeRenderer.color = ownerColor
            drawHexTriangles(cx, cy, HEX_RADIUS - 2f)
        }

        // Selection highlight
        if (selected) {
            shapeRenderer.color = Color.YELLOW
            drawHexTriangles(cx, cy, HEX_RADIUS + 2f)
        }

        // Building indicators — small dots
        val buildingCount = region.buildings.size
        for (i in 0 until buildingCount) {
            val bx = cx - 8f + (i % 3) * 6f
            val by = cy + HEX_RADIUS * 0.4f - (i / 3) * 5f
            shapeRenderer.color = Color.GOLD
            shapeRenderer.circle(bx, by, 2f)
        }

        shapeRenderer.end()
    }

    private fun drawHexTriangles(cx: Float, cy: Float, radius: Float) {
        for (i in 0..5) {
            val angle1 = Math.toRadians((60 * i).toDouble()).toFloat()
            val angle2 = Math.toRadians((60 * (i + 1)).toDouble()).toFloat()
            val x1 = cx + radius * cos(angle1)
            val y1 = cy + radius * sin(angle1)
            val x2 = cx + radius * cos(angle2)
            val y2 = cy + radius * sin(angle2)
            shapeRenderer.triangle(cx, cy, x1, y1, x2, y2)
        }
    }
}
