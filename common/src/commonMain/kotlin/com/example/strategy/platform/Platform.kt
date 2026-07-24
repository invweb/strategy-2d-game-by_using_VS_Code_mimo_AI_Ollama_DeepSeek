package com.example.strategy.platform

// Platform abstraction — interface with implementation only in desktop
interface Platform {
    fun readTextFile(path: String): String?
    fun writeTextFile(path: String, content: String)
    fun getAssetPath(assetName: String): String
}

// Global platform instance — set by desktop module at startup
object PlatformProvider {
    lateinit var platform: Platform
    var httpClient: HttpClient? = null
}
