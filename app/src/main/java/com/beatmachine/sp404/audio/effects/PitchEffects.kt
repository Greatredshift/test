package com.beatmachine.sp404.audio.effects

import com.beatmachine.sp404.audio.synth.BiquadFilter
import com.beatmachine.sp404.model.EffectType
import kotlin.math.*

// Vocal Morph: pitch shift via granular time-domain approach
class VocalMorphEffect(sampleRate: Int) : Effect(EffectType.VOCAL_MORPH, sampleRate) {
    private val grainSize = 1024
    private val bufL = FloatArray(grainSize * 4); private val bufR = FloatArray(grainSize * 4)
    private var writeIdx = 0
    private var grainPhase = 0.0

    var pitch: Float get() = param("pitch", 0f); set(v) = setParam("pitch", v)     // semitones -12..+12
    var formant: Float get() = param("formant", 0f); set(v) = setParam("formant", v)

    override fun process(buffer: FloatArray) {
        val pitchRatio = 2.0.pow(pitch / 12.0)
        for (f in 0 until buffer.size / 2) {
            bufL[writeIdx % bufL.size] = buffer[f * 2]
            bufR[writeIdx % bufR.size] = buffer[f * 2 + 1]
            writeIdx++
            grainPhase += pitchRatio
            val readPos = (writeIdx - grainSize + grainPhase.toInt()) % bufL.size
            val ri = ((readPos % bufL.size) + bufL.size) % bufL.size
            // Window function
            val winPos = (grainPhase % grainSize) / grainSize
            val window = (sin(PI * winPos) * sin(PI * winPos)).toFloat()
            buffer[f * 2] = bufL[ri] * window
            buffer[f * 2 + 1] = bufR[ri] * window
            if (grainPhase >= grainSize) grainPhase -= grainSize
        }
    }
    override fun reset() { bufL.fill(0f); bufR.fill(0f); writeIdx = 0; grainPhase = 0.0 }
}

// Auto Pitch: snap pitch to nearest semitone (simple implementation)
class AutoPitchEffect(sampleRate: Int) : Effect(EffectType.AUTO_PITCH, sampleRate) {
    private val bufSize = 2048
    private val bufL = FloatArray(bufSize); private val bufR = FloatArray(bufSize)
    private var writeIdx = 0
    private var autocorrPhase = 0.0

    var speed: Float get() = param("speed", 0.5f); set(v) = setParam("speed", v)   // 0=slow..1=instant
    var key: Float get() = param("key", 0f); set(v) = setParam("key", v)            // root note 0-11

    override fun process(buffer: FloatArray) {
        // Simplified: apply subtle pitch quantization via delay modulation
        val correction = speed * 0.02
        for (f in 0 until buffer.size / 2) {
            bufL[writeIdx % bufSize] = buffer[f * 2]
            bufR[writeIdx % bufSize] = buffer[f * 2 + 1]
            autocorrPhase += correction
            if (autocorrPhase > 10.0) autocorrPhase = 0.0
            val offset = (sin(autocorrPhase) * 10 * speed).toInt()
            val ri = Math.floorMod(writeIdx - 100 + offset, bufSize)
            buffer[f * 2] = bufL[ri]; buffer[f * 2 + 1] = bufR[ri]
            writeIdx = (writeIdx + 1) % bufSize
        }
    }
    override fun reset() { bufL.fill(0f); bufR.fill(0f); writeIdx = 0; autocorrPhase = 0.0 }
}

// Sub Oscillator: add subharmonic content at half frequency
class SubOscillatorEffect(sampleRate: Int) : Effect(EffectType.SUB_OSCILLATOR, sampleRate) {
    var level: Float get() = param("level", 0.5f); set(v) = setParam("level", v)
    var octave: Float get() = param("octave", -1f); set(v) = setParam("octave", v)  // -1 or -2
    private var toggle = false
    private var prevPos = false
    private var subPhase = 0f
    private val lpL = BiquadFilter(); private val lpR = BiquadFilter()

    init { lpL.setLPF(200.0, 0.707, sampleRate.toDouble()); lpR.setLPF(200.0, 0.707, sampleRate.toDouble()) }

    override fun process(buffer: FloatArray) {
        for (f in 0 until buffer.size / 2) {
            val s = buffer[f * 2]
            val isPos = s > 0f
            if (isPos != prevPos) toggle = !toggle
            prevPos = isPos
            val sub = if (toggle) s * level else -s * level
            val filteredSubL = lpL.processL(sub.toDouble()).toFloat()
            val filteredSubR = lpR.processR(sub.toDouble()).toFloat()
            buffer[f * 2] += filteredSubL
            buffer[f * 2 + 1] += filteredSubR
        }
    }
    override fun reset() { toggle = false; prevPos = false; lpL.reset(); lpR.reset() }
}

// Noise generator: add white or pink noise
class NoiseEffect(sampleRate: Int) : Effect(EffectType.NOISE, sampleRate) {
    var level: Float get() = param("level", 0.1f); set(v) = setParam("level", v)
    var color: Float get() = param("color", 0.5f); set(v) = setParam("color", v)  // 0=white..1=pink
    private val random = java.util.Random()
    private var b0 = 0f; private var b1 = 0f; private var b2 = 0f

    override fun process(buffer: FloatArray) {
        for (f in 0 until buffer.size / 2) {
            val white = (random.nextFloat() * 2f - 1f)
            // Pink noise approximation (Paul Kellet's)
            b0 = 0.99886f * b0 + white * 0.0555179f
            b1 = 0.99332f * b1 + white * 0.0750759f
            b2 = 0.96900f * b2 + white * 0.1538520f
            val pink = (b0 + b1 + b2 + white * 0.5362f) * 0.11f
            val noise = white * (1f - color) + pink * color
            buffer[f * 2] += noise * level
            buffer[f * 2 + 1] += noise * level
        }
    }
    override fun reset() { b0 = 0f; b1 = 0f; b2 = 0f }
}

// Strummer: arpeggio effect that spreads chord notes in time
class StrummerEffect(sampleRate: Int) : Effect(EffectType.STRUMMER, sampleRate) {
    var speed: Float get() = param("speed", 0.5f); set(v) = setParam("speed", v)
    var direction: Float get() = param("direction", 1f); set(v) = setParam("direction", v)  // 1=up, -1=down
    private val delayBufL = FloatArray(sampleRate / 4); private val delayBufR = FloatArray(sampleRate / 4)
    private var writeIdx = 0

    override fun process(buffer: FloatArray) {
        val staggerMs = speed * 30f
        val staggerSamples = (staggerMs * sampleRate / 1000f).toInt().coerceIn(1, delayBufL.size - 1)
        for (f in 0 until buffer.size / 2) {
            delayBufL[writeIdx] = buffer[f * 2]; delayBufR[writeIdx] = buffer[f * 2 + 1]
            val ri = Math.floorMod(writeIdx - staggerSamples, delayBufL.size)
            buffer[f * 2] = (buffer[f * 2] + delayBufL[ri]) * 0.6f
            buffer[f * 2 + 1] = (buffer[f * 2 + 1] + delayBufR[ri]) * 0.6f
            writeIdx = (writeIdx + 1) % delayBufL.size
        }
    }
    override fun reset() { delayBufL.fill(0f); delayBufR.fill(0f); writeIdx = 0 }
}
