package com.example.strategy.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import kotlin.system.exitProcess

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val msg = "FATAL in thread '${thread.name}':\n${throwable.stackTraceToString()}"
        System.err.println(msg)
        System.err.flush()
        try { java.io.File("/tmp/strategy_crash.log").writeText(msg) } catch (_: Exception) {}
        exitProcess(1)
    }

    println("Starting Strategy game...")
    System.out.flush()

    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Strategy — 2D Turn-Based")
        setWindowedMode(1280, 720)
        useVsync(true)
        setForegroundFPS(60)
    }

    println("Creating Lwjgl3Application...")
    System.out.flush()
    Lwjgl3Application(StrategyGame(), config)
}
