package com.beatmachine.sp404.audio.effects

import com.beatmachine.sp404.model.EffectType
import kotlin.math.*

class DelayEffect(type: EffectType, sampleRate: Int) : Effect(type, sampleRate) {
    private val maxDelaySamples = (sampleRate * 2.5).toInt()
    private val bufL = FloatArray(maxDelaySamples)
    private val bufR = FloatArray(maxDelaySamples)
    private var writeIdx = 0
    private var wowPhase = 0.0    // tape wow/flutter

    var time: Float
        get() = param("time", 0.4f)
        set(v) = setParam("time", v)
    var feedback: Float
        get() = param("feedback", 0.4f)
        set(v) = setParam("feedback", v)
    var highCut: Float
        get() = param("highCut", 0.8f)
        set(v) = setParam("highCut", v)
    private var lpStateL = 0f; private var lpStateR = 0f

    override fun process(buffer: FloatArray) {
        val delaySamples = (time * sampleRate).toInt().coerceIn(1, maxDelaySamples - 1)
        val fb = feedback.coerceIn(0f, 0.95f)
        val lpCoeff = highCut.coerceIn(0.01f, 0.99f)

        for (frame in 0 until buffer.size / 2) {
            val inL = buffer[frame * 2]
            val inR = buffer[frame * 2 + 1]

            // Tape wow/flutter modulation
            val wowOffset = if (type == EffectType.DELAY_TAPE) {
                wowPhase += 2.0 * PI * 0.7 / sampleRate
                (sin(wowPhase) * 30.0).toInt()
            } else 0

            val readIdx = Math.floorMod(writeIdx - delaySamples + wowOffset, maxDelaySamples)

            val delayedL = bufL[readIdx]
            val delayedR = when (type) {
                EffectType.DELAY_PANNING -> bufR[Math.floorMod(readIdx - delaySamples / 2, maxDelaySamples)]
                else -> bufR[readIdx]
            }

            // LP filter on feedback (tape warmth / high-cut)
            lpStateL = lpStateL * (1f - lpCoeff) + delayedL * lpCoeff
            lpStateR = lpStateR * (1f - lpCoeff) + delayedR * lpCoeff

            bufL[writeIdx] = inL + lpStateL * fb
            bufR[writeIdx] = inR + lpStateR * fb
            writeIdx = (writeIdx + 1) % maxDelaySamples

            buffer[frame * 2] = inL + delayedL
            buffer[frame * 2 + 1] = inR + delayedR
        }
    }

    override fun reset() { bufL.fill(0f); bufR.fill(0f); writeIdx = 0; wowPhase = 0.0 }
}
