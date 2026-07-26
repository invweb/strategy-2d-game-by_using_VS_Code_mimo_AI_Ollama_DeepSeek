package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
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
import com.example.strategy.model.*
import com.example.strategy.logic.ActionQueue
import com.example.strategy.logic.Economy

class GameUI(
    private val stage: Stage,
    private val skin: Skin,
    private val stateProvider: () -> GameState,
    private val selectedRegionProvider: () -> Region?,
    private val selectedRegionsProvider: () -> List<Region>,
    private val actionUsedThisTurnProvider: () -> Boolean,
    private val aiPendingProvider: () -> Boolean,
    private val gameOverProvider: () -> Boolean,
    private val actionHandler: (String) -> Unit,
    private val undoHandler: () -> Unit,
    private val menuHandler: () -> Unit,
    private val camera: com.badlogic.gdx.graphics.OrthographicCamera,
    private val soundPlayer: (SoundManager.SoundType) -> Unit,
    private val stateSetter: (GameState) -> Unit,
    private val resetMode: () -> Unit,
    private val infoLabelRef: () -> Label,
    private val statusLabelRef: () -> Label,
) {
    lateinit var infoLabel: Label
        private set
    lateinit var statusLabel: Label
        private set
    lateinit var diplomacyLabel: Label
        private set
    lateinit var techLabel: Label
        private set
    lateinit var statsLabel: Label
        private set
    var showStats = false
    val actionButtons = mutableListOf<TextButton>()
    val techButtons = mutableListOf<TextButton>()

    private var zoomHintTimer = 0f
    private var zoomHintLabel: Label? = null

    fun build() {
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
                override fun clicked(event: InputEvent?, x: Float, y: Float) { actionHandler(action) }
            })
            actionButtons.add(b)
            return b
        }

        fun diploBtn(text: String, action: String): TextButton {
            val b = TextButton(text, skin)
            b.label.setFontScale(0.65f)
            b.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) { actionHandler(action) }
            })
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

        val upgradeBtn = TextButton(Locale.UPGRADE_BTN, skin)
        upgradeBtn.label.setFontScale(0.7f); upgradeBtn.label.color = Color(0.8f, 0.7f, 0.2f, 1f)
        upgradeBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { actionHandler(Actions.UPGRADE) } })
        actionButtons.add(upgradeBtn)
        panel.add(upgradeBtn).fillX().padLeft(10f)

        val endBtn = TextButton(Locale.END_TURN, skin)
        endBtn.label.setFontScale(0.75f); endBtn.label.color = Color.GOLD
        endBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { actionHandler(Actions.END_TURN) } })
        panel.add(endBtn).fillX().padLeft(10f)

        val menuBtn = TextButton(Locale.MENU, skin)
        menuBtn.label.setFontScale(0.75f); menuBtn.label.color = Color.LIGHT_GRAY
        menuBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { menuHandler() } })
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
                val saveInQuit = TextButton(Locale.SAVE, skin)
                saveInQuit.label.setFontScale(0.9f); saveInQuit.color = Color(0.3f, 0.6f, 0.3f, 1f)
                saveInQuit.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        win.remove(); showSaveDialog(); menuHandler()
                    }
                })
                val noSaveBtn = TextButton(Locale.NO_SAVE, skin)
                noSaveBtn.label.setFontScale(0.9f)
                noSaveBtn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) { win.remove(); menuHandler() }
                })
                val cancelBtn = TextButton(Locale.CANCEL, skin)
                cancelBtn.label.setFontScale(0.9f)
                cancelBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { win.remove() } })
                win.add(saveInQuit).width(120f).padRight(8f)
                win.add(noSaveBtn).width(120f).padRight(8f)
                win.add(cancelBtn).width(120f)
                win.pack()
                win.setPosition(Gdx.graphics.width / 2f - win.width / 2f, Gdx.graphics.height / 2f - win.height / 2f)
                stage.addActor(win)
            }
        })
        panel.add(quitBtn).fillX().padLeft(10f)

        val undoBtn = TextButton(Locale.UNDO, skin)
        undoBtn.label.setFontScale(0.7f); undoBtn.label.color = Color(0.8f, 0.8f, 0.2f, 1f)
        undoBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { undoHandler() }
        })
        panel.add(undoBtn).fillX().padLeft(10f)

        val diploPanel = Table(skin).apply { right().top().pad(10f); defaults().pad(2f) }
        diplomacyLabel = Label(Locale.DIPLOMACY, skin)
        diplomacyLabel.color = Color.CYAN
        diploPanel.add(diplomacyLabel).colspan(2).row()
        diploPanel.add(diploBtn(Locale.DIPLO_ALLIANCE, Actions.DIPLO_ALLIANCE)).fillX()
        diploPanel.add(diploBtn(Locale.DIPLO_BREAK, Actions.DIPLO_BREAK)).fillX().row()
        diploPanel.add(diploBtn(Locale.DIPLO_TRADE, Actions.DIPLO_TRADE)).fillX()
        diploPanel.add(diploBtn(Locale.DIPLO_CANCEL_TRADE, Actions.DIPLO_CANCEL_TRADE)).fillX().row()

        val techPanel = Table(skin).apply { left().bottom().pad(10f); defaults().pad(2f) }
        techLabel = Label(Locale.TECHS, skin)
        techLabel.color = Color.YELLOW
        techPanel.add(techLabel).colspan(2).row()
        for (tech in com.example.strategy.model.TECH_TREE) {
            val b = TextButton(tech.name.take(8), skin)
            b.label.setFontScale(0.55f)
            b.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { actionHandler("${Actions.RESEARCH}:${tech.type.name}") } })
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

    fun update(delta: Float) {
        if (zoomHintTimer > 0f) {
            zoomHintTimer -= delta
            if (zoomHintTimer <= 0f) { zoomHintLabel?.setText(""); zoomHintTimer = 0f }
        }
    }

    fun updateInfoLabel() {
        val state = stateProvider()
        val r = selectedRegionProvider()
        val player = state.currentPlayer() ?: return
        val myTerritories = state.map.regions.count { it.ownerId == 0 }
        val enemyTerritories = state.map.regions.count { it.ownerId == 1 }
        val myPop = state.map.regions.filter { it.ownerId == 0 }.sumOf { it.population }
        val enemyPop = state.map.regions.filter { it.ownerId == 1 }.sumOf { it.population }
        val income = Economy.calculateIncome(player, state.map)
        val upkeep = Economy.upkeepCost(player, state.map)

        val diplo = state.diplomacy.getRelation(state.currentPlayerId, 1)
        val diploStatus = when (diplo.status) {
            DiplomacyStatus.ALLIED -> "ALLIED (${diplo.turnsAllied} turns)"
            DiplomacyStatus.TRADE_PARTNERS -> "TRADE PARTNERS"
            DiplomacyStatus.ENEMY -> "ENEMY"
            DiplomacyStatus.NEUTRAL -> "NEUTRAL"
        }
        diplomacyLabel.setText("${Locale.DIPLOMACY} $diploStatus${if (diplo.tradeActive) " + ${Locale.TRADE_ACTIVE}" else ""}")

        val selectedRegions = selectedRegionsProvider()
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
        val actionUsedThisTurn = actionUsedThisTurnProvider()
        statusLabel.setText(
            "${Locale.TURN} ${state.turn} | ${player.name}\n" +
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
        val history = stateProvider().history
        if (history.isEmpty()) { statsLabel.setText(Locale.NO_HISTORY); return }
        statsLabel.setText(Locale.STATS_HEADER + "\n" + history.takeLast(5).joinToString("\n") { h ->
            "T${h.turn}: ${h.territories} ${Locale.TERR}, ${h.population} ${Locale.POP}, F${h.resources.food} W${h.resources.wood} S${h.resources.stone} G${h.resources.gold}"
        })
    }

    private fun showSaveDialog() {
        val state = stateProvider()
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
                            stateSetter(loaded)
                            resetMode()
                            statusLabel.setText(Locale.LOADED + saveName); statusLabel.color = Color.CYAN
                            updateInfoLabel(); w.remove()
                        } else { statusLabel.setText(Locale.LOAD_FAILED); statusLabel.color = Color.RED }
                    }
                })
                val trashRegion = SkinFactory.makeTrashIcon()
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

    fun showGameOverDialog(message: String) {
        val win = Window("", skin)
        win.isModal = true; win.isMovable = true; win.pad(24f)
        val label = Label(message, skin)
        label.setFontScale(1.5f)
        label.color = if (message == Locale.VICTORY) Color.GOLD else Color.RED
        win.add(label).row()
        val menuBtn = TextButton(Locale.MENU, skin)
        menuBtn.label.setFontScale(0.9f)
        menuBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { win.remove(); menuHandler() }
        })
        win.add(menuBtn).width(150f).padTop(15f)
        win.pack()
        win.setPosition(Gdx.graphics.width / 2f - win.width / 2f, Gdx.graphics.height / 2f - win.height / 2f)
        stage.addActor(win)
    }

    fun showTutorialHint() {
        val label = Label(Locale.TUTORIAL_HINT, skin)
        label.color = Color(1f, 1f, 0.5f, 0.9f)
        label.setFontScale(0.9f)
        label.setWrap(true)
        label.setSize(500f, 60f)
        label.setPosition(Gdx.graphics.width / 2f - 250f, Gdx.graphics.height - 50f)
        stage.addActor(label)
        tutorialLabel = label
        tutorialTimer = 8f
    }

    private var tutorialTimer = 8f
    private var tutorialLabel: Label? = null

    fun updateTutorial(delta: Float) {
        if (tutorialTimer > 0f) {
            tutorialTimer -= delta
            if (tutorialTimer <= 0f) { tutorialLabel?.remove(); tutorialTimer = 0f }
        }
    }

    private var eventTimer = 0f
    private var eventLabel: Label? = null

    fun showEventNotification(description: String) {
        eventLabel?.remove()
        val label = Label(description, skin)
        label.color = Color(1f, 0.9f, 0.3f, 0.95f)
        label.setFontScale(0.85f)
        label.setWrap(true)
        label.setSize(450f, 50f)
        label.setPosition(Gdx.graphics.width / 2f - 225f, Gdx.graphics.height - 90f)
        stage.addActor(label)
        eventLabel = label
        eventTimer = 4f
    }

    fun updateEvent(delta: Float) {
        if (eventTimer > 0f) {
            eventTimer -= delta
            if (eventTimer <= 0f) { eventLabel?.remove(); eventTimer = 0f }
        }
    }

    fun showUpgradeDialog(region: Region, onUpgrade: (BuildingType) -> Unit) {
        val win = Window("Upgrade", skin)
        win.isModal = true; win.isMovable = true; win.pad(16f); win.defaults().pad(5f)
        win.add(Label("${Locale.UPGRADE_BTN} — ${region.name}", skin)).colspan(2).row()
        val upgradeable = region.buildings.filter { it.level < 3 }
        if (upgradeable.isEmpty()) {
            win.add(Label(Locale.NO_UPGRADABLE, skin)).colspan(2).row()
        }
        for (building in upgradeable) {
            val cost = com.example.strategy.model.Resources(
                food = 15 * building.level,
                wood = 10 * building.level,
                stone = 10 * building.level,
                gold = 15 * building.level
            )
            val costText = "${building.type.name} Lv${building.level}→${building.level + 1} (${cost.food}F ${cost.wood}W ${cost.stone}S ${cost.gold}G)"
            val b = TextButton(costText, skin)
            b.label.setFontScale(0.7f)
            b.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    win.remove(); onUpgrade(building.type)
                }
            })
            win.add(b).colspan(2).fillX().row()
        }
        val closeBtn = TextButton(Locale.CANCEL, skin)
        closeBtn.label.setFontScale(0.9f)
        closeBtn.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { win.remove() } })
        win.add(closeBtn).width(120f)
        win.pack()
        win.setPosition(Gdx.graphics.width / 2f - win.width / 2f, Gdx.graphics.height / 2f - win.height / 2f)
        stage.addActor(win)
    }
}
