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
import com.badlogic.gdx.utils.viewport.ScreenViewport

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
        skin = SkinFactory.createSkin()
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        val root = Table(skin).apply { setFillParent(true) }

        val title = Label(Locale.MULTIPLAYER, skin)
        title.setFontScale(2.5f)
        title.color = Color(0.9f, 0.8f, 0.2f, 1f)
        root.add(title).padBottom(30f).row()

        statusLabel = Label("", skin)
        statusLabel!!.color = Color.LIGHT_GRAY
        root.add(statusLabel).padBottom(20f).row()

        val configPanel = Table(skin).apply { defaults().pad(5f) }

        val serverLabel = Label(Locale.SERVER_URL, skin)
        serverLabel.color = Color.CYAN
        configPanel.add(serverLabel)
        val serverField = TextField("localhost:8080", skin)
        serverField.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                serverUrl = serverField.text.trim()
            }
        })
        configPanel.add(serverField).width(250f).row()

        val nameLabel = Label(Locale.YOUR_NAME, skin)
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

        val createBtn = TextButton(Locale.CREATE_ROOM, skin)
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

        val joinBtn = TextButton(Locale.JOIN_ROOM, skin)
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

        val backBtn = TextButton(Locale.BACK, skin)
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
        statusLabel?.setText("${Locale.CONNECTING} $serverUrl...")
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
                    statusLabel?.setText(Locale.CONNECTED)
                    networkClient!!.sendJson("CreateRoom", "playerName" to playerName)
                }
            } else {
                Gdx.app.postRunnable {
                    statusLabel?.setText(Locale.CONNECTION_FAILED)
                    statusLabel?.color = Color.RED
                }
            }
        }.start()
    }

    private fun showJoinDialog() {
        val win = Window(Locale.JOIN_ROOM, skin)
        win.isModal = true; win.isMovable = true; win.pad(16f)

        val idLabel = Label(Locale.ROOM_ID, skin)
        idLabel.color = Color.CYAN
        win.add(idLabel)
        val idField = TextField("", skin)
        win.add(idField).width(200f).row()

        val joinBtn = TextButton(Locale.JOIN, skin)
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
        statusLabel?.setText("${Locale.CONNECTING} $serverUrl...")
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
                    statusLabel?.setText(Locale.CONNECTION_FAILED)
                    statusLabel?.color = Color.RED
                }
            }
        }.start()
    }

    private fun handleServerMessage(message: NetworkClient.ServerMessage) {
        when (message) {
            is NetworkClient.ServerMessage.RoomCreated -> {
                statusLabel?.setText(Locale.ROOM_CREATED)
                statusLabel?.color = Color.GREEN
                roomIdLabel?.setText("Room ID: ${message.roomId}")
                waitingLabel?.setText(Locale.WAITING_FOR_OPPONENT)
            }
            is NetworkClient.ServerMessage.RoomJoined -> {
                statusLabel?.setText("${Locale.JOINED_ROOM} ${message.roomId}")
                statusLabel?.color = Color.GREEN
                waitingLabel?.setText(Locale.CONNECTING_TO_GAME)
            }
            is NetworkClient.ServerMessage.WaitingForPlayer -> {
                waitingLabel?.setText(Locale.WAITING_FOR_OPPONENT_JOIN)
                waitingLabel?.color = Color.YELLOW
            }
            is NetworkClient.ServerMessage.GameStarted -> {
                game.setScreen(NetworkGameScreen(game, networkClient!!, message.yourPlayerId))
            }
            is NetworkClient.ServerMessage.Error -> {
                statusLabel?.setText("${Locale.ERROR} ${message.message}")
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
}
