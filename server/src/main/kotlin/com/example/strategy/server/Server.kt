package com.example.strategy.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

val rooms = ConcurrentHashMap<String, GameRoom>()
val sessionToPlayer = ConcurrentHashMap<WebSocketSession, Pair<String, Int>>()

fun main() {
    println("Starting Strategy Multiplayer Server on port 8080...")
    embeddedServer(Netty, port = 8080) {
        install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 60.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        routing {
            webSocket("/game") {
                println("[Server] New connection from ${call.request.local.remoteAddress}")
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val clientMsg = Protocol.decodeClientMessage(text)
                            if (clientMsg == null) {
                                send(Frame.Text(Protocol.encode(ServerMessage.Error("Invalid message"))))
                                continue
                            }
                            handleMessage(this, clientMsg)
                        }
                    }
                } catch (e: Exception) {
                    println("[Server] Error: ${e.message}")
                } finally {
                    handleDisconnect(this)
                    println("[Server] Connection closed")
                }
            }
        }
    }.start(wait = true)
}

suspend fun handleMessage(session: WebSocketSession, message: ClientMessage) {
    when (message) {
        is ClientMessage.CreateRoom -> {
            val roomId = "room_${System.currentTimeMillis() % 100000}"
            val room = GameRoom(roomId)
            rooms[roomId] = room
            val player = GameRoom.Player(0, message.playerName, session)
            room.addPlayer(player)
            sessionToPlayer[session] = roomId to 0
            session.send(Frame.Text(Protocol.encode(ServerMessage.RoomCreated(roomId))))
            println("[Server] Room $roomId created by ${message.playerName}")
        }
        is ClientMessage.JoinRoom -> {
            val room = rooms[message.roomId]
            if (room == null) {
                session.send(Frame.Text(Protocol.encode(ServerMessage.Error("Room not found"))))
                return
            }
            val player = GameRoom.Player(1, message.playerName, session)
            if (!room.addPlayer(player)) {
                session.send(Frame.Text(Protocol.encode(ServerMessage.Error("Room is full"))))
                return
            }
            sessionToPlayer[session] = message.roomId to 1
            session.send(Frame.Text(Protocol.encode(ServerMessage.RoomJoined(message.roomId, 1))))
            println("[Server] ${message.playerName} joined room ${message.roomId}")
        }
        is ClientMessage.GameAction -> {
            val (roomId, playerId) = sessionToPlayer[session] ?: return
            val room = rooms[roomId] ?: return
            room.handleAction(playerId, message)
        }
        is ClientMessage.EndTurn -> {
            val (roomId, playerId) = sessionToPlayer[session] ?: return
            val room = rooms[roomId] ?: return
            room.handleEndTurn(playerId, message.state)
        }
        is ClientMessage.Chat -> {
            val (roomId, playerId) = sessionToPlayer[session] ?: return
            val room = rooms[roomId] ?: return
            val playerName = room.players.find { it.id == playerId }?.name ?: "Unknown"
            room.broadcast(ServerMessage.Error("[$playerName]: ${message.text}"))
        }
    }
}

fun handleDisconnect(session: WebSocketSession) {
    val (roomId, playerId) = sessionToPlayer.remove(session) ?: return
    val room = rooms[roomId] ?: return
    room.removePlayer(playerId)
    if (room.players.isEmpty()) {
        rooms.remove(roomId)
        println("[Server] Room $roomId removed (empty)")
    }
}
