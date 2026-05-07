package com.beatmachine.sp404.audio.effects

import com.beatmachine.sp404.model.EffectType
import kotlin.math.*

// Freeverb (Schroeder/Moorer) reverb
class ReverbEffect(type: EffectType, sampleRate: Int) : Effect(type, sampleRate) {

    // Comb filter delays at 44100Hz (scaled for other rates)
    private val combTuningL = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
    private val combTuningR = intArrayOf(1139, 1211, 1300, 1379, 1445, 1514, 1580, 1640)
    private val allpassTuning = intArrayOf(556, 441, 341, 225)

    private val scale = sampleRate.toDouble() / 44100.0
    private val numCombs = 8
    private val numAllpass = 4

    private val combBufL = Array(numCombs) { FloatArray((combTuningL[it] * scale).toInt() + 1) }
    private val combBufR = Array(numCombs) { FloatArray((combTuningR[it] * scale).toInt() + 1) }
    private val combIdxL = IntArray(numCombs)
    private val combIdxR = IntArray(numCombs)
    private val combFilterL = FloatArray(numCombs)
    private val combFilterR = FloatArray(numCombs)

    private val apBufL = Array(numAllpass) { FloatArray((allpassTuning[it] * scale).toInt() + 1) }
    private val apBufR = Array(numAllpass) { FloatArray((allpassTuning[it] * scale).toInt() + 1) }
    private val apIdxL = IntArray(numAllpass)
    private val apIdxR = IntArray(numAllpass)

    // Parameters
    var roomSize: Float
        get() = param("roomSize", 0.5f)
        set(v) = setParam("roomSize", v)
    var damping: Float
        get() = param("damping", 0.5f)
        set(v) = setParam("damping", v)
    var width: Float
        get() = param("width", 1.0f)
        set(v) = setParam("width", v)

    init {
        applyType(type)
    }

    private fun applyType(t: EffectType) {
        when (t) {
            EffectType.REVERB_ROOM -> { roomSize = 0.5f; damping = 0.5f }
            EffectType.REVERB_HALL -> { roomSize = 0.75f; damping = 0.3f }
            EffectType.REVERB_CATHEDRAL -> { roomSize = 0.92f; damping = 0.1f }
            EffectType.REVERB_MODULATE -> { roomSize = 0.7f; damping = 0.4f }
            else -> {}
        }
    }

    override fun process(buffer: FloatArray) {
        val feedback = roomSize * 0.28f + 0.7f
        val damp1 = damping * 0.4f
        val damp2 = 1f - damp1
        val wet1 = (width / 2f + 0.5f)
        val wet2 = (width / 2f - 0.5f).let { if (it < 0f) 0f else it }

        for (frame in 0 until buffer.size / 2) {
            val inL = buffer[frame * 2]
            val inR = buffer[frame * 2 + 1]
            val mono = (inL + inR) * 0.015f

            var outL = 0f; var outR = 0f

            // Comb filters
            for (c in 0 until numCombs) {
                val outCL = combBufL[c][combIdxL[c]]
                combFilterL[c] = outCL * damp2 + combFilterL[c] * damp1
                combBufL[c][combIdxL[c]] = mono + feedback * combFilterL[c]
                combIdxL[c] = (combIdxL[c] + 1) % combBufL[c].size
                outL += outCL

                val outCR = combBufR[c][combIdxR[c]]
                combFilterR[c] = outCR * damp2 + combFilterR[c] * damp1
                combBufR[c][combIdxR[c]] = mono + feedback * combFilterR[c]
                combIdxR[c] = (combIdxR[c] + 1) % combBufR[c].size
                outR += outCR
            }

            // All-pass filters
            for (a in 0 until numAllpass) {
                val bufOutL = apBufL[a][apIdxL[a]]
                apBufL[a][apIdxL[a]] = outL + 0.5f * bufOutL
                outL = bufOutL - outL
                apIdxL[a] = (apIdxL[a] + 1) % apBufL[a].size

                val bufOutR = apBufR[a][apIdxR[a]]
                apBufR[a][apIdxR[a]] = outR + 0.5f * bufOutR
                outR = bufOutR - outR
                apIdxR[a] = (apIdxR[a] + 1) % apBufR[a].size
            }

            // Modulate mode: slight chorus on reverb tail
            if (type == EffectType.REVERB_MODULATE) {
                val mod = sin(frame * 0.01).toFloat() * 0.001f
                outL *= (1f + mod); outR *= (1f - mod)
            }

            buffer[frame * 2] = outL * wet1 + outR * wet2
            buffer[frame * 2 + 1] = outR * wet1 + outL * wet2
        }
    }

    override fun reset() {
        for (c in 0 until numCombs) {
            combBufL[c].fill(0f); combBufR[c].fill(0f)
            combIdxL[c] = 0; combIdxR[c] = 0
            combFilterL[c] = 0f; combFilterR[c] = 0f
        }
        for (a in 0 until numAllpass) {
            apBufL[a].fill(0f); apBufR[a].fill(0f)
            apIdxL[a] = 0; apIdxR[a] = 0
        }
    }
}
