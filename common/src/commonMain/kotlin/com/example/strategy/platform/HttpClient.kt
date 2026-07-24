package com.example.strategy.platform

// HTTP client abstraction — implementation only in desktop module
interface HttpClient {
    fun postJson(url: String, jsonBody: String): String?
}
