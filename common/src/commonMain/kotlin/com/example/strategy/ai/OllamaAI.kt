package com.example.strategy.ai

import com.example.strategy.logic.ActionQueue
import com.example.strategy.model.*
import com.example.strategy.platform.HttpClient
import com.example.strategy.platform.PlatformProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

// AI advisor — sends game state to Ollama and parses response into actions
object OllamaAI {

    private const val OLLAMA_URL = "http://localhost:11434/api/generate"
    private val json = Json { ignoreUnknownKeys = true }

    data class AIAction(
        val actionType: ActionQueue.ActionType,
        val targetRegionId: Int,
        val param: String = ""
    )

    fun decide(gameState: GameState): AIAction? {
        val client = PlatformProvider.httpClient ?: return null
        val player = gameState.currentPlayer() ?: return null
        if (player.isHuman) return null

        val prompt = buildPrompt(gameState, player)
        val requestBody = buildJsonObject {
            put("model", JsonPrimitive("deepseek-r1:7b"))
            put("prompt", JsonPrimitive(prompt))
            put("stream", JsonPrimitive(false))
            put("options", buildJsonObject {
                put("temperature", JsonPrimitive(0.3))
                put("num_predict", JsonPrimitive(200))
            })
        }

        return try {
            val response = client.postJson(OLLAMA_URL, requestBody.toString())
            if (response == null) {
                println("[OllamaAI] No response from Ollama")
                return fallbackAction(gameState, player)
            }
            parseResponse(response, gameState, player)
        } catch (e: Exception) {
            println("[OllamaAI] Error: ${e.message}")
            fallbackAction(gameState, player)
        }
    }

    private fun buildPrompt(state: GameState, player: Player): String {
        val ownedRegions = state.map.regions.filter { it.ownerId == player.id }
        val enemyRegions = state.map.regions.filter {
            it.ownerId != null && it.ownerId != player.id && it.terrain != TerrainType.WATER
        }

        val regionInfo = ownedRegions.joinToString("\n") { r ->
            val buildings = r.buildings.joinToString(",") { it.type.name }
            "  ${r.name}(${r.terrain.name}): pop=${r.population}, buildings=[$buildings]"
        }

        val enemyInfo = enemyRegions.take(5).joinToString("\n") { r ->
            "  ${r.name}(${r.terrain.name}): pop=${r.population}, owner=${r.ownerId}"
        }

        return """You are an AI ruler in a turn-based strategy game.

YOUR STATE:
  Resources: Food=${player.resources.food}, Wood=${player.resources.wood}, Stone=${player.resources.stone}, Iron=${player.resources.iron}, Gold=${player.resources.gold}
  Turn: ${state.turn}

YOUR REGIONS:
$regionInfo

ENEMY REGIONS:
$enemyInfo

AVAILABLE ACTIONS (choose exactly ONE):
  BUILD_FARM:REGION_ID — costs 10 Food + 5 Wood (region must have no buildings)
  BUILD_BARRACKS:REGION_ID — costs 15 Wood + 10 Stone + 10 Gold (region must have no buildings)
  BUILD_MINE:REGION_ID — costs 5 Wood + 15 Stone + 5 Iron (region must have no buildings)
  RECRUIT:REGION_ID — costs 10 Food + 5 Gold (requires Barracks in region)
  DEVELOP:REGION_ID — costs 10 Gold, +3 population
  END_TURN — skip actions

Reply with ONLY one line in format: ACTION:REGION_ID
Example: BUILD_FARM:3"""
    }

    private fun parseResponse(response: String, state: GameState, player: Player): AIAction? {
        val responseText = try {
            val obj = json.parseToJsonElement(response) as JsonObject
            obj["response"]?.jsonPrimitive?.contentOrNull ?: ""
        } catch (_: Exception) {
            response
        }

        println("[OllamaAI] Response: $responseText")

        val lines = responseText.lines()
        for (line in lines) {
            val trimmed = line.trim().uppercase()
            val match = Regex("""(BUILD_FARM|BUILD_BARRACKS|BUILD_MINE|RECRUIT|DEVELOP|END_TURN)[:\s]+(\d+)""").find(trimmed)
            if (match != null) {
                val actionStr = match.groupValues[1]
                val regionId = match.groupValues[2].toIntOrNull() ?: continue
                val actionType = when (actionStr) {
                    "BUILD_FARM", "BUILD_BARRACKS", "BUILD_MINE" -> ActionQueue.ActionType.BUILD
                    "RECRUIT" -> ActionQueue.ActionType.RECRUIT
                    "DEVELOP" -> ActionQueue.ActionType.DEVELOP
                    "END_TURN" -> return null
                    else -> continue
                }
                val param = when (actionStr) {
                    "BUILD_FARM" -> "FARM"
                    "BUILD_BARRACKS" -> "BARRACKS"
                    "BUILD_MINE" -> "MINE"
                    else -> ""
                }
                val region = state.map.getRegionById(regionId)
                if (region != null && region.ownerId == player.id) {
                    return AIAction(actionType, regionId, param)
                }
            }
        }

        return fallbackAction(state, player)
    }

    // Simple fallback if Ollama fails or returns invalid response
    private fun fallbackAction(state: GameState, player: Player): AIAction? {
        val ownedRegions = state.map.regions.filter { it.ownerId == player.id }

        if (player.resources.canAfford(Resources(food = 10, wood = 5))) {
            val emptyRegion = ownedRegions.firstOrNull { it.buildings.isEmpty() && it.terrain != TerrainType.WATER }
            if (emptyRegion != null) {
                return AIAction(ActionQueue.ActionType.BUILD, emptyRegion.id, "FARM")
            }
        }

        val barracksRegion = ownedRegions.firstOrNull {
            it.buildings.any { b -> b.type == BuildingType.BARRACKS }
        }
        if (barracksRegion != null && player.resources.canAfford(Resources(food = 10, gold = 5))) {
            return AIAction(ActionQueue.ActionType.RECRUIT, barracksRegion.id)
        }

        if (player.resources.canAfford(Resources(gold = 10))) {
            val region = ownedRegions.firstOrNull()
            if (region != null) {
                return AIAction(ActionQueue.ActionType.DEVELOP, region.id)
            }
        }

        return null
    }
}
