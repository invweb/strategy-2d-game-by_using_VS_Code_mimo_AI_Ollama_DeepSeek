package com.example.strategy.server

import kotlin.test.*

class ProtocolTest {

    @Test
    fun encodeRoomCreated() {
        val msg = ServerMessage.RoomCreated("abc123")
        val json = Protocol.encode(msg)
        assertTrue(json.contains("\"type\":\"RoomCreated\""))
        assertTrue(json.contains("\"roomId\":\"abc123\""))
    }

    @Test
    fun encodeRoomJoined() {
        val msg = ServerMessage.RoomJoined("abc123", 0)
        val json = Protocol.encode(msg)
        assertTrue(json.contains("\"type\":\"RoomJoined\""))
        assertTrue(json.contains("\"playerId\":0"))
    }

    @Test
    fun encodeWaitingForPlayer() {
        val msg = ServerMessage.WaitingForPlayer("abc123")
        val json = Protocol.encode(msg)
        assertTrue(json.contains("\"type\":\"WaitingForPlayer\""))
    }

    @Test
    fun encodeActionApplied() {
        val msg = ServerMessage.ActionApplied(com.example.strategy.platform.GameFactory.createDefaultGameState())
        val json = Protocol.encode(msg)
        assertTrue(json.contains("\"type\":\"ActionApplied\""))
    }

    @Test
    fun encodeError() {
        val msg = ServerMessage.Error("Something went wrong")
        val json = Protocol.encode(msg)
        assertTrue(json.contains("\"type\":\"Error\""))
        assertTrue(json.contains("\"message\":\"Something went wrong\""))
    }

    @Test
    fun encodeOpponentDisconnected() {
        val msg = ServerMessage.OpponentDisconnected(1)
        val json = Protocol.encode(msg)
        assertTrue(json.contains("\"type\":\"OpponentDisconnected\""))
        assertTrue(json.contains("\"playerId\":1"))
    }

    @Test
    fun decodeCreateRoom() {
        val json = """{"type":"CreateRoom","playerName":"Alice"}"""
        val msg = Protocol.decodeClientMessage(json)
        assertNotNull(msg)
        assertTrue(msg is ClientMessage.CreateRoom)
        assertEquals("Alice", msg.playerName)
    }

    @Test
    fun decodeJoinRoom() {
        val json = """{"type":"JoinRoom","roomId":"xyz","playerName":"Bob"}"""
        val msg = Protocol.decodeClientMessage(json)
        assertNotNull(msg)
        assertTrue(msg is ClientMessage.JoinRoom)
        assertEquals("xyz", msg.roomId)
        assertEquals("Bob", msg.playerName)
    }

    @Test
    fun decodeGameAction() {
        val json = """{"type":"GameAction","action":"BUILD","targetRegionId":5,"param":"FARM"}"""
        val msg = Protocol.decodeClientMessage(json)
        assertNotNull(msg)
        assertTrue(msg is ClientMessage.GameAction)
        assertEquals("BUILD", msg.type)
        assertEquals(5, msg.targetRegionId)
        assertEquals("FARM", msg.param)
    }

    @Test
    fun decodeEndTurn() {
        val state = com.example.strategy.platform.GameFactory.createDefaultGameState()
        val stateJson = com.example.strategy.serialization.GameStateSerializer.serialize(state)
        val json = """{"type":"EndTurn","state":$stateJson}"""
        val msg = Protocol.decodeClientMessage(json)
        assertNotNull(msg)
        assertTrue(msg is ClientMessage.EndTurn)
    }

    @Test
    fun decodeChat() {
        val json = """{"type":"Chat","text":"Hello!"}"""
        val msg = Protocol.decodeClientMessage(json)
        assertNotNull(msg)
        assertTrue(msg is ClientMessage.Chat)
        assertEquals("Hello!", msg.text)
    }

    @Test
    fun decodeInvalidReturnsNull() {
        val msg = Protocol.decodeClientMessage("not json at all")
        assertNull(msg)
    }

    @Test
    fun decodeUnknownTypeReturnsNull() {
        val msg = Protocol.decodeClientMessage("""{"type":"UnknownType"}""")
        assertNull(msg)
    }
}
