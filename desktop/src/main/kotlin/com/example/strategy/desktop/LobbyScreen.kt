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
import com.badlogic.gdx.utils.viewport.ScreenViewport
import java.io.File

class LobbyScreen(private val game: StrategyGame) : ScreenAdapter() {

    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private var networkClient: NetworkClient? = null
    private var waitingLabel: Label? = null
    private var roomIdLabel: Label? = null
    private var statusLabel: Label? = null
    private var serverUrl = "localhost:8080"
    private var playerName = "Player"

    override fun show() {
        Locale.load()
        skin = createSkin()
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        val root = Table(skin).apply { setFillParent(true) }

        val title = Label("MULTIPLAYER", skin)
        title.setFontScale(2.5f)
        title.color = Color(0.9f, 0.8f, 0.2f, 1f)
        root.add(title).padBottom(30f).row()

        statusLabel = Label("", skin)
        statusLabel!!.color = Color.LIGHT_GRAY
        root.add(statusLabel).padBottom(20f).row()

        val configPanel = Table(skin).apply { defaults().pad(5f) }

        val serverLabel = Label("Server URL:", skin)
        serverLabel.color = Color.CYAN
        configPanel.add(serverLabel)
        val serverField = TextField("localhost:8080", skin)
        serverField.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                serverUrl = serverField.text.trim()
            }
        })
        configPanel.add(serverField).width(250f).row()

        val nameLabel = Label("Your name:", skin)
        nameLabel.color = Color.CYAN
        configPanel.add(nameLabel)
        val nameField = TextField("Player", skin)
        nameField.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                playerName = nameField.text.trim()
            }
        })
        configPanel.add(nameField).width(250f).row()

        root.add(configPanel).padBottom(30f).row()

        val btnPanel = Table(skin).apply { defaults().pad(8f) }

        val createBtn = TextButton("CREATE ROOM", skin)
        createBtn.label.setFontScale(1.0f)
        createBtn.color = Color(0.3f, 0.6f, 0.3f, 1f)
        createBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                serverUrl = serverField.text.trim()
                playerName = nameField.text.trim()
                connectAndCreateRoom()
            }
        })
        btnPanel.add(createBtn).width(200f).height(50f).row()

        val joinBtn = TextButton("JOIN ROOM", skin)
        joinBtn.label.setFontScale(1.0f)
        joinBtn.color = Color(0.3f, 0.5f, 0.7f, 1f)
        joinBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showJoinDialog()
            }
        })
        btnPanel.add(joinBtn).width(200f).height(50f).row()

        root.add(btnPanel).padBottom(20f).row()

        waitingLabel = Label("", skin)
        waitingLabel!!.color = Color.YELLOW
        root.add(waitingLabel).padBottom(10f).row()

        roomIdLabel = Label("", skin)
        roomIdLabel!!.color = Color.CYAN
        root.add(roomIdLabel).padBottom(20f).row()

        val backBtn = TextButton("BACK", skin)
        backBtn.label.setFontScale(0.9f)
        backBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                networkClient?.disconnect()
                game.setScreen(MenuScreen(game))
            }
        })
        root.add(backBtn).width(120f).height(40f)

        stage.addActor(root)
    }

    private fun connectAndCreateRoom() {
        networkClient?.disconnect()
        networkClient = NetworkClient { message -> handleServerMessage(message) }
        statusLabel?.setText("Connecting to $serverUrl...")
        statusLabel?.color = Color.LIGHT_GRAY

        networkClient!!.connect(serverUrl)

        // Poll connection status
        Thread {
            var attempts = 0
            while (!networkClient!!.connected && attempts < 30) {
                Thread.sleep(100)
                attempts++
            }
            if (networkClient!!.connected) {
                Gdx.app.postRunnable {
                    statusLabel?.setText("Connected! Creating room...")
                    networkClient!!.sendJson("CreateRoom", "playerName" to playerName)
                }
            } else {
                Gdx.app.postRunnable {
                    statusLabel?.setText("Connection failed!")
                    statusLabel?.color = Color.RED
                }
            }
        }.start()
    }

    private fun showJoinDialog() {
        val win = Window("Join Room", skin)
        win.isModal = true; win.isMovable = true; win.pad(16f)

        val idLabel = Label("Room ID:", skin)
        idLabel.color = Color.CYAN
        win.add(idLabel)
        val idField = TextField("", skin)
        win.add(idField).width(200f).row()

        val joinBtn = TextButton("JOIN", skin)
        joinBtn.label.setFontScale(0.9f)
        joinBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val roomId = idField.text.trim()
                if (roomId.isEmpty()) return
                win.remove()
                serverUrl = "localhost:8080"
                connectAndJoinRoom(roomId)
            }
        })
        win.add(joinBtn).width(100f)

        val cancelBtn = TextButton(Locale.CANCEL, skin)
        cancelBtn.label.setFontScale(0.9f)
        cancelBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { win.remove() }
        })
        win.add(cancelBtn).width(100f)

        win.pack()
        win.setPosition(Gdx.graphics.width / 2f - win.width / 2f, Gdx.graphics.height / 2f - win.height / 2f)
        stage.addActor(win)
    }

    private fun connectAndJoinRoom(roomId: String) {
        networkClient?.disconnect()
        networkClient = NetworkClient { message -> handleServerMessage(message) }
        statusLabel?.setText("Connecting to $serverUrl...")
        networkClient!!.connect(serverUrl)

        Thread {
            var attempts = 0
            while (!networkClient!!.connected && attempts < 30) {
                Thread.sleep(100)
                attempts++
            }
            if (networkClient!!.connected) {
                Gdx.app.postRunnable {
                    statusLabel?.setText("Connected! Joining room...")
                    networkClient!!.sendJson("JoinRoom", "roomId" to roomId, "playerName" to playerName)
                }
            } else {
                Gdx.app.postRunnable {
                    statusLabel?.setText("Connection failed!")
                    statusLabel?.color = Color.RED
                }
            }
        }.start()
    }

    private fun handleServerMessage(message: NetworkClient.ServerMessage) {
        when (message) {
            is NetworkClient.ServerMessage.RoomCreated -> {
                statusLabel?.setText("Room created!")
                statusLabel?.color = Color.GREEN
                roomIdLabel?.setText("Room ID: ${message.roomId}")
                waitingLabel?.setText("Waiting for opponent...")
            }
            is NetworkClient.ServerMessage.RoomJoined -> {
                statusLabel?.setText("Joined room ${message.roomId}")
                statusLabel?.color = Color.GREEN
                waitingLabel?.setText("Connecting to game...")
            }
            is NetworkClient.ServerMessage.WaitingForPlayer -> {
                waitingLabel?.setText("Waiting for opponent to join...")
                waitingLabel?.color = Color.YELLOW
            }
            is NetworkClient.ServerMessage.GameStarted -> {
                game.setScreen(NetworkGameScreen(game, networkClient!!, message.yourPlayerId))
            }
            is NetworkClient.ServerMessage.Error -> {
                statusLabel?.setText("Error: ${message.message}")
                statusLabel?.color = Color.RED
            }
            else -> {}
        }
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
        networkClient?.dispose()
        stage.dispose()
        skin.dispose()
    }

    private fun createSkin(): Skin {
        val s = Skin()
        val font = generateFont()
        s.add("default-font", font, com.badlogic.gdx.graphics.g2d.BitmapFont::class.java)
        val upPix = com.badlogic.gdx.graphics.Pixmap(4, 4, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888).apply { setColor(Color(0.2f, 0.2f, 0.25f, 1f)); fill() }
        val downPix = com.badlogic.gdx.graphics.Pixmap(4, 4, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888).apply { setColor(Color(0.3f, 0.3f, 0.35f, 1f)); fill() }
        val overPix = com.badlogic.gdx.graphics.Pixmap(4, 4, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888).apply { setColor(Color(0.25f, 0.25f, 0.3f, 1f)); fill() }
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
        return s
    }

    private fun generateFont(): com.badlogic.gdx.graphics.g2d.BitmapFont {
        val fontPaths = arrayOf("/System/Library/Fonts/Supplemental/Arial.ttf", "/System/Library/Fonts/Helvetica.ttc", "/Library/Fonts/Arial.ttf")
        var generator: com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator? = null
        for (path in fontPaths) { if (File(path).exists()) { generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(Gdx.files.absolute(path)); break } }
        if (generator == null) generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(Gdx.files.absolute(fontPaths[0]))
        val params = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter()
        params.size = 16; params.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear; params.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        params.characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,*':?!@#$%&()-+=/<>" + "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" + "äöüÄÖÜß «»—…"
        val font = generator.generateFont(params); generator.dispose(); return font
    }
}
