package com.example.strategy.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import java.io.PrintStream
import java.time.Instant
import kotlin.system.exitProcess

val logFile = java.io.File(System.getProperty("user.home"), "strategy_crash.log")
val logStream: PrintStream = PrintStream(logFile, Charsets.UTF_8)

fun main() {
    System.setErr(object : PrintStream(System.err) {
        override fun println(s: String) { logStream.println("[STDERR] $s"); logStream.flush(); super.println(s) }
        override fun println() { logStream.println(); logStream.flush(); super.println() }
        override fun print(s: String) { logStream.print("[STDERR] $s"); logStream.flush(); super.print(s) }
    })

    Runtime.getRuntime().addShutdownHook(Thread {
        logStream.println("[SHUTDOWN] JVM shutting down at ${Instant.now()}")
        val threads = Thread.getAllStackTraces()
        for ((t, stack) in threads) {
            if (t.isAlive && !t.isDaemon) {
                logStream.println("  Thread '${t.name}' state=${t.state}")
                for (frame in stack.take(10)) {
                    logStream.println("    at $frame")
                }
            }
        }
        logStream.flush()
    })

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val msg = "FATAL in thread '${thread.name}':\n${throwable.stackTraceToString()}"
        logStream.println(msg)
        logStream.flush()
        System.err.println(msg)
        System.err.flush()
        exitProcess(1)
    }

    logStream.println("=== START at ${Instant.now()} ===")
    logStream.flush()

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
    logStream.println("[EXIT] Lwjgl3Application returned normally at ${Instant.now()}")
    logStream.flush()
}
