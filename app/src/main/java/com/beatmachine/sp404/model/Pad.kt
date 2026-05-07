package com.beatmachine.sp404.model

import android.net.Uri

enum class PadPlayMode { ONESHOT, TOGGLE, HOLD, GATE }
enum class PadTriggerMode { POLY, MONO, CHOKE }

data class PadEffect(
    val type: EffectType,
    val params: MutableMap<String, Float> = mutableMapOf()
)

data class Pad(
    val index: Int,
    val bankId: Int,
    var name: String = "Pad ${index + 1}",
    var sampleUri: Uri? = null,
    var sampleData: FloatArray? = null,
    var sampleName: String = "",
    var volume: Float = 1.0f,
    var pan: Float = 0.0f,        // -1 to +1
    var pitch: Float = 0.0f,      // semitones
    var startPoint: Float = 0.0f, // 0..1
    var endPoint: Float = 1.0f,   // 0..1
    var loopStart: Float = 0.0f,
    var loopEnd: Float = 1.0f,
    var isLooping: Boolean = false,
    var playMode: PadPlayMode = PadPlayMode.ONESHOT,
    var triggerMode: PadTriggerMode = PadTriggerMode.POLY,
    var chokeGroup: Int = 0,       // 0 = no choke group
    var filterType: EffectType = EffectType.LOW_PASS_FILTER,
    var filterCutoff: Float = 1.0f,
    var filterResonance: Float = 0.0f,
    var sendReverb: Float = 0.0f,
    var sendDelay: Float = 0.0f,
    var isMuted: Boolean = false,
    var isSoloActive: Boolean = false,
    var color: PadColor = PadColor.RED,
    var insertEffect1: PadEffect? = null,
    var insertEffect2: PadEffect? = null,
    var useSynth: Boolean = false,
    var synthPatchId: Int = 0
) {
    val isEmpty: Boolean get() = sampleData == null && !useSynth

    fun clone(): Pad = copy(
        sampleData = sampleData?.copyOf(),
        insertEffect1 = insertEffect1?.copy(params = insertEffect1!!.params.toMutableMap()),
        insertEffect2 = insertEffect2?.copy(params = insertEffect2!!.params.toMutableMap())
    )
}

enum class PadColor(val hex: Long) {
    RED(0xFFCC2200),
    ORANGE(0xFFCC6600),
    AMBER(0xFFCCAA00),
    GREEN(0xFF44CC00),
    TEAL(0xFF00CC88),
    BLUE(0xFF0088CC),
    PURPLE(0xFF4400CC),
    PINK(0xFFCC0088),
    WARM(0xFFCC4422),
    GREY(0xFF888888)
}
