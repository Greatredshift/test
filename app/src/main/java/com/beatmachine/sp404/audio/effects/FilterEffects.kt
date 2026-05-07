package com.beatmachine.sp404.audio.effects

import com.beatmachine.sp404.audio.synth.BiquadFilter
import com.beatmachine.sp404.model.EffectType
import kotlin.math.*

class LowPassFilterEffect(sampleRate: Int) : Effect(EffectType.LOW_PASS_FILTER, sampleRate) {
    private val bqL = BiquadFilter(); private val bqR = BiquadFilter()
    var cutoff: Float get() = param("cutoff", 0.5f); set(v) = setParam("cutoff", v)
    var resonance: Float get() = param("resonance", 0.3f); set(v) = setParam("resonance", v)

    override fun process(buffer: FloatArray) {
        val fc = 20.0 * (20000.0 / 20.0).pow(cutoff.toDouble())
        val q = 0.5 + resonance * 9.5
        bqL.setLPF(fc, q, sampleRate.toDouble()); bqR.setLPF(fc, q, sampleRate.toDouble())
        for (f in 0 until buffer.size / 2) {
            buffer[f * 2] = bqL.processL(buffer[f * 2].toDouble()).toFloat()
            buffer[f * 2 + 1] = bqR.processR(buffer[f * 2 + 1].toDouble()).toFloat()
        }
    }
    override fun reset() { bqL.reset(); bqR.reset() }
}

class HighPassFilterEffect(sampleRate: Int) : Effect(EffectType.HIGH_PASS_FILTER, sampleRate) {
    private val bqL = BiquadFilter(); private val bqR = BiquadFilter()
    var cutoff: Float get() = param("cutoff", 0.1f); set(v) = setParam("cutoff", v)
    var resonance: Float get() = param("resonance", 0.3f); set(v) = setParam("resonance", v)

    override fun process(buffer: FloatArray) {
        val fc = 20.0 * (20000.0 / 20.0).pow(cutoff.toDouble())
        val q = 0.5 + resonance * 9.5
        bqL.setHPF(fc, q, sampleRate.toDouble()); bqR.setHPF(fc, q, sampleRate.toDouble())
        for (f in 0 until buffer.size / 2) {
            buffer[f * 2] = bqL.processL(buffer[f * 2].toDouble()).toFloat()
            buffer[f * 2 + 1] = bqR.processR(buffer[f * 2 + 1].toDouble()).toFloat()
        }
    }
    override fun reset() { bqL.reset(); bqR.reset() }
}

class BandPassFilterEffect(sampleRate: Int) : Effect(EffectType.BAND_PASS_FILTER, sampleRate) {
    private val bqL = BiquadFilter(); private val bqR = BiquadFilter()
    var center: Float get() = param("center", 0.4f); set(v) = setParam("center", v)
    var bandwidth: Float get() = param("bandwidth", 0.5f); set(v) = setParam("bandwidth", v)

    override fun process(buffer: FloatArray) {
        val fc = 20.0 * (20000.0 / 20.0).pow(center.toDouble())
        val q = 0.5 + (1.0 - bandwidth) * 9.0
        bqL.setBPF(fc, q, sampleRate.toDouble()); bqR.setBPF(fc, q, sampleRate.toDouble())
        for (f in 0 until buffer.size / 2) {
            buffer[f * 2] = bqL.processL(buffer[f * 2].toDouble()).toFloat()
            buffer[f * 2 + 1] = bqR.processR(buffer[f * 2 + 1].toDouble()).toFloat()
        }
    }
    override fun reset() { bqL.reset(); bqR.reset() }
}

// Isolator: 3-band EQ kill (low/mid/high independently controlled)
class IsolatorEffect(sampleRate: Int) : Effect(EffectType.ISOLATOR, sampleRate) {
    private val lpL = BiquadFilter(); private val lpR = BiquadFilter()
    private val bpL = BiquadFilter(); private val bpR = BiquadFilter()
    private val hpL = BiquadFilter(); private val hpR = BiquadFilter()

    var lowGain: Float get() = param("low", 1.0f); set(v) = setParam("low", v)
    var midGain: Float get() = param("mid", 1.0f); set(v) = setParam("mid", v)
    var highGain: Float get() = param("high", 1.0f); set(v) = setParam("high", v)

    init {
        val sr = sampleRate.toDouble()
        lpL.setLPF(250.0, 0.707, sr); lpR.setLPF(250.0, 0.707, sr)
        bpL.setBPF(2000.0, 0.707, sr); bpR.setBPF(2000.0, 0.707, sr)
        hpL.setHPF(8000.0, 0.707, sr); hpR.setHPF(8000.0, 0.707, sr)
    }

    override fun process(buffer: FloatArray) {
        for (f in 0 until buffer.size / 2) {
            val inL = buffer[f * 2].toDouble()
            val inR = buffer[f * 2 + 1].toDouble()
            buffer[f * 2] = (lpL.processL(inL) * lowGain + bpL.processL(inL) * midGain + hpL.processL(inL) * highGain).toFloat()
            buffer[f * 2 + 1] = (lpR.processR(inR) * lowGain + bpR.processR(inR) * midGain + hpR.processR(inR) * highGain).toFloat()
        }
    }
    override fun reset() { lpL.reset(); lpR.reset(); bpL.reset(); bpR.reset(); hpL.reset(); hpR.reset() }
}

// 3-Band Parametric EQ
class EQEffect(sampleRate: Int) : Effect(EffectType.EQ_THREE_BAND, sampleRate) {
    private val loShelfL = BiquadFilter(); private val loShelfR = BiquadFilter()
    private val midPeakL = BiquadFilter(); private val midPeakR = BiquadFilter()
    private val hiShelfL = BiquadFilter(); private val hiShelfR = BiquadFilter()
    private var dirty = true

    var lowGainDb: Float get() = param("lowGain", 0f); set(v) { setParam("lowGain", v); dirty = true }
    var lowFreq: Float get() = param("lowFreq", 120f); set(v) { setParam("lowFreq", v); dirty = true }
    var midGainDb: Float get() = param("midGain", 0f); set(v) { setParam("midGain", v); dirty = true }
    var midFreq: Float get() = param("midFreq", 1000f); set(v) { setParam("midFreq", v); dirty = true }
    var midQ: Float get() = param("midQ", 1.0f); set(v) { setParam("midQ", v); dirty = true }
    var highGainDb: Float get() = param("highGain", 0f); set(v) { setParam("highGain", v); dirty = true }
    var highFreq: Float get() = param("highFreq", 8000f); set(v) { setParam("highFreq", v); dirty = true }

    private fun updateCoeffs() {
        val sr = sampleRate.toDouble()
        loShelfL.setLowShelf(lowFreq.toDouble(), lowGainDb.toDouble(), sr)
        loShelfR.setLowShelf(lowFreq.toDouble(), lowGainDb.toDouble(), sr)
        midPeakL.setPeakEQ(midFreq.toDouble(), midQ.toDouble(), midGainDb.toDouble(), sr)
        midPeakR.setPeakEQ(midFreq.toDouble(), midQ.toDouble(), midGainDb.toDouble(), sr)
        hiShelfL.setHighShelf(highFreq.toDouble(), highGainDb.toDouble(), sr)
        hiShelfR.setHighShelf(highFreq.toDouble(), highGainDb.toDouble(), sr)
        dirty = false
    }

    override fun process(buffer: FloatArray) {
        if (dirty) updateCoeffs()
        for (f in 0 until buffer.size / 2) {
            buffer[f * 2] = hiShelfL.processL(midPeakL.processL(loShelfL.processL(buffer[f * 2].toDouble()))).toFloat()
            buffer[f * 2 + 1] = hiShelfR.processR(midPeakR.processR(loShelfR.processR(buffer[f * 2 + 1].toDouble()))).toFloat()
        }
    }
    override fun reset() { loShelfL.reset(); loShelfR.reset(); midPeakL.reset(); midPeakR.reset(); hiShelfL.reset(); hiShelfR.reset() }
}

// Center Canceller (vocal remover) using mid-side processing
class CenterCancellerEffect(sampleRate: Int) : Effect(EffectType.CENTER_CANCELLER, sampleRate) {
    var amount: Float get() = param("amount", 1.0f); set(v) = setParam("amount", v)
    var lowCut: Float get() = param("lowCut", 200f); set(v) = setParam("lowCut", v)
    var highCut: Float get() = param("highCut", 5000f); set(v) = setParam("highCut", v)

    override fun process(buffer: FloatArray) {
        for (f in 0 until buffer.size / 2) {
            val l = buffer[f * 2]; val r = buffer[f * 2 + 1]
            val mid = (l + r) * 0.5f * amount
            buffer[f * 2] = l - mid
            buffer[f * 2 + 1] = r - mid
        }
    }
}
