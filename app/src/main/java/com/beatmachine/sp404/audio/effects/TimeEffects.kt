package com.beatmachine.sp404.audio.effects

import com.beatmachine.sp404.model.EffectType
import kotlin.math.*

// Vinyl Simulator: wow/flutter LFO + crackle noise + HPF presence
class VinylSimulatorEffect(sampleRate: Int) : Effect(EffectType.VINYL_SIMULATOR, sampleRate) {
    private val maxDelay = (sampleRate * 0.05).toInt()
    private val bufL = FloatArray(maxDelay); private val bufR = FloatArray(maxDelay)
    private var writeIdx = 0
    private var wowPhase = 0.0; private var flutterPhase = 0.0; private var cracklePhase = 0.0
    private val random = java.util.Random()
    private var hpStateL = 0f; private var hpStateR = 0f; private var prevL = 0f; private var prevR = 0f

    var wowRate: Float get() = param("wow", 0.6f); set(v) = setParam("wow", v)
    var flutter: Float get() = param("flutter", 0.3f); set(v) = setParam("flutter", v)
    var crackle: Float get() = param("crackle", 0.2f); set(v) = setParam("crackle", v)
    var warmth: Float get() = param("warmth", 0.5f); set(v) = setParam("warmth", v)

    override fun process(buffer: FloatArray) {
        val wowDt = wowRate.toDouble() / sampleRate
        val flutterDt = 5.0 / sampleRate
        val lpCoeff = warmth * 0.95f
        for (f in 0 until buffer.size / 2) {
            wowPhase += wowDt; if (wowPhase >= 1.0) wowPhase -= 1.0
            flutterPhase += flutterDt; if (flutterPhase >= 1.0) flutterPhase -= 1.0
            val wow = sin(2.0 * PI * wowPhase) * 0.01
            val fl = sin(2.0 * PI * flutterPhase) * flutter * 0.003
            val pitchMod = wow + fl
            val delSamples = ((maxDelay * 0.3) * (1.0 + pitchMod)).toInt().coerceIn(1, maxDelay - 1)
            bufL[writeIdx] = buffer[f * 2]; bufR[writeIdx] = buffer[f * 2 + 1]
            val readIdx = Math.floorMod(writeIdx - delSamples, maxDelay)
            writeIdx = (writeIdx + 1) % maxDelay

            // LP warmth
            var outL = bufL[readIdx]; var outR = bufR[readIdx]
            hpStateL = hpStateL * lpCoeff + outL * (1f - lpCoeff)
            hpStateR = hpStateR * lpCoeff + outR * (1f - lpCoeff)
            outL = hpStateL; outR = hpStateR

            // Crackle noise
            cracklePhase += 1.0 / sampleRate
            val crackleNoise = if (random.nextFloat() < crackle * 0.002f) {
                (random.nextFloat() * 2f - 1f) * crackle * 0.3f
            } else 0f

            buffer[f * 2] = outL + crackleNoise
            buffer[f * 2 + 1] = outR + crackleNoise
        }
    }
    override fun reset() { bufL.fill(0f); bufR.fill(0f); writeIdx = 0; hpStateL = 0f; hpStateR = 0f }
}

// Scatter: records incoming audio and repeats/reverses/gates it
class ScatterEffect(sampleRate: Int) : Effect(EffectType.SCATTER, sampleRate) {
    private val bufSize = (sampleRate * 2).toInt()
    private val bufL = FloatArray(bufSize); private val bufR = FloatArray(bufSize)
    private var writeIdx = 0; private var readIdx = 0
    private var recording = false; private var playing = false

    var scatterMode: Int get() = param("mode", 0f).toInt(); set(v) = setParam("mode", v.toFloat())
    var length: Float get() = param("length", 0.25f); set(v) = setParam("length", v)
    var repeats: Float get() = param("repeats", 4f); set(v) = setParam("repeats", v)
    var trigger: Boolean = false

    private var repeatCount = 0; private var maxRepeats = 4; private var segLen = 0
    private var direction = 1

    override fun process(buffer: FloatArray) {
        segLen = (length * sampleRate).toInt().coerceIn(64, bufSize - 1)
        maxRepeats = repeats.toInt().coerceIn(1, 32)
        for (f in 0 until buffer.size / 2) {
            if (trigger) {
                recording = true; playing = false
                writeIdx = 0; readIdx = 0; repeatCount = 0
                trigger = false
                direction = if (scatterMode == 1) -1 else 1
            }
            if (recording) {
                bufL[writeIdx] = buffer[f * 2]; bufR[writeIdx] = buffer[f * 2 + 1]
                writeIdx++
                if (writeIdx >= segLen) { recording = false; playing = true; readIdx = if (direction < 0) segLen - 1 else 0 }
            }
            if (playing) {
                val ri = readIdx.coerceIn(0, segLen - 1)
                val outL = bufL[ri]; val outR = bufR[ri]
                when (scatterMode) {
                    2 -> { buffer[f * 2] = if (repeatCount % 2 == 0) outL else 0f; buffer[f * 2 + 1] = if (repeatCount % 2 == 0) outR else 0f } // gate
                    else -> { buffer[f * 2] = outL; buffer[f * 2 + 1] = outR }
                }
                readIdx += direction
                if (readIdx >= segLen || readIdx < 0) {
                    repeatCount++
                    if (repeatCount >= maxRepeats) playing = false
                    else { readIdx = if (direction < 0) segLen - 1 else 0; if (scatterMode == 3) direction = -direction }
                }
            }
        }
    }
    override fun reset() { bufL.fill(0f); bufR.fill(0f); writeIdx = 0; readIdx = 0; recording = false; playing = false }
}

// Roll: beat-synchronized repeat
class RollEffect(sampleRate: Int) : Effect(EffectType.ROLL, sampleRate) {
    private val maxDelay = (sampleRate * 2).toInt()
    private val bufL = FloatArray(maxDelay); private val bufR = FloatArray(maxDelay)
    private var writeIdx = 0

    var bpm: Float get() = param("bpm", 120f); set(v) = setParam("bpm", v)
    var division: Float get() = param("division", 4f); set(v) = setParam("division", v)  // subdivisions per beat
    var decay: Float get() = param("decay", 0.7f); set(v) = setParam("decay", v)

    override fun process(buffer: FloatArray) {
        val beatsPerSec = bpm / 60.0
        val delaySamples = (sampleRate / (beatsPerSec * division)).toInt().coerceIn(64, maxDelay - 1)
        for (f in 0 until buffer.size / 2) {
            val readIdx = Math.floorMod(writeIdx - delaySamples, maxDelay)
            val echL = bufL[readIdx] * decay; val echR = bufR[readIdx] * decay
            bufL[writeIdx] = buffer[f * 2] + echL; bufR[writeIdx] = buffer[f * 2 + 1] + echR
            buffer[f * 2] += echL; buffer[f * 2 + 1] += echR
            writeIdx = (writeIdx + 1) % maxDelay
        }
    }
    override fun reset() { bufL.fill(0f); bufR.fill(0f); writeIdx = 0 }
}

// Wave Repeat: repeat a segment of waveform
class WaveRepeatEffect(sampleRate: Int) : Effect(EffectType.WAVE_REPEAT, sampleRate) {
    private val segSize = (sampleRate * 0.1).toInt()
    private val capBufL = FloatArray(segSize); private val capBufR = FloatArray(segSize)
    private var capIdx = 0; private var readIdx = 0; private var phase = 0.0

    var size: Float get() = param("size", 0.5f); set(v) = setParam("size", v)
    var rate: Float get() = param("rate", 1.0f); set(v) = setParam("rate", v)

    override fun process(buffer: FloatArray) {
        val segLen = (size * segSize).toInt().coerceAtLeast(64)
        for (f in 0 until buffer.size / 2) {
            capBufL[capIdx % segLen] = buffer[f * 2]
            capBufR[capIdx % segLen] = buffer[f * 2 + 1]
            capIdx = (capIdx + 1) % segLen
            phase += rate.toDouble() / sampleRate * 10.0
            if (phase >= 1.0) phase -= 1.0
            val ri = (phase * segLen).toInt().coerceIn(0, segLen - 1)
            buffer[f * 2] = capBufL[ri]; buffer[f * 2 + 1] = capBufR[ri]
        }
    }
    override fun reset() { capBufL.fill(0f); capBufR.fill(0f); capIdx = 0; readIdx = 0; phase = 0.0 }
}
