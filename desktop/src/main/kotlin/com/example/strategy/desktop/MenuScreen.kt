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
    private var selectedSize = com.example.strategy.platform.GameFactory.MapSize.MEDIUM
    private var selectedTerrain = com.example.strategy.platform.GameFactory.TerrainStyle.BALANCED
    private var selectedDifficulty = com.example.strategy.model.Difficulty.NORMAL
    private lateinit var sizeLabel: Label
    private lateinit var terrainLabel: Label
    private lateinit var difficultyLabel: Label

    override fun show() {
        skin = createSkin()
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        val root = Table(skin).apply { setFillParent(true) }

        val title = Label("STRATEGY", skin)
        title.setFontScale(3f)
        title.color = Color(0.9f, 0.8f, 0.2f, 1f)
        root.add(title).padBottom(10f).row()

        val subtitle = Label("2D Turn-Based Strategy", skin)
        subtitle.setFontScale(1.2f)
        subtitle.color = Color(0.7f, 0.7f, 0.7f, 1f)
        root.add(subtitle).padBottom(40f).row()

        val configPanel = Table(skin).apply { defaults().pad(5f) }

        val sizeTitle = Label("Map Size:", skin)
        sizeTitle.color = Color.CYAN
        configPanel.add(sizeTitle)

        val sizes = com.example.strategy.platform.GameFactory.MapSize.entries
        var sizeIdx = sizes.indexOf(selectedSize)
        sizeLabel = Label(selectedSize.name, skin)
        sizeLabel.color = Color.WHITE
        val prevSizeBtn = TextButton("<", skin)
        prevSizeBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                sizeIdx = (sizeIdx - 1 + sizes.size) % sizes.size
                selectedSize = sizes[sizeIdx]
                sizeLabel.setText(selectedSize.name)
            }
        })
        val nextSizeBtn = TextButton(">", skin)
        nextSizeBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                sizeIdx = (sizeIdx + 1) % sizes.size
                selectedSize = sizes[sizeIdx]
                sizeLabel.setText(selectedSize.name)
            }
        })
        configPanel.add(prevSizeBtn).width(40f)
        configPanel.add(sizeLabel).width(100f)
        configPanel.add(nextSizeBtn).width(40f).row()

        val terrainTitle = Label("Terrain:", skin)
        terrainTitle.color = Color.CYAN
        configPanel.add(terrainTitle)

        val terrains = com.example.strategy.platform.GameFactory.TerrainStyle.entries
        var terrIdx = terrains.indexOf(selectedTerrain)
        terrainLabel = Label(selectedTerrain.name, skin)
        terrainLabel.color = Color.WHITE
        val prevTerrBtn = TextButton("<", skin)
        prevTerrBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                terrIdx = (terrIdx - 1 + terrains.size) % terrains.size
                selectedTerrain = terrains[terrIdx]
                terrainLabel.setText(selectedTerrain.name)
            }
        })
        val nextTerrBtn = TextButton(">", skin)
        nextTerrBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                terrIdx = (terrIdx + 1) % terrains.size
                selectedTerrain = terrains[terrIdx]
                terrainLabel.setText(selectedTerrain.name)
            }
        })
        configPanel.add(prevTerrBtn).width(40f)
        configPanel.add(terrainLabel).width(100f)
        configPanel.add(nextTerrBtn).width(40f).row()

        val diffTitle = Label("Difficulty:", skin)
        diffTitle.color = Color.CYAN
        configPanel.add(diffTitle)

        val diffs = com.example.strategy.model.Difficulty.entries
        var diffIdx = diffs.indexOf(selectedDifficulty)
        difficultyLabel = Label(selectedDifficulty.displayName, skin)
        difficultyLabel.color = Color.WHITE
        val prevDiffBtn = TextButton("<", skin)
        prevDiffBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                diffIdx = (diffIdx - 1 + diffs.size) % diffs.size
                selectedDifficulty = diffs[diffIdx]
                difficultyLabel.setText(selectedDifficulty.displayName)
            }
        })
        val nextDiffBtn = TextButton(">", skin)
        nextDiffBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                diffIdx = (diffIdx + 1) % diffs.size
                selectedDifficulty = diffs[diffIdx]
                difficultyLabel.setText(selectedDifficulty.displayName)
            }
        })
        configPanel.add(prevDiffBtn).width(40f)
        configPanel.add(difficultyLabel).width(100f)
        configPanel.add(nextDiffBtn).width(40f).row()

        root.add(configPanel).padBottom(30f).row()

        val newGameBtn = makeButton("NEW GAME", Color(0.3f, 0.6f, 0.3f, 1f))
        newGameBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.gameState = com.example.strategy.platform.GameFactory.createGameState(selectedSize, selectedTerrain, selectedDifficulty)
                game.setScreen(GameScreen(game))
            }
        })

        val loadBtn = makeButton("LOAD GAME", Color(0.3f, 0.5f, 0.7f, 1f))
        loadBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val loaded = SaveManager.load()
                if (loaded != null) {
                    game.gameState = loaded
                    game.setScreen(GameScreen(game))
                }
            }
        })

        val quitBtn = makeButton("QUIT", Color(0.7f, 0.3f, 0.3f, 1f))
        quitBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Gdx.app.exit()
            }
        })

        root.add(newGameBtn).width(300f).height(60f).padBottom(15f).row()
        root.add(loadBtn).width(300f).height(60f).padBottom(15f).row()
        root.add(quitBtn).width(300f).height(60f).padBottom(15f).row()

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
