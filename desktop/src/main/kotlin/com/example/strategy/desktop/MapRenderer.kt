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
                    val iconSize = tileSize * 0.4f; val gap = 3f
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
                val s = tileSize.toInt()
                val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
                when (terrain) {
                    TerrainType.PLAINS -> {
                        pix.setColor(0.38f, 0.68f, 0.25f, 1f); pix.fill()
                        pix.setColor(0.35f, 0.62f, 0.22f, 1f)
                        for (i in 0..30) { val gx = (i * 37 % s); val gy = (i * 53 % s); pix.fillRectangle(gx, gy, 2, 3) }
                        pix.setColor(0.42f, 0.74f, 0.30f, 1f)
                        for (i in 0..15) { val gx = (i * 41 % s); val gy = (i * 67 % s); pix.fillRectangle(gx, gy, 1, 2) }
                        pix.setColor(0.45f, 0.78f, 0.32f, 1f)
                        for (i in 0..8) { val gx = (i * 29 % (s-4)) + 2; val gy = (i * 47 % (s-6)) + 2; pix.fillCircle(gx, gy, 2) }
                    }
                    TerrainType.FOREST -> {
                        pix.setColor(0.22f, 0.48f, 0.18f, 1f); pix.fill()
                        pix.setColor(0.18f, 0.42f, 0.15f, 1f)
                        for (i in 0..25) { val gx = (i * 37 % s); val gy = (i * 53 % s); pix.fillRectangle(gx, gy, 2, 3) }
                        val trees = listOf(Pair(10, 12), Pair(22, 8), Pair(16, 22), Pair(28, 18), Pair(6, 26))
                        for ((tx, ty) in trees) {
                            pix.setColor(0.35f, 0.22f, 0.10f, 1f); pix.fillRectangle(tx - 1, ty + 5, 3, 6)
                            pix.setColor(0.12f, 0.38f, 0.10f, 1f); pix.fillCircle(tx, ty, 7)
                            pix.setColor(0.16f, 0.45f, 0.13f, 1f); pix.fillCircle(tx, ty - 1, 5)
                            pix.setColor(0.20f, 0.52f, 0.16f, 1f); pix.fillCircle(tx - 1, ty - 2, 3)
                        }
                    }
                    TerrainType.MOUNTAIN -> {
                        pix.setColor(0.50f, 0.44f, 0.38f, 1f); pix.fill()
                        pix.setColor(0.46f, 0.40f, 0.34f, 1f)
                        for (i in 0..20) { val gx = (i * 41 % s); val gy = (i * 59 % s); pix.fillRectangle(gx, gy, 3, 2) }
                        pix.setColor(0.55f, 0.50f, 0.44f, 1f)
                        pix.fillTriangle(s / 2, 4, s / 2 - 22, s - 6, s / 2 + 22, s - 6)
                        pix.setColor(0.60f, 0.55f, 0.48f, 1f)
                        pix.fillTriangle(s / 2, 4, s / 2 - 16, s - 14, s / 2 + 16, s - 14)
                        pix.setColor(0.88f, 0.90f, 0.93f, 1f)
                        pix.fillTriangle(s / 2, 4, s / 2 - 8, 18, s / 2 + 8, 18)
                        pix.setColor(0.95f, 0.96f, 0.98f, 1f)
                        pix.fillTriangle(s / 2, 4, s / 2 - 5, 12, s / 2 + 5, 12)
                        pix.setColor(0.42f, 0.36f, 0.30f, 1f)
                        pix.fillTriangle(s / 2 - 20, s - 6, s / 2 - 28, s, s / 2 - 4, s)
                        pix.fillTriangle(s / 2 + 20, s - 6, s / 2 + 4, s, s / 2 + 28, s)
                    }
                    TerrainType.HILLS -> {
                        pix.setColor(0.44f, 0.60f, 0.28f, 1f); pix.fill()
                        pix.setColor(0.40f, 0.56f, 0.25f, 1f)
                        for (i in 0..20) { val gx = (i * 37 % s); val gy = (i * 53 % s); pix.fillRectangle(gx, gy, 2, 2) }
                        pix.setColor(0.48f, 0.64f, 0.32f, 1f)
                        pix.fillCircle(10, 22, 10); pix.fillCircle(22, 18, 11); pix.fillCircle(32, 24, 9)
                        pix.setColor(0.52f, 0.68f, 0.36f, 1f)
                        pix.fillCircle(10, 20, 7); pix.fillCircle(22, 16, 8); pix.fillCircle(32, 22, 6)
                        pix.setColor(0.46f, 0.62f, 0.30f, 1f)
                        pix.fillCircle(16, 26, 8); pix.fillCircle(28, 28, 7)
                    }
                    TerrainType.WATER -> {
                        pix.setColor(0.22f, 0.46f, 0.78f, 1f); pix.fill()
                        pix.setColor(0.28f, 0.52f, 0.84f, 1f)
                        for (wy in 4 until s step 7) {
                            for (wx in 2 until s - 2 step 9) {
                                val offset = ((wy / 7) % 2) * 4
                                pix.fillRectangle(wx + offset, wy, 6, 2)
                            }
                        }
                        pix.setColor(0.32f, 0.58f, 0.88f, 1f)
                        for (wy in 8 until s step 10) {
                            for (wx in 5 until s - 5 step 12) {
                                pix.fillCircle(wx, wy, 2)
                            }
                        }
                    }
                }
                if (owner != null) {
                    val bc = if (owner == 0) Color(0.3f, 0.3f, 1f, 0.6f) else Color(1f, 0.3f, 0.3f, 0.6f)
                    pix.setColor(bc)
                    pix.fillRectangle(0, 0, s, 3); pix.fillRectangle(0, s - 3, s, 3)
                    pix.fillRectangle(0, 0, 3, s); pix.fillRectangle(s - 3, 0, 3, s)
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
        buildingIcons[BuildingType.FARM] = makeIcon { p ->
            p.setColor(0.3f, 0.55f, 0.2f, 1f); p.fillRectangle(4, 14, 40, 20)
            p.setColor(0.9f, 0.85f, 0.15f, 1f)
            for (i in 0..4) { val bx = 8 + i * 8; p.fillRectangle(bx, 8, 2, 12); p.fillCircle(bx, 8, 3) }
        }
        buildingIcons[BuildingType.LUMBER_MILL] = makeIcon { p ->
            p.setColor(0.45f, 0.3f, 0.1f, 1f); p.fillRectangle(20, 20, 8, 20)
            p.setColor(0.15f, 0.45f, 0.15f, 1f); p.fillCircle(24, 16, 14)
            p.setColor(0.2f, 0.55f, 0.2f, 1f); p.fillCircle(24, 14, 10)
        }
        buildingIcons[BuildingType.BARRACKS] = makeIcon { p ->
            p.setColor(0.7f, 0.15f, 0.15f, 1f); p.fillCircle(24, 24, 16)
            p.setColor(0.9f, 0.85f, 0.1f, 1f); p.fillCircle(24, 24, 10)
        }
        buildingIcons[BuildingType.MINE] = makeIcon { p ->
            p.setColor(0.35f, 0.3f, 0.25f, 1f); p.fillRectangle(4, 12, 40, 28)
            p.setColor(0.15f, 0.12f, 0.1f, 1f); p.fillCircle(24, 28, 12)
        }
        buildingIcons[BuildingType.WALL] = makeIcon { p ->
            p.setColor(0.55f, 0.52f, 0.48f, 1f); p.fillRectangle(2, 20, 44, 20)
            p.setColor(0.4f, 0.38f, 0.35f, 1f)
            p.fillRectangle(12, 20, 2, 20); p.fillRectangle(24, 20, 2, 20); p.fillRectangle(36, 20, 2, 20)
        }
        buildingIcons[BuildingType.QUARRY] = makeIcon { p ->
            p.setColor(0.6f, 0.58f, 0.55f, 1f); p.fillRectangle(6, 18, 18, 14); p.fillRectangle(26, 22, 16, 10)
        }
        buildingIcons[BuildingType.MARKET] = makeIcon { p ->
            p.setColor(0.8f, 0.2f, 0.2f, 1f); p.fillTriangle(6, 8, 24, 2, 42, 8)
            p.setColor(0.45f, 0.3f, 0.1f, 1f); p.fillRectangle(10, 8, 3, 28); p.fillRectangle(35, 8, 3, 28)
        }
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
