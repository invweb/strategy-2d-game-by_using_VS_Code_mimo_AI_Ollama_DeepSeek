package com.example.strategy.ai

import com.example.strategy.logic.ActionQueue
import com.example.strategy.model.*
import com.example.strategy.platform.PlatformProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object OllamaAI {

    private val json = Json { ignoreUnknownKeys = true }

    data class AIAction(
        val actionType: ActionQueue.ActionType,
        val targetRegionId: Int,
        val param: String = ""
    )

    fun decide(gameState: GameState): AIAction? {
        val player = gameState.currentPlayer() ?: return null
        if (player.isHuman) return null

        return when (AISettings.backend) {
            AISettings.Backend.NONE -> fallbackAction(gameState, player)
            AISettings.Backend.OLLAMA -> callOllama(gameState, player)
            AISettings.Backend.LM_STUDIO -> callLmStudio(gameState, player)
        }
    }

    private fun callOllama(gameState: GameState, player: Player): AIAction? {
        val client = PlatformProvider.httpClient ?: return fallbackAction(gameState, player)
        val prompt = buildPrompt(gameState, player)
        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(AISettings.ollamaModel))
            put("prompt", JsonPrimitive(prompt))
            put("stream", JsonPrimitive(false))
            put("options", buildJsonObject {
                put("temperature", JsonPrimitive(0.3))
                put("num_predict", JsonPrimitive(200))
            })
        }

        return try {
            val response = client.postJson("${AISettings.ollamaUrl}/api/generate", requestBody.toString())
            if (response == null) {
                println("[AI] No response from Ollama")
                return fallbackAction(gameState, player)
            }
            val responseText = try {
                val obj = json.parseToJsonElement(response) as JsonObject
                obj["response"]?.jsonPrimitive?.contentOrNull ?: ""
            } catch (_: Exception) { response }

            println("[Ollama] Response: $responseText")
            parseAction(responseText, gameState, player)
        } catch (e: Exception) {
            println("[Ollama] Error: ${e.message}")
            fallbackAction(gameState, player)
        }
    }

    private fun callLmStudio(gameState: GameState, player: Player): AIAction? {
        val client = PlatformProvider.httpClient ?: return fallbackAction(gameState, player)
        val prompt = buildPrompt(gameState, player)
        val model = AISettings.lmStudioModel.ifEmpty { "default" }

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("messages", kotlinx.serialization.json.buildJsonArray {
                add(buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("content", JsonPrimitive(prompt))
                })
            })
            put("temperature", JsonPrimitive(0.3))
            put("max_tokens", JsonPrimitive(200))
        }

        return try {
            val url = "${AISettings.lmStudioUrl}/v1/chat/completions"
            val response = client.postJson(url, requestBody.toString())
            if (response == null) {
                println("[AI] No response from LM Studio")
                return fallbackAction(gameState, player)
            }
            val responseText = try {
                val obj = json.parseToJsonElement(response) as JsonObject
                val choices = obj["choices"]?.jsonArray
                choices?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull ?: ""
            } catch (_: Exception) { response }

            println("[LMStudio] Response: $responseText")
            parseAction(responseText, gameState, player)
        } catch (e: Exception) {
            println("[LMStudio] Error: ${e.message}")
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

        val diplo = state.diplomacy.getRelation(player.id, 1)
        val diploStatus = when (diplo.status) {
            DiplomacyStatus.ALLIED -> "ALLIED"
            DiplomacyStatus.TRADE_PARTNERS -> "TRADE_PARTNERS"
            DiplomacyStatus.ENEMY -> "ENEMY"
            DiplomacyStatus.NEUTRAL -> "NEUTRAL"
        }

        return """You are an AI ruler in a turn-based strategy game.

YOUR STATE:
  Resources: Food=${player.resources.food}, Wood=${player.resources.wood}, Stone=${player.resources.stone}, Iron=${player.resources.iron}, Gold=${player.resources.gold}
  Turn: ${state.turn}

YOUR REGIONS:
$regionInfo

ENEMY REGIONS:
$enemyInfo

DIPLOMACY: $diploStatus (trade=${diplo.tradeActive})

AVAILABLE ACTIONS (choose exactly ONE):
  BUILD_FARM:REGION_ID — costs 10 Food + 5 Wood (region must have no buildings)
  BUILD_BARRACKS:REGION_ID — costs 15 Wood + 10 Stone + 10 Gold (region must have no buildings)
  BUILD_MINE:REGION_ID — costs 5 Wood + 15 Stone + 5 Iron (region must have no buildings)
  RECRUIT:REGION_ID — costs 10 Food + 5 Gold (requires Barracks in region)
  DEVELOP:REGION_ID — costs 10 Gold, +3 population
  PROPOSE_ALLIANCE:1 — form alliance with enemy
  BREAK_ALLIANCE:1 — break existing alliance
  PROPOSE_TRADE:1 — establish trade route with enemy
  CANCEL_TRADE:1 — cancel trade route
  END_TURN — skip actions

Reply with ONLY one line in format: ACTION:TARGET_ID
Example: BUILD_FARM:3"""
    }

    private fun parseAction(responseText: String, state: GameState, player: Player): AIAction? {
        val lines = responseText.lines()
        for (line in lines) {
            val trimmed = line.trim().uppercase()
            val match = Regex("""(BUILD_FARM|BUILD_BARRACKS|BUILD_MINE|RECRUIT|DEVELOP|PROPOSE_ALLIANCE|BREAK_ALLIANCE|PROPOSE_TRADE|CANCEL_TRADE|END_TURN)[:\s]+(\d+)""").find(trimmed)
            if (match != null) {
                val actionStr = match.groupValues[1]
                val regionId = match.groupValues[2].toIntOrNull() ?: continue
                val actionType = when (actionStr) {
                    "BUILD_FARM", "BUILD_BARRACKS", "BUILD_MINE" -> ActionQueue.ActionType.BUILD
                    "RECRUIT" -> ActionQueue.ActionType.RECRUIT
                    "DEVELOP" -> ActionQueue.ActionType.DEVELOP
                    "PROPOSE_ALLIANCE" -> ActionQueue.ActionType.PROPOSE_ALLIANCE
                    "BREAK_ALLIANCE" -> ActionQueue.ActionType.BREAK_ALLIANCE
                    "PROPOSE_TRADE" -> ActionQueue.ActionType.PROPOSE_TRADE
                    "CANCEL_TRADE" -> ActionQueue.ActionType.CANCEL_TRADE
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
                if (actionType == ActionQueue.ActionType.BUILD) {
                    if (region != null && region.ownerId == player.id) {
                        return AIAction(actionType, regionId, param)
                    }
                } else if (actionType in listOf(ActionQueue.ActionType.PROPOSE_ALLIANCE, ActionQueue.ActionType.BREAK_ALLIANCE, ActionQueue.ActionType.PROPOSE_TRADE, ActionQueue.ActionType.CANCEL_TRADE)) {
                    return AIAction(actionType, regionId, param)
                } else {
                    if (region != null && region.ownerId == player.id) {
                        return AIAction(actionType, regionId, param)
                    }
                }
            }
        }
        return fallbackAction(state, player)
    }

    private fun fallbackAction(state: GameState, player: Player): AIAction? {
        val ownedRegions = state.map.regions.filter { it.ownerId == player.id }
        val emptyRegions = ownedRegions.filter { it.buildings.isEmpty() && it.terrain != TerrainType.WATER }
        val barracksRegions = ownedRegions.filter { it.buildings.any { b -> b.type == BuildingType.BARRACKS } }

        val hasEnemyNeighbor = ownedRegions.any { region ->
            state.map.getNeighbors(region).any { it.ownerId != null && it.ownerId != player.id }
        }

        if (hasEnemyNeighbor && barracksRegions.isEmpty() && player.resources.canAfford(Resources(wood = 15, stone = 10, gold = 10))) {
            val target = emptyRegions.firstOrNull() ?: ownedRegions.firstOrNull()
            if (target != null) return AIAction(ActionQueue.ActionType.BUILD, target.id, "BARRACKS")
        }

        if (hasEnemyNeighbor && player.resources.canAfford(Resources(stone = 20, iron = 5))) {
            val wallRegion = ownedRegions.firstOrNull { r ->
                r.buildings.any { it.type == BuildingType.BARRACKS } && !r.buildings.any { it.type == BuildingType.WALL }
            }
            if (wallRegion != null) return AIAction(ActionQueue.ActionType.BUILD, wallRegion.id, "WALL")
        }

        if (player.resources.canAfford(Resources(food = 10, wood = 5))) {
            val target = emptyRegions.firstOrNull { it.terrain == TerrainType.PLAINS } ?: emptyRegions.firstOrNull()
            if (target != null) return AIAction(ActionQueue.ActionType.BUILD, target.id, "FARM")
        }

        if (player.resources.canAfford(Resources(wood = 5, stone = 15, iron = 5))) {
            val target = emptyRegions.firstOrNull { it.terrain == TerrainType.MOUNTAIN } ?: emptyRegions.firstOrNull()
            if (target != null) return AIAction(ActionQueue.ActionType.BUILD, target.id, "MINE")
        }

        if (player.resources.canAfford(Resources(food = 10, gold = 5)) && barracksRegions.isNotEmpty()) {
            return AIAction(ActionQueue.ActionType.RECRUIT, barracksRegions.first().id)
        }

        if (player.resources.canAfford(Resources(gold = 10))) {
            val region = ownedRegions.maxByOrNull { it.population }
            if (region != null) return AIAction(ActionQueue.ActionType.DEVELOP, region.id)
        }

        if (state.diplomacy.getRelation(player.id, 1).status == DiplomacyStatus.NEUTRAL) {
            if (player.resources.food > 30 && player.resources.gold > 20) {
                return AIAction(ActionQueue.ActionType.PROPOSE_TRADE, 1)
            }
        }

        if (state.diplomacy.isAllied(player.id, 1) && state.turn > 10) {
            val totalPop = ownedRegions.sumOf { it.population }
            if (totalPop > 60) return AIAction(ActionQueue.ActionType.BREAK_ALLIANCE, 1)
        }

        return null
    }
}
