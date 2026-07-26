package com.example.strategy.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Disposable
import kotlin.math.sin
import kotlin.math.abs

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
        ERROR,
        MOVE,
        VICTORY,
        DEFEAT
    }

    init {
        generateAllSounds()
    }

    private fun generateAllSounds() {
        sounds[SoundType.BUILD] = generateChord(floatArrayOf(330f, 415f, 495f), 0.15f)
        sounds[SoundType.RECRUIT] = generateArpeggio(floatArrayOf(440f, 554f, 659f), 0.12f)
        sounds[SoundType.ATTACK] = generateImpact(0.25f)
        sounds[SoundType.SELECT] = generateBeep(880f, 0.04f, waveform = Waveform.SINE)
        sounds[SoundType.END_TURN] = generateChord(floatArrayOf(262f, 330f, 392f), 0.25f)
        sounds[SoundType.RESEARCH] = generateArpeggio(floatArrayOf(523f, 659f, 784f, 1047f), 0.18f)
        sounds[SoundType.ALLIANCE] = generateChord(floatArrayOf(440f, 554f, 659f, 880f), 0.3f)
        sounds[SoundType.ERROR] = generateBeep(180f, 0.15f, waveform = Waveform.SQUARE)
        sounds[SoundType.MOVE] = generateSweep(300f, 600f, 0.1f)
        sounds[SoundType.VICTORY] = generateFanfare(0.5f)
        sounds[SoundType.DEFEAT] = generateDescend(0.4f)
    }

    private enum class Waveform { SINE, SQUARE, SAWTOOTH }

    private fun generateBeep(freq: Float, duration: Float, waveform: Waveform = Waveform.SINE): Sound {
        val sampleRate = 22050
        val samples = (sampleRate * duration).toInt()
        val data = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toFloat() / sampleRate
            val envelope = (1f - (i.toFloat() / samples)).let { it * it }
            val raw = when (waveform) {
                Waveform.SINE -> sin(2.0 * Math.PI * freq * t)
                Waveform.SQUARE -> if (sin(2.0 * Math.PI * freq * t) > 0) 1.0 else -1.0
                Waveform.SAWTOOTH -> 2.0 * (freq * t % 1.0) - 1.0
            }
            data[i] = (raw * 6000 * envelope).toInt().toShort()
        }
        return createSoundFromSamples(data, sampleRate)
    }

    private fun generateChord(freqs: FloatArray, duration: Float): Sound {
        val sampleRate = 22050
        val samples = (sampleRate * duration).toInt()
        val data = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toFloat() / sampleRate
            val fadeOut = (1f - (i.toFloat() / samples)).let { it * it }
            val fadeIn = (i.toFloat() / (samples * 0.1f)).coerceIn(0f, 1f)
            val envelope = fadeIn * fadeOut
            var value = 0.0
            for (freq in freqs) {
                value += sin(2.0 * Math.PI * freq * t)
            }
            value /= freqs.size
            data[i] = (value * 5000 * envelope).toInt().toShort()
        }
        return createSoundFromSamples(data, sampleRate)
    }

    private fun generateArpeggio(freqs: FloatArray, totalDuration: Float): Sound {
        val sampleRate = 22050
        val samples = (sampleRate * totalDuration).toInt()
        val data = ShortArray(samples)
        val noteDuration = samples / freqs.size
        for (i in 0 until samples) {
            val noteIndex = (i / noteDuration).coerceIn(0, freqs.size - 1)
            val t = i.toFloat() / sampleRate
            val notePos = i % noteDuration
            val envelope = (1f - (notePos.toFloat() / noteDuration)).let { it * it }
            val value = sin(2.0 * Math.PI * freqs[noteIndex] * t)
            data[i] = (value * 5500 * envelope).toInt().toShort()
        }
        return createSoundFromSamples(data, sampleRate)
    }

    private fun generateSweep(startFreq: Float, endFreq: Float, duration: Float): Sound {
        val sampleRate = 22050
        val samples = (sampleRate * duration).toInt()
        val data = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toFloat() / sampleRate
            val progress = i.toFloat() / samples
            val freq = startFreq + (endFreq - startFreq) * progress
            val envelope = 1f - progress * progress
            val value = sin(2.0 * Math.PI * freq * t)
            data[i] = (value * 5000 * envelope).toInt().toShort()
        }
        return createSoundFromSamples(data, sampleRate)
    }

    private fun generateImpact(duration: Float): Sound {
        val sampleRate = 22050
        val samples = (sampleRate * duration).toInt()
        val data = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toFloat() / sampleRate
            val envelope = kotlin.math.exp(-8.0 * t)
            val noise = Math.random() * 2 - 1
            val tone = sin(2.0 * Math.PI * 120.0 * t)
            val value = noise * 0.6 + tone * 0.4
            data[i] = (value * 7000 * envelope).toInt().toShort()
        }
        return createSoundFromSamples(data, sampleRate)
    }

    private fun generateFanfare(duration: Float): Sound {
        val sampleRate = 22050
        val samples = (sampleRate * duration).toInt()
        val data = ShortArray(samples)
        val notes = floatArrayOf(523f, 659f, 784f, 1047f, 784f, 1047f)
        val noteDuration = samples / notes.size
        for (i in 0 until samples) {
            val noteIndex = (i / noteDuration).coerceIn(0, notes.size - 1)
            val t = i.toFloat() / sampleRate
            val notePos = i % noteDuration
            val envelope = if (noteIndex == notes.size - 1) {
                (1f - (notePos.toFloat() / noteDuration)).let { it * it }
            } else {
                val attack = (notePos.toFloat() / (noteDuration * 0.1f)).coerceIn(0f, 1f)
                val release = (1f - (notePos.toFloat() / noteDuration)).let { it * it }
                attack * release
            }
            val value = sin(2.0 * Math.PI * notes[noteIndex] * t) * 0.7 +
                        sin(2.0 * Math.PI * notes[noteIndex] * 2 * t) * 0.3
            data[i] = (value * 5000 * envelope).toInt().toShort()
        }
        return createSoundFromSamples(data, sampleRate)
    }

    private fun generateDescend(duration: Float): Sound {
        val sampleRate = 22050
        val samples = (sampleRate * duration).toInt()
        val data = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toFloat() / sampleRate
            val progress = i.toFloat() / samples
            val freq = 600f - 400f * progress
            val envelope = 1f - progress
            val value = sin(2.0 * Math.PI * freq * t) * 0.7 +
                        sin(2.0 * Math.PI * freq * 0.5 * t) * 0.3
            data[i] = (value * 5000 * envelope * envelope).toInt().toShort()
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
