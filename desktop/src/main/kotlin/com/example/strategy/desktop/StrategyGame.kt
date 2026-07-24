package com.example.strategy.desktop

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.example.strategy.model.GameState
import com.example.strategy.platform.GameFactory
import com.example.strategy.platform.PlatformProvider

// Main Game class — manages screens and shared state
class StrategyGame : Game() {
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    var gameState: GameState = GameFactory.createDefaultGameState()

    override fun create() {
        // Initialize platform abstraction
        PlatformProvider.platform = DesktopPlatform()
        PlatformProvider.httpClient = DesktopHttpClient()

        batch = SpriteBatch()
        font = BitmapFont()
        font.data.setScale(1.2f)
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
        super.dispose()
    }
}
