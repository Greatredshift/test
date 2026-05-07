package com.beatmachine.sp404.audio

import kotlin.math.floor

class SamplePlayer(
    val padIndex: Int,
    val bankId: Int,
    private val data: FloatArray,
    private val sampleRate: Int,
    private val engineSampleRate: Int
) {
    var volume: Float = 1.0f
    var pan: Float = 0.0f          // -1..+1
    var pitchSemitones: Float = 0f
    var startPoint: Float = 0f
    var endPoint: Float = 1f
    var isLooping: Boolean = false
    var loopStart: Float = 0f
    var loopEnd: Float = 1f

    private val isStereo = data.size % 2 == 0 && data.size > 1024
    private val frameCount = if (isStereo) data.size / 2 else data.size

    private var playheadFrames = 0.0
    private var _isPlaying = false
    val isPlaying: Boolean get() = _isPlaying

    private val pitchRatio get(): Double {
        val srRatio = sampleRate.toDouble() / engineSampleRate
        val pitchFactor = Math.pow(2.0, pitchSemitones.toDouble() / 12.0)
        return srRatio * pitchFactor
    }

    fun play() {
        playheadFrames = startPoint * frameCount
        _isPlaying = true
    }

    fun stop() {
        _isPlaying = false
    }

    fun render(buffer: FloatArray) {
        if (!_isPlaying) return
        val numFrames = buffer.size / 2
        val ratio = pitchRatio
        val startFrame = startPoint * frameCount
        val endFrame = endPoint * frameCount
        val loopStartFrame = loopStart * frameCount
        val loopEndFrame = loopEnd * frameCount
        val panL = if (pan > 0f) 1f - pan else 1f
        val panR = if (pan < 0f) 1f + pan else 1f

        for (f in 0 until numFrames) {
            if (!_isPlaying) break
            val i = floor(playheadFrames).toInt()
            val frac = (playheadFrames - floor(playheadFrames)).toFloat()
            val sampleL = interpolate(i, 0, frac, isStereo)
            val sampleR = if (isStereo) interpolate(i, 1, frac, true) else sampleL

            buffer[f * 2] += sampleL * volume * panL
            buffer[f * 2 + 1] += sampleR * volume * panR

            playheadFrames += ratio
            if (playheadFrames >= endFrame) {
                if (isLooping) {
                    playheadFrames = loopStartFrame + (playheadFrames - loopEndFrame)
                } else {
                    _isPlaying = false
                }
            }
            if (isLooping && playheadFrames >= loopEndFrame) {
                playheadFrames = loopStartFrame
            }
        }
    }

    private fun interpolate(frame: Int, channel: Int, frac: Float, stereo: Boolean): Float {
        val ch = if (stereo) 2 else 1
        val idx0 = (frame * ch + channel).coerceIn(0, data.size - 1)
        val idx1 = ((frame + 1) * ch + channel).coerceIn(0, data.size - 1)
        return data[idx0] * (1f - frac) + data[idx1] * frac
    }
}
