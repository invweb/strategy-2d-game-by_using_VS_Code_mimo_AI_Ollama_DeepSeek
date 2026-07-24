package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport

// Main menu screen — first screen when game starts
class MenuScreen(private val game: StrategyGame) : ScreenAdapter() {

    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {
        skin = createSkin()
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        val root = Table(skin).apply { setFillParent(true) }

        // Title
        val title = Label("STRATEGY", skin)
        title.setFontScale(3f)
        title.color = Color(0.9f, 0.8f, 0.2f, 1f)
        root.add(title).padBottom(10f).row()

        val subtitle = Label("2D Turn-Based Strategy", skin)
        subtitle.setFontScale(1.2f)
        subtitle.color = Color(0.7f, 0.7f, 0.7f, 1f)
        root.add(subtitle).padBottom(60f).row()

        // Buttons
        val newGameBtn = makeButton("NEW GAME", Color(0.3f, 0.6f, 0.3f, 1f))
        newGameBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.gameState = com.example.strategy.platform.GameFactory.createDefaultGameState()
                game.setScreen(GameScreen(game))
            }
        })

        val quitBtn = makeButton("QUIT", Color(0.7f, 0.3f, 0.3f, 1f))
        quitBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Gdx.app.exit()
            }
        })

        root.add(newGameBtn).width(300f).height(60f).padBottom(15f).row()
        root.add(quitBtn).width(300f).height(60f).padBottom(15f).row()

        // Footer
        val footer = Label("Ollama AI Powered  |  Kotlin + libGDX", skin)
        footer.setFontScale(0.8f)
        footer.color = Color(0.5f, 0.5f, 0.5f, 1f)
        root.add(footer).padTop(40f).row()

        stage.addActor(root)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
    }

    private fun makeButton(text: String, bgColor: Color): TextButton {
        val btn = TextButton(text, skin)
        btn.label.setFontScale(1.3f)
        btn.label.color = Color.WHITE
        btn.color = bgColor
        return btn
    }

    private fun createSkin(): Skin {
        val s = Skin()
        val font = BitmapFont()
        font.data.setScale(1.0f)
        s.add("default-font", font, BitmapFont::class.java)

        val upPix = Pixmap(4, 4, Pixmap.Format.RGBA8888).apply { setColor(Color(0.2f, 0.2f, 0.25f, 1f)); fill() }
        val downPix = Pixmap(4, 4, Pixmap.Format.RGBA8888).apply { setColor(Color(0.3f, 0.3f, 0.35f, 1f)); fill() }
        val overPix = Pixmap(4, 4, Pixmap.Format.RGBA8888).apply { setColor(Color(0.25f, 0.25f, 0.3f, 1f)); fill() }
        val upTex = Texture(upPix); upPix.dispose()
        val downTex = Texture(downPix); downPix.dispose()
        val overTex = Texture(overPix); overPix.dispose()

        s.add("default", TextButton.TextButtonStyle().apply {
            this.font = font
            fontColor = Color.WHITE
            up = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 2, 2, 2, 2))
            down = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(downTex, 2, 2, 2, 2))
            over = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(overTex, 2, 2, 2, 2))
        })

        s.add("default", Label.LabelStyle(font, Color.WHITE))
        return s
    }
}
