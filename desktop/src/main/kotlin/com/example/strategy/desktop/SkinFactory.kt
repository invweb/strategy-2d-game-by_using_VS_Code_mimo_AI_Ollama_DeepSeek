package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable

object SkinFactory {

    fun createSkin(): Skin {
        val s = Skin()
        val font = generateFont()
        s.add("default-font", font, BitmapFont::class.java)

        val upPix = Pixmap(4, 4, Pixmap.Format.RGBA8888).apply { setColor(Color(0.2f, 0.2f, 0.25f, 1f)); fill() }
        val downPix = Pixmap(4, 4, Pixmap.Format.RGBA8888).apply { setColor(Color(0.3f, 0.3f, 0.35f, 1f)); fill() }
        val overPix = Pixmap(4, 4, Pixmap.Format.RGBA8888).apply { setColor(Color(0.25f, 0.25f, 0.3f, 1f)); fill() }
        val upTex = Texture(upPix); upPix.dispose()
        val downTex = Texture(downPix); downPix.dispose()
        val overTex = Texture(overPix); overPix.dispose()

        s.add("default", TextButton.TextButtonStyle().apply {
            this.font = font; fontColor = Color.WHITE
            up = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 2, 2, 2, 2))
            down = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(downTex, 2, 2, 2, 2))
            over = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(overTex, 2, 2, 2, 2))
        })
        s.add("default", com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(font, Color.WHITE))

        val windowBgPix = Pixmap(32, 32, Pixmap.Format.RGBA8888).apply { setColor(0.12f, 0.14f, 0.2f, 1f); fill() }
        val windowBgTex = Texture(windowBgPix); windowBgPix.dispose()
        val windowBg = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(windowBgTex, 4, 4, 4, 4))
        s.add("default", Window.WindowStyle(font, Color.CYAN, windowBg))
        s.add("default", TextField.TextFieldStyle().apply {
            this.font = font; fontColor = Color.WHITE; background = windowBg
            cursor = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1))
            selection = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1))
        })
        val listSelTex = Texture(Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(0.3f, 0.5f, 0.7f, 1f); fill() })
        val listStyle = com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle()
        listStyle.javaClass.getDeclaredField("font").apply { isAccessible = true }.set(listStyle, font)
        listStyle.selection = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(listSelTex, 0, 0, 0, 0))
        listStyle.fontColorSelected = Color.WHITE; listStyle.fontColorUnselected = Color.LIGHT_GRAY
        s.add("default", listStyle)
        s.add("default", com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle().apply {
            background = windowBg
            vScroll = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1))
            hScroll = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1))
            vScrollKnob = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(downTex, 1, 1, 1, 1))
            hScrollKnob = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(downTex, 1, 1, 1, 1))
        })
        return s
    }

    fun generateFont(): BitmapFont {
        val fontPaths = arrayOf(
            "/System/Library/Fonts/Supplemental/Arial.ttf",
            "/System/Library/Fonts/Helvetica.ttc",
            "/Library/Fonts/Arial.ttf",
            "/System/Library/Fonts/SFNSMono.ttf"
        )
        var generator: com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator? = null
        for (path in fontPaths) {
            try {
                if (java.io.File(path).exists()) {
                    generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(Gdx.files.absolute(path))
                    break
                }
            } catch (_: Exception) {}
        }
        if (generator == null) {
            generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(Gdx.files.absolute(fontPaths[0]))
        }
        val params = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter()
        params.size = 16
        params.minFilter = Texture.TextureFilter.Linear
        params.magFilter = Texture.TextureFilter.Linear
        params.characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,*':?!@#$%&()-+=/<>" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "äöüÄÖÜß «»—…"
        val font = generator.generateFont(params)
        generator.dispose()
        return font
    }

    fun makeTrashIcon(): com.badlogic.gdx.graphics.g2d.TextureRegion {
        val p = Pixmap(24, 24, Pixmap.Format.RGBA8888)
        p.setColor(Color(0.9f, 0.2f, 0.2f, 1f))
        p.fillRectangle(5, 4, 14, 3)
        p.fillRectangle(7, 7, 2, 14)
        p.fillRectangle(11, 7, 2, 14)
        p.fillRectangle(15, 7, 2, 14)
        p.fillRectangle(3, 18, 18, 3)
        p.fillRectangle(9, 1, 6, 4)
        val t = Texture(p); p.dispose()
        return com.badlogic.gdx.graphics.g2d.TextureRegion(t)
    }
}
