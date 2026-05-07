package com.beatmachine.sp404.audio.synth

import kotlin.math.*

class Voice(private val sampleRate: Int) {
    val osc1 = Oscillator(sampleRate).apply { waveShape = WaveShape.SAWTOOTH }
    val osc2 = Oscillator(sampleRate).apply { waveShape = WaveShape.SQUARE; detuneCents = 7.0 }
    val osc3 = Oscillator(sampleRate).apply { waveShape = WaveShape.TRIANGLE; volume = 0f }
    val subOsc = Oscillator(sampleRate).apply { waveShape = WaveShape.SQUARE; volume = 0f }

    val ampEnv = Envelope(sampleRate)
    val filterEnv = Envelope(sampleRate)
    val filter = SynthFilter(sampleRate)

    var midiNote: Int = 60
    var velocity: Float = 1.0f
    var isActive: Boolean = false
    var portamentoTime: Float = 0f     // seconds; 0 = instant
    var portamentoEnabled: Boolean = false

    private var targetFreq = 440.0
    private var currentFreq = 440.0
    private val portamentoRate get() = if (portamentoTime <= 0f) Double.MAX_VALUE
        else exp(ln(targetFreq / currentFreq.coerceAtLeast(1.0)) / (portamentoTime * sampleRate))

    fun noteOn(note: Int, vel: Float, previousFreq: Double = -1.0) {
        midiNote = note
        velocity = vel
        targetFreq = 440.0 * 2.0.pow((note - 69) / 12.0)
        currentFreq = if (portamentoEnabled && previousFreq > 0) previousFreq else targetFreq
        osc1.frequency = currentFreq
        osc2.frequency = currentFreq
        osc3.frequency = currentFreq
        subOsc.frequency = currentFreq / 2.0
        osc1.reset(); osc2.reset(); osc3.reset(); subOsc.reset()
        ampEnv.noteOn()
        filterEnv.noteOn()
        isActive = true
    }

    fun noteOff() {
        ampEnv.noteOff()
        filterEnv.noteOff()
    }

    fun render(buffer: FloatArray, lfo1: Float, lfo2: Float) {
        if (!isActive) return
        val numFrames = buffer.size / 2

        for (f in 0 until numFrames) {
            // Portamento
            if (portamentoEnabled && currentFreq != targetFreq) {
                currentFreq = if (currentFreq < targetFreq)
                    (currentFreq * portamentoRate).coerceAtMost(targetFreq)
                else
                    (currentFreq / portamentoRate).coerceAtLeast(targetFreq)
            }

            // LFO pitch modulation
            val pitchMod = 2.0.pow(lfo1.toDouble() * 0.05)
            val freq = currentFreq * pitchMod
            osc1.frequency = freq
            osc2.frequency = freq
            osc3.frequency = freq
            subOsc.frequency = freq / 2.0

            val ampLevel = ampEnv.process() * velocity
            val filterLevel = filterEnv.process()

            if (ampLevel == 0f && !ampEnv.isActive) {
                isActive = false
                break
            }

            // Accumulate oscillators for this single frame (mono mix)
            var s = 0f
            s += generateOscSample(osc1)
            s += generateOscSample(osc2)
            s += generateOscSample(osc3)
            s += generateOscSample(subOsc)

            // Filter
            val (fl, fr) = filter.process(s.toDouble(), s.toDouble(), filterLevel, lfo2)

            val outL = (fl * ampLevel).toFloat()
            val outR = (fr * ampLevel).toFloat()
            buffer[f * 2] += outL
            buffer[f * 2 + 1] += outR
        }
    }

    private fun generateOscSample(osc: Oscillator): Float {
        if (!osc.enabled || osc.volume == 0f) return 0f
        val buf = FloatArray(2)
        osc.render(buf, 0, 1)
        return buf[0]
    }
}

