package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
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
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.strategy.model.*
import com.example.strategy.logic.ActionQueue
import com.example.strategy.logic.Economy
import com.example.strategy.logic.TurnManager

class GameScreen(private val game: StrategyGame) : ScreenAdapter() {

    private lateinit var stage: Stage
    private val camera = OrthographicCamera()
    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()

    private var selectedRegion: Region? = null
    private val selectedRegions = mutableListOf<Region>()
    private var state = game.gameState
    private var actionUsedThisTurn = false
    private var attackMode = false
    private var attackSourceId = -1
    private var moveMode = false
    private var moveSourceId = -1

    private val tileSize = 128f
    private val animManager = AnimationManager()
    private var soundManager: SoundManager? = null
    private var animTime = 0f
    private var aiPending = false
    private var alive = false
    private var tutorialTimer = 8f
    private var tutorialLabel: Label? = null
    private var zoomHintTimer = 0f
    private var zoomHintLabel: Label? = null

    private lateinit var mapRenderer: MapRenderer
    private lateinit var gameInput: GameInput

    private lateinit var infoLabel: Label
    private lateinit var statusLabel: Label
    private lateinit var diplomacyLabel: Label
    private lateinit var techLabel: Label
    private lateinit var statsLabel: Label
    private lateinit var skin: Skin
    private var showStats = false
    private val actionButtons = mutableListOf<TextButton>()
    private val buildButtons = mutableListOf<TextButton>()
    private val diploButtons = mutableListOf<TextButton>()
    private val techButtons = mutableListOf<TextButton>()

    override fun show() {
        alive = true
        soundManager?.dispose()
        soundManager = SoundManager()
        skin = createSkin()
        stage = Stage(ScreenViewport())

        mapRenderer = MapRenderer(batch, shapeRenderer, tileSize, game)
        mapRenderer.generateAll()

        gameInput = GameInput(
            game, camera, stage, tileSize,
            stateProvider = { state },
            stateSetter = { state = it; game.gameState = it },
            selectedRegionProvider = { selectedRegion },
            selectedRegionSetter = { selectedRegion = it },
            selectedRegionsProvider = { selectedRegions },
            actionUsedThisTurnProvider = { actionUsedThisTurn },
            actionUsedThisTurnSetter = { actionUsedThisTurn = it },
            attackModeProvider = { attackMode },
            attackModeSetter = { attackMode = it },
            attackSourceIdProvider = { attackSourceId },
            attackSourceIdSetter = { attackSourceId = it },
            moveModeProvider = { moveMode },
            moveModeSetter = { moveMode = it },
            moveSourceIdProvider = { moveSourceId },
            moveSourceIdSetter = { moveSourceId = it },
            aiPendingProvider = { aiPending },
            updateInfoLabel = { updateInfoLabel() },
            infoLabelSetter = { infoLabel.setText(it) },
            statusLabelSetter = { statusLabel.setText(it) },
            statusLabelColorSetter = { statusLabel.color = it },
            soundPlayer = { soundManager?.play(it) },
            mapRenderer = mapRenderer
        )

        buildUI()
        gameInput.setup()

        val mapPixelW = state.map.width * tileSize
        val mapPixelH = state.map.height * tileSize
        camera.position.set(mapPixelW / 2f, mapPixelH / 2f, 0f)
        camera.zoom = 1.2f
        camera.viewportWidth = Gdx.graphics.width.toFloat()
        camera.viewportHeight = Gdx.graphics.height.toFloat()
        camera.update()

        state = TurnManager.startTurn(state)
        game.gameState = state
        updateInfoLabel()

        tutorialTimer = 8f
        tutorialLabel = Label(Locale.TUTORIAL_HINT, skin)
        tutorialLabel!!.color = Color(1f, 1f, 0.5f, 0.9f)
        tutorialLabel!!.setFontScale(0.9f)
        tutorialLabel!!.setWrap(true)
        tutorialLabel!!.setSize(500f, 60f)
        tutorialLabel!!.setPosition(Gdx.graphics.width / 2f - 250f, Gdx.graphics.height - 50f)
        stage.addActor(tutorialLabel)
    }

    private fun buildUI() {
        val root = Table(skin).apply { setFillParent(true) }

        val topPanel = Table(skin).apply { left().top().pad(10f); defaults().left().padBottom(2f) }
        infoLabel = Label(Locale.CLICK_REGION, skin)
        statusLabel = Label("", skin)
        statsLabel = Label("", skin)
        statsLabel.color = Color.LIGHT_GRAY
        topPanel.add(infoLabel).row()
        topPanel.add(statusLabel).row()
        topPanel.add(statsLabel).row()
        root.add(topPanel).left().top().expandX().colspan(2).row()

        val panel = Table(skin).apply { right().bottom().pad(10f); defaults().pad(3f).right() }

        fun btn(text: String, action: String): TextButton {
            val b = TextButton(text, skin)
            b.label.setFontScale(0.75f)
            b.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) { handleAction(action) }
            })
            actionButtons.add(b)
            return b
        }

        fun diploBtn(text: String, action: String): TextButton {
            val b = TextButton(text, skin)
            b.label.setFontScale(0.65f)
            b.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) { handleAction(action) }
            })
            diploButtons.add(b)
            return b
        }

        panel.add(btn(Locale.BUILD_FARM, Actions.BUILD_FARM)).fillX()
        panel.add(btn(Locale.BUILD_LUMBER, Actions.BUILD_LUMBER_MILL)).fillX()
        panel.add(btn(Locale.BUILD_BARRACKS_COST, Actions.BUILD_BARRACKS)).fillX().row()
        panel.add(btn(Locale.BUILD_MINE_COST, Actions.BUILD_MINE)).fillX()
        panel.add(btn(Locale.RECRUIT_COST, Actions.RECRUIT)).fillX()
        panel.add(btn(Locale.RECRUIT_INFANTRY_COST, Actions.RECRUIT_INFANTRY)).fillX().row()
        panel.add(btn(Locale.RECRUIT_CAVALRY_COST, Actions.RECRUIT_CAVALRY)).fillX().row()
        panel.add(btn(Locale.RECRUIT_SIEGE_COST, Actions.RECRUIT_SIEGE)).fillX()
        panel.add(btn(Locale.DEVELOP_COST, Actions.DEVELOP)).fillX()
        panel.add(btn(Locale.MOVE_BTN, Actions.MOVE)).fillX().row()
        panel.add(btn(Locale.ATTACK_BTN, Actions.ATTACK)).fillX()

        val endBtn = TextButton(Locale.END_TURN, skin)
        endBtn.label.setFontScale(0.75f); endBtn.label.color = Color.GOLD
        endBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { handleAction(Actions.END_TURN) } })
        panel.add(endBtn).fillX().padLeft(10f)

        val menuBtn = TextButton(Locale.MENU, skin)
        menuBtn.label.setFontScale(0.75f); menuBtn.label.color = Color.LIGHT_GRAY
        menuBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { game.setScreen(MenuScreen(game)) } })
        panel.add(menuBtn).fillX().padLeft(10f)

        val saveBtn = TextButton(Locale.SAVE, skin)
        saveBtn.label.setFontScale(0.7f); saveBtn.label.color = Color.CYAN
        saveBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { showSaveDialog() } })
        panel.add(saveBtn).fillX().padLeft(10f)

        val loadBtn = TextButton(Locale.LOAD, skin)
        loadBtn.label.setFontScale(0.7f); loadBtn.label.color = Color.CYAN
        loadBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { showLoadDialog() } })
        panel.add(loadBtn).fillX().padLeft(20f)

        val quitBtn = TextButton(Locale.QUIT, skin)
        quitBtn.label.setFontScale(0.75f); quitBtn.label.color = Color.RED
        quitBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val win = Window("", skin)
                win.isModal = true; win.isMovable = true; win.pad(16f)
                win.add(Label(Locale.SAVE_QUESTION, skin)).colspan(2).row()
                val saveBtn = TextButton(Locale.SAVE, skin)
                saveBtn.label.setFontScale(0.9f); saveBtn.color = Color(0.3f, 0.6f, 0.3f, 1f)
                saveBtn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        win.remove(); showSaveDialog(); game.setScreen(MenuScreen(game))
                    }
                })
                val noSaveBtn = TextButton(Locale.NO_SAVE, skin)
                noSaveBtn.label.setFontScale(0.9f)
                noSaveBtn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) { win.remove(); game.setScreen(MenuScreen(game)) }
                })
                val cancelBtn = TextButton(Locale.CANCEL, skin)
                cancelBtn.label.setFontScale(0.9f)
                cancelBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { win.remove() } })
                win.add(saveBtn).width(120f).padRight(8f)
                win.add(noSaveBtn).width(120f).padRight(8f)
                win.add(cancelBtn).width(120f)
                win.pack()
                win.setPosition(Gdx.graphics.width / 2f - win.width / 2f, Gdx.graphics.height / 2f - win.height / 2f)
                stage.addActor(win)
            }
        })
        panel.add(quitBtn).fillX().padLeft(10f)

        val diploPanel = Table(skin).apply { right().top().pad(10f); defaults().pad(2f) }
        diplomacyLabel = Label(Locale.DIPLOMACY, skin)
        diplomacyLabel.color = Color.CYAN
        diploPanel.add(diplomacyLabel).colspan(2).row()
        diploPanel.add(diploBtn("Alliance", Actions.DIPLO_ALLIANCE)).fillX()
        diploPanel.add(diploBtn("Break", Actions.DIPLO_BREAK)).fillX().row()
        diploPanel.add(diploBtn("Trade", Actions.DIPLO_TRADE)).fillX()
        diploPanel.add(diploBtn("Cancel", Actions.DIPLO_CANCEL_TRADE)).fillX().row()

        val techPanel = Table(skin).apply { left().bottom().pad(10f); defaults().pad(2f) }
        techLabel = Label(Locale.TECHS, skin)
        techLabel.color = Color.YELLOW
        techPanel.add(techLabel).colspan(2).row()
        for (tech in com.example.strategy.model.TECH_TREE) {
            val b = TextButton(tech.name.take(8), skin)
            b.label.setFontScale(0.55f)
            b.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { handleAction("${Actions.RESEARCH}:${tech.type.name}") } })
            techButtons.add(b)
            techPanel.add(b).fillX().colspan(2).row()
        }

        val zoomPanel = Table(skin).apply { left().bottom().pad(10f); defaults().pad(3f) }
        zoomHintLabel = Label("", skin)
        zoomHintLabel!!.color = Color.LIGHT_GRAY
        zoomHintLabel!!.setFontScale(0.7f)
        zoomPanel.add(zoomHintLabel).colspan(2).row()

        val zoomBgPix = Pixmap(16, 16, Pixmap.Format.RGBA8888).apply { setColor(1f, 1f, 0.4f, 0.35f); fill() }
        val zoomBgTex = com.badlogic.gdx.graphics.Texture(zoomBgPix); zoomBgPix.dispose()
        val zoomUp = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(zoomBgTex, 4, 4, 4, 4))
        val zoomDownPix = Pixmap(16, 16, Pixmap.Format.RGBA8888).apply { setColor(1f, 1f, 0.4f, 0.55f); fill() }
        val zoomDownTex = com.badlogic.gdx.graphics.Texture(zoomDownPix); zoomDownPix.dispose()
        val zoomDown = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(zoomDownTex, 4, 4, 4, 4))
        val zoomBtnStyle = TextButton.TextButtonStyle().apply {
            font = skin.getFont("default-font"); fontColor = Color.WHITE; up = zoomUp; down = zoomDown
        }
        skin.add("zoom-btn", zoomBtnStyle)
        val zoomInBtn = TextButton("+", zoomBtnStyle)
        zoomInBtn.label.setFontScale(1.2f)
        zoomInBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                camera.zoom = (camera.zoom - 0.1f).coerceIn(0.3f, 3.0f); camera.update()
                zoomHintLabel?.setText(Locale.ZOOM_HINT); zoomHintTimer = 2f
            }
        })
        val zoomOutBtn = TextButton("-", zoomBtnStyle)
        zoomOutBtn.label.setFontScale(1.2f)
        zoomOutBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                camera.zoom = (camera.zoom + 0.1f).coerceIn(0.3f, 3.0f); camera.update()
                zoomHintLabel?.setText(Locale.ZOOM_HINT); zoomHintTimer = 2f
            }
        })
        zoomPanel.add(zoomInBtn).width(50f)
        zoomPanel.add(zoomOutBtn).width(50f)

        root.add().expandY()
        root.add(diploPanel).right().top().pad(10f).padTop(40f).row()
        root.add(zoomPanel).left().bottom().pad(10f)
        root.add(panel).right().bottom().pad(10f)
        stage.addActor(root)
    }

    private fun updateInfoLabel() {
        val r = selectedRegion
        val player = state.currentPlayer()
        val myTerritories = state.map.regions.count { it.ownerId == 0 }
        val enemyTerritories = state.map.regions.count { it.ownerId == 1 }
        val myPop = state.map.regions.filter { it.ownerId == 0 }.sumOf { it.population }
        val enemyPop = state.map.regions.filter { it.ownerId == 1 }.sumOf { it.population }
        val income = Economy.calculateIncome(player!!, state.map)
        val upkeep = Economy.upkeepCost(player, state.map)

        val diplo = state.diplomacy.getRelation(state.currentPlayerId, 1)
        val diploStatus = when (diplo.status) {
            DiplomacyStatus.ALLIED -> "ALLIED (${diplo.turnsAllied} turns)"
            DiplomacyStatus.TRADE_PARTNERS -> "TRADE PARTNERS"
            DiplomacyStatus.ENEMY -> "ENEMY"
            DiplomacyStatus.NEUTRAL -> "NEUTRAL"
        }
        diplomacyLabel.setText("${Locale.DIPLOMACY} $diploStatus${if (diplo.tradeActive) " + ${Locale.TRADE_ACTIVE}" else ""}")

        if (selectedRegions.size > 1) {
            val totalPop = selectedRegions.sumOf { it.population }
            val totalAttack = selectedRegions.sumOf { it.population + it.units.totalAttack() }
            val totalDefense = selectedRegions.sumOf { it.population + it.units.totalDefense() + it.buildings.count { b -> b.type == BuildingType.WALL } * 5 }
            val names = selectedRegions.joinToString { it.name }
            infoLabel.setText("${Locale.SELECTED} ${selectedRegions.size} ${Locale.TERRITORIES.lowercase()}: $names\n${Locale.TOTAL_POP}: $totalPop | ${Locale.ATTACK}: $totalAttack | ${Locale.DEFENSE}: $totalDefense")
        } else if (r == null) {
            infoLabel.setText(Locale.CLICK_REGION)
        } else if (!state.fog.isExplored(0, r.id)) {
            infoLabel.setText(Locale.UNKNOWN_TERRITORY)
        } else {
            val owner = when (r.ownerId) { 0 -> Locale.YOURS; 1 -> Locale.ENEMY; else -> Locale.NEUTRAL }
            val buildings = if (r.buildings.isEmpty()) Locale.NO_BUILDINGS else r.buildings.joinToString { it.type.name }
            val attack = r.population + r.units.totalAttack()
            val defense = r.population + r.units.totalDefense() + r.buildings.count { it.type == BuildingType.WALL } * 5
            val unitInfo = if (r.units.units.isEmpty()) Locale.NO_UNITS else r.units.units.joinToString { "${it.count} ${it.type.name.lowercase()}" }
            infoLabel.setText("${r.name} | ${r.terrain} | $owner\n${Locale.POPULATION}: ${r.population} (${Locale.ATTACK}: $attack, ${Locale.DEFENSE}: $defense)\n${Locale.UNITS}: $unitInfo\n${Locale.BUILDINGS}: $buildings")
        }

        val aiInfo = when (com.example.strategy.ai.AISettings.backend) {
            com.example.strategy.ai.AISettings.Backend.NONE -> "AI: Fallback"
            com.example.strategy.ai.AISettings.Backend.OLLAMA -> "AI: Ollama (${com.example.strategy.ai.AISettings.ollamaModel})"
            com.example.strategy.ai.AISettings.Backend.LM_STUDIO -> "AI: LM Studio (${com.example.strategy.ai.AISettings.lmStudioModel.ifEmpty { "default" }})"
        }
        val isMyTurn = state.currentPlayerId == 0
        statusLabel.setText(
            "${Locale.TURN} ${state.turn} | ${player?.name ?: "?"}\n" +
            "${Locale.TERRITORIES}: ${Locale.YOURS} $myTerritories vs $enemyTerritories\n" +
            "${Locale.POPULATION}: ${Locale.YOURS} $myPop vs $enemyPop\n" +
            "${Locale.INCOME}: +${income.food}F +${income.wood}W +${income.stone}S +${income.gold}G | ${Locale.UPKEEP}: -${upkeep.food}F\n" +
            "$aiInfo\n" +
            when {
                actionUsedThisTurn -> Locale.ACTION_USED
                isMyTurn -> Locale.YOUR_TURN
                else -> Locale.WAITING
            }
        )
        statusLabel.color = when {
            actionUsedThisTurn -> Color.ORANGE
            isMyTurn -> Color.GREEN
            else -> Color.GRAY
        }

        val dimmed = actionUsedThisTurn || !isMyTurn
        for (b in actionButtons) b.color.a = if (dimmed) 0.3f else 1f
        for ((i, b) in techButtons.withIndex()) {
            val tech = com.example.strategy.model.TECH_TREE[i]
            val researched = state.currentPlayer()?.techs?.isResearched(tech.type) == true
            val canResearch = state.currentPlayer()?.techs?.canResearch(tech.type) == true
            when {
                researched -> { b.color.a = 1f; b.label.color = Color.GREEN }
                canResearch && !dimmed -> { b.color.a = 1f; b.label.color = Color.WHITE }
                else -> { b.color.a = 0.3f; b.label.color = Color.GRAY }
            }
        }
        updateStatsLabel()
    }

    private fun updateStatsLabel() {
        if (!showStats) { statsLabel.setText(""); return }
        val history = state.history
        if (history.isEmpty()) { statsLabel.setText(Locale.NO_HISTORY); return }
        statsLabel.setText(Locale.STATS_HEADER + "\n" + history.takeLast(5).joinToString("\n") { h ->
            "T${h.turn}: ${h.territories} ${Locale.TERR}, ${h.population} ${Locale.POP}, F${h.resources.food} W${h.resources.wood} S${h.resources.stone} G${h.resources.gold}"
        })
    }

    private fun showSaveDialog() {
        val win = Window(Locale.SAVE, skin)
        win.isModal = true; win.isMovable = true; win.pad(16f); win.defaults().pad(5f)
        win.add(Label(Locale.SELECT_SAVE, skin)).colspan(2).row()
        val nameField = TextField("save_${state.turn}", skin)
        win.add(nameField).colspan(2).width(250f).padBottom(8f).row()
        val saveBtn = TextButton(Locale.SAVE, skin)
        saveBtn.label.setFontScale(0.9f)
        saveBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val name = nameField.text.trim()
                if (name.isEmpty()) return
                if (SaveManager.save(state, name)) { statusLabel.setText(Locale.SAVED + name); statusLabel.color = Color.CYAN }
                else { statusLabel.setText(Locale.SAVE_FAILED); statusLabel.color = Color.RED }
                win.remove()
            }
        })
        win.add(saveBtn).width(120f)
        val cancelBtn = TextButton(Locale.CANCEL, skin)
        cancelBtn.label.setFontScale(0.9f)
        cancelBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { win.remove() } })
        win.add(cancelBtn).width(120f)
        win.pack()
        win.setPosition(Gdx.graphics.width / 2f - win.width / 2f, Gdx.graphics.height / 2f - win.height / 2f)
        stage.addActor(win)
    }

    private fun showLoadDialog() {
        val saves = SaveManager.listSaves()
        if (saves.isEmpty()) { statusLabel.setText(Locale.NO_SAVES); statusLabel.color = Color.RED; return }

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
                            state = loaded; game.gameState = state; selectedRegion = null; selectedRegions.clear()
                            actionUsedThisTurn = false; attackMode = false; attackSourceId = -1; moveMode = false; moveSourceId = -1
                            statusLabel.setText(Locale.LOADED + saveName); statusLabel.color = Color.CYAN
                            updateInfoLabel(); w.remove()
                        } else { statusLabel.setText(Locale.LOAD_FAILED); statusLabel.color = Color.RED }
                    }
                })
                val trashRegion = makeTrashIcon()
                val delStyle = com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle()
                delStyle.imageUp = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(trashRegion.texture, 0, 0, 0, 0))
                delStyle.imageDown = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(trashRegion.texture, 0, 0, 0, 0))
                val delBtn = com.badlogic.gdx.scenes.scene2d.ui.ImageButton(delStyle)
                delBtn.image.setScale(1.2f); delBtn.color = Color(0.8f, 0.25f, 0.25f, 1f)
                delBtn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        val confirm = Window(Locale.CONFIRM_DELETE, skin)
                        confirm.isModal = true; confirm.isMovable = true; confirm.pad(16f)
                        confirm.add(Label("${Locale.CONFIRM_DELETE} \"$saveName\"?", skin)).row()
                        val yesBtn = TextButton(Locale.DELETE, skin)
                        yesBtn.label.setFontScale(0.9f); yesBtn.color = Color(0.8f, 0.2f, 0.2f, 1f)
                        yesBtn.addListener(object : ClickListener() {
                            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                                SaveManager.deleteSave(saveName); confirm.remove()
                                val remaining = SaveManager.listSaves()
                                if (remaining.isEmpty()) w.remove() else rebuildList(remaining)
                            }
                        })
                        val noBtn = TextButton(Locale.CANCEL, skin)
                        noBtn.label.setFontScale(0.9f)
                        noBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { confirm.remove() } })
                        confirm.add(yesBtn).width(100f).padRight(8f)
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
            stage.addActor(w); win = w
        }
        rebuildList(saves)
    }

    private fun makeTrashIcon(): com.badlogic.gdx.graphics.g2d.TextureRegion {
        val p = com.badlogic.gdx.graphics.Pixmap(24, 24, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        p.setColor(Color(0.9f, 0.2f, 0.2f, 1f))
        p.fillRectangle(5, 4, 14, 3); p.fillRectangle(7, 7, 2, 14); p.fillRectangle(11, 7, 2, 14)
        p.fillRectangle(15, 7, 2, 14); p.fillRectangle(3, 18, 18, 3); p.fillRectangle(9, 1, 6, 4)
        val t = com.badlogic.gdx.graphics.Texture(p); p.dispose()
        return com.badlogic.gdx.graphics.g2d.TextureRegion(t)
    }

    private fun handleAction(actionType: String) {
        try {
            if (actionType == Actions.END_TURN) {
                state = TurnManager.endTurn(state)
                game.gameState = state; selectedRegion = null; selectedRegions.clear(); actionUsedThisTurn = false
                attackMode = false; attackSourceId = -1; moveMode = false; moveSourceId = -1
                soundManager?.play(SoundManager.SoundType.END_TURN)
                runAITurns(); updateInfoLabel()
                return
            }
            if (actionUsedThisTurn || aiPending) return
            if (actionType.startsWith("DIPLO_")) { handleDiploAction(actionType); return }
            if (actionType.startsWith(Actions.RESEARCH + ":")) {
                val action = ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.RESEARCH, 0, actionType.removePrefix(Actions.RESEARCH + ":"))
                ActionQueue.enqueue(action); state = ActionQueue.processAll(state)
                game.gameState = state; actionUsedThisTurn = true
                soundManager?.play(SoundManager.SoundType.RESEARCH); updateInfoLabel()
                return
            }
            val region = selectedRegion
            if (region == null) return
            if (actionType == Actions.ATTACK) {
                if (region.ownerId != state.currentPlayerId) return
                attackMode = true; attackSourceId = region.id
                infoLabel.setText("${Locale.ATTACK_MODE} ${region.name}")
                return
            }
            if (actionType == Actions.MOVE) {
                if (region.ownerId != state.currentPlayerId) return
                moveMode = true; moveSourceId = region.id
                infoLabel.setText("${Locale.MOVE_MODE} ${region.name}")
                return
            }
            val action = when (actionType) {
                Actions.BUILD_FARM -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.BUILD, region.id, "FARM")
                Actions.BUILD_LUMBER_MILL -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.BUILD, region.id, "LUMBER_MILL")
                Actions.BUILD_BARRACKS -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.BUILD, region.id, "BARRACKS")
                Actions.BUILD_MINE -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.BUILD, region.id, "MINE")
                Actions.RECRUIT -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.RECRUIT, region.id)
                Actions.RECRUIT_INFANTRY -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.RECRUIT_INFANTRY, region.id)
                Actions.RECRUIT_CAVALRY -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.RECRUIT_CAVALRY, region.id)
                Actions.RECRUIT_SIEGE -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.RECRUIT_SIEGE, region.id)
                Actions.DEVELOP -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.DEVELOP, region.id)
                else -> return
            }
            ActionQueue.enqueue(action); state = ActionQueue.processAll(state)
            game.gameState = state; selectedRegion = state.map.getRegionById(region.id); actionUsedThisTurn = true
            updateInfoLabel()
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Action error: ${e.message}")
        }
    }

    private fun handleDiploAction(actionType: String) {
        if (actionUsedThisTurn) return
        val action = when (actionType) {
            Actions.DIPLO_ALLIANCE -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.PROPOSE_ALLIANCE, 1)
            Actions.DIPLO_BREAK -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.BREAK_ALLIANCE, 1)
            Actions.DIPLO_TRADE -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.PROPOSE_TRADE, 1)
            Actions.DIPLO_CANCEL_TRADE -> ActionQueue.GameAction(state.currentPlayerId, ActionQueue.ActionType.CANCEL_TRADE, 1)
            else -> return
        }
        ActionQueue.enqueue(action); state = ActionQueue.processAll(state)
        game.gameState = state; actionUsedThisTurn = true
        soundManager?.play(SoundManager.SoundType.ALLIANCE); updateInfoLabel()
    }

    private fun runAITurns() {
        if (state.currentPlayerId == 0) return
        aiPending = true
        state = TurnManager.startTurn(state)
        game.gameState = state
        Thread {
            try {
                val aiAction = com.example.strategy.ai.OllamaAI.decide(state)
                Gdx.app.postRunnable { if (alive) applyAIAction(aiAction) }
            } catch (e: Exception) {
                Gdx.app.postRunnable { if (alive) applyAIAction(null) }
            }
        }.start()
    }

    private fun applyAIAction(aiAction: com.example.strategy.ai.OllamaAI.AIAction?) {
        if (aiAction != null) {
            val action = ActionQueue.GameAction(state.currentPlayerId, aiAction.actionType, aiAction.targetRegionId, aiAction.param)
            ActionQueue.enqueue(action); state = ActionQueue.processAll(state)
        }
        state = TurnManager.endTurn(state)
        game.gameState = state; aiPending = false; updateInfoLabel()
    }

    override fun render(delta: Float) {
        try {
            Gdx.gl.glClearColor(0.1f, 0.15f, 0.3f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            if (gameInput.isPanning) {
                if (!Gdx.input.isButtonPressed(1) && !Gdx.input.isButtonPressed(2)) {
                } else {
                    camera.translate(
                        (Gdx.input.x.toFloat() - Gdx.input.x.toFloat()) * 1.5f * camera.zoom,
                        (Gdx.input.y.toFloat() - Gdx.input.y.toFloat()) * 1.5f * camera.zoom
                    )
                }
            }

            camera.update()
            batch.projectionMatrix = camera.combined
            animTime += delta
            if (zoomHintTimer > 0f) {
                zoomHintTimer -= delta
                if (zoomHintTimer <= 0f) { zoomHintLabel?.setText(""); zoomHintTimer = 0f }
            }
            if (tutorialTimer > 0f) {
                tutorialTimer -= delta
                if (tutorialTimer <= 0f) { tutorialLabel?.remove(); tutorialTimer = 0f }
            }

            mapRenderer.drawTiles(state, animTime, actionUsedThisTurn, selectedRegions)
            mapRenderer.drawSelectionBox(gameInput.isBoxSelecting, gameInput.boxStartScreenX, gameInput.boxStartScreenY)

            animManager.update(delta)
            if (animManager.hasAnimations()) {
                shapeRenderer.projectionMatrix = camera.combined
                animManager.render(shapeRenderer, tileSize)
            }

            stage.act(delta); stage.draw()

            game.batch.begin()
            val player = state.currentPlayer()
            val resText = "Food: ${player?.resources?.food ?: 0}   Wood: ${player?.resources?.wood ?: 0}   Stone: ${player?.resources?.stone ?: 0}   Gold: ${player?.resources?.gold ?: 0}   Iron: ${player?.resources?.iron ?: 0}"
            val resLayout = GlyphLayout(game.font, resText)
            game.font.color = Color.WHITE
            game.font.draw(game.batch, resText, Gdx.graphics.width - resLayout.width - 12f, Gdx.graphics.height - 12f)
            game.batch.end()
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Render error: ${e.message}", e)
        }
    }

    override fun hide() { alive = false }
    override fun resize(width: Int, height: Int) { camera.viewportWidth = width.toFloat(); camera.viewportHeight = height.toFloat(); camera.update(); stage.viewport.update(width, height, true) }
    override fun dispose() { batch.dispose(); shapeRenderer.dispose(); soundManager?.dispose(); mapRenderer.dispose(); stage.dispose(); skin.dispose() }

    private fun createSkin(): Skin {
        val s = Skin()
        val font = generateFont()
        s.add("default-font", font, BitmapFont::class.java)
        val upPix = com.badlogic.gdx.graphics.Pixmap(4, 4, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888).apply { setColor(Color(0.25f, 0.25f, 0.3f, 0.9f)); fill() }
        val downPix = com.badlogic.gdx.graphics.Pixmap(4, 4, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888).apply { setColor(Color(0.35f, 0.35f, 0.4f, 1f)); fill() }
        val overPix = com.badlogic.gdx.graphics.Pixmap(4, 4, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888).apply { setColor(Color(0.3f, 0.3f, 0.35f, 1f)); fill() }
        val upTex = com.badlogic.gdx.graphics.Texture(upPix); upPix.dispose()
        val downTex = com.badlogic.gdx.graphics.Texture(downPix); downPix.dispose()
        val overTex = com.badlogic.gdx.graphics.Texture(overPix); overPix.dispose()
        s.add("default", TextButton.TextButtonStyle().apply { this.font = font; fontColor = Color.WHITE; up = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 2, 2, 2, 2)); down = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(downTex, 2, 2, 2, 2)); over = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(overTex, 2, 2, 2, 2)) })
        s.add("default", Label.LabelStyle(font, Color.WHITE))
        val windowBgPix = com.badlogic.gdx.graphics.Pixmap(32, 32, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888).apply { setColor(0.08f, 0.09f, 0.14f, 1f); fill() }
        val windowBgTex = com.badlogic.gdx.graphics.Texture(windowBgPix); windowBgPix.dispose()
        val windowBg = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(windowBgTex, 4, 4, 4, 4))
        s.add("default", Window.WindowStyle(font, Color.CYAN, windowBg))
        s.add("default", TextField.TextFieldStyle().apply { this.font = font; fontColor = Color.WHITE; background = windowBg; cursor = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1)); selection = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1)) })
        val listSelTex = com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888).apply { setColor(0.3f, 0.5f, 0.7f, 1f); fill() })
        val listStyle = com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle()
        listStyle.javaClass.getDeclaredField("font").apply { isAccessible = true }.set(listStyle, font)
        listStyle.selection = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(listSelTex, 0, 0, 0, 0))
        listStyle.fontColorSelected = Color.WHITE; listStyle.fontColorUnselected = Color.LIGHT_GRAY
        s.add("default", listStyle)
        s.add("default", com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle().apply { background = windowBg; vScroll = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1)); hScroll = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(upTex, 1, 1, 1, 1)); vScrollKnob = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(downTex, 1, 1, 1, 1)); hScrollKnob = NinePatchDrawable(com.badlogic.gdx.graphics.g2d.NinePatch(downTex, 1, 1, 1, 1)) })
        return s
    }

    private fun generateFont(): BitmapFont {
        val fontPaths = arrayOf("/System/Library/Fonts/Supplemental/Arial.ttf", "/System/Library/Fonts/Helvetica.ttc", "/Library/Fonts/Arial.ttf")
        var generator: com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator? = null
        for (path in fontPaths) { if (java.io.File(path).exists()) { generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(Gdx.files.absolute(path)); break } }
        if (generator == null) generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(Gdx.files.absolute(fontPaths[0]))
        val params = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter()
        params.size = 16; params.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear; params.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        params.characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,*':?!@#$%&()-+=/<>" + "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" + "äöüÄÖÜß «»—…"
        val font = generator.generateFont(params); generator.dispose(); return font
    }
}
