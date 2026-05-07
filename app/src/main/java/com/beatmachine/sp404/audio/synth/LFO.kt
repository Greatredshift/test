package com.beatmachine.sp404.audio.synth

import kotlin.math.*

enum class LFOShape { SINE, SQUARE, TRIANGLE, SAWTOOTH, SAW_DOWN, RANDOM, SAMPLE_AND_HOLD }
enum class LFOTarget { PITCH, FILTER, AMPLITUDE, PAN, PULSEWIDTH, OSC2_PITCH }

class LFO(private val sampleRate: Int) {
    var shape: LFOShape = LFOShape.SINE
    var rate: Float = 2.0f         // Hz
    var depth: Float = 0.5f        // 0..1
    var target: LFOTarget = LFOTarget.PITCH
    var keySync: Boolean = false
    var enabled: Boolean = true
    var tempoSync: Boolean = false  // if true, rate is in beats
    var bpm: Double = 120.0

    private var phase = 0.0
    private var holdValue = 0f
    private var prevPhase = 0.0
    private val random = java.util.Random()

    val effectiveRate: Double get() {
        return if (tempoSync) bpm / 60.0 * rate else rate.toDouble()
    }

    fun sync() { phase = 0.0; prevPhase = 0.0 }

    fun process(): Float {
        if (!enabled) return 0f
        val dt = effectiveRate / sampleRate
        val v = when (shape) {
            LFOShape.SINE -> sin(2.0 * PI * phase).toFloat()
            LFOShape.SQUARE -> if (phase < 0.5) 1f else -1f
            LFOShape.TRIANGLE -> (2.0 * abs(2.0 * phase - 1.0) - 1.0).toFloat()
            LFOShape.SAWTOOTH -> (2.0 * phase - 1.0).toFloat()
            LFOShape.SAW_DOWN -> (1.0 - 2.0 * phase).toFloat()
            LFOShape.RANDOM -> {
                if (phase < prevPhase) holdValue = random.nextFloat() * 2f - 1f
                holdValue
            }
            LFOShape.SAMPLE_AND_HOLD -> {
                if (phase < prevPhase) holdValue = random.nextFloat() * 2f - 1f
                holdValue
            }
        }
        prevPhase = phase
        phase += dt
        if (phase >= 1.0) phase -= 1.0
        return v * depth
    }
}
