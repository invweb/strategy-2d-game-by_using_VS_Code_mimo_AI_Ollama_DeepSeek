package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.example.strategy.ai.AISettings
import com.badlogic.gdx.utils.viewport.ScreenViewport

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
        Locale.load()
        skin = SkinFactory.createSkin()
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        val root = Table(skin).apply { setFillParent(true) }

        val title = Label(Locale.MAIN_TITLE, skin)
        title.setFontScale(3f)
        title.color = Color(0.9f, 0.8f, 0.2f, 1f)
        root.add(title).padBottom(10f).row()

        val subtitle = Label(Locale.SUBTITLE, skin)
        subtitle.setFontScale(1.2f)
        subtitle.color = Color(0.7f, 0.7f, 0.7f, 1f)
        root.add(subtitle).padBottom(40f).row()

        val configPanel = Table(skin).apply { defaults().pad(5f) }

        val sizeTitle = Label(Locale.MAP_SIZE, skin)
        sizeTitle.color = Color.CYAN
        configPanel.add(sizeTitle)

        val sizes = com.example.strategy.platform.GameFactory.MapSize.entries
        var sizeIdx = sizes.indexOf(selectedSize)
        sizeLabel = Label(Locale.mapSizeName(selectedSize.name), skin)
        sizeLabel.color = Color.WHITE
        val prevSizeBtn = TextButton("<", skin)
        prevSizeBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                sizeIdx = (sizeIdx - 1 + sizes.size) % sizes.size
                selectedSize = sizes[sizeIdx]
                sizeLabel.setText(Locale.mapSizeName(selectedSize.name))
            }
        })
        val nextSizeBtn = TextButton(">", skin)
        nextSizeBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                sizeIdx = (sizeIdx + 1) % sizes.size
                selectedSize = sizes[sizeIdx]
                sizeLabel.setText(Locale.mapSizeName(selectedSize.name))
            }
        })
        configPanel.add(prevSizeBtn).width(40f)
        configPanel.add(sizeLabel).width(100f)
        configPanel.add(nextSizeBtn).width(40f).row()

        val terrainTitle = Label(Locale.TERRAIN, skin)
        terrainTitle.color = Color.CYAN
        configPanel.add(terrainTitle)

        val terrains = com.example.strategy.platform.GameFactory.TerrainStyle.entries
        var terrIdx = terrains.indexOf(selectedTerrain)
        terrainLabel = Label(Locale.terrainName(selectedTerrain.name), skin)
        terrainLabel.color = Color.WHITE
        val prevTerrBtn = TextButton("<", skin)
        prevTerrBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                terrIdx = (terrIdx - 1 + terrains.size) % terrains.size
                selectedTerrain = terrains[terrIdx]
                terrainLabel.setText(Locale.terrainName(selectedTerrain.name))
            }
        })
        val nextTerrBtn = TextButton(">", skin)
        nextTerrBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                terrIdx = (terrIdx + 1) % terrains.size
                selectedTerrain = terrains[terrIdx]
                terrainLabel.setText(Locale.terrainName(selectedTerrain.name))
            }
        })
        configPanel.add(prevTerrBtn).width(40f)
        configPanel.add(terrainLabel).width(100f)
        configPanel.add(nextTerrBtn).width(40f).row()

        val diffTitle = Label(Locale.DIFFICULTY, skin)
        diffTitle.color = Color.CYAN
        configPanel.add(diffTitle)

        val diffs = com.example.strategy.model.Difficulty.entries
        var diffIdx = diffs.indexOf(selectedDifficulty)
        difficultyLabel = Label(Locale.difficultyName(selectedDifficulty.displayName), skin)
        difficultyLabel.color = Color.WHITE
        val prevDiffBtn = TextButton("<", skin)
        prevDiffBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                diffIdx = (diffIdx - 1 + diffs.size) % diffs.size
                selectedDifficulty = diffs[diffIdx]
                difficultyLabel.setText(Locale.difficultyName(selectedDifficulty.displayName))
            }
        })
        val nextDiffBtn = TextButton(">", skin)
        nextDiffBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                diffIdx = (diffIdx + 1) % diffs.size
                selectedDifficulty = diffs[diffIdx]
                difficultyLabel.setText(Locale.difficultyName(selectedDifficulty.displayName))
            }
        })
        configPanel.add(prevDiffBtn).width(40f)
        configPanel.add(difficultyLabel).width(100f)
        configPanel.add(nextDiffBtn).width(40f).row()

        root.add(configPanel).padBottom(30f).row()

        val newGameBtn = makeButton(Locale.NEW_GAME, Color(0.3f, 0.6f, 0.3f, 1f))
        newGameBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.gameState = com.example.strategy.platform.GameFactory.createGameState(selectedSize, selectedTerrain, selectedDifficulty)
                game.setScreen(GameScreen(game))
            }
        })

        val loadBtn = makeButton(Locale.LOAD_GAME, Color(0.3f, 0.5f, 0.7f, 1f))
        loadBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { showLoadDialog() }
        })

        val settingsBtn = makeButton(Locale.SETTINGS, Color(0.4f, 0.4f, 0.5f, 1f))
        settingsBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { showSettingsDialog() }
        })

        val quitBtn = makeButton(Locale.QUIT, Color(0.7f, 0.3f, 0.3f, 1f))
        quitBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { Gdx.app.exit() }
        })

        root.add(newGameBtn).width(300f).height(60f).padBottom(15f).row()
        root.add(loadBtn).width(300f).height(60f).padBottom(15f).row()

        val multiBtn = makeButton(Locale.MULTIPLAYER, Color(0.3f, 0.5f, 0.7f, 1f))
        multiBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.setScreen(LobbyScreen(game))
            }
        })
        root.add(multiBtn).width(300f).height(60f).padBottom(15f).row()

        root.add(settingsBtn).width(300f).height(60f).padBottom(15f).row()
        root.add(quitBtn).width(300f).height(60f).padBottom(15f).row()

        val footer = Label(Locale.FOOTER, skin)
        footer.setFontScale(0.8f)
        footer.color = Color(0.5f, 0.5f, 0.5f, 1f)
        root.add(footer).padTop(40f).row()

        stage.addActor(root)
    }

    private fun showSettingsDialog() {
        AISettings.load()
        val win = Window(Locale.SETTINGS, skin)
        win.isModal = true; win.isMovable = true; win.pad(16f); win.defaults().pad(5f)

        val langTitle = Label(Locale.LANGUAGE, skin)
        langTitle.color = Color.CYAN
        win.add(langTitle).colspan(2).row()

        val langs = Locale.Lang.entries
        var langIdx = langs.indexOf(Locale.get())
        val langLabel = Label(Locale.get().displayName, skin)
        langLabel.color = Color.WHITE
        val prevLangBtn = TextButton("<", skin)
        val nextLangBtn = TextButton(">", skin)
        prevLangBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                langIdx = (langIdx - 1 + langs.size) % langs.size
                Locale.set(langs[langIdx])
                win.remove()
                game.setScreen(MenuScreen(game))
            }
        })
        nextLangBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                langIdx = (langIdx + 1) % langs.size
                Locale.set(langs[langIdx])
                win.remove()
                game.setScreen(MenuScreen(game))
            }
        })
        win.add(prevLangBtn).width(40f)
        win.add(langLabel).width(120f)
        win.add(nextLangBtn).width(40f).row()

        val aiTitle = Label(Locale.AI_BACKEND, skin)
        aiTitle.color = Color.CYAN
        win.add(aiTitle).colspan(4).padTop(15f).row()

        val backends = AISettings.Backend.entries
        var backendIdx = backends.indexOf(AISettings.backend)
        val backendLabel = Label(AISettings.backend.displayName, skin)
        backendLabel.color = Color.WHITE
        val prevBackendBtn = TextButton("<", skin)
        val nextBackendBtn = TextButton(">", skin)
        prevBackendBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                backendIdx = (backendIdx - 1 + backends.size) % backends.size
                AISettings.setBackend(backends[backendIdx])
                backendLabel.setText(AISettings.backend.displayName)
            }
        })
        nextBackendBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                backendIdx = (backendIdx + 1) % backends.size
                AISettings.setBackend(backends[backendIdx])
                backendLabel.setText(AISettings.backend.displayName)
            }
        })
        win.add(prevBackendBtn).width(40f)
        win.add(backendLabel).width(160f).colspan(2)
        win.add(nextBackendBtn).width(40f).row()

        val ollamaUrlLabel = Label(Locale.AI_OLLAMA_URL, skin)
        ollamaUrlLabel.color = Color.LIGHT_GRAY
        win.add(ollamaUrlLabel).colspan(4).row()
        val ollamaUrlField = TextField(AISettings.ollamaUrl, skin)
        win.add(ollamaUrlField).colspan(4).width(300f).padBottom(4f).row()

        val ollamaModelLabel = Label(Locale.AI_OLLAMA_MODEL, skin)
        ollamaModelLabel.color = Color.LIGHT_GRAY
        win.add(ollamaModelLabel).colspan(4).row()
        val ollamaModelField = TextField(AISettings.ollamaModel, skin)
        win.add(ollamaModelField).colspan(4).width(300f).padBottom(4f).row()

        val lmUrlLabel = Label(Locale.AI_LMSTUDIO_URL, skin)
        lmUrlLabel.color = Color.LIGHT_GRAY
        win.add(lmUrlLabel).colspan(4).row()
        val lmUrlField = TextField(AISettings.lmStudioUrl, skin)
        win.add(lmUrlField).colspan(4).width(300f).padBottom(4f).row()

        val lmModelLabel = Label(Locale.AI_LMSTUDIO_MODEL, skin)
        lmModelLabel.color = Color.LIGHT_GRAY
        win.add(lmModelLabel).colspan(4).row()
        val lmModelField = TextField(AISettings.lmStudioModel, skin)
        win.add(lmModelField).colspan(4).width(300f).padBottom(4f).row()

        val closeBtn = TextButton(Locale.CLOSE, skin)
        closeBtn.label.setFontScale(0.9f)
        closeBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                AISettings.setOllamaUrl(ollamaUrlField.text.trim())
                AISettings.setOllamaModel(ollamaModelField.text.trim())
                AISettings.setLmStudioUrl(lmUrlField.text.trim())
                AISettings.setLmStudioModel(lmModelField.text.trim())
                win.remove()
            }
        })
        win.add(closeBtn).width(120f).colspan(4).padTop(15f)

        win.pack()
        win.setPosition(Gdx.graphics.width / 2f - win.width / 2f, Gdx.graphics.height / 2f - win.height / 2f)
        stage.addActor(win)
    }

    private fun showLoadDialog() {
        val saves = SaveManager.listSaves()
        if (saves.isEmpty()) {
            val msg = Window("Info", skin)
            msg.isModal = true; msg.pad(16f)
            msg.add(Label(Locale.NO_SAVES, skin)).row()
            val okBtn = TextButton(Locale.OK, skin)
            okBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { msg.remove() } })
            msg.add(okBtn).width(80f).padTop(8f)
            msg.pack()
            msg.setPosition(Gdx.graphics.width / 2f - msg.width / 2f, Gdx.graphics.height / 2f - msg.height / 2f)
            stage.addActor(msg)
            return
        }

        var win: Window? = null

        fun rebuildList(savesList: List<String>) {
            win?.remove()
            if (savesList.isEmpty()) return

            val w = Window(Locale.LOAD_GAME, skin)
            w.isModal = true; w.isMovable = true; w.pad(16f)
            w.add(Label(Locale.SELECT_SAVE, skin)).colspan(3).row()

            val listTable = Table(skin)
            for (saveName in savesList) {
                val nameBtn = TextButton(saveName, skin)
                nameBtn.label.setFontScale(0.8f)
                nameBtn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        val loaded = SaveManager.load(saveName)
                        if (loaded != null) {
                            game.gameState = loaded
                            game.setScreen(GameScreen(game))
                            w.remove()
                        }
                    }
                })

                val trashRegion = SkinFactory.makeTrashIcon()
                val delStyle = com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle()
                delStyle.imageUp = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(trashRegion.texture, 0, 0, 0, 0))
                delStyle.imageDown = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(trashRegion.texture, 0, 0, 0, 0))
                val delBtn = com.badlogic.gdx.scenes.scene2d.ui.ImageButton(delStyle)
                delBtn.image.setScale(1.2f)
                delBtn.color = Color(0.8f, 0.25f, 0.25f, 1f)
                delBtn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        val confirm = Window(Locale.CONFIRM_DELETE, skin)
                        confirm.isModal = true; confirm.isMovable = true; confirm.pad(16f)
                        confirm.add(Label("${Locale.CONFIRM_DELETE} \"$saveName\"?", skin)).row()
                        val yesBtn = TextButton(Locale.DELETE, skin)
                        yesBtn.label.setFontScale(0.9f); yesBtn.color = Color(0.8f, 0.2f, 0.2f, 1f)
                        yesBtn.addListener(object : ClickListener() {
                            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                                SaveManager.deleteSave(saveName)
                                confirm.remove()
                                val remaining = SaveManager.listSaves()
                                if (remaining.isEmpty()) w.remove()
                                else rebuildList(remaining)
                            }
                        })
                        val noBtn = TextButton(Locale.CANCEL, skin)
                        noBtn.label.setFontScale(0.9f)
                        noBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { confirm.remove() } })
                        confirm.add(yesBtn).width(100f).padRight(10f)
                        confirm.add(noBtn).width(100f)
                        confirm.pack()
                        confirm.setPosition(Gdx.graphics.width / 2f - confirm.width / 2f, Gdx.graphics.height / 2f - confirm.height / 2f)
                        stage.addActor(confirm)
                    }
                })

                listTable.add(nameBtn).width(200f).fillX().padRight(8f)
                listTable.add(delBtn).width(40f)
                listTable.row()
            }

            val scrollPane = com.badlogic.gdx.scenes.scene2d.ui.ScrollPane(listTable, skin)
            w.add(scrollPane).colspan(3).width(260f).height(180f).padBottom(8f).row()

            val closeBtn = TextButton(Locale.CLOSE, skin)
            closeBtn.label.setFontScale(0.9f)
            closeBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { w.remove() } })
            w.add(closeBtn).width(120f)

            w.pack()
            w.setPosition(Gdx.graphics.width / 2f - w.width / 2f, Gdx.graphics.height / 2f - w.height / 2f)
            stage.addActor(w)
            win = w
        }

        rebuildList(saves)
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
}
