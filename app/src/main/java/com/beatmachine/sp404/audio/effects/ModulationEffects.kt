package com.beatmachine.sp404.audio.effects

import com.beatmachine.sp404.model.EffectType
import kotlin.math.*

// Chorus: short delay (20-30ms) with LFO
class ChorusEffect(sampleRate: Int) : Effect(EffectType.CHORUS, sampleRate) {
    private val maxDelay = (sampleRate * 0.05).toInt()
    private val bufL = FloatArray(maxDelay)
    private val bufR = FloatArray(maxDelay)
    private var writeIdx = 0
    private var lfoPhase = 0.0
    var rate: Float get() = param("rate", 1.5f); set(v) = setParam("rate", v)
    var depth: Float get() = param("depth", 0.5f); set(v) = setParam("depth", v)

    override fun process(buffer: FloatArray) {
        val lfoRate = rate.toDouble() / sampleRate
        val depthSamples = depth * maxDelay * 0.4f
        for (frame in 0 until buffer.size / 2) {
            val modL = sin(2.0 * PI * lfoPhase)
            val modR = sin(2.0 * PI * (lfoPhase + 0.33))
            lfoPhase += lfoRate; if (lfoPhase >= 1.0) lfoPhase -= 1.0
            val delL = ((maxDelay * 0.4) + depthSamples * modL).toInt().coerceIn(1, maxDelay - 1)
            val delR = ((maxDelay * 0.4) + depthSamples * modR).toInt().coerceIn(1, maxDelay - 1)
            bufL[writeIdx] = buffer[frame * 2]; bufR[writeIdx] = buffer[frame * 2 + 1]
            val wL = Math.floorMod(writeIdx - delL, maxDelay)
            val wR = Math.floorMod(writeIdx - delR, maxDelay)
            buffer[frame * 2] += bufL[wL] * 0.7f
            buffer[frame * 2 + 1] += bufR[wR] * 0.7f
            writeIdx = (writeIdx + 1) % maxDelay
        }
    }
    override fun reset() { bufL.fill(0f); bufR.fill(0f); writeIdx = 0; lfoPhase = 0.0 }
}

// Flanger: very short delay (1-10ms) with LFO + feedback
class FlangerEffect(sampleRate: Int) : Effect(EffectType.FLANGER, sampleRate) {
    private val maxDelay = (sampleRate * 0.02).toInt()
    private val bufL = FloatArray(maxDelay)
    private val bufR = FloatArray(maxDelay)
    private var writeIdx = 0
    private var lfoPhase = 0.0
    var rate: Float get() = param("rate", 0.5f); set(v) = setParam("rate", v)
    var depth: Float get() = param("depth", 0.8f); set(v) = setParam("depth", v)
    var feedback: Float get() = param("feedback", 0.5f); set(v) = setParam("feedback", v)

    override fun process(buffer: FloatArray) {
        val lfoRate = rate.toDouble() / sampleRate
        val depthSamples = depth * (maxDelay - 1)
        val fb = feedback
        for (frame in 0 until buffer.size / 2) {
            val mod = sin(2.0 * PI * lfoPhase)
            lfoPhase += lfoRate; if (lfoPhase >= 1.0) lfoPhase -= 1.0
            val del = ((maxDelay / 2.0) + (depthSamples / 2.0) * mod).toInt().coerceIn(1, maxDelay - 1)
            val rL = Math.floorMod(writeIdx - del, maxDelay)
            val rR = Math.floorMod(writeIdx - del, maxDelay)
            val delL = bufL[rL]; val delR = bufR[rR]
            bufL[writeIdx] = buffer[frame * 2] + delL * fb
            bufR[writeIdx] = buffer[frame * 2 + 1] + delR * fb
            buffer[frame * 2] += delL * 0.7f
            buffer[frame * 2 + 1] += delR * 0.7f
            writeIdx = (writeIdx + 1) % maxDelay
        }
    }
    override fun reset() { bufL.fill(0f); bufR.fill(0f); writeIdx = 0; lfoPhase = 0.0 }
}

// Phaser: all-pass filter chain with LFO
class PhaserEffect(sampleRate: Int) : Effect(EffectType.PHASER, sampleRate) {
    private val stages = 6
    private val x1L = DoubleArray(stages); private val x2L = DoubleArray(stages)
    private val y1L = DoubleArray(stages); private val y2L = DoubleArray(stages)
    private val x1R = DoubleArray(stages); private val x2R = DoubleArray(stages)
    private val y1R = DoubleArray(stages); private val y2R = DoubleArray(stages)
    private var lfoPhase = 0.0
    var rate: Float get() = param("rate", 0.8f); set(v) = setParam("rate", v)
    var depth: Float get() = param("depth", 1.0f); set(v) = setParam("depth", v)
    var feedback: Float get() = param("feedback", 0.5f); set(v) = setParam("feedback", v)
    private var fbL = 0.0; private var fbR = 0.0

    override fun process(buffer: FloatArray) {
        val lfoRate = rate.toDouble() / sampleRate
        for (frame in 0 until buffer.size / 2) {
            val mod = sin(2.0 * PI * lfoPhase)
            lfoPhase += lfoRate; if (lfoPhase >= 1.0) lfoPhase -= 1.0
            val fc = 500.0 + depth * 3500.0 * (mod * 0.5 + 0.5)
            val g = tan(PI * fc / sampleRate).let { (it - 1) / (it + 1) }

            var sL = buffer[frame * 2].toDouble() + fbL * feedback
            var sR = buffer[frame * 2 + 1].toDouble() + fbR * feedback
            for (s in 0 until stages) {
                val outL = g * sL + x1L[s] - g * y1L[s]
                x1L[s] = sL; y1L[s] = outL; sL = outL
                val outR = g * sR + x1R[s] - g * y1R[s]
                x1R[s] = sR; y1R[s] = outR; sR = outR
            }
            fbL = sL; fbR = sR
            buffer[frame * 2] = (buffer[frame * 2] + sL).toFloat() * 0.5f
            buffer[frame * 2 + 1] = (buffer[frame * 2 + 1] + sR).toFloat() * 0.5f
        }
    }
    override fun reset() {
        x1L.fill(0.0); x2L.fill(0.0); y1L.fill(0.0); y2L.fill(0.0)
        x1R.fill(0.0); x2R.fill(0.0); y1R.fill(0.0); y2R.fill(0.0)
        lfoPhase = 0.0; fbL = 0.0; fbR = 0.0
    }
}

// Tremolo: LFO amplitude modulation
class TremoloEffect(sampleRate: Int) : Effect(EffectType.TREMOLO, sampleRate) {
    private var lfoPhase = 0.0
    var rate: Float get() = param("rate", 4.0f); set(v) = setParam("rate", v)
    var depth: Float get() = param("depth", 0.7f); set(v) = setParam("depth", v)
    var shape: Int get() = param("shape", 0f).toInt(); set(v) = setParam("shape", v.toFloat())

    override fun process(buffer: FloatArray) {
        val dt = rate.toDouble() / sampleRate
        for (frame in 0 until buffer.size / 2) {
            val mod = when (shape) {
                1 -> if (lfoPhase < 0.5) 1f else -1f       // square
                2 -> (2f * abs(2f * lfoPhase.toFloat() - 1f) - 1f)  // triangle
                else -> sin(2.0 * PI * lfoPhase).toFloat() // sine
            }
            val gain = 1f - depth * (mod * 0.5f + 0.5f)
            buffer[frame * 2] *= gain
            buffer[frame * 2 + 1] *= gain
            lfoPhase += dt; if (lfoPhase >= 1.0) lfoPhase -= 1.0
        }
    }
    override fun reset() { lfoPhase = 0.0 }
}

// Ring Modulator: multiply by carrier sine
class RingModulatorEffect(sampleRate: Int) : Effect(EffectType.RING_MODULATOR, sampleRate) {
    private var phase = 0.0
    var frequency: Float get() = param("frequency", 440f); set(v) = setParam("frequency", v)
    var mix: Float get() = param("mix", 1.0f); set(v) = setParam("mix", v)

    override fun process(buffer: FloatArray) {
        val dt = frequency.toDouble() / sampleRate
        for (frame in 0 until buffer.size / 2) {
            val carrier = sin(2.0 * PI * phase).toFloat()
            phase += dt; if (phase >= 1.0) phase -= 1.0
            val m = mix
            buffer[frame * 2] = buffer[frame * 2] * (1f - m) + buffer[frame * 2] * carrier * m
            buffer[frame * 2 + 1] = buffer[frame * 2 + 1] * (1f - m) + buffer[frame * 2 + 1] * carrier * m
        }
    }
    override fun reset() { phase = 0.0 }
}
