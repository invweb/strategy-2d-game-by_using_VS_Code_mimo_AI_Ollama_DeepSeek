package com.example.strategy.ai

import com.example.strategy.platform.PlatformProvider

object AISettings {
    enum class Backend(val displayName: String) {
        NONE("None (Fallback only)"),
        OLLAMA("Ollama"),
        LM_STUDIO("LM Studio")
    }

    private const val PREFS_FILE = "ai_prefs.txt"

    var backend: Backend = Backend.NONE
        private set
    var ollamaUrl: String = "http://localhost:11434"
        private set
    var ollamaModel: String = "deepseek-r1:7b"
        private set
    var lmStudioUrl: String = "http://localhost:1234"
        private set
    var lmStudioModel: String = ""
        private set

    fun setBackend(b: Backend) { backend = b; save() }
    fun setOllamaUrl(url: String) { ollamaUrl = url; save() }
    fun setOllamaModel(model: String) { ollamaModel = model; save() }
    fun setLmStudioUrl(url: String) { lmStudioUrl = url; save() }
    fun setLmStudioModel(model: String) { lmStudioModel = model; save() }

    fun load() {
        try {
            val content = PlatformProvider.platform.readTextFile(PREFS_FILE) ?: return
            for (line in content.lines()) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    when (parts[0]) {
                        "backend" -> backend = Backend.entries.find { it.name == parts[1] } ?: Backend.NONE
                        "ollamaUrl" -> ollamaUrl = parts[1]
                        "ollamaModel" -> ollamaModel = parts[1]
                        "lmStudioUrl" -> lmStudioUrl = parts[1]
                        "lmStudioModel" -> lmStudioModel = parts[1]
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun save() {
        try {
            val content = buildString {
                appendLine("backend=${backend.name}")
                appendLine("ollamaUrl=$ollamaUrl")
                appendLine("ollamaModel=$ollamaModel")
                appendLine("lmStudioUrl=$lmStudioUrl")
                appendLine("lmStudioModel=$lmStudioModel")
            }
            PlatformProvider.platform.writeTextFile(PREFS_FILE, content)
        } catch (_: Exception) {}
    }
}
