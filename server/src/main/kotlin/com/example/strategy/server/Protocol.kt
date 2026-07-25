package com.example.strategy.server

import com.example.strategy.model.GameState
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true }

object Protocol {
    fun encode(message: ServerMessage): String {
        val obj = buildJsonObject {
            when (message) {
                is ServerMessage.RoomCreated -> {
                    put("type", "RoomCreated")
                    put("roomId", message.roomId)
                }
                is ServerMessage.RoomJoined -> {
                    put("type", "RoomJoined")
                    put("roomId", message.roomId)
                    put("playerId", message.playerId)
                }
                is ServerMessage.WaitingForPlayer -> {
                    put("type", "WaitingForPlayer")
                    put("roomId", message.roomId)
                }
                is ServerMessage.GameStarted -> {
                    put("type", "GameStarted")
                    put("yourPlayerId", message.yourPlayerId)
                }
                is ServerMessage.TurnUpdate -> {
                    put("type", "TurnUpdate")
                    put("currentPlayerId", message.currentPlayerId)
                }
                is ServerMessage.ActionApplied -> {
                    put("type", "ActionApplied")
                }
                is ServerMessage.Error -> {
                    put("type", "Error")
                    put("message", message.message)
                }
                is ServerMessage.OpponentDisconnected -> {
                    put("type", "OpponentDisconnected")
                    put("playerId", message.playerId)
                }
            }
        }
        return obj.toString()
    }

    fun decodeClientMessage(text: String): ClientMessage? {
        return try {
            val obj = json.parseToJsonElement(text).jsonObject
            when (obj["type"]?.jsonPrimitive?.content) {
                "CreateRoom" -> ClientMessage.CreateRoom(obj["playerName"]?.jsonPrimitive?.content ?: "")
                "JoinRoom" -> ClientMessage.JoinRoom(
                    obj["roomId"]?.jsonPrimitive?.content ?: "",
                    obj["playerName"]?.jsonPrimitive?.content ?: ""
                )
                "GameAction" -> ClientMessage.GameAction(
                    obj["action"]?.jsonPrimitive?.content ?: "",
                    obj["targetRegionId"]?.jsonPrimitive?.intOrNull ?: 0,
                    obj["param"]?.jsonPrimitive?.content ?: ""
                )
                "EndTurn" -> ClientMessage.EndTurn(json.decodeFromString(GameState.serializer(), obj["state"]?.toString() ?: "{}"))
                "Chat" -> ClientMessage.Chat(obj["text"]?.jsonPrimitive?.content ?: "")
                else -> null
            }
        } catch (e: Exception) {
            println("[Protocol] Decode error: ${e.message}")
            null
        }
    }
}

sealed class ServerMessage {
    data class RoomCreated(val roomId: String) : ServerMessage()
    data class RoomJoined(val roomId: String, val playerId: Int) : ServerMessage()
    data class WaitingForPlayer(val roomId: String) : ServerMessage()
    data class GameStarted(val state: GameState, val yourPlayerId: Int) : ServerMessage()
    data class TurnUpdate(val state: GameState, val currentPlayerId: Int) : ServerMessage()
    data class ActionApplied(val state: GameState) : ServerMessage()
    data class Error(val message: String) : ServerMessage()
    data class OpponentDisconnected(val playerId: Int) : ServerMessage()
}

sealed class ClientMessage {
    data class CreateRoom(val playerName: String) : ClientMessage()
    data class JoinRoom(val roomId: String, val playerName: String) : ClientMessage()
    data class GameAction(val type: String, val targetRegionId: Int = 0, val param: String = "") : ClientMessage()
    data class EndTurn(val state: GameState) : ClientMessage()
    data class Chat(val text: String) : ClientMessage()
}
