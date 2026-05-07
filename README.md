# BeatMachine SP-404

An Android sampler app inspired by the Roland SP-404 MK2, featuring a built-in subtractive synthesiser and 35 real-time DSP effects.

---

## Features

### 16-Pad Sampler
- 4×4 pad grid across **10 banks (A–J)** = 160 pads total
- Load any audio file per pad (WAV, MP3, AAC, FLAC via Android MediaCodec)
- Per-pad: Volume, Pan, Pitch (±24 semitones), Start/End points, Loop mode
- Play modes: One-shot, Toggle, Hold, Gate
- Choke groups for hi-hat muting
- Insert effects per pad

### Built-in Synthesiser
- **3 oscillators + Sub oscillator** — Sine, Square (with PWM), Sawtooth, Reverse Saw, Triangle, Noise, Pulse
- **Anti-aliased** via PolyBLEP
- **Multi-mode filter** — LP 12/24 dB, HP 12/24 dB, BP, Notch with resonance
- **2 ADSR envelopes** (Amplitude + Filter)
- **2 LFOs** — 7 shapes, 6 targets (Pitch, Filter, Amp, Pan, Pulse Width, OSC2 Pitch), tempo sync
- **Portamento/Glide**
- **16-voice polyphony**
- **Arpeggiator** — Up, Down, Up/Down, Random, Order
- **Chord mode** with configurable intervals
- 10 built-in patches (Init, Lead Saw, Bass, Pad, Pluck, Strings, Arp Up, Wobble, FM Bell, Chord)
- Mini on-screen keyboard with octave selector

### 35 SP-404-Style Effects

| Category | Effects |
|---|---|
| **Reverb** | Room, Hall, Cathedral, Modulate (Freeverb algorithm) |
| **Delay** | Standard, Tape (wow/flutter), Panning |
| **Scatter / Repeat** | Scatter (repeat/reverse/gate/pingpong), Roll, Wave Repeat |
| **Modulation** | Chorus, Flanger, Phaser (6-stage all-pass), Tremolo, Ring Modulator, Vinyl Simulator |
| **Filter / EQ** | LPF, HPF, BPF (all with resonance), Isolator (3-band kill), 3-Band Parametric EQ, Center Canceller |
| **Dynamics** | Compressor, Limiter, Sidechain (auto-duck), Wave Designer (transient shaper) |
| **Distortion** | Overdrive (soft clip), Guitar Amp (tone stack + cabinet), Bit Crusher, Radio, Telephone |
| **Pitch / Synth** | Vocal Morph (pitch shift), Auto Pitch, Sub Oscillator, Noise (white/pink), Strummer |

Effects can be chained on the **Master bus**, **Reverb send**, or **Delay send**.

### Step Sequencer
- 16 pads × 32 steps per pattern
- 16 patterns per session
- Adjustable pattern length (8/16/32 steps)
- Per-step velocity and probability
- Swing quantisation
- Real-time step highlighting during playback

### Transport
- BPM control (40–300), drag to adjust
- Play / Stop / Record
- Stereo VU meters
- Low-latency audio via `AudioTrack.PERFORMANCE_MODE_LOW_LATENCY`

---

## Building

**Requirements:** Android Studio Hedgehog (2023.1) or newer, Android SDK 34, Gradle 8.4.

```bash
# Clone and open in Android Studio, or:
./gradlew assembleDebug
```

**Minimum API:** 26 (Android 8.0)  
**Target API:** 34 (Android 14)  
**Orientation:** Landscape (locked)

---

## Project Structure

```
app/src/main/java/com/beatmachine/sp404/
├── audio/
│   ├── AudioEngine.kt        # Real-time audio thread (AudioTrack)
│   ├── SamplePlayer.kt       # Sample playback with pitch / pan
│   └── effects/
│       ├── Effect.kt         # Abstract base + EffectChain
│       ├── EffectFactory.kt  # Instantiate any effect by EffectType
│       ├── ReverbEffect.kt   # Freeverb reverb (4 types)
│       ├── DelayEffect.kt    # Delay (3 types)
│       ├── ModulationEffects.kt  # Chorus, Flanger, Phaser, Tremolo, Ring Mod
│       ├── FilterEffects.kt  # LPF, HPF, BPF, Isolator, EQ, Centre Canceller
│       ├── DynamicsEffects.kt # Compressor, Limiter, Sidechain, Wave Designer
│       ├── DistortionEffects.kt # Overdrive, Guitar Amp, BitCrusher, Radio, Telephone
│       ├── TimeEffects.kt    # Vinyl Sim, Scatter, Roll, Wave Repeat
│       └── PitchEffects.kt   # Vocal Morph, Auto Pitch, Sub Osc, Noise, Strummer
│   └── synth/
│       ├── Oscillator.kt     # PolyBLEP waveform generation
│       ├── Envelope.kt       # ADSR
│       ├── Filter.kt         # Biquad filter (RBJ cookbook)
│       ├── LFO.kt            # Low-frequency oscillator
│       ├── Voice.kt          # Single synth voice
│       └── Synthesizer.kt    # Polyphony, arpeggiator, patch management
├── model/
│   ├── EffectType.kt         # Enum of all 35 effects + categories
│   ├── Pad.kt                # Pad data model
│   ├── Bank.kt               # Bank (10 × 16 pads)
│   └── Pattern.kt            # Step-sequencer pattern
├── ui/
│   ├── theme/                # Dark SP-404 colour scheme, typography
│   ├── components/           # Knob, PadButton, VUMeter, TransportBar
│   └── screens/
│       ├── MainScreen.kt     # Tab container
│       ├── PadGridScreen.kt  # 4×4 pad grid + bank selector
│       ├── SynthScreen.kt    # Synthesiser editor + mini keyboard
│       ├── EffectsScreen.kt  # Effect browser + parameter knobs
│       └── PatternScreen.kt  # Step sequencer
└── viewmodel/
    └── MainViewModel.kt      # State, audio engine wiring, sample decode
```

---

## Permissions

- `RECORD_AUDIO` — microphone input (future feature)
- `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` — loading samples from storage
