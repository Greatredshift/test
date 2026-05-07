package com.beatmachine.sp404.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatmachine.sp404.audio.AudioEngine
import com.beatmachine.sp404.audio.SamplePlayer
import com.beatmachine.sp404.audio.effects.EffectChain
import com.beatmachine.sp404.audio.effects.EffectFactory
import com.beatmachine.sp404.audio.synth.SynthPatch
import com.beatmachine.sp404.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

data class UiState(
    val banks: List<Bank> = Bank.createDefault(),
    val currentBankIndex: Int = 0,
    val patterns: List<Pattern> = Pattern.createDefault(),
    val currentPatternIndex: Int = 0,
    val bpm: Double = 120.0,
    val isPlaying: Boolean = false,
    val isRecording: Boolean = false,
    val currentStep: Int = -1,
    val activePads: Set<String> = emptySet(),
    val selectedPad: Int? = null,
    val recordingPad: Int? = null,
    val masterEffects: List<EffectType> = emptyList(),
    val reverbEffects: List<EffectType> = emptyList(),
    val delayEffects: List<EffectType> = emptyList(),
    val effectParams: Map<EffectType, Map<String, Float>> = emptyMap()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = AudioEngine()
    private var samplePickerLauncher: (() -> Unit)? = null
    fun setSamplePickerLauncher(launcher: () -> Unit) { samplePickerLauncher = launcher }
    fun openSamplePicker() = samplePickerLauncher?.invoke()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val vuLevel = audioEngine.vuLevel

    private val _synthPatch = MutableStateFlow(SynthPatch())
    val synthPatch: StateFlow<SynthPatch> = _synthPatch.asStateFlow()

    private val _synthPatches = MutableStateFlow(audioEngine.synthesizer.availablePatches.toList())
    val synthPatches: StateFlow<List<SynthPatch>> = _synthPatches.asStateFlow()

    // Map of bankId_padIndex -> SamplePlayer
    private val samplePlayers = mutableMapOf<String, SamplePlayer>()

    init {
        audioEngine.start()
        audioEngine.bpm = 120.0
        audioEngine.onBeat = { step ->
            val state = _uiState.value
            if (state.isPlaying) {
                val pattern = state.patterns[state.currentPatternIndex]
                _uiState.value = state.copy(currentStep = step)
                // Trigger active steps for current bank
                val bank = state.banks[state.currentBankIndex]
                pattern.padSequences.forEachIndexed { padIdx, seq ->
                    if (step < seq.steps.size && seq.steps[step].active) {
                        val key = "${state.currentBankIndex}_$padIdx"
                        samplePlayers[key]?.let { audioEngine.triggerPad(it) }
                            ?: if (bank.pads[padIdx].useSynth) {
                                val note = 48 + padIdx
                                audioEngine.synthesizer.noteOn(note, seq.steps[step].velocity)
                            }
                    }
                }
            }
        }
    }

    fun selectBank(index: Int) {
        _uiState.value = _uiState.value.copy(currentBankIndex = index)
    }

    fun selectPattern(index: Int) {
        _uiState.value = _uiState.value.copy(currentPatternIndex = index)
    }

    fun setBpm(bpm: Double) {
        audioEngine.bpm = bpm
        _uiState.value = _uiState.value.copy(bpm = bpm)
    }

    fun togglePlayStop() {
        val state = _uiState.value
        if (state.isPlaying) {
            audioEngine.resetClock()
            _uiState.value = state.copy(isPlaying = false, currentStep = -1)
        } else {
            audioEngine.resetClock()
            _uiState.value = state.copy(isPlaying = true)
        }
    }

    fun toggleRecord() {
        _uiState.value = _uiState.value.copy(isRecording = !_uiState.value.isRecording)
    }

    fun padPressed(pad: Pad) {
        val key = "${_uiState.value.currentBankIndex}_${pad.index}"
        val player = samplePlayers[key]
        if (player != null) {
            audioEngine.triggerPad(player)
        } else if (pad.useSynth) {
            audioEngine.synthesizer.noteOn(60 + pad.index, 0.8f)
        }
        _uiState.value = _uiState.value.copy(
            activePads = _uiState.value.activePads + key,
            selectedPad = pad.index
        )
    }

    fun padReleased(pad: Pad) {
        val key = "${_uiState.value.currentBankIndex}_${pad.index}"
        if (pad.useSynth) audioEngine.synthesizer.noteOff(60 + pad.index)
        _uiState.value = _uiState.value.copy(activePads = _uiState.value.activePads - key)
    }

    fun padLongPress(pad: Pad) {
        _uiState.value = _uiState.value.copy(selectedPad = pad.index)
    }

    fun loadSample(padIndex: Int, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val pcm = decodeToPcmFloat(context, uri) ?: return@launch
            val bankIndex = _uiState.value.currentBankIndex
            val key = "${bankIndex}_$padIndex"
            val player = SamplePlayer(
                padIndex = padIndex,
                bankId = bankIndex,
                data = pcm,
                sampleRate = 44100,
                engineSampleRate = AudioEngine.SAMPLE_RATE
            )
            samplePlayers[key] = player
            audioEngine.addSamplePlayer(player)

            withContext(Dispatchers.Main) {
                val banks = _uiState.value.banks.toMutableList()
                val bank = banks[bankIndex]
                val pads = bank.pads.toMutableList()
                pads[padIndex] = pads[padIndex].copy(
                    sampleData = pcm,
                    sampleUri = uri,
                    sampleName = uri.lastPathSegment?.substringAfterLast('/')?.take(12) ?: "Sample"
                )
                banks[bankIndex] = bank.copy(pads = pads)
                _uiState.value = _uiState.value.copy(banks = banks)
            }
        }
    }

    fun setPadVolume(volume: Float) {
        updateSelectedPad { it.copy(volume = volume) }
        val state = _uiState.value
        val key = "${state.currentBankIndex}_${state.selectedPad}"
        samplePlayers[key]?.volume = volume
    }

    fun setPadPan(pan: Float) {
        updateSelectedPad { it.copy(pan = pan) }
        val state = _uiState.value
        val key = "${state.currentBankIndex}_${state.selectedPad}"
        samplePlayers[key]?.pan = pan
    }

    fun setPadPitch(pitch: Float) {
        updateSelectedPad { it.copy(pitch = pitch) }
        val state = _uiState.value
        val key = "${state.currentBankIndex}_${state.selectedPad}"
        samplePlayers[key]?.pitchSemitones = pitch
    }

    private fun updateSelectedPad(transform: (Pad) -> Pad) {
        val state = _uiState.value
        val padIndex = state.selectedPad ?: return
        val banks = state.banks.toMutableList()
        val bank = banks[state.currentBankIndex]
        val pads = bank.pads.toMutableList()
        pads[padIndex] = transform(pads[padIndex])
        banks[state.currentBankIndex] = bank.copy(pads = pads)
        _uiState.value = state.copy(banks = banks)
    }

    fun addMasterEffect(type: EffectType) {
        val effect = EffectFactory.create(type, AudioEngine.SAMPLE_RATE)
        audioEngine.masterEffectChain.add(effect)
        _uiState.value = _uiState.value.copy(masterEffects = _uiState.value.masterEffects + type)
    }

    fun removeMasterEffect(type: EffectType) {
        val chain = audioEngine.masterEffectChain
        chain.getEffects().firstOrNull { it.type == type }?.let { chain.remove(it) }
        _uiState.value = _uiState.value.copy(masterEffects = _uiState.value.masterEffects - type)
    }

    fun addSendEffect(type: EffectType, sendIndex: Int) {
        val effect = EffectFactory.create(type, AudioEngine.SAMPLE_RATE)
        if (sendIndex == 0) audioEngine.reverbSend.add(effect)
        else audioEngine.delaySend.add(effect)
        val list = if (sendIndex == 0) _uiState.value.reverbEffects else _uiState.value.delayEffects
        val updated = list + type
        _uiState.value = if (sendIndex == 0)
            _uiState.value.copy(reverbEffects = updated)
        else
            _uiState.value.copy(delayEffects = updated)
    }

    fun setEffectParam(type: EffectType, key: String, value: Float) {
        val allChains = listOf(
            audioEngine.masterEffectChain,
            audioEngine.reverbSend,
            audioEngine.delaySend
        )
        for (chain in allChains) {
            chain.getEffects().filter { it.type == type }.forEach { it.setParam(key, value) }
        }
        val params = _uiState.value.effectParams.toMutableMap()
        val effectParams = (params[type] ?: emptyMap()).toMutableMap()
        effectParams[key] = value
        params[type] = effectParams
        _uiState.value = _uiState.value.copy(effectParams = params)
    }

    fun toggleStep(padIndex: Int, stepIndex: Int) {
        val state = _uiState.value
        val patterns = state.patterns.toMutableList()
        val pattern = patterns[state.currentPatternIndex]
        val sequences = pattern.padSequences.toMutableList()
        val seq = sequences[padIndex]
        val steps = seq.steps.toMutableList()
        steps[stepIndex] = steps[stepIndex].copy(active = !steps[stepIndex].active)
        sequences[padIndex] = seq.copy(steps = steps)
        patterns[state.currentPatternIndex] = pattern.copy(padSequences = sequences)
        _uiState.value = state.copy(patterns = patterns)
    }

    fun setPatternLength(length: Int) {
        val state = _uiState.value
        audioEngine.stepsPerBar = length
        val patterns = state.patterns.toMutableList()
        patterns[state.currentPatternIndex] = patterns[state.currentPatternIndex].copy(length = length)
        _uiState.value = state.copy(patterns = patterns)
    }

    fun setPatternSwing(swing: Float) {
        val state = _uiState.value
        val patterns = state.patterns.toMutableList()
        patterns[state.currentPatternIndex] = patterns[state.currentPatternIndex].copy(swing = swing)
        _uiState.value = state.copy(patterns = patterns)
    }

    fun synthNoteOn(note: Int, velocity: Float) = audioEngine.synthesizer.noteOn(note, velocity)
    fun synthNoteOff(note: Int) = audioEngine.synthesizer.noteOff(note)

    fun updatePatch(patch: SynthPatch) {
        _synthPatch.value = patch
        audioEngine.synthesizer.patch = patch
    }

    fun loadPatch(patch: SynthPatch) {
        updatePatch(patch)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }

    // Decode any audio file to a FloatArray of PCM samples using MediaCodec
    private fun decodeToPcmFloat(context: Context, uri: Uri): FloatArray? = try {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        var audioTrackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioTrackIndex = i; format = fmt; break
            }
        }
        if (audioTrackIndex < 0 || format == null) return null
        extractor.selectTrack(audioTrackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val outputList = mutableListOf<Short>()
        var sawEOS = false
        val bufferInfo = MediaCodec.BufferInfo()

        while (!sawEOS) {
            val inputIdx = codec.dequeueInputBuffer(10_000L)
            if (inputIdx >= 0) {
                val inputBuf = codec.getInputBuffer(inputIdx)!!
                val sampleSize = extractor.readSampleData(inputBuf, 0)
                if (sampleSize < 0) {
                    codec.queueInputBuffer(inputIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    sawEOS = true
                } else {
                    codec.queueInputBuffer(inputIdx, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }
            val outputIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
            if (outputIdx >= 0) {
                val outputBuf = codec.getOutputBuffer(outputIdx)!!
                val shortBuf = outputBuf.asShortBuffer()
                while (shortBuf.hasRemaining()) outputList.add(shortBuf.get())
                codec.releaseOutputBuffer(outputIdx, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
        codec.stop(); codec.release(); extractor.release()

        FloatArray(outputList.size) { outputList[it] / 32768f }
    } catch (e: Exception) {
        null
    }
}
