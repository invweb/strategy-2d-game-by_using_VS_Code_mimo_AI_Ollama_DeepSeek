package com.example.strategy.desktop

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.example.strategy.model.*

class MapRenderer(
    private val batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer,
    private val tileSize: Float,
    private val game: StrategyGame
) {
    val tileTextures = mutableMapOf<TileKey, TextureRegion>()
    val buildingIcons = mutableMapOf<BuildingType, TextureRegion>()
    val unitIcons = mutableMapOf<UnitType, TextureRegion>()

    data class TileKey(val terrain: TerrainType, val ownerId: Int?)

    fun generateAll() {
        generateTileTextures()
        generateBuildingIcons()
        generateUnitIcons()
    }

    fun drawTiles(state: GameState, animTime: Float, actionUsedThisTurn: Boolean, selectedRegions: List<Region>) {
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
    }

    fun drawSelectionBox(isBoxSelecting: Boolean, boxStartScreenX: Int, boxStartScreenY: Int) {
        shapeRenderer.projectionMatrix = batch.projectionMatrix
        shapeRenderer.color = Color.YELLOW
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.end()

        if (isBoxSelecting) {
            val sx1 = minOf(boxStartScreenX.toFloat(), com.badlogic.gdx.Gdx.input.x.toFloat())
            val sy1 = minOf((com.badlogic.gdx.Gdx.graphics.height - boxStartScreenY).toFloat(), (com.badlogic.gdx.Gdx.graphics.height - com.badlogic.gdx.Gdx.input.y).toFloat())
            val sx2 = maxOf(boxStartScreenX.toFloat(), com.badlogic.gdx.Gdx.input.x.toFloat())
            val sy2 = maxOf((com.badlogic.gdx.Gdx.graphics.height - boxStartScreenY).toFloat(), (com.badlogic.gdx.Gdx.graphics.height - com.badlogic.gdx.Gdx.input.y).toFloat())
            val sw = sx2 - sx1; val sh = sy2 - sy1
            if (sw > 0f && sh > 0f) {
                val screenProj = com.badlogic.gdx.math.Matrix4().apply { setToOrtho2D(0f, 0f, com.badlogic.gdx.Gdx.graphics.width.toFloat(), com.badlogic.gdx.Gdx.graphics.height.toFloat()) }
                shapeRenderer.projectionMatrix = screenProj
                shapeRenderer.color = Color(0.5f, 1f, 0.5f, 0.4f)
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                shapeRenderer.rect(sx1, sy1, sw, sh)
                shapeRenderer.end()
                shapeRenderer.color = Color(0.5f, 1f, 0.5f, 1f)
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                shapeRenderer.rect(sx1, sy1, sw, sh)
                shapeRenderer.end()
            }
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
            val p = Pixmap(s, s, Pixmap.Format.RGBA8888); draw(p)
            val t = Texture(p); p.dispose(); return TextureRegion(t)
        }
        buildingIcons[BuildingType.FARM] = makeIcon { p -> p.setColor(0.3f, 0.55f, 0.2f, 1f); p.fillRectangle(4, 14, 40, 20); p.setColor(0.9f, 0.85f, 0.15f, 1f); for (i in 0..4) { val bx = 8 + i * 8; p.fillRectangle(bx, 8, 2, 12); p.fillCircle(bx, 8, 3) } }
        buildingIcons[BuildingType.LUMBER_MILL] = makeIcon { p -> p.setColor(0.45f, 0.3f, 0.1f, 1f); p.fillRectangle(18, 16, 8, 22); p.setColor(0.15f, 0.45f, 0.15f, 1f); p.fillCircle(22, 12, 14) }
        buildingIcons[BuildingType.BARRACKS] = makeIcon { p -> p.setColor(0.7f, 0.15f, 0.15f, 1f); p.fillCircle(18, 24, 14); p.setColor(0.9f, 0.85f, 0.1f, 1f); p.fillCircle(18, 24, 8) }
        buildingIcons[BuildingType.MINE] = makeIcon { p -> p.setColor(0.35f, 0.3f, 0.25f, 1f); p.fillRectangle(4, 12, 40, 28); p.setColor(0.15f, 0.12f, 0.1f, 1f); p.fillCircle(24, 28, 12) }
        buildingIcons[BuildingType.WALL] = makeIcon { p -> p.setColor(0.55f, 0.52f, 0.48f, 1f); p.fillRectangle(2, 20, 44, 20); p.setColor(0.4f, 0.38f, 0.35f, 1f); p.fillRectangle(12, 20, 2, 20); p.fillRectangle(24, 20, 2, 20); p.fillRectangle(36, 20, 2, 20) }
        buildingIcons[BuildingType.QUARRY] = makeIcon { p -> p.setColor(0.6f, 0.58f, 0.55f, 1f); p.fillRectangle(6, 18, 18, 14); p.fillRectangle(26, 22, 16, 10) }
        buildingIcons[BuildingType.MARKET] = makeIcon { p -> p.setColor(0.8f, 0.2f, 0.2f, 1f); p.fillTriangle(6, 8, 24, 2, 42, 8); p.setColor(0.45f, 0.3f, 0.1f, 1f); p.fillRectangle(10, 8, 3, 28); p.fillRectangle(35, 8, 3, 28) }
    }

    private fun generateUnitIcons() {
        val s = 48
        fun makeIcon(draw: (Pixmap) -> Unit): TextureRegion {
            val p = Pixmap(s, s, Pixmap.Format.RGBA8888); draw(p)
            val t = Texture(p); p.dispose(); return TextureRegion(t)
        }
        unitIcons[UnitType.INFANTRY] = makeIcon { p -> p.setColor(0.2f, 0.5f, 0.2f, 1f); p.fillCircle(24, 12, 8); p.fillRectangle(20, 20, 8, 16); p.fillRectangle(14, 24, 6, 4); p.fillRectangle(28, 24, 6, 4); p.fillRectangle(20, 36, 4, 8); p.fillRectangle(26, 36, 4, 8) }
        unitIcons[UnitType.CAVALRY] = makeIcon { p -> p.setColor(0.55f, 0.35f, 0.15f, 1f); p.fillCircle(16, 20, 12); p.fillRectangle(8, 20, 24, 10); p.fillRectangle(6, 30, 6, 12); p.fillRectangle(16, 30, 6, 12); p.fillRectangle(26, 30, 6, 12); p.setColor(0.3f, 0.6f, 0.3f, 1f); p.fillCircle(32, 16, 8) }
        unitIcons[UnitType.SIEGE] = makeIcon { p -> p.setColor(0.45f, 0.3f, 0.1f, 1f); p.fillRectangle(8, 28, 32, 8); p.fillRectangle(10, 20, 4, 12); p.fillRectangle(34, 20, 4, 12); p.fillRectangle(8, 16, 32, 4); p.setColor(0.7f, 0.7f, 0.7f, 1f); p.fillCircle(24, 10, 6) }
    }

    fun dispose() {
        tileTextures.values.forEach { it.texture.dispose() }
        buildingIcons.values.forEach { it.texture.dispose() }
        unitIcons.values.forEach { it.texture.dispose() }
    }
}
