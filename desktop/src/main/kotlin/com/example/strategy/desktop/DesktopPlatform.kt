package com.example.strategy.desktop

import com.example.strategy.platform.Platform
import java.io.File

// Desktop actual implementation — file I/O for JVM
class DesktopPlatform : Platform {
    private val baseDir = File(System.getProperty("user.dir"))

    override fun readTextFile(path: String): String? {
        val file = File(baseDir, path)
        return if (file.exists()) file.readText() else null
    }

    override fun writeTextFile(path: String, content: String) {
        File(baseDir, path).writeText(content)
    }

    override fun getAssetPath(assetName: String): String =
        File(baseDir, "assets/$assetName").absolutePath
}
