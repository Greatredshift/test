package com.beatmachine.sp404.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatmachine.sp404.model.EffectCategory
import com.beatmachine.sp404.model.EffectType
import com.beatmachine.sp404.ui.components.LabeledKnob
import com.beatmachine.sp404.ui.theme.*
import com.beatmachine.sp404.viewmodel.MainViewModel

@Composable
fun EffectsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf(EffectCategory.REVERB) }
    var selectedEffect by remember { mutableStateOf<EffectType?>(null) }
    var selectedSlot by remember { mutableStateOf(0) }  // 0=master, 1=reverb, 2=delay

    Row(modifier = modifier.fillMaxSize()) {
        // Category selector
        Column(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .background(SpDarkSurface)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("FX TYPE", fontSize = 8.sp, color = SpTextDim, modifier = Modifier.padding(bottom = 4.dp))
            EffectCategory.values().forEach { cat ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selectedCategory == cat) SpRed else SpSurface,
                            RoundedCornerShape(3.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(cat.displayName, fontSize = 9.sp, color = if (selectedCategory == cat) SpText else SpTextDim)
                }
            }
        }

        // Effect list
        LazyColumn(
            modifier = Modifier
                .width(140.dp)
                .fillMaxHeight()
                .background(SpBlack)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val effects = EffectType.byCategory(selectedCategory)
            items(effects) { effect ->
                val isActive = uiState.masterEffects.contains(effect)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selectedEffect == effect) SpSurface else Color.Transparent,
                            RoundedCornerShape(3.dp)
                        )
                        .border(
                            0.5.dp,
                            if (isActive) SpGreen else Color.Transparent,
                            RoundedCornerShape(3.dp)
                        )
                        .clickable { selectedEffect = effect }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(6.dp).background(if (isActive) SpGreen else SpSurfaceLight, RoundedCornerShape(3.dp)))
                        Text(effect.displayName, fontSize = 9.sp, color = if (selectedEffect == effect) SpText else SpTextDim)
                    }
                }
            }
        }

        // Effect params + chain controls
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().background(SpDarkSurface).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedEffect != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedEffect!!.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpText)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EffectActionButton("ADD MASTER") { viewModel.addMasterEffect(selectedEffect!!) }
                        EffectActionButton("ADD REVERB") { viewModel.addSendEffect(selectedEffect!!, 0) }
                        EffectActionButton("ADD DELAY") { viewModel.addSendEffect(selectedEffect!!, 1) }
                    }
                }

                // Effect-specific knobs
                EffectParamKnobs(effect = selectedEffect!!, params = uiState.effectParams[selectedEffect!!] ?: mapOf(), onParamChange = { k, v -> viewModel.setEffectParam(selectedEffect!!, k, v) })
            } else {
                Text("Select an effect", fontSize = 12.sp, color = SpTextDim, modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            // Active effect chains
            Spacer(Modifier.weight(1f))
            Text("MASTER CHAIN", fontSize = 9.sp, color = SpTextDim)
            ActiveEffectChain(
                effects = uiState.masterEffects,
                onRemove = { viewModel.removeMasterEffect(it) }
            )
        }
    }
}

@Composable
private fun EffectActionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(SpRed, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 8.sp, color = SpText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActiveEffectChain(effects: List<EffectType>, onRemove: (EffectType) -> Unit) {
    val scrollState = rememberScrollState()
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        effects.forEach { effect ->
            Box(
                modifier = Modifier
                    .background(SpSurface, RoundedCornerShape(3.dp))
                    .border(0.5.dp, SpGreen, RoundedCornerShape(3.dp))
                    .clickable { onRemove(effect) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(effect.displayName, fontSize = 8.sp, color = SpGreen)
            }
        }
        if (effects.isEmpty()) {
            Text("No effects in chain", fontSize = 9.sp, color = SpTextDisabled)
        }
    }
}

@Composable
private fun EffectParamKnobs(
    effect: EffectType,
    params: Map<String, Float>,
    onParamChange: (String, Float) -> Unit
) {
    val knobs: List<Triple<String, String, Pair<Float, Float>>> = when (effect) {
        EffectType.REVERB_ROOM, EffectType.REVERB_HALL, EffectType.REVERB_CATHEDRAL, EffectType.REVERB_MODULATE ->
            listOf(Triple("roomSize", "ROOM", 0f to 1f), Triple("damping", "DAMP", 0f to 1f), Triple("width", "WIDTH", 0f to 1f))
        EffectType.DELAY_STANDARD, EffectType.DELAY_TAPE, EffectType.DELAY_PANNING ->
            listOf(Triple("time", "TIME", 0f to 2f), Triple("feedback", "FEEDBK", 0f to 0.95f), Triple("highCut", "HI CUT", 0f to 1f))
        EffectType.CHORUS ->
            listOf(Triple("rate", "RATE", 0.1f to 5f), Triple("depth", "DEPTH", 0f to 1f))
        EffectType.FLANGER ->
            listOf(Triple("rate", "RATE", 0.1f to 5f), Triple("depth", "DEPTH", 0f to 1f), Triple("feedback", "FEEDBK", 0f to 0.95f))
        EffectType.PHASER ->
            listOf(Triple("rate", "RATE", 0.1f to 4f), Triple("depth", "DEPTH", 0f to 1f), Triple("feedback", "FEEDBK", 0f to 0.95f))
        EffectType.TREMOLO ->
            listOf(Triple("rate", "RATE", 0.1f to 20f), Triple("depth", "DEPTH", 0f to 1f))
        EffectType.RING_MODULATOR ->
            listOf(Triple("frequency", "FREQ", 20f to 2000f), Triple("mix", "MIX", 0f to 1f))
        EffectType.VINYL_SIMULATOR ->
            listOf(Triple("wow", "WOW", 0f to 3f), Triple("flutter", "FLUTTER", 0f to 1f), Triple("crackle", "CRACKLE", 0f to 1f), Triple("warmth", "WARMTH", 0f to 1f))
        EffectType.LOW_PASS_FILTER, EffectType.HIGH_PASS_FILTER ->
            listOf(Triple("cutoff", "CUTOFF", 0f to 1f), Triple("resonance", "RES", 0f to 1f))
        EffectType.BAND_PASS_FILTER ->
            listOf(Triple("center", "CENTER", 0f to 1f), Triple("bandwidth", "BW", 0f to 1f))
        EffectType.ISOLATOR ->
            listOf(Triple("low", "LOW", 0f to 1f), Triple("mid", "MID", 0f to 1f), Triple("high", "HIGH", 0f to 1f))
        EffectType.EQ_THREE_BAND ->
            listOf(Triple("lowGain", "LO dB", -15f to 15f), Triple("midGain", "MID dB", -15f to 15f), Triple("highGain", "HI dB", -15f to 15f))
        EffectType.COMPRESSOR ->
            listOf(Triple("threshold", "THRESH", -60f to 0f), Triple("ratio", "RATIO", 1f to 20f), Triple("attack", "ATK", 0.001f to 0.5f), Triple("release", "REL", 0.01f to 2f))
        EffectType.LIMITER ->
            listOf(Triple("ceiling", "CEIL", -12f to 0f), Triple("release", "REL", 0.01f to 1f))
        EffectType.OVERDRIVE ->
            listOf(Triple("drive", "DRIVE", 0f to 1f), Triple("tone", "TONE", 0f to 1f), Triple("level", "LEVEL", 0f to 1f))
        EffectType.GUITAR_AMP ->
            listOf(Triple("gain", "GAIN", 0f to 1f), Triple("bass", "BASS", 0f to 1f), Triple("mid", "MID", 0f to 1f), Triple("treble", "TREBLE", 0f to 1f))
        EffectType.BIT_CRUSHER ->
            listOf(Triple("bits", "BITS", 1f to 16f), Triple("srr", "SR RED", 0f to 1f))
        EffectType.RADIO ->
            listOf(Triple("tuning", "TUNING", 0f to 1f), Triple("noise", "NOISE", 0f to 1f), Triple("bandwidth", "BW", 0f to 1f))
        EffectType.TELEPHONE ->
            listOf(Triple("presence", "PRES", 0f to 1f), Triple("distortion", "DIST", 0f to 1f))
        EffectType.SCATTER ->
            listOf(Triple("length", "LENGTH", 0.01f to 1f), Triple("repeats", "RPTS", 1f to 32f))
        EffectType.ROLL ->
            listOf(Triple("bpm", "BPM", 60f to 200f), Triple("division", "DIV", 1f to 16f), Triple("decay", "DECAY", 0f to 0.95f))
        EffectType.SIDECHAIN ->
            listOf(Triple("depth", "DEPTH", 0f to 1f), Triple("attack", "ATK", 0.001f to 0.5f), Triple("release", "REL", 0.05f to 2f))
        EffectType.WAVE_DESIGNER ->
            listOf(Triple("attack", "ATK BOOST", 0f to 1f), Triple("sustain", "SUS LEVEL", 0f to 1f))
        EffectType.VOCAL_MORPH ->
            listOf(Triple("pitch", "PITCH", -12f to 12f), Triple("formant", "FORMANT", -1f to 1f))
        EffectType.AUTO_PITCH ->
            listOf(Triple("speed", "SPEED", 0f to 1f), Triple("key", "KEY", 0f to 11f))
        EffectType.SUB_OSCILLATOR ->
            listOf(Triple("level", "LEVEL", 0f to 1f))
        EffectType.NOISE ->
            listOf(Triple("level", "LEVEL", 0f to 1f), Triple("color", "COLOR", 0f to 1f))
        EffectType.STRUMMER ->
            listOf(Triple("speed", "SPEED", 0f to 1f), Triple("direction", "DIR", -1f to 1f))
        EffectType.CENTER_CANCELLER ->
            listOf(Triple("amount", "AMOUNT", 0f to 1f))
        EffectType.WAVE_REPEAT ->
            listOf(Triple("size", "SIZE", 0f to 1f), Triple("rate", "RATE", 0.1f to 4f))
    }

    val knobScrollState = rememberScrollState()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(knobScrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        knobs.forEach { (key, label, range) ->
            val v = params[key] ?: ((range.first + range.second) / 2f)
            LabeledKnob(
                value = v,
                onValueChange = { onParamChange(key, it) },
                label = label,
                color = SpOrange,
                min = range.first,
                max = range.second,
                displayValue = "%.2f".format(v)
            )
        }
    }
}

