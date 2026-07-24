package com.example.strategy.desktop

import com.example.strategy.platform.HttpClient
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Desktop HTTP client — uses Java HttpURLConnection
class DesktopHttpClient : HttpClient {

    override fun postJson(url: String, jsonBody: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val code = connection.responseCode
            if (code == 200) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
            } else {
                println("[DesktopHttpClient] HTTP $code")
                null
            }
        } catch (e: Exception) {
            println("[DesktopHttpClient] Error: ${e.message}")
            null
        } finally {
            connection.disconnect()
        }
    }
}
