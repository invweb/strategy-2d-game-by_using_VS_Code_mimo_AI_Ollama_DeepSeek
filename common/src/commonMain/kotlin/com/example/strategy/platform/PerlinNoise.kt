package com.example.strategy.platform

import kotlin.random.Random

class PerlinNoise(seed: Long = Random.nextLong()) {

    private val perm = IntArray(512)
    private val grad3 = arrayOf(
        intArrayOf(1,1,0), intArrayOf(-1,1,0), intArrayOf(1,-1,0), intArrayOf(-1,-1,0),
        intArrayOf(1,0,1), intArrayOf(-1,0,1), intArrayOf(1,0,-1), intArrayOf(-1,0,-1),
        intArrayOf(0,1,1), intArrayOf(0,-1,1), intArrayOf(0,1,-1), intArrayOf(0,-1,-1)
    )

    init {
        val p = IntArray(256)
        val rng = Random(seed)
        for (i in 0 until 256) p[i] = i
        for (i in 255 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = p[i]; p[i] = p[j]; p[j] = tmp
        }
        for (i in 0 until 512) perm[i] = p[i and 255]
    }

    private fun dot(g: IntArray, x: Double, y: Double): Double = g[0] * x + g[1] * y

    private fun fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

    private fun lerp(a: Double, b: Double, t: Double): Double = a + t * (b - a)

    fun noise(x: Double, y: Double): Double {
        val xi = x.toInt() and 255
        val yi = y.toInt() and 255
        val xf = x - x.toInt().toDouble()
        val yf = y - y.toInt().toDouble()

        val u = fade(xf)
        val v = fade(yf)

        val aa = perm[perm[xi] + yi] % 12
        val ab = perm[perm[xi] + yi + 1] % 12
        val ba = perm[perm[xi + 1] + yi] % 12
        val bb = perm[perm[xi + 1] + yi + 1] % 12

        val x1 = lerp(dot(grad3[aa], xf, yf), dot(grad3[ba], xf - 1, yf), u)
        val x2 = lerp(dot(grad3[ab], xf, yf - 1), dot(grad3[bb], xf - 1, yf - 1), u)
        return lerp(x1, x2, v)
    }

    fun octaveNoise(x: Double, y: Double, octaves: Int = 4, persistence: Double = 0.5): Double {
        var total = 0.0
        var frequency = 1.0
        var amplitude = 1.0
        var maxValue = 0.0
        for (i in 0 until octaves) {
            total += noise(x * frequency, y * frequency) * amplitude
            maxValue += amplitude
            amplitude *= persistence
            frequency *= 2.0
        }
        return total / maxValue
    }
}
