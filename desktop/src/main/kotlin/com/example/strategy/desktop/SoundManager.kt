package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Disposable
import kotlin.math.sin

class SoundManager : Disposable {

    private val sounds = mutableMapOf<SoundType, Sound>()

    enum class SoundType {
        BUILD,
        RECRUIT,
        ATTACK,
        SELECT,
        END_TURN,
        RESEARCH,
        ALLIANCE,
        ERROR
    }

    init {
        generateAllSounds()
    }

    private fun generateAllSounds() {
        sounds[SoundType.BUILD] = generateBeep(440f, 0.15f)
        sounds[SoundType.RECRUIT] = generateBeep(523f, 0.1f)
        sounds[SoundType.ATTACK] = generateNoise(0.2f)
        sounds[SoundType.SELECT] = generateBeep(660f, 0.05f)
        sounds[SoundType.END_TURN] = generateBeep(330f, 0.2f)
        sounds[SoundType.RESEARCH] = generateBeep(880f, 0.15f)
        sounds[SoundType.ALLIANCE] = generateBeep(550f, 0.2f)
        sounds[SoundType.ERROR] = generateBeep(200f, 0.1f)
    }

    private fun generateBeep(freq: Float, duration: Float): Sound {
        val sampleRate = 22050
        val samples = (sampleRate * duration).toInt()
        val data = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toFloat() / sampleRate
            val envelope = 1f - (i.toFloat() / samples)
            val value = (sin(2.0 * Math.PI * freq * t) * 8000 * envelope).toInt().toShort()
            data[i] = value
        }
        return createSoundFromSamples(data, sampleRate)
    }

    private fun generateNoise(duration: Float): Sound {
        val sampleRate = 22050
        val samples = (sampleRate * duration).toInt()
        val data = ShortArray(samples)
        for (i in 0 until samples) {
            val envelope = 1f - (i.toFloat() / samples)
            val value = (Math.random() * 6000 * envelope - 3000 * envelope).toInt().toShort()
            data[i] = value
        }
        return createSoundFromSamples(data, sampleRate)
    }

    private fun createSoundFromSamples(samples: ShortArray, sampleRate: Int): Sound {
        val file = java.io.File.createTempFile("sound_", ".wav")
        file.deleteOnExit()

        val dataSize = samples.size * 2
        val fileSize = 44 + dataSize

        java.io.FileOutputStream(file).use { fos ->
            val dos = java.io.DataOutputStream(fos)
            dos.writeBytes("RIFF")
            dos.writeInt(java.lang.Integer.reverseBytes(fileSize - 8))
            dos.writeBytes("WAVE")
            dos.writeBytes("fmt ")
            dos.writeInt(java.lang.Integer.reverseBytes(16))
            dos.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
            dos.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
            dos.writeInt(java.lang.Integer.reverseBytes(sampleRate))
            dos.writeInt(java.lang.Integer.reverseBytes(sampleRate * 2))
            dos.writeShort(java.lang.Short.reverseBytes(2.toShort()).toInt())
            dos.writeShort(java.lang.Short.reverseBytes(16.toShort()).toInt())
            dos.writeBytes("data")
            dos.writeInt(java.lang.Integer.reverseBytes(dataSize))
            for (sample in samples) {
                dos.writeShort(java.lang.Short.reverseBytes(sample).toInt())
            }
        }

        val sound = Gdx.audio.newSound(Gdx.files.absolute(file.absolutePath))
        file.delete()
        return sound
    }

    fun play(type: SoundType) {
        sounds[type]?.play(0.5f)
    }

    override fun dispose() {
        sounds.values.forEach { it.dispose() }
    }
}
