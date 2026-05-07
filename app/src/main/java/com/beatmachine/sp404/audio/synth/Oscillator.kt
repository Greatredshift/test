package com.beatmachine.sp404.audio.synth

import kotlin.math.*

enum class WaveShape { SINE, SQUARE, SAWTOOTH, SAW_DOWN, TRIANGLE, NOISE, PULSE }

class Oscillator(private val sampleRate: Int) {
    var waveShape: WaveShape = WaveShape.SAWTOOTH
    var frequency: Double = 440.0
    var detuneCents: Double = 0.0
    var pulseWidth: Double = 0.5   // 0.01..0.99 for PULSE
    var volume: Float = 1.0f
    var enabled: Boolean = true

    private var phase: Double = 0.0
    private val random = java.util.Random()
    private var prevNoise: Float = 0f

    private val effectiveFreq get() = frequency * 2.0.pow(detuneCents / 1200.0)

    fun noteFrequency(midiNote: Int) = 440.0 * 2.0.pow((midiNote - 69) / 12.0)

    fun render(buffer: FloatArray, startFrame: Int = 0, frames: Int = buffer.size / 2) {
        if (!enabled) return
        val dt = effectiveFreq / sampleRate
        for (f in startFrame until startFrame + frames) {
            val sample = when (waveShape) {
                WaveShape.SINE -> sin(2.0 * PI * phase).toFloat()
                WaveShape.SQUARE -> {
                    val raw = if (phase < 0.5) 1.0 else -1.0
                    (raw + polyBlep(phase, dt) - polyBlep((phase + 0.5) % 1.0, dt)).toFloat()
                }
                WaveShape.SAWTOOTH -> {
                    val raw = 2.0 * phase - 1.0
                    (raw - polyBlep(phase, dt)).toFloat()
                }
                WaveShape.SAW_DOWN -> {
                    val raw = 1.0 - 2.0 * phase
                    (raw + polyBlep(phase, dt)).toFloat()
                }
                WaveShape.TRIANGLE -> {
                    val sq = if (phase < 0.5) 1.0 else -1.0
                    val integrated = 4.0 * sq * phase - 2.0 * sq - (if (phase < 0.5) 0.0 else 4.0 * (phase - 0.5))
                    // Simpler triangle from integrated square
                    (2.0 * abs(2.0 * phase - 1.0) - 1.0).toFloat()
                }
                WaveShape.NOISE -> {
                    prevNoise = (random.nextFloat() * 2f - 1f)
                    prevNoise
                }
                WaveShape.PULSE -> {
                    val pw = pulseWidth.coerceIn(0.01, 0.99)
                    val raw = if (phase < pw) 1.0 else -1.0
                    (raw + polyBlep(phase, dt) - polyBlep((phase + (1.0 - pw)) % 1.0, dt)).toFloat()
                }
            }

            val base = f * 2
            if (base + 1 < buffer.size) {
                buffer[base] += sample * volume
                buffer[base + 1] += sample * volume
            }

            phase += dt
            if (phase >= 1.0) phase -= 1.0
        }
    }

    private fun polyBlep(t: Double, dt: Double): Double {
        return when {
            t < dt -> {
                val u = t / dt
                u + u - u * u - 1.0
            }
            t > 1.0 - dt -> {
                val u = (t - 1.0) / dt
                u * u + u + u + 1.0
            }
            else -> 0.0
        }
    }

    fun reset() { phase = 0.0 }
}
