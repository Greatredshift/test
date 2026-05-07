package com.beatmachine.sp404.audio.effects

import com.beatmachine.sp404.model.EffectType
import kotlin.math.*

class CompressorEffect(sampleRate: Int) : Effect(EffectType.COMPRESSOR, sampleRate) {
    var threshold: Float get() = param("threshold", -12f); set(v) = setParam("threshold", v)
    var ratio: Float get() = param("ratio", 4f); set(v) = setParam("ratio", v)
    var attack: Float get() = param("attack", 0.01f); set(v) = setParam("attack", v)
    var release: Float get() = param("release", 0.1f); set(v) = setParam("release", v)
    var makeupGain: Float get() = param("makeup", 0f); set(v) = setParam("makeup", v)

    private var envelope = 0f

    override fun process(buffer: FloatArray) {
        val attackCoeff = exp(-1f / (attack * sampleRate))
        val releaseCoeff = exp(-1f / (release * sampleRate))
        val threshLin = 10f.pow(threshold / 20f)
        val makeupLin = 10f.pow(makeupGain / 20f)

        for (f in 0 until buffer.size / 2) {
            val l = buffer[f * 2]; val r = buffer[f * 2 + 1]
            val peak = max(abs(l), abs(r))
            envelope = if (peak > envelope) attackCoeff * envelope + (1f - attackCoeff) * peak
                       else releaseCoeff * envelope + (1f - releaseCoeff) * peak

            val gainReduction = if (envelope > threshLin) {
                val overDb = 20f * log10(envelope / threshLin)
                val gainDb = -overDb * (1f - 1f / ratio)
                10f.pow(gainDb / 20f)
            } else 1f

            buffer[f * 2] = l * gainReduction * makeupLin
            buffer[f * 2 + 1] = r * gainReduction * makeupLin
        }
    }
    override fun reset() { envelope = 0f }
}

class LimiterEffect(sampleRate: Int) : Effect(EffectType.LIMITER, sampleRate) {
    var ceiling: Float get() = param("ceiling", -0.3f); set(v) = setParam("ceiling", v)
    var release: Float get() = param("release", 0.05f); set(v) = setParam("release", v)
    private var gain = 1f

    override fun process(buffer: FloatArray) {
        val relCoeff = exp(-1f / (release * sampleRate))
        val ceilLin = 10f.pow(ceiling / 20f)
        for (f in 0 until buffer.size / 2) {
            val l = buffer[f * 2]; val r = buffer[f * 2 + 1]
            val peak = max(abs(l), abs(r))
            val targetGain = if (peak > ceilLin) ceilLin / peak else 1f
            gain = if (targetGain < gain) targetGain else relCoeff * gain + (1f - relCoeff) * targetGain
            buffer[f * 2] = l * gain
            buffer[f * 2 + 1] = r * gain
        }
    }
    override fun reset() { gain = 1f }
}

// Sidechain Compressor (ducking: triggered externally)
class SidechainEffect(sampleRate: Int) : Effect(EffectType.SIDECHAIN, sampleRate) {
    var depth: Float get() = param("depth", 0.9f); set(v) = setParam("depth", v)
    var attack: Float get() = param("attack", 0.005f); set(v) = setParam("attack", v)
    var release: Float get() = param("release", 0.3f); set(v) = setParam("release", v)
    var bpm: Float get() = param("bpm", 120f); set(v) = setParam("bpm", v)

    private var sidePhase = 0.0
    private var envelope = 0f

    override fun process(buffer: FloatArray) {
        val stepsPerSec = bpm / 60.0
        val attackCoeff = exp(-1f / (attack * sampleRate))
        val releaseCoeff = exp(-1f / (release * sampleRate))
        for (f in 0 until buffer.size / 2) {
            sidePhase += stepsPerSec / sampleRate
            if (sidePhase >= 1.0) sidePhase -= 1.0
            val trigger = if (sidePhase < 0.5) 1f else 0f
            envelope = if (trigger > envelope) attackCoeff * envelope + (1f - attackCoeff) * trigger
                       else releaseCoeff * envelope + (1f - releaseCoeff) * trigger
            val gain = 1f - depth * envelope
            buffer[f * 2] *= gain
            buffer[f * 2 + 1] *= gain
        }
    }
    override fun reset() { sidePhase = 0.0; envelope = 0f }
}

// Wave Designer (transient shaper): boost/cut attack and sustain
class WaveDesignerEffect(sampleRate: Int) : Effect(EffectType.WAVE_DESIGNER, sampleRate) {
    var attackBoost: Float get() = param("attack", 0.5f); set(v) = setParam("attack", v)
    var sustainLevel: Float get() = param("sustain", 0.5f); set(v) = setParam("sustain", v)
    private var envFast = 0f; private var envSlow = 0f

    override fun process(buffer: FloatArray) {
        val fastAttack = exp(-1f / (0.001f * sampleRate))
        val fastRelease = exp(-1f / (0.05f * sampleRate))
        val slowAttack = exp(-1f / (0.05f * sampleRate))
        val slowRelease = exp(-1f / (0.5f * sampleRate))
        for (f in 0 until buffer.size / 2) {
            val l = buffer[f * 2]; val r = buffer[f * 2 + 1]
            val peak = max(abs(l), abs(r))
            envFast = if (peak > envFast) fastAttack * envFast + (1f - fastAttack) * peak
                      else fastRelease * envFast + (1f - fastRelease) * peak
            envSlow = if (peak > envSlow) slowAttack * envSlow + (1f - slowAttack) * peak
                      else slowRelease * envSlow + (1f - slowRelease) * peak
            val transient = (envFast - envSlow).coerceAtLeast(0f)
            val attGain = 1f + (attackBoost - 0.5f) * 2f * transient
            val susGain = 1f + (sustainLevel - 0.5f) * 2f * envSlow
            val gain = (attGain * susGain).coerceIn(0f, 4f)
            buffer[f * 2] = l * gain
            buffer[f * 2 + 1] = r * gain
        }
    }
    override fun reset() { envFast = 0f; envSlow = 0f }
}
