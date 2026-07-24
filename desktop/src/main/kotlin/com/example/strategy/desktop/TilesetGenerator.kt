package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import java.io.File

// Generates a tileset PNG and .tmx map at runtime — no external assets needed
object TilesetGenerator {

    private const val TILE_SIZE = 32
    private const val MAP_W = 10
    private const val MAP_H = 8
    private const val TILES_PER_ROW = 5
    private const val TILESET_COLS = 5

    // Tile IDs in the tileset: 0=plains, 1=forest, 2=mountain, 3=hills, 4=water
    private val TERRAIN_TILE = mapOf(
        "PLAINS" to 0,
        "FOREST" to 1,
        "MOUNTAIN" to 2,
        "HILLS" to 3,
        "WATER" to 4
    )

    fun generate(assetsDir: File) {
        assetsDir.mkdirs()
        generateTileset(assetsDir)
        generateTmxMap(assetsDir)
    }

    // Create tileset.png — 5 tiles in a row, each 32x32
    private fun generateTileset(dir: File) {
        val file = File(dir, "tileset.png")
        if (file.exists()) return

        val pix = Pixmap(TILE_SIZE * TILESET_COLS, TILE_SIZE, Pixmap.Format.RGBA8888)

        // Plains — green
        pix.setColor(0.4f, 0.7f, 0.3f, 1f)
        pix.fillRectangle(0, 0, TILE_SIZE, TILE_SIZE)
        pix.setColor(0.35f, 0.65f, 0.25f, 1f)
        for (i in 0..5) {
            pix.fillCircle(4 + i * 5, TILE_SIZE / 2, 2)
        }

        // Forest — dark green
        pix.setColor(0.15f, 0.45f, 0.15f, 1f)
        pix.fillRectangle(TILE_SIZE, 0, TILE_SIZE, TILE_SIZE)
        pix.setColor(0.1f, 0.35f, 0.1f, 1f)
        for (i in 0..2) {
            val tx = TILE_SIZE + 6 + i * 10
            pix.fillTriangle(tx, 4, tx - 5, 16, tx + 5, 16)
            pix.fillRectangle(tx - 1, 16, 3, 14)
        }

        // Mountain — gray
        pix.setColor(0.5f, 0.45f, 0.4f, 1f)
        pix.fillRectangle(TILE_SIZE * 2, 0, TILE_SIZE, TILE_SIZE)
        pix.setColor(0.6f, 0.55f, 0.5f, 1f)
        pix.fillTriangle(TILE_SIZE * 2 + 16, 4, TILE_SIZE * 2 + 4, 28, TILE_SIZE * 2 + 28, 28)
        pix.setColor(0.7f, 0.7f, 0.7f, 1f)
        pix.fillTriangle(TILE_SIZE * 2 + 16, 4, TILE_SIZE * 2 + 12, 12, TILE_SIZE * 2 + 20, 12)

        // Hills — tan
        pix.setColor(0.6f, 0.55f, 0.35f, 1f)
        pix.fillRectangle(TILE_SIZE * 3, 0, TILE_SIZE, TILE_SIZE)
        pix.setColor(0.55f, 0.5f, 0.3f, 1f)
        pix.fillCircle(TILE_SIZE * 3 + 16, TILE_SIZE - 8, 12)
        pix.fillCircle(TILE_SIZE * 3 + 10, TILE_SIZE - 12, 8)

        // Water — blue
        pix.setColor(0.2f, 0.4f, 0.8f, 1f)
        pix.fillRectangle(TILE_SIZE * 4, 0, TILE_SIZE, TILE_SIZE)
        pix.setColor(0.3f, 0.5f, 0.9f, 1f)
        for (i in 0..3) {
            pix.fillCircle(TILE_SIZE * 4 + 4 + i * 8, TILE_SIZE / 2 + (i % 2) * 4 - 2, 3)
        }

        val bytes = pix像素ToBytes(pix)
        file.writeBytes(bytes)
        pix.dispose()
    }

    // Convert Pixmap to PNG bytes
    private fun pix像素ToBytes(pix: Pixmap): ByteArray {
        // Use Pixmap IO — write to a temp buffer via libGDX
        val buf = java.io.ByteArrayOutputStream()
        // libGDX Pixmap doesn't have direct PNG encode in pure Java
        // Use AWT if available
        val img = java.awt.image.BufferedImage(pix.width, pix.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pix.height) {
            for (x in 0 until pix.width) {
                val c = pix.getPixel(x, pix.height - 1 - y)
                val a = ((c shr 24) and 0xFF)
                val r = ((c shr 16) and 0xFF)
                val g = ((c shr 8) and 0xFF)
                val b = (c and 0xFF)
                img.setRGB(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }
        javax.imageio.ImageIO.write(img, "png", buf)
        return buf.toByteArray()
    }

    // Generate .tmx map file
    private fun generateTmxMap(dir: File) {
        val file = File(dir, "map.tmx")
        if (file.exists()) return

        val terrainMap = generateTerrainGrid()

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<map version="1.10" tiledversion="1.10.2" orientation="orthogonal" renderorder="right-down" width="$MAP_W" height="$MAP_H" tilewidth="$TILE_SIZE" tileheight="$TILE_SIZE" infinite="0" nextlayerid="2" nextobjectid="1">""")
        sb.appendLine("""  <tileset firstgid="1" name="tileset" tilewidth="$TILE_SIZE" tileheight="$TILE_SIZE" tilecount="5" columns="$TILESET_COLS">""")
        sb.appendLine("""    <image source="tileset.png" width="${TILE_SIZE * TILESET_COLS}" height="$TILE_SIZE"/>""")
        sb.appendLine("""  </tileset>""")
        sb.appendLine("""  <layer id="1" name="terrain" width="$MAP_W" height="$MAP_H">""")
        sb.appendLine("""    <data encoding="csv">""")

        for (y in 0 until MAP_H) {
            val row = (0 until MAP_W).joinToString(",") { x ->
                (terrainMap[y][x] + 1).toString() // TMX tile IDs are 1-based
            }
            sb.append("      ")
            sb.append(row)
            if (y < MAP_H - 1) sb.append(",")
            sb.appendLine()
        }

        sb.appendLine("""    </data>""")
        sb.appendLine("""  </layer>""")
        sb.appendLine("""</map>""")

        file.writeText(sb.toString())
    }

    // Generate terrain grid matching GameFactory
    private fun generateTerrainGrid(): Array<IntArray> {
        val grid = Array(MAP_H) { IntArray(MAP_W) }
        for (y in 0 until MAP_H) {
            for (x in 0 until MAP_W) {
                val terrain = when {
                    x == 0 || y == 0 || x == MAP_W - 1 || y == MAP_H - 1 -> "WATER"
                    (x + y) % 5 == 0 -> "MOUNTAIN"
                    (x * y) % 7 == 0 -> "FOREST"
                    (x + y) % 3 == 0 -> "HILLS"
                    else -> "PLAINS"
                }
                grid[y][x] = TERRAIN_TILE[terrain]!!
            }
        }
        return grid
    }
}
