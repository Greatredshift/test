package com.beatmachine.sp404.audio.effects

import com.beatmachine.sp404.audio.synth.BiquadFilter
import com.beatmachine.sp404.model.EffectType
import kotlin.math.*

class OverdriveEffect(sampleRate: Int) : Effect(EffectType.OVERDRIVE, sampleRate) {
    var drive: Float get() = param("drive", 0.5f); set(v) = setParam("drive", v)
    var tone: Float get() = param("tone", 0.5f); set(v) = setParam("tone", v)
    var level: Float get() = param("level", 0.8f); set(v) = setParam("level", v)
    private val toneFilterL = BiquadFilter(); private val toneFilterR = BiquadFilter()

    override fun process(buffer: FloatArray) {
        val gain = 1f + drive * 40f
        val fc = 500.0 + tone * 7000.0
        toneFilterL.setLPF(fc, 0.707, sampleRate.toDouble())
        toneFilterR.setLPF(fc, 0.707, sampleRate.toDouble())
        for (f in 0 until buffer.size / 2) {
            var l = buffer[f * 2] * gain; var r = buffer[f * 2 + 1] * gain
            // Soft clip (cubic)
            l = l / (1f + abs(l))
            r = r / (1f + abs(r))
            buffer[f * 2] = toneFilterL.processL(l.toDouble()).toFloat() * level
            buffer[f * 2 + 1] = toneFilterR.processR(r.toDouble()).toFloat() * level
        }
    }
    override fun reset() { toneFilterL.reset(); toneFilterR.reset() }
}

// Guitar Amp: pre-gain, tone stack, soft clip, cabinet sim
class GuitarAmpEffect(sampleRate: Int) : Effect(EffectType.GUITAR_AMP, sampleRate) {
    var gain: Float get() = param("gain", 0.5f); set(v) = setParam("gain", v)
    var bass: Float get() = param("bass", 0.5f); set(v) = setParam("bass", v)
    var mid: Float get() = param("mid", 0.5f); set(v) = setParam("mid", v)
    var treble: Float get() = param("treble", 0.5f); set(v) = setParam("treble", v)
    var presence: Float get() = param("presence", 0.5f); set(v) = setParam("presence", v)

    private val bassFilterL = BiquadFilter(); private val bassFilterR = BiquadFilter()
    private val midFilterL = BiquadFilter(); private val midFilterR = BiquadFilter()
    private val trebleFilterL = BiquadFilter(); private val trebleFilterR = BiquadFilter()
    private val cabFilterL = BiquadFilter(); private val cabFilterR = BiquadFilter()
    private var prevL = 0f; private var prevR = 0f

    override fun process(buffer: FloatArray) {
        val sr = sampleRate.toDouble()
        val bassGain = (bass - 0.5f) * 24f
        val midGain = (mid - 0.5f) * 18f
        val trebleGain = (treble - 0.5f) * 18f

        bassFilterL.setLowShelf(150.0, bassGain.toDouble(), sr)
        bassFilterR.setLowShelf(150.0, bassGain.toDouble(), sr)
        midFilterL.setPeakEQ(800.0, 0.7, midGain.toDouble(), sr)
        midFilterR.setPeakEQ(800.0, 0.7, midGain.toDouble(), sr)
        trebleFilterL.setHighShelf(4000.0, trebleGain.toDouble(), sr)
        trebleFilterR.setHighShelf(4000.0, trebleGain.toDouble(), sr)
        // Cabinet: LP + slight resonance
        cabFilterL.setLPF(4000.0, 0.8, sr); cabFilterR.setLPF(4000.0, 0.8, sr)

        val preGain = 1f + gain * 60f
        for (f in 0 until buffer.size / 2) {
            var l = buffer[f * 2] * preGain
            var r = buffer[f * 2 + 1] * preGain
            // Hard clip at ±1 then soft shape
            l = tanh(l.toDouble()).toFloat()
            r = tanh(r.toDouble()).toFloat()
            // Tone stack
            l = bassFilterL.processL(l.toDouble()).toFloat()
            l = midFilterL.processL(l.toDouble()).toFloat()
            l = trebleFilterL.processL(l.toDouble()).toFloat()
            r = bassFilterR.processR(r.toDouble()).toFloat()
            r = midFilterR.processR(r.toDouble()).toFloat()
            r = trebleFilterR.processR(r.toDouble()).toFloat()
            // Cabinet
            l = cabFilterL.processL(l.toDouble()).toFloat()
            r = cabFilterR.processR(r.toDouble()).toFloat()
            // DC block
            val dcL = l - prevL + 0.995f * (if (f > 0) buffer[(f - 1) * 2] else 0f)
            prevL = l
            buffer[f * 2] = dcL * 0.3f
            buffer[f * 2 + 1] = r * 0.3f
        }
    }
    override fun reset() {
        bassFilterL.reset(); bassFilterR.reset()
        midFilterL.reset(); midFilterR.reset()
        trebleFilterL.reset(); trebleFilterR.reset()
        cabFilterL.reset(); cabFilterR.reset()
        prevL = 0f; prevR = 0f
    }
}

// Bit Crusher: reduce bit depth and/or sample rate
class BitCrusherEffect(sampleRate: Int) : Effect(EffectType.BIT_CRUSHER, sampleRate) {
    var bits: Float get() = param("bits", 8f); set(v) = setParam("bits", v)
    var sampleRateReduction: Float get() = param("srr", 0f); set(v) = setParam("srr", v)
    private var holdL = 0f; private var holdR = 0f; private var holdCounter = 0

    override fun process(buffer: FloatArray) {
        val levels = 2f.pow(bits.coerceIn(1f, 16f))
        val holdSamples = (1 + sampleRateReduction * 15).toInt()
        for (f in 0 until buffer.size / 2) {
            if (holdCounter <= 0) {
                holdL = (floor(buffer[f * 2] * levels + 0.5f) / levels).coerceIn(-1f, 1f)
                holdR = (floor(buffer[f * 2 + 1] * levels + 0.5f) / levels).coerceIn(-1f, 1f)
                holdCounter = holdSamples
            }
            holdCounter--
            buffer[f * 2] = holdL
            buffer[f * 2 + 1] = holdR
        }
    }
    override fun reset() { holdL = 0f; holdR = 0f; holdCounter = 0 }
}

// Radio Effect: narrow bandpass + AM noise
class RadioEffect(sampleRate: Int) : Effect(EffectType.RADIO, sampleRate) {
    var tuning: Float get() = param("tuning", 0.5f); set(v) = setParam("tuning", v)
    var noise: Float get() = param("noise", 0.2f); set(v) = setParam("noise", v)
    var bandwidth: Float get() = param("bandwidth", 0.3f); set(v) = setParam("bandwidth", v)
    private val bpL = BiquadFilter(); private val bpR = BiquadFilter()
    private val random = java.util.Random()

    override fun process(buffer: FloatArray) {
        val fc = 500.0 + tuning * 4500.0
        val q = 2.0 + (1.0 - bandwidth) * 15.0
        bpL.setBPF(fc, q, sampleRate.toDouble()); bpR.setBPF(fc, q, sampleRate.toDouble())
        for (f in 0 until buffer.size / 2) {
            val n = (random.nextFloat() * 2f - 1f) * noise * 0.1f
            var l = bpL.processL(buffer[f * 2].toDouble()).toFloat() + n
            var r = bpR.processR(buffer[f * 2 + 1].toDouble()).toFloat() + n
            l = l / (1f + abs(l))   // soft saturation
            r = r / (1f + abs(r))
            buffer[f * 2] = l; buffer[f * 2 + 1] = r
        }
    }
    override fun reset() { bpL.reset(); bpR.reset() }
}

// Telephone: very narrow bandpass (300-3000Hz) + hard clip
class TelephoneEffect(sampleRate: Int) : Effect(EffectType.TELEPHONE, sampleRate) {
    var presence: Float get() = param("presence", 0.5f); set(v) = setParam("presence", v)
    var distortion: Float get() = param("distortion", 0.3f); set(v) = setParam("distortion", v)
    private val lpL = BiquadFilter(); private val lpR = BiquadFilter()
    private val hpL = BiquadFilter(); private val hpR = BiquadFilter()

    override fun process(buffer: FloatArray) {
        val sr = sampleRate.toDouble()
        lpL.setLPF(3000.0, 0.7, sr); lpR.setLPF(3000.0, 0.7, sr)
        hpL.setHPF(300.0, 0.7, sr); hpR.setHPF(300.0, 0.7, sr)
        val drive = 1f + distortion * 8f
        for (f in 0 until buffer.size / 2) {
            var l = hpL.processL(lpL.processL(buffer[f * 2].toDouble())).toFloat() * drive
            var r = hpR.processR(lpR.processR(buffer[f * 2 + 1].toDouble())).toFloat() * drive
            l = l.coerceIn(-1f, 1f); r = r.coerceIn(-1f, 1f)
            buffer[f * 2] = l * 0.6f; buffer[f * 2 + 1] = r * 0.6f
        }
    }
    override fun reset() { lpL.reset(); lpR.reset(); hpL.reset(); hpR.reset() }
}
