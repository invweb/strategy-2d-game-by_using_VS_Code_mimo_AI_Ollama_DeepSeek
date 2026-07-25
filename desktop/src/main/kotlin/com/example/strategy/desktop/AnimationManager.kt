package com.example.strategy.desktop

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class AnimationManager {

    private val animations = mutableListOf<GameAnimation>()

    data class GameAnimation(
        val type: AnimType,
        val fromX: Float,
        val fromY: Float,
        val toX: Float,
        val toY: Float,
        var progress: Float = 0f,
        val duration: Float = 0.5f,
        val color: Color = Color.WHITE
    ) {
        val isDone: Boolean get() = progress >= 1f
    }

    enum class AnimType {
        MOVE,
        ATTACK,
        BUILD,
        RESEARCH,
        DAMAGE
    }

    fun addMove(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        animations.add(GameAnimation(AnimType.MOVE, fromX, fromY, toX, toY, color = Color(0.5f, 0.8f, 1f, 0.9f)))
    }

    fun addAttack(x: Float, y: Float) {
        animations.add(GameAnimation(AnimType.ATTACK, x, y, x, y, duration = 0.4f, color = Color(1f, 0.2f, 0.2f, 0.9f)))
    }

    fun addDamage(x: Float, y: Float, damage: Int) {
        animations.add(GameAnimation(AnimType.DAMAGE, x, y, x, y - 30f, duration = 1f, color = Color(1f, 0.3f, 0.3f, 0.9f)))
    }

    fun addBuild(x: Float, y: Float) {
        animations.add(GameAnimation(AnimType.BUILD, x, y, x, y, duration = 0.4f, color = Color(0.2f, 0.8f, 0.2f, 0.8f)))
    }

    fun addResearch(x: Float, y: Float) {
        animations.add(GameAnimation(AnimType.RESEARCH, x, y, x, y, duration = 0.5f, color = Color(1f, 1f, 0.2f, 0.8f)))
    }

    fun update(delta: Float) {
        animations.forEach { it.progress += delta / it.duration }
        animations.removeAll { it.isDone }
    }

    fun render(sr: ShapeRenderer, tileSize: Float) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        for (anim in animations) {
            val alpha = when {
                anim.progress < 0.2f -> anim.progress / 0.2f
                anim.progress > 0.8f -> (1f - anim.progress) / 0.2f
                else -> 1f
            }

            when (anim.type) {
                AnimType.MOVE -> {
                    val cx = anim.fromX + (anim.toX - anim.fromX) * anim.progress
                    val cy = anim.fromY + (anim.toY - anim.fromY) * anim.progress
                    sr.setColor(anim.color.r, anim.color.g, anim.color.b, alpha * 0.8f)
                    sr.rect(cx - 8f, cy - 8f, 16f, 16f)
                }
                AnimType.ATTACK -> {
                    val size = tileSize * (0.5f + anim.progress * 0.5f)
                    sr.setColor(anim.color.r, anim.color.g, anim.color.b, alpha * 0.6f)
                    sr.rect(anim.fromX - size / 2f, anim.fromY - size / 2f, size, size)
                }
                AnimType.BUILD -> {
                    val size = tileSize * 0.3f
                    val offsetY = anim.progress * 20f
                    sr.setColor(anim.color.r, anim.color.g, anim.color.b, alpha * 0.7f)
                    sr.rect(anim.fromX - size / 2f, anim.fromY + offsetY, size, size)
                }
                AnimType.RESEARCH -> {
                    val size = tileSize * 0.4f
                    val wobble = kotlin.math.sin(anim.progress * 6.28f * 3f) * 5f
                    sr.setColor(anim.color.r, anim.color.g, anim.color.b, alpha * 0.7f)
                    sr.rect(anim.fromX - size / 2f + wobble, anim.fromY, size, size)
                }
                AnimType.DAMAGE -> {
                    val cx = anim.fromX + (anim.toX - anim.fromX) * anim.progress
                    val cy = anim.fromY + (anim.toY - anim.fromY) * anim.progress
                    sr.setColor(anim.color.r, anim.color.g, anim.color.b, alpha * 0.8f)
                    sr.rect(cx - 6f, cy - 6f, 12f, 12f)
                }
            }
        }
        sr.setColor(Color.WHITE)
        sr.end()
    }

    fun hasAnimations(): Boolean = animations.isNotEmpty()
}
