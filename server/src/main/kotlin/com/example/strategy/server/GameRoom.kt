package com.example.strategy.server

import com.example.strategy.model.GameState
import com.example.strategy.platform.GameFactory
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameRoom(val id: String) {

    data class Player(val id: Int, val name: String, val session: WebSocketSession)

    val players = mutableListOf<Player>()
    var state: GameState = GameFactory.createDefaultGameState()
        private set
    var started = false
        private set

    private val scope = CoroutineScope(Dispatchers.Default)

    fun addPlayer(player: Player): Boolean {
        if (players.size >= 2) return false
        players.add(player)
        if (players.size == 1) {
            scope.launch {
                sendTo(player.session, ServerMessage.WaitingForPlayer(id))
            }
        }
        if (players.size == 2) {
            startGame()
        }
        return true
    }

    private fun startGame() {
        started = true
        state = GameFactory.createDefaultGameState()
        state = state.copy(
            players = state.players.map { p ->
                if (p.id == 0) p.copy(name = players[0].name, isHuman = true)
                else if (p.id == 1) p.copy(name = players[1].name, isHuman = false)
                else p
            }
        )
        broadcast(ServerMessage.GameStarted(state, 0))
        broadcast(ServerMessage.TurnUpdate(state, state.currentPlayerId))
    }

    fun handleAction(playerId: Int, action: ClientMessage.GameAction) {
        if (!started) return
        if (state.currentPlayerId != playerId) {
            val player = players.find { it.id == playerId } ?: return
            scope.launch { sendTo(player.session, ServerMessage.Error("Not your turn")) }
            return
        }

        val actionType = try {
            com.example.strategy.logic.ActionQueue.ActionType.valueOf(action.type)
        } catch (_: Exception) { return }

        val gameAction = com.example.strategy.logic.ActionQueue.GameAction(
            playerId, actionType, action.targetRegionId, action.param
        )
        com.example.strategy.logic.ActionQueue.enqueue(gameAction)
        state = com.example.strategy.logic.ActionQueue.processAll(state)
        broadcast(ServerMessage.ActionApplied(state))
    }

    fun handleEndTurn(playerId: Int, clientState: GameState) {
        if (!started) return
        if (state.currentPlayerId != playerId) return

        state = com.example.strategy.logic.TurnManager.endTurn(state)

        if (state.currentPlayerId != 0) {
            runAIFallback()
            state = com.example.strategy.logic.TurnManager.endTurn(state)
        }

        broadcast(ServerMessage.TurnUpdate(state, state.currentPlayerId))
    }

    private fun runAIFallback() {
        val aiAction = com.example.strategy.ai.OllamaAI.decide(state)
        if (aiAction != null) {
            val action = com.example.strategy.logic.ActionQueue.GameAction(
                state.currentPlayerId, aiAction.actionType, aiAction.targetRegionId, aiAction.param
            )
            com.example.strategy.logic.ActionQueue.enqueue(action)
            state = com.example.strategy.logic.ActionQueue.processAll(state)
        }
    }

    fun removePlayer(playerId: Int) {
        players.removeAll { it.id == playerId }
        if (players.isNotEmpty()) {
            broadcast(ServerMessage.OpponentDisconnected(playerId))
        }
    }

    private suspend fun sendTo(session: WebSocketSession, message: ServerMessage) {
        try {
            session.send(Frame.Text(Protocol.encode(message)))
        } catch (_: Exception) {}
    }

    fun broadcast(message: ServerMessage) {
        scope.launch {
            for (player in players) {
                sendTo(player.session, message)
            }
        }
    }
}
