package com.beatmachine.sp404.audio.synth

import kotlin.math.*

enum class FilterMode { LPF12, LPF24, HPF12, HPF24, BPF, NOTCH }

class BiquadFilter {
    private var b0 = 1.0; private var b1 = 0.0; private var b2 = 0.0
    private var a1 = 0.0; private var a2 = 0.0
    private var x1L = 0.0; private var x2L = 0.0; private var y1L = 0.0; private var y2L = 0.0
    private var x1R = 0.0; private var x2R = 0.0; private var y1R = 0.0; private var y2R = 0.0

    fun setLPF(fc: Double, q: Double, sr: Double) {
        val w0 = 2.0 * PI * fc / sr
        val alpha = sin(w0) / (2.0 * q.coerceAtLeast(0.01))
        val cosw0 = cos(w0)
        val norm = 1.0 + alpha
        b0 = (1.0 - cosw0) / 2.0 / norm; b1 = (1.0 - cosw0) / norm; b2 = b0
        a1 = -2.0 * cosw0 / norm; a2 = (1.0 - alpha) / norm
    }

    fun setHPF(fc: Double, q: Double, sr: Double) {
        val w0 = 2.0 * PI * fc / sr
        val alpha = sin(w0) / (2.0 * q.coerceAtLeast(0.01))
        val cosw0 = cos(w0)
        val norm = 1.0 + alpha
        b0 = (1.0 + cosw0) / 2.0 / norm; b1 = -(1.0 + cosw0) / norm; b2 = b0
        a1 = -2.0 * cosw0 / norm; a2 = (1.0 - alpha) / norm
    }

    fun setBPF(fc: Double, q: Double, sr: Double) {
        val w0 = 2.0 * PI * fc / sr
        val alpha = sin(w0) / (2.0 * q.coerceAtLeast(0.01))
        val cosw0 = cos(w0)
        val norm = 1.0 + alpha
        b0 = q * alpha / norm; b1 = 0.0; b2 = -b0
        a1 = -2.0 * cosw0 / norm; a2 = (1.0 - alpha) / norm
    }

    fun setNotch(fc: Double, q: Double, sr: Double) {
        val w0 = 2.0 * PI * fc / sr
        val alpha = sin(w0) / (2.0 * q.coerceAtLeast(0.01))
        val cosw0 = cos(w0)
        val norm = 1.0 + alpha
        b0 = 1.0 / norm; b1 = -2.0 * cosw0 / norm; b2 = b0
        a1 = b1; a2 = (1.0 - alpha) / norm
    }

    fun setPeakEQ(fc: Double, q: Double, gainDb: Double, sr: Double) {
        val A = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * fc / sr
        val alpha = sin(w0) / (2.0 * q)
        val cosw0 = cos(w0)
        val norm = 1.0 + alpha / A
        b0 = (1.0 + alpha * A) / norm; b1 = -2.0 * cosw0 / norm; b2 = (1.0 - alpha * A) / norm
        a1 = b1; a2 = (1.0 - alpha / A) / norm
    }

    fun setLowShelf(fc: Double, gainDb: Double, sr: Double) {
        val A = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * fc / sr
        val cosw0 = cos(w0); val sinw0 = sin(w0)
        val alpha = sinw0 / 2.0 * sqrt(2.0)
        val sqA2a = 2.0 * sqrt(A) * alpha
        val norm = (A + 1) + (A - 1) * cosw0 + sqA2a
        b0 = A * ((A + 1) - (A - 1) * cosw0 + sqA2a) / norm
        b1 = 2.0 * A * ((A - 1) - (A + 1) * cosw0) / norm
        b2 = A * ((A + 1) - (A - 1) * cosw0 - sqA2a) / norm
        a1 = -2.0 * ((A - 1) + (A + 1) * cosw0) / norm
        a2 = ((A + 1) + (A - 1) * cosw0 - sqA2a) / norm
    }

    fun setHighShelf(fc: Double, gainDb: Double, sr: Double) {
        val A = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * fc / sr
        val cosw0 = cos(w0); val sinw0 = sin(w0)
        val alpha = sinw0 / 2.0 * sqrt(2.0)
        val sqA2a = 2.0 * sqrt(A) * alpha
        val norm = (A + 1) - (A - 1) * cosw0 + sqA2a
        b0 = A * ((A + 1) + (A - 1) * cosw0 + sqA2a) / norm
        b1 = -2.0 * A * ((A - 1) + (A + 1) * cosw0) / norm
        b2 = A * ((A + 1) + (A - 1) * cosw0 - sqA2a) / norm
        a1 = 2.0 * ((A - 1) - (A + 1) * cosw0) / norm
        a2 = ((A + 1) - (A - 1) * cosw0 - sqA2a) / norm
    }

    fun processL(x: Double): Double {
        val y = b0 * x + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
        x2L = x1L; x1L = x; y2L = y1L; y1L = y
        return y
    }

    fun processR(x: Double): Double {
        val y = b0 * x + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
        x2R = x1R; x1R = x; y2R = y1R; y1R = y
        return y
    }

    fun reset() { x1L = 0.0; x2L = 0.0; y1L = 0.0; y2L = 0.0; x1R = 0.0; x2R = 0.0; y1R = 0.0; y2R = 0.0 }
}

class SynthFilter(private val sampleRate: Int) {
    var mode: FilterMode = FilterMode.LPF12
    var cutoff: Float = 1.0f     // 0..1 (maps to 20Hz..20kHz)
    var resonance: Float = 0.0f  // 0..1
    var envAmount: Float = 0.5f  // -1..1
    var lfoAmount: Float = 0.0f

    private val bq1 = BiquadFilter()
    private val bq2 = BiquadFilter()

    private fun fc(env: Float, lfo: Float): Double {
        val modulated = (cutoff + envAmount * env + lfoAmount * lfo).coerceIn(0f, 1f)
        return 20.0 * (20000.0 / 20.0).pow(modulated.toDouble())
    }

    private fun q(): Double = (0.707 + resonance * 9.0).coerceAtLeast(0.01)

    fun process(bufferL: Double, bufferR: Double, env: Float, lfo: Float): Pair<Double, Double> {
        val fc = fc(env, lfo)
        val q = q()
        val sr = sampleRate.toDouble()
        when (mode) {
            FilterMode.LPF12, FilterMode.LPF24 -> { bq1.setLPF(fc, q, sr); bq2.setLPF(fc, q, sr) }
            FilterMode.HPF12, FilterMode.HPF24 -> { bq1.setHPF(fc, q, sr); bq2.setHPF(fc, q, sr) }
            FilterMode.BPF -> { bq1.setBPF(fc, q, sr); bq2.setBPF(fc, q, sr) }
            FilterMode.NOTCH -> { bq1.setNotch(fc, q, sr); bq2.setNotch(fc, q, sr) }
        }
        val outL1 = bq1.processL(bufferL)
        val outR1 = bq1.processR(bufferR)
        return if (mode == FilterMode.LPF24 || mode == FilterMode.HPF24) {
            bq2.processL(outL1) to bq2.processR(outR1)
        } else {
            outL1 to outR1
        }
    }

    fun reset() { bq1.reset(); bq2.reset() }
}
