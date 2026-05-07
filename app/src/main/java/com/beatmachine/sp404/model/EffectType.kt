package com.beatmachine.sp404.model

enum class EffectType(val displayName: String, val category: EffectCategory) {
    // Reverb types
    REVERB_ROOM("Reverb Room", EffectCategory.REVERB),
    REVERB_HALL("Reverb Hall", EffectCategory.REVERB),
    REVERB_CATHEDRAL("Reverb Cathedral", EffectCategory.REVERB),
    REVERB_MODULATE("Reverb Modulate", EffectCategory.REVERB),

    // Delay types
    DELAY_STANDARD("Delay Standard", EffectCategory.DELAY),
    DELAY_TAPE("Delay Tape", EffectCategory.DELAY),
    DELAY_PANNING("Delay Panning", EffectCategory.DELAY),

    // Scatter / Repeat
    SCATTER("Scatter", EffectCategory.SCATTER),
    ROLL("Roll", EffectCategory.SCATTER),
    WAVE_REPEAT("Wave Repeat", EffectCategory.SCATTER),

    // Modulation
    CHORUS("Chorus", EffectCategory.MODULATION),
    FLANGER("Flanger", EffectCategory.MODULATION),
    PHASER("Phaser", EffectCategory.MODULATION),
    TREMOLO("Tremolo", EffectCategory.MODULATION),
    RING_MODULATOR("Ring Modulator", EffectCategory.MODULATION),
    VINYL_SIMULATOR("Vinyl Simulator", EffectCategory.MODULATION),

    // Filter / EQ
    LOW_PASS_FILTER("Low Pass Filter", EffectCategory.FILTER),
    HIGH_PASS_FILTER("High Pass Filter", EffectCategory.FILTER),
    BAND_PASS_FILTER("Band Pass Filter", EffectCategory.FILTER),
    ISOLATOR("Isolator", EffectCategory.FILTER),
    EQ_THREE_BAND("3-Band EQ", EffectCategory.FILTER),
    CENTER_CANCELLER("Center Canceller", EffectCategory.FILTER),

    // Dynamics
    COMPRESSOR("Compressor", EffectCategory.DYNAMICS),
    LIMITER("Limiter", EffectCategory.DYNAMICS),
    SIDECHAIN("Sidechain", EffectCategory.DYNAMICS),
    WAVE_DESIGNER("Wave Designer", EffectCategory.DYNAMICS),

    // Distortion / Character
    OVERDRIVE("Overdrive", EffectCategory.DISTORTION),
    GUITAR_AMP("Guitar Amp", EffectCategory.DISTORTION),
    BIT_CRUSHER("Bit Crusher", EffectCategory.DISTORTION),
    RADIO("Radio", EffectCategory.DISTORTION),
    TELEPHONE("Telephone", EffectCategory.DISTORTION),

    // Pitch / Synthesis
    VOCAL_MORPH("Vocal Morph", EffectCategory.PITCH),
    AUTO_PITCH("Auto Pitch", EffectCategory.PITCH),
    SUB_OSCILLATOR("Sub Oscillator", EffectCategory.PITCH),
    STRUMMER("Strummer", EffectCategory.PITCH),
    NOISE("Noise", EffectCategory.PITCH);

    companion object {
        fun byCategory(cat: EffectCategory) = values().filter { it.category == cat }
    }
}

enum class EffectCategory(val displayName: String) {
    REVERB("Reverb"),
    DELAY("Delay"),
    SCATTER("Scatter"),
    MODULATION("Modulation"),
    FILTER("Filter / EQ"),
    DYNAMICS("Dynamics"),
    DISTORTION("Distortion"),
    PITCH("Pitch / Synth")
}
