package com.example.strategy.desktop

import com.badlogic.gdx.utils.Disposable
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue

class NetworkClient(var onMessage: (ServerMessage) -> Unit = {}) : Disposable {

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: OutputStreamWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var connected = false
        private set
    var serverUrl: String = "localhost:8080"

    sealed class ServerMessage {
        data class RoomCreated(val roomId: String) : ServerMessage()
        data class RoomJoined(val roomId: String, val playerId: Int) : ServerMessage()
        data class WaitingForPlayer(val roomId: String) : ServerMessage()
        data class GameStarted(val yourPlayerId: Int) : ServerMessage()
        data class TurnUpdate(val currentPlayerId: Int) : ServerMessage()
        class ActionApplied() : ServerMessage()
        data class Error(val message: String) : ServerMessage()
        data class OpponentDisconnected(val playerId: Int) : ServerMessage()
        data class Raw(val text: String) : ServerMessage()
    }

    fun connect(url: String = "localhost:8080") {
        serverUrl = url
        scope.launch {
            try {
                val parts = url.split(":")
                val host = parts[0]
                val port = parts.getOrNull(1)?.toIntOrNull() ?: 8080

                socket = Socket(host, port)
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                writer = OutputStreamWriter(socket!!.getOutputStream())

                val key = java.util.Base64.getEncoder().encodeToString(ByteArray(16) { (Math.random() * 256).toInt().toByte() })
                val upgradeRequest = "GET /game HTTP/1.1\r\n" +
                    "Host: $host:$port\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Key: $key\r\n" +
                    "Sec-WebSocket-Version: 13\r\n" +
                    "\r\n"
                writer!!.write(upgradeRequest)
                writer!!.flush()

                val response = reader!!.readLine()
                if (response?.contains("101") == true) {
                    var line: String? = reader!!.readLine()
                    while (line != null && line.isNotEmpty()) {
                        line = reader!!.readLine()
                    }
                    connected = true
                    println("[Network] Connected to $url")
                    startReading()
                } else {
                    println("[Network] Connection failed: $response")
                    connected = false
                }
            } catch (e: Exception) {
                println("[Network] Connection error: ${e.message}")
                connected = false
            }
        }
    }

    private suspend fun startReading() {
        while (connected) {
            try {
                val message = readWebSocketFrame() ?: break
                val parsed = parseMessage(message)
                val callback = onMessage
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    callback(parsed)
                }
            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                println("[Network] Read error: ${e.message}")
                break
            }
        }
        connected = false
    }

    private fun readWebSocketFrame(): String? {
        val input = socket?.getInputStream() ?: return null
        val firstByte = input.read()
        if (firstByte == -1) return null
        val secondByte = input.read()
        if (secondByte == -1) return null

        val payloadLength = secondByte and 0x7F
        var actualLength = payloadLength.toLong()

        if (payloadLength == 126) {
            val b1 = input.read()
            val b2 = input.read()
            actualLength = ((b1 shl 8) or b2).toLong()
        } else if (payloadLength == 127) {
            var len = 0L
            for (i in 0..7) {
                len = (len shl 8) or input.read().toLong()
            }
            actualLength = len
        }

        val masked = (secondByte and 0x80) != 0
        val mask = if (masked) {
            val m = ByteArray(4)
            input.read(m)
            m
        } else null

        val payload = ByteArray(actualLength.toInt())
        var bytesRead = 0
        while (bytesRead < actualLength) {
            val read = input.read(payload, bytesRead, (actualLength - bytesRead).toInt())
            if (read == -1) return null
            bytesRead += read
        }

        if (masked && mask != null) {
            for (i in payload.indices) {
                payload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
            }
        }

        return String(payload)
    }

    private fun parseMessage(text: String): ServerMessage {
        return try {
            val obj = com.badlogic.gdx.utils.Json().fromJson(
                com.badlogic.gdx.utils.JsonValue::class.java, text
            )
            when (obj.getString("type", "")) {
                "RoomCreated" -> ServerMessage.RoomCreated(obj.getString("roomId", ""))
                "RoomJoined" -> ServerMessage.RoomJoined(obj.getString("roomId", ""), obj.getInt("playerId", 1))
                "WaitingForPlayer" -> ServerMessage.WaitingForPlayer(obj.getString("roomId", ""))
                "GameStarted" -> ServerMessage.GameStarted(obj.getInt("yourPlayerId", 0))
                "TurnUpdate" -> ServerMessage.TurnUpdate(obj.getInt("currentPlayerId", 0))
                "ActionApplied" -> ServerMessage.ActionApplied()
                "Error" -> ServerMessage.Error(obj.getString("message", ""))
                "OpponentDisconnected" -> ServerMessage.OpponentDisconnected(obj.getInt("playerId", 0))
                else -> ServerMessage.Raw(text)
            }
        } catch (_: Exception) {
            ServerMessage.Raw(text)
        }
    }

    fun send(message: String) {
        if (!connected) return
        scope.launch {
            try {
                val payload = message.toByteArray()
                val masked = ByteArray(4) { (Math.random() * 256).toInt().toByte() }
                val output = socket?.getOutputStream() ?: return@launch

                output.write(0x81)
                if (payload.size < 126) {
                    output.write(0x80 or payload.size)
                } else {
                    output.write(0x80 or 126)
                    output.write((payload.size shr 8) and 0xFF)
                    output.write(payload.size and 0xFF)
                }
                output.write(masked)
                for (i in payload.indices) {
                    output.write((payload[i].toInt() xor masked[i % 4].toInt()))
                }
                output.flush()
            } catch (e: Exception) {
                println("[Network] Send error: ${e.message}")
            }
        }
    }

    fun sendJson(type: String, vararg pairs: Pair<String, Any>) {
        val json = buildString {
            append("{")
            append("\"type\":\"$type\"")
            for ((key, value) in pairs) {
                append(",\"$key\":")
                append(when (value) {
                    is String -> "\"$value\""
                    is Number -> value.toString()
                    else -> "\"$value\""
                })
            }
            append("}")
        }
        send(json)
    }

    fun sendGameState(state: com.example.strategy.model.GameState, type: String) {
        val stateJson = com.example.strategy.serialization.GameStateSerializer.serialize(state)
        send("{\"type\":\"$type\",\"state\":$stateJson}")
    }

    fun disconnect() {
        connected = false
        try { socket?.close() } catch (_: Exception) {}
        scope.cancel()
    }

    override fun dispose() { disconnect() }
}
