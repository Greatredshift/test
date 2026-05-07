package com.beatmachine.sp404.audio.synth

enum class EnvStage { IDLE, ATTACK, DECAY, SUSTAIN, RELEASE }

class Envelope(private val sampleRate: Int) {
    var attack: Float = 0.005f    // seconds
    var decay: Float = 0.1f
    var sustain: Float = 0.7f     // 0..1 level
    var release: Float = 0.3f

    private var stage = EnvStage.IDLE
    private var level = 0.0
    private var releaseLevel = 0.0

    val isActive get() = stage != EnvStage.IDLE

    fun noteOn() {
        stage = EnvStage.ATTACK
        // Keep current level for click-free re-triggering
    }

    fun noteOff() {
        if (stage != EnvStage.IDLE) {
            releaseLevel = level
            stage = EnvStage.RELEASE
        }
    }

    fun process(): Float {
        val attackSamples = (attack * sampleRate).toDouble().coerceAtLeast(1.0)
        val decaySamples = (decay * sampleRate).toDouble().coerceAtLeast(1.0)
        val releaseSamples = (release * sampleRate).toDouble().coerceAtLeast(1.0)

        when (stage) {
            EnvStage.ATTACK -> {
                level += 1.0 / attackSamples
                if (level >= 1.0) {
                    level = 1.0
                    stage = EnvStage.DECAY
                }
            }
            EnvStage.DECAY -> {
                level -= (1.0 - sustain) / decaySamples
                if (level <= sustain) {
                    level = sustain.toDouble()
                    stage = EnvStage.SUSTAIN
                }
            }
            EnvStage.SUSTAIN -> level = sustain.toDouble()
            EnvStage.RELEASE -> {
                level -= releaseLevel / releaseSamples
                if (level <= 0.0) {
                    level = 0.0
                    stage = EnvStage.IDLE
                }
            }
            EnvStage.IDLE -> level = 0.0
        }
        return level.toFloat()
    }

    fun reset() {
        stage = EnvStage.IDLE
        level = 0.0
    }
}
