package com.beatmachine.sp404.audio.synth

import kotlin.math.*

enum class ArpMode { OFF, UP, DOWN, UP_DOWN, RANDOM, ORDER }

data class SynthPatch(
    val name: String = "Init",
    var osc1Wave: WaveShape = WaveShape.SAWTOOTH,
    var osc2Wave: WaveShape = WaveShape.SQUARE,
    var osc3Wave: WaveShape = WaveShape.TRIANGLE,
    var osc2Detune: Double = 7.0,
    var osc3Detune: Double = -5.0,
    var osc1Vol: Float = 0.8f,
    var osc2Vol: Float = 0.6f,
    var osc3Vol: Float = 0.0f,
    var subVol: Float = 0.0f,
    var filterMode: FilterMode = FilterMode.LPF24,
    var filterCutoff: Float = 0.7f,
    var filterResonance: Float = 0.2f,
    var filterEnvAmount: Float = 0.4f,
    var ampAttack: Float = 0.005f,
    var ampDecay: Float = 0.1f,
    var ampSustain: Float = 0.7f,
    var ampRelease: Float = 0.3f,
    var filterAttack: Float = 0.005f,
    var filterDecay: Float = 0.3f,
    var filterSustain: Float = 0.3f,
    var filterRelease: Float = 0.2f,
    var lfo1Shape: LFOShape = LFOShape.SINE,
    var lfo1Rate: Float = 3.0f,
    var lfo1Depth: Float = 0.0f,
    var lfo1Target: LFOTarget = LFOTarget.PITCH,
    var lfo2Shape: LFOShape = LFOShape.SINE,
    var lfo2Rate: Float = 0.5f,
    var lfo2Depth: Float = 0.0f,
    var lfo2Target: LFOTarget = LFOTarget.FILTER,
    var portamento: Float = 0f,
    var portamentoEnabled: Boolean = false,
    var unisonVoices: Int = 1,
    var unisonSpread: Double = 15.0,
    var arpMode: ArpMode = ArpMode.OFF,
    var arpRate: Float = 4f,      // steps per beat
    var chordMode: Boolean = false,
    var chordIntervals: List<Int> = listOf(0, 4, 7)   // major chord
)

class Synthesizer(private val sampleRate: Int) {
    private val maxVoices = 16
    private val voices = List(maxVoices) { Voice(sampleRate) }

    val lfo1 = LFO(sampleRate)
    val lfo2 = LFO(sampleRate)

    var patch: SynthPatch = SynthPatch()
    var masterVolume: Float = 0.8f
    var enabled: Boolean = true

    private var lastNote = -1

    // Arpeggiator state
    private val heldNotes = mutableListOf<Int>()
    private var arpStep = 0
    private var arpPhase = 0.0
    private val arpNotes = mutableListOf<Int>()

    fun noteOn(midiNote: Int, velocity: Float) {
        if (!enabled) return
        applyPatch()

        if (patch.chordMode) {
            patch.chordIntervals.forEach { interval ->
                triggerNote(midiNote + interval, velocity)
            }
        } else if (patch.arpMode != ArpMode.OFF) {
            if (!heldNotes.contains(midiNote)) heldNotes.add(midiNote)
            rebuildArp()
        } else {
            triggerNote(midiNote, velocity)
        }
        lastNote = midiNote
    }

    fun noteOff(midiNote: Int) {
        heldNotes.remove(midiNote)
        if (patch.arpMode == ArpMode.OFF) {
            voices.filter { it.isActive && it.midiNote == midiNote }.forEach { it.noteOff() }
        }
    }

    fun allNotesOff() {
        voices.forEach { it.noteOff() }
        heldNotes.clear()
        arpNotes.clear()
    }

    private fun triggerNote(note: Int, velocity: Float) {
        val prevFreq = if (lastNote >= 0) 440.0 * 2.0.pow((lastNote - 69) / 12.0) else -1.0
        val voice = findFreeVoice() ?: stealVoice()
        applyPatchToVoice(voice)
        voice.noteOn(note, velocity, prevFreq)
    }

    private fun rebuildArp() {
        arpNotes.clear()
        val sorted = heldNotes.sorted()
        when (patch.arpMode) {
            ArpMode.UP -> arpNotes.addAll(sorted)
            ArpMode.DOWN -> arpNotes.addAll(sorted.reversed())
            ArpMode.UP_DOWN -> {
                arpNotes.addAll(sorted)
                if (sorted.size > 2) arpNotes.addAll(sorted.drop(1).dropLast(1).reversed())
            }
            ArpMode.RANDOM -> arpNotes.addAll(sorted.shuffled())
            ArpMode.ORDER -> arpNotes.addAll(heldNotes)
            ArpMode.OFF -> {}
        }
    }

    fun render(buffer: FloatArray) {
        if (!enabled) return
        val l1 = lfo1.process()
        val l2 = lfo2.process()

        // Arpeggiator tick
        if (patch.arpMode != ArpMode.OFF && arpNotes.isNotEmpty()) {
            val stepsPerSec = patch.arpRate * (120.0 / 60.0)
            arpPhase += stepsPerSec * (buffer.size / 2) / sampleRate
            while (arpPhase >= 1.0) {
                arpPhase -= 1.0
                arpStep = (arpStep + 1) % arpNotes.size
                triggerNote(arpNotes[arpStep], 0.8f)
            }
        }

        voices.forEach { voice ->
            if (voice.isActive) voice.render(buffer, l1, l2)
        }

        // Apply master volume
        for (i in buffer.indices) buffer[i] *= masterVolume
    }

    private fun applyPatch() {
        lfo1.shape = patch.lfo1Shape
        lfo1.rate = patch.lfo1Rate
        lfo1.depth = patch.lfo1Depth
        lfo1.target = patch.lfo1Target
        lfo2.shape = patch.lfo2Shape
        lfo2.rate = patch.lfo2Rate
        lfo2.depth = patch.lfo2Depth
        lfo2.target = patch.lfo2Target
    }

    private fun applyPatchToVoice(v: Voice) {
        v.osc1.waveShape = patch.osc1Wave; v.osc1.volume = patch.osc1Vol; v.osc1.enabled = true
        v.osc2.waveShape = patch.osc2Wave; v.osc2.volume = patch.osc2Vol; v.osc2.detuneCents = patch.osc2Detune; v.osc2.enabled = true
        v.osc3.waveShape = patch.osc3Wave; v.osc3.volume = patch.osc3Vol; v.osc3.detuneCents = patch.osc3Detune; v.osc3.enabled = patch.osc3Vol > 0f
        v.subOsc.volume = patch.subVol; v.subOsc.enabled = patch.subVol > 0f

        v.ampEnv.attack = patch.ampAttack; v.ampEnv.decay = patch.ampDecay
        v.ampEnv.sustain = patch.ampSustain; v.ampEnv.release = patch.ampRelease
        v.filterEnv.attack = patch.filterAttack; v.filterEnv.decay = patch.filterDecay
        v.filterEnv.sustain = patch.filterSustain; v.filterEnv.release = patch.filterRelease

        v.filter.mode = patch.filterMode
        v.filter.cutoff = patch.filterCutoff
        v.filter.resonance = patch.filterResonance
        v.filter.envAmount = patch.filterEnvAmount
        v.filter.lfoAmount = if (patch.lfo2Target == LFOTarget.FILTER) patch.lfo2Depth else 0f

        v.portamentoEnabled = patch.portamentoEnabled
        v.portamentoTime = patch.portamento
    }

    private fun findFreeVoice() = voices.firstOrNull { !it.isActive }
    private fun stealVoice(): Voice {
        val oldest = voices.minByOrNull { if (it.isActive) it.midiNote else Int.MAX_VALUE }!!
        oldest.noteOff()
        return oldest
    }

    val availablePatches = mutableListOf(
        SynthPatch(name = "Init"),
        SynthPatch(name = "Lead Saw", osc1Wave = WaveShape.SAWTOOTH, filterCutoff = 0.6f, filterResonance = 0.4f, ampAttack = 0.01f),
        SynthPatch(name = "Bass", osc1Wave = WaveShape.SAWTOOTH, subVol = 0.8f, filterCutoff = 0.4f, ampDecay = 0.2f, ampSustain = 0.5f),
        SynthPatch(name = "Pad", osc1Wave = WaveShape.SAWTOOTH, osc2Wave = WaveShape.SAWTOOTH, osc2Detune = 12.0, ampAttack = 0.5f, ampRelease = 1.0f, filterCutoff = 0.5f),
        SynthPatch(name = "Pluck", osc1Wave = WaveShape.SAWTOOTH, filterCutoff = 0.8f, filterEnvAmount = 0.9f, filterDecay = 0.08f, ampDecay = 0.15f, ampSustain = 0f),
        SynthPatch(name = "Strings", osc1Wave = WaveShape.SAWTOOTH, osc2Wave = WaveShape.SAWTOOTH, osc2Detune = 7.0, osc3Wave = WaveShape.SAWTOOTH, osc3Vol = 0.4f, osc3Detune = -7.0, ampAttack = 0.3f, ampRelease = 0.8f),
        SynthPatch(name = "Arp Up", arpMode = ArpMode.UP, filterCutoff = 0.7f, ampDecay = 0.12f, ampSustain = 0.5f),
        SynthPatch(name = "Wobble", lfo2Target = LFOTarget.FILTER, lfo2Depth = 0.5f, lfo2Rate = 4.0f, filterCutoff = 0.5f, filterResonance = 0.5f),
        SynthPatch(name = "FM Bell", osc1Wave = WaveShape.SINE, osc2Wave = WaveShape.SINE, filterMode = FilterMode.BPF, filterCutoff = 0.7f, ampDecay = 0.5f, ampSustain = 0f),
        SynthPatch(name = "Chord", chordMode = true, chordIntervals = listOf(0, 4, 7), osc1Wave = WaveShape.SAWTOOTH, ampAttack = 0.1f)
    )
}
