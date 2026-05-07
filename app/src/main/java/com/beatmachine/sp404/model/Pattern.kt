package com.beatmachine.sp404.model

data class Step(
    val index: Int,
    var active: Boolean = false,
    var velocity: Float = 1.0f,
    var probability: Float = 1.0f,
    var pitchOffset: Float = 0f,       // semitones
    var volumeOffset: Float = 0f,
    var microTiming: Float = 0f,       // -0.5 to +0.5 of step
    var paramLocks: MutableMap<String, Float> = mutableMapOf()
)

data class PadSequence(
    val padIndex: Int,
    val steps: List<Step> = List(32) { Step(it) }
)

data class Pattern(
    val id: Int,
    val name: String = "Pattern ${id + 1}",
    val length: Int = 16,           // 1–32 steps
    val padSequences: List<PadSequence> = List(16) { PadSequence(it) },
    var swing: Float = 0f,          // 0..1 (0 = no swing, 0.5 = max)
    var timeSignatureNum: Int = 4,
    var timeSignatureDen: Int = 4,
    var tempo: Double = 120.0
) {
    companion object {
        fun createDefault(count: Int = 16) = List(count) { Pattern(it) }
    }
}
