package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.example.strategy.model.*
import kotlin.random.Random

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
        generateTerrainTiles()
        generateBuildingIcons()
        generateUnitIcons()
    }

    fun drawTiles(state: GameState, animTime: Float, actionUsedThisTurn: Boolean, selectedRegions: List<Region>, reachableRegions: Set<Region> = emptySet()) {
        batch.begin()
        for (region in state.map.regions) {
            val explored = state.fog.isExplored(0, region.id)
            if (!explored) {
                val x = region.tileX * tileSize
                val y = (state.map.height - 1 - region.tileY) * tileSize
                batch.setColor(0.08f, 0.08f, 0.12f, 1f)
                batch.draw(tileTextures[TileKey(region.terrain, null)], x, y, tileSize, tileSize)
                batch.setColor(Color.WHITE)
                continue
            }
            val tr = tileTextures[TileKey(region.terrain, null)] ?: continue
            val x = region.tileX * tileSize
            val y = (state.map.height - 1 - region.tileY) * tileSize
            batch.draw(tr, x, y, tileSize, tileSize)

            if (reachableRegions.contains(region) && region.terrain != TerrainType.WATER) {
                val pulse = (kotlin.math.sin(animTime * 4f) * 0.15f + 0.3f)
                batch.setColor(0.3f, 0.8f, 1f, pulse)
                batch.draw(tr, x, y, tileSize, tileSize)
                batch.setColor(Color.WHITE)
            }

            if (region.ownerId != null) {
                val bc = if (region.ownerId == 0) Color(0.2f, 0.4f, 1f, 0.35f) else Color(1f, 0.2f, 0.2f, 0.35f)
                batch.setColor(bc)
                batch.draw(tr, x, y, tileSize, tileSize)
                batch.setColor(Color.WHITE)
            }

            if (region.terrain != TerrainType.WATER) {
                val popText = "${region.population}"
                val popColor = when { region.ownerId == 0 -> Color(0.5f, 0.8f, 1f, 0.9f); region.ownerId == 1 -> Color(1f, 0.5f, 0.5f, 0.9f); else -> Color.WHITE }
                game.font.color = Color.BLACK; game.font.draw(batch, popText, x + tileSize - 28f, y + tileSize - 6f)
                game.font.color = popColor; game.font.draw(batch, popText, x + tileSize - 30f, y + tileSize - 4f)

                if (region.buildings.isNotEmpty()) {
                    val iconSize = tileSize * 0.45f; val gap = 4f
                    val totalW = region.buildings.size * iconSize + (region.buildings.size - 1) * gap
                    var sx = x + (tileSize - totalW) / 2f
                    for (building in region.buildings) { buildingIcons[building.type]?.let { batch.draw(it, sx, y + 6f, iconSize, iconSize) }; sx += iconSize + gap }
                }
                if (region.units.units.isNotEmpty()) {
                    val iconSize = tileSize * 0.28f; val gap = 3f
                    val unitList = region.units.units.filter { it.count > 0 }
                    val totalW = unitList.size * (iconSize + 10f) + (unitList.size - 1) * gap
                    var sx = x + (tileSize - totalW) / 2f
                    for (unit in unitList) {
                        unitIcons[unit.type]?.let { batch.draw(it, sx, y + 6f, iconSize, iconSize) }
                        val ct = "${unit.count}"
                        game.font.color = Color.BLACK; game.font.draw(batch, ct, sx + iconSize - 2f, y + 6f + iconSize - 2f)
                        game.font.color = Color.WHITE; game.font.draw(batch, ct, sx + iconSize - 4f, y + 6f + iconSize - 4f)
                        sx += iconSize + 10f + gap
                    }
                }
                if (region.ownerId == 0 && !actionUsedThisTurn) {
                    if (region.buildings.isEmpty() || region.buildings.any { it.type == BuildingType.BARRACKS }) {
                        val pulse = (kotlin.math.sin(animTime * 3f) * 0.2f + 0.35f)
                        batch.setColor(0.2f, 1f, 0.2f, pulse)
                        batch.draw(tileTextures[TileKey(region.terrain, null)], x, y, tileSize, tileSize)
                        batch.setColor(Color.WHITE)
                    }
                }
            }
        }
        selectedRegions.forEach { r ->
            val x = r.tileX * tileSize; val y = (state.map.height - 1 - r.tileY) * tileSize
            batch.setColor(1f, 1f, 0f, 0.85f)
            batch.draw(tileTextures[TileKey(r.terrain, null)], x, y, tileSize, tileSize)
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

    private fun generateTerrainTiles() {
        val rng = Random(42)
        for (terrain in TerrainType.entries) {
            val s = 48
            val p = Pixmap(s, s, Pixmap.Format.RGBA8888)
            when (terrain) {
                TerrainType.PLAINS -> {
                    p.setColor(0.45f, 0.75f, 0.25f, 1f); p.fill()
                    for (i in 0..20) {
                        p.setColor(0.4f + rng.nextFloat() * 0.1f, 0.65f + rng.nextFloat() * 0.15f, 0.2f + rng.nextFloat() * 0.1f, 1f)
                        p.fillRectangle(rng.nextInt(s), rng.nextInt(s), 2, 3)
                    }
                }
                TerrainType.FOREST -> {
                    p.setColor(0.15f, 0.4f, 0.12f, 1f); p.fill()
                    for (i in 0..8) {
                        val tx = rng.nextInt(4, s - 4)
                        val ty = rng.nextInt(8, s - 4)
                        p.setColor(0.1f, 0.5f, 0.1f, 1f); p.fillCircle(tx, ty, 6)
                        p.setColor(0.08f, 0.35f, 0.08f, 1f); p.fillCircle(tx - 2, ty + 2, 4)
                    }
                    for (i in 0..4) {
                        val tx = rng.nextInt(6, s - 6)
                        p.setColor(0.3f, 0.15f, 0.08f, 1f); p.fillRectangle(tx, rng.nextInt(10, s - 4), 2, 8)
                    }
                }
                TerrainType.MOUNTAIN -> {
                    p.setColor(0.45f, 0.42f, 0.38f, 1f); p.fill()
                    for (i in 0..3) {
                        val mx = rng.nextInt(8, s - 8)
                        val my = rng.nextInt(10, s - 8)
                        p.setColor(0.55f, 0.5f, 0.45f, 1f)
                        p.fillTriangle(mx, my - 12, mx - 8, my + 4, mx + 8, my + 4)
                        p.setColor(0.7f, 0.68f, 0.65f, 1f)
                        p.fillTriangle(mx, my - 12, mx - 3, my - 4, mx + 3, my - 4)
                    }
                }
                TerrainType.HILLS -> {
                    p.setColor(0.55f, 0.5f, 0.32f, 1f); p.fill()
                    for (i in 0..4) {
                        val hx = rng.nextInt(6, s - 6)
                        val hy = rng.nextInt(12, s - 6)
                        p.setColor(0.6f, 0.55f, 0.35f, 1f)
                        p.fillCircle(hx, hy, 8)
                        p.setColor(0.65f, 0.6f, 0.4f, 1f)
                        p.fillCircle(hx - 1, hy + 1, 5)
                    }
                }
                TerrainType.WATER -> {
                    p.setColor(0.15f, 0.35f, 0.75f, 1f); p.fill()
                    for (i in 0..6) {
                        p.setColor(0.2f, 0.45f, 0.85f, 1f)
                        p.fillRectangle(rng.nextInt(s), rng.nextInt(s), rng.nextInt(6, 14), 2)
                    }
                }
                TerrainType.DESERT -> {
                    p.setColor(0.85f, 0.75f, 0.45f, 1f); p.fill()
                    for (i in 0..15) {
                        p.setColor(0.8f + rng.nextFloat() * 0.1f, 0.7f + rng.nextFloat() * 0.1f, 0.4f + rng.nextFloat() * 0.1f, 1f)
                        p.fillRectangle(rng.nextInt(s), rng.nextInt(s), 2, 2)
                    }
                }
                TerrainType.SWAMP -> {
                    p.setColor(0.3f, 0.38f, 0.22f, 1f); p.fill()
                    for (i in 0..8) {
                        p.setColor(0.25f, 0.35f, 0.2f, 1f)
                        p.fillCircle(rng.nextInt(s), rng.nextInt(s), 4)
                    }
                    for (i in 0..3) {
                        p.setColor(0.2f, 0.3f, 0.15f, 1f)
                        p.fillRectangle(rng.nextInt(s), rng.nextInt(s), rng.nextInt(4, 10), 2)
                    }
                }
                TerrainType.SNOW -> {
                    p.setColor(0.9f, 0.92f, 0.96f, 1f); p.fill()
                    for (i in 0..12) {
                        p.setColor(1f, 1f, 1f, 1f)
                        p.fillCircle(rng.nextInt(s), rng.nextInt(s), 1)
                    }
                    for (i in 0..4) {
                        p.setColor(0.85f, 0.88f, 0.93f, 1f)
                        p.fillCircle(rng.nextInt(s), rng.nextInt(s), 3)
                    }
                }
            }
            val t = Texture(p); p.dispose()
            tileTextures[TileKey(terrain, null)] = TextureRegion(t)
        }
    }

    private fun generateBuildingIcons() {
        val s = 48
        fun makeIcon(draw: (Pixmap) -> Unit): TextureRegion {
            val p = Pixmap(s, s, Pixmap.Format.RGBA8888); draw(p)
            val t = Texture(p); p.dispose(); return TextureRegion(t)
        }
        buildingIcons[BuildingType.FARM] = makeIcon { p ->
            p.setColor(0.3f, 0.6f, 0.15f, 1f); p.fillRectangle(4, 12, 40, 24)
            p.setColor(1f, 0.85f, 0.1f, 1f)
            for (i in 0..5) { val bx = 6 + i * 7; p.fillRectangle(bx, 6, 2, 14); p.fillCircle(bx, 6, 3) }
            p.setColor(0.5f, 0.35f, 0.1f, 1f); p.fillRectangle(2, 36, 44, 4)
        }
        buildingIcons[BuildingType.LUMBER_MILL] = makeIcon { p ->
            p.setColor(0.5f, 0.3f, 0.1f, 1f); p.fillRectangle(18, 18, 12, 22)
            p.setColor(0.15f, 0.5f, 0.15f, 1f); p.fillCircle(24, 14, 14)
            p.setColor(0.2f, 0.6f, 0.2f, 1f); p.fillCircle(24, 12, 10)
            p.setColor(0.1f, 0.4f, 0.1f, 1f); p.fillCircle(20, 16, 6)
        }
        buildingIcons[BuildingType.BARRACKS] = makeIcon { p ->
            p.setColor(0.7f, 0.12f, 0.12f, 1f); p.fillRectangle(8, 16, 32, 24)
            p.setColor(0.85f, 0.15f, 0.15f, 1f); p.fillRectangle(10, 18, 28, 20)
            p.setColor(1f, 0.9f, 0.1f, 1f); p.fillCircle(24, 28, 6)
            p.setColor(0.6f, 0.1f, 0.1f, 1f); p.fillRectangle(20, 8, 8, 10)
        }
        buildingIcons[BuildingType.MINE] = makeIcon { p ->
            p.setColor(0.4f, 0.35f, 0.28f, 1f); p.fillRectangle(6, 14, 36, 26)
            p.setColor(0.2f, 0.15f, 0.1f, 1f); p.fillCircle(24, 26, 12)
            p.setColor(0.7f, 0.65f, 0.2f, 1f); p.fillCircle(18, 22, 3); p.fillCircle(30, 20, 2)
        }
        buildingIcons[BuildingType.WALL] = makeIcon { p ->
            p.setColor(0.6f, 0.58f, 0.52f, 1f); p.fillRectangle(2, 18, 44, 24)
            p.setColor(0.5f, 0.48f, 0.44f, 1f)
            p.fillRectangle(2, 36, 44, 6)
            p.fillRectangle(10, 18, 4, 24); p.fillRectangle(22, 18, 4, 24); p.fillRectangle(34, 18, 4, 24)
            p.setColor(0.7f, 0.68f, 0.62f, 1f)
            p.fillRectangle(4, 18, 6, 6); p.fillRectangle(16, 18, 6, 6); p.fillRectangle(28, 18, 6, 6); p.fillRectangle(38, 18, 6, 6)
        }
        buildingIcons[BuildingType.QUARRY] = makeIcon { p ->
            p.setColor(0.65f, 0.62f, 0.58f, 1f); p.fillRectangle(4, 16, 20, 18); p.fillRectangle(28, 20, 16, 14)
            p.setColor(0.55f, 0.52f, 0.48f, 1f); p.fillRectangle(8, 12, 12, 6)
        }
        buildingIcons[BuildingType.MARKET] = makeIcon { p ->
            p.setColor(0.85f, 0.18f, 0.18f, 1f); p.fillTriangle(4, 10, 24, 0, 44, 10)
            p.setColor(0.5f, 0.3f, 0.1f, 1f); p.fillRectangle(8, 10, 4, 28); p.fillRectangle(36, 10, 4, 28)
            p.setColor(1f, 0.85f, 0.1f, 1f); p.fillRectangle(12, 18, 24, 4)
        }
    }

    private fun generateUnitIcons() {
        val s = 48
        fun makeIcon(draw: (Pixmap) -> Unit): TextureRegion {
            val p = Pixmap(s, s, Pixmap.Format.RGBA8888); draw(p)
            val t = Texture(p); p.dispose(); return TextureRegion(t)
        }
        unitIcons[UnitType.INFANTRY] = makeIcon { p ->
            p.setColor(0.2f, 0.55f, 0.2f, 1f); p.fillCircle(24, 10, 7)
            p.setColor(0.15f, 0.45f, 0.15f, 1f); p.fillRectangle(20, 17, 8, 18)
            p.fillRectangle(14, 22, 6, 3); p.fillRectangle(28, 22, 6, 3)
            p.fillRectangle(20, 35, 4, 8); p.fillRectangle(26, 35, 4, 8)
            p.setColor(0.7f, 0.7f, 0.2f, 1f); p.fillRectangle(22, 17, 4, 2)
        }
        unitIcons[UnitType.CAVALRY] = makeIcon { p ->
            p.setColor(0.6f, 0.38f, 0.15f, 1f)
            p.fillCircle(14, 20, 10); p.fillRectangle(6, 18, 20, 10)
            p.fillRectangle(6, 28, 5, 12); p.fillRectangle(14, 28, 5, 12); p.fillRectangle(22, 28, 5, 12)
            p.setColor(0.15f, 0.1f, 0.05f, 1f); p.fillCircle(10, 16, 4)
            p.setColor(0.3f, 0.65f, 0.3f, 1f); p.fillCircle(30, 14, 8)
            p.setColor(0.25f, 0.55f, 0.25f, 1f); p.fillCircle(30, 14, 5)
        }
        unitIcons[UnitType.SIEGE] = makeIcon { p ->
            p.setColor(0.5f, 0.35f, 0.12f, 1f)
            p.fillRectangle(6, 26, 36, 10); p.fillRectangle(8, 18, 4, 12); p.fillRectangle(36, 18, 4, 12)
            p.fillRectangle(6, 14, 36, 4)
            p.setColor(0.75f, 0.75f, 0.75f, 1f); p.fillCircle(24, 8, 6)
            p.setColor(0.5f, 0.5f, 0.5f, 1f); p.fillCircle(24, 8, 3)
        }
    }

    fun dispose() {
        tileTextures.values.forEach { it.texture.dispose() }
        buildingIcons.values.forEach { it.texture.dispose() }
        unitIcons.values.forEach { it.texture.dispose() }
    }
}
