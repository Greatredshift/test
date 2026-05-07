package com.beatmachine.sp404.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.beatmachine.sp404.audio.effects.EffectChain
import com.beatmachine.sp404.audio.synth.Synthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class AudioEngine {
    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_FRAMES = 256
        const val BUFFER_SIZE = BUFFER_FRAMES * 2   // stereo
    }

    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    val synthesizer = Synthesizer(SAMPLE_RATE)
    val masterEffectChain = EffectChain(SAMPLE_RATE)
    val reverbSend = EffectChain(SAMPLE_RATE)
    val delaySend = EffectChain(SAMPLE_RATE)

    private val samplePlayers = CopyOnWriteArrayList<SamplePlayer>()

    private val _cpuLoad = MutableStateFlow(0f)
    val cpuLoad: StateFlow<Float> = _cpuLoad

    private val _vuLevel = MutableStateFlow(0f to 0f)
    val vuLevel: StateFlow<Pair<Float, Float>> = _vuLevel

    var masterVolume: Float = 0.8f
    var reverbReturnLevel: Float = 0.5f
    var delayReturnLevel: Float = 0.5f

    // Timing
    var bpm: Double = 120.0
    private var sampleClock: Long = 0
    var onBeat: ((step: Int) -> Unit)? = null
    var stepsPerBar: Int = 16

    fun start() {
        if (isRunning.getAndSet(true)) return

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufBytes = maxOf(minBuf, BUFFER_SIZE * Float.SIZE_BYTES * 4)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        audioTrack!!.play()

        audioThread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            val mainBuf = FloatArray(BUFFER_SIZE)
            val auxBuf = FloatArray(BUFFER_SIZE)
            val reverbBuf = FloatArray(BUFFER_SIZE)
            val delayBuf = FloatArray(BUFFER_SIZE)

            while (isRunning.get()) {
                val t0 = System.nanoTime()
                mainBuf.fill(0f)
                reverbBuf.fill(0f)
                delayBuf.fill(0f)

                // Tick sequencer timing
                tickSequencer(BUFFER_FRAMES)

                // Synth
                auxBuf.fill(0f)
                synthesizer.render(auxBuf)
                mixInto(mainBuf, auxBuf, 1f)

                // Sample players
                for (player in samplePlayers) {
                    if (player.isPlaying) {
                        auxBuf.fill(0f)
                        player.render(auxBuf)
                        mixInto(mainBuf, auxBuf, 1f)
                        mixInto(reverbBuf, auxBuf, player.volume * 0.5f)
                        mixInto(delayBuf, auxBuf, player.volume * 0.3f)
                    }
                }

                // Reverb + delay sends
                reverbSend.process(reverbBuf)
                mixInto(mainBuf, reverbBuf, reverbReturnLevel)
                delaySend.process(delayBuf)
                mixInto(mainBuf, delayBuf, delayReturnLevel)

                // Master FX
                masterEffectChain.process(mainBuf)

                // Master volume + VU
                var pkL = 0f; var pkR = 0f
                for (i in mainBuf.indices step 2) {
                    mainBuf[i] *= masterVolume
                    mainBuf[i + 1] *= masterVolume
                    if (mainBuf[i] > pkL) pkL = mainBuf[i]
                    if (mainBuf[i + 1] > pkR) pkR = mainBuf[i + 1]
                }
                _vuLevel.tryEmit(pkL to pkR)

                audioTrack?.write(mainBuf, 0, mainBuf.size, AudioTrack.WRITE_BLOCKING)

                val elapsed = (System.nanoTime() - t0) / 1e9
                val bufDur = BUFFER_FRAMES.toDouble() / SAMPLE_RATE
                _cpuLoad.tryEmit((elapsed / bufDur).toFloat().coerceIn(0f, 1f))
            }
        }, "AudioEngine")
        audioThread!!.isDaemon = true
        audioThread!!.start()
    }

    private fun tickSequencer(frames: Int) {
        val samplesPerBeat = SAMPLE_RATE * 60.0 / bpm
        val samplesPerStep = samplesPerBeat / (stepsPerBar / 4.0)
        val prevStep = (sampleClock / samplesPerStep).toLong()
        sampleClock += frames
        val nextStep = (sampleClock / samplesPerStep).toLong()
        if (nextStep > prevStep) {
            onBeat?.invoke((nextStep % stepsPerBar).toInt())
        }
    }

    private fun mixInto(dst: FloatArray, src: FloatArray, gain: Float) {
        for (i in dst.indices) dst[i] += src[i] * gain
    }

    fun addSamplePlayer(player: SamplePlayer) = samplePlayers.add(player)
    fun removeSamplePlayer(player: SamplePlayer) = samplePlayers.remove(player)
    fun clearSamplePlayers() = samplePlayers.clear()

    fun triggerPad(player: SamplePlayer) {
        player.play()
        if (!samplePlayers.contains(player)) samplePlayers.add(player)
    }

    fun stopPad(player: SamplePlayer) = player.stop()

    fun resetClock() { sampleClock = 0 }

    fun stop() {
        isRunning.set(false)
        audioThread?.join(500)
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
