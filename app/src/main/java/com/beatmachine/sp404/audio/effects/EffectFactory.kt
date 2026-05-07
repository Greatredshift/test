package com.beatmachine.sp404.audio.effects

import com.beatmachine.sp404.model.EffectType

object EffectFactory {
    fun create(type: EffectType, sampleRate: Int): Effect = when (type) {
        EffectType.REVERB_ROOM,
        EffectType.REVERB_HALL,
        EffectType.REVERB_CATHEDRAL,
        EffectType.REVERB_MODULATE -> ReverbEffect(type, sampleRate)

        EffectType.DELAY_STANDARD,
        EffectType.DELAY_TAPE,
        EffectType.DELAY_PANNING -> DelayEffect(type, sampleRate)

        EffectType.CHORUS -> ChorusEffect(sampleRate)
        EffectType.FLANGER -> FlangerEffect(sampleRate)
        EffectType.PHASER -> PhaserEffect(sampleRate)
        EffectType.TREMOLO -> TremoloEffect(sampleRate)
        EffectType.RING_MODULATOR -> RingModulatorEffect(sampleRate)
        EffectType.VINYL_SIMULATOR -> VinylSimulatorEffect(sampleRate)

        EffectType.LOW_PASS_FILTER -> LowPassFilterEffect(sampleRate)
        EffectType.HIGH_PASS_FILTER -> HighPassFilterEffect(sampleRate)
        EffectType.BAND_PASS_FILTER -> BandPassFilterEffect(sampleRate)
        EffectType.ISOLATOR -> IsolatorEffect(sampleRate)
        EffectType.EQ_THREE_BAND -> EQEffect(sampleRate)
        EffectType.CENTER_CANCELLER -> CenterCancellerEffect(sampleRate)

        EffectType.COMPRESSOR -> CompressorEffect(sampleRate)
        EffectType.LIMITER -> LimiterEffect(sampleRate)
        EffectType.SIDECHAIN -> SidechainEffect(sampleRate)
        EffectType.WAVE_DESIGNER -> WaveDesignerEffect(sampleRate)

        EffectType.OVERDRIVE -> OverdriveEffect(sampleRate)
        EffectType.GUITAR_AMP -> GuitarAmpEffect(sampleRate)
        EffectType.BIT_CRUSHER -> BitCrusherEffect(sampleRate)
        EffectType.RADIO -> RadioEffect(sampleRate)
        EffectType.TELEPHONE -> TelephoneEffect(sampleRate)

        EffectType.VOCAL_MORPH -> VocalMorphEffect(sampleRate)
        EffectType.AUTO_PITCH -> AutoPitchEffect(sampleRate)
        EffectType.SUB_OSCILLATOR -> SubOscillatorEffect(sampleRate)
        EffectType.NOISE -> NoiseEffect(sampleRate)
        EffectType.STRUMMER -> StrummerEffect(sampleRate)

        EffectType.SCATTER -> ScatterEffect(sampleRate)
        EffectType.ROLL -> RollEffect(sampleRate)
        EffectType.WAVE_REPEAT -> WaveRepeatEffect(sampleRate)
    }
}
