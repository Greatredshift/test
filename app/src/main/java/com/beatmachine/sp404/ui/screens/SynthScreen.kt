package com.beatmachine.sp404.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatmachine.sp404.audio.synth.*
import com.beatmachine.sp404.ui.components.LabeledKnob
import com.beatmachine.sp404.ui.theme.*
import com.beatmachine.sp404.viewmodel.MainViewModel

@Composable
fun SynthScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val patch by viewModel.synthPatch.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("OSC", "FILTER", "ENV", "LFO", "ARP", "PATCHES")

    Column(modifier = modifier.fillMaxSize().background(SpBlack)) {
        // Tab bar
        Row(
            modifier = Modifier.fillMaxWidth().background(SpDarkSurface).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { i, tab ->
                Box(
                    modifier = Modifier
                        .background(if (selectedTab == i) SpRed else SpSurface, RoundedCornerShape(3.dp))
                        .clickable { selectedTab = i }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(tab, fontSize = 11.sp, color = SpText, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        when (selectedTab) {
            0 -> OscillatorTab(patch, viewModel)
            1 -> FilterTab(patch, viewModel)
            2 -> EnvelopeTab(patch, viewModel)
            3 -> LFOTab(patch, viewModel)
            4 -> ArpTab(patch, viewModel)
            5 -> PatchesTab(viewModel)
        }

        Spacer(Modifier.weight(1f))

        // Mini keyboard
        MiniKeyboard(onNoteOn = { note, vel -> viewModel.synthNoteOn(note, vel) }, onNoteOff = { viewModel.synthNoteOff(it) })
    }
}

@Composable
private fun OscillatorTab(patch: SynthPatch, vm: MainViewModel) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OscColumn("OSC 1", patch.osc1Wave, patch.osc1Vol, SpRed,
            onWave = { vm.updatePatch(patch.copy(osc1Wave = it)) },
            onVol = { vm.updatePatch(patch.copy(osc1Vol = it)) })
        OscColumn("OSC 2", patch.osc2Wave, patch.osc2Vol, SpOrange,
            onWave = { vm.updatePatch(patch.copy(osc2Wave = it)) },
            onVol = { vm.updatePatch(patch.copy(osc2Vol = it)) },
            detune = patch.osc2Detune.toFloat(), onDetune = { vm.updatePatch(patch.copy(osc2Detune = it.toDouble())) })
        OscColumn("OSC 3", patch.osc3Wave, patch.osc3Vol, SpAmber,
            onWave = { vm.updatePatch(patch.copy(osc3Wave = it)) },
            onVol = { vm.updatePatch(patch.copy(osc3Vol = it)) },
            detune = patch.osc3Detune.toFloat(), onDetune = { vm.updatePatch(patch.copy(osc3Detune = it.toDouble())) })
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SUB OSC", fontSize = 9.sp, color = SpTextDim)
            LabeledKnob(patch.subVol, { vm.updatePatch(patch.copy(subVol = it)) }, "LEVEL", color = SpPurple)
        }
    }
}

@Composable
private fun OscColumn(
    label: String, wave: WaveShape, vol: Float, color: Color,
    onWave: (WaveShape) -> Unit, onVol: (Float) -> Unit,
    detune: Float? = null, onDetune: ((Float) -> Unit)? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            WaveShape.values().forEach { ws ->
                Box(
                    modifier = Modifier.size(28.dp)
                        .background(if (wave == ws) color else SpSurface, RoundedCornerShape(3.dp))
                        .clickable { onWave(ws) },
                    contentAlignment = Alignment.Center
                ) { Text(ws.name.take(3), fontSize = 7.sp, color = SpText) }
            }
        }
        LabeledKnob(vol, onVol, "VOL", color = color)
        if (detune != null && onDetune != null) {
            LabeledKnob(detune, onDetune, "DETUNE", color = color, min = -100f, max = 100f, displayValue = "%+.0f".format(detune))
        }
    }
}

@Composable
private fun FilterTab(patch: SynthPatch, vm: MainViewModel) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterMode.values().forEach { mode ->
                Box(
                    modifier = Modifier
                        .background(if (patch.filterMode == mode) SpCyan else SpSurface, RoundedCornerShape(3.dp))
                        .clickable { vm.updatePatch(patch.copy(filterMode = mode)) }
                        .padding(8.dp, 4.dp)
                ) { Text(mode.name, fontSize = 9.sp, color = SpText) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledKnob(patch.filterCutoff, { vm.updatePatch(patch.copy(filterCutoff = it)) }, "CUTOFF", color = SpCyan)
            LabeledKnob(patch.filterResonance, { vm.updatePatch(patch.copy(filterResonance = it)) }, "RESO", color = SpCyan)
            LabeledKnob(patch.filterEnvAmount, { vm.updatePatch(patch.copy(filterEnvAmount = it)) }, "ENV AMT", color = SpAmber, min = -1f, max = 1f)
        }
    }
}

@Composable
private fun EnvelopeTab(patch: SynthPatch, vm: MainViewModel) {
    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("AMP ENVELOPE", fontSize = 10.sp, color = SpGreen, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledKnob(patch.ampAttack, { vm.updatePatch(patch.copy(ampAttack = it)) }, "ATK", color = SpGreen, min = 0f, max = 5f)
                LabeledKnob(patch.ampDecay, { vm.updatePatch(patch.copy(ampDecay = it)) }, "DEC", color = SpGreen, min = 0f, max = 5f)
                LabeledKnob(patch.ampSustain, { vm.updatePatch(patch.copy(ampSustain = it)) }, "SUS", color = SpGreen)
                LabeledKnob(patch.ampRelease, { vm.updatePatch(patch.copy(ampRelease = it)) }, "REL", color = SpGreen, min = 0f, max = 10f)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FILTER ENVELOPE", fontSize = 10.sp, color = SpCyan, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledKnob(patch.filterAttack, { vm.updatePatch(patch.copy(filterAttack = it)) }, "ATK", color = SpCyan, min = 0f, max = 5f)
                LabeledKnob(patch.filterDecay, { vm.updatePatch(patch.copy(filterDecay = it)) }, "DEC", color = SpCyan, min = 0f, max = 5f)
                LabeledKnob(patch.filterSustain, { vm.updatePatch(patch.copy(filterSustain = it)) }, "SUS", color = SpCyan)
                LabeledKnob(patch.filterRelease, { vm.updatePatch(patch.copy(filterRelease = it)) }, "REL", color = SpCyan, min = 0f, max = 10f)
            }
        }
        LabeledKnob(patch.portamento, { vm.updatePatch(patch.copy(portamento = it, portamentoEnabled = it > 0.001f)) }, "PORTA", color = SpPurple, min = 0f, max = 2f)
    }
}

@Composable
private fun LFOTab(patch: SynthPatch, vm: MainViewModel) {
    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        LFOColumn("LFO 1", patch.lfo1Shape, patch.lfo1Rate, patch.lfo1Depth, patch.lfo1Target, SpOrange,
            onShape = { vm.updatePatch(patch.copy(lfo1Shape = it)) },
            onRate = { vm.updatePatch(patch.copy(lfo1Rate = it)) },
            onDepth = { vm.updatePatch(patch.copy(lfo1Depth = it)) },
            onTarget = { vm.updatePatch(patch.copy(lfo1Target = it)) })
        LFOColumn("LFO 2", patch.lfo2Shape, patch.lfo2Rate, patch.lfo2Depth, patch.lfo2Target, SpPurple,
            onShape = { vm.updatePatch(patch.copy(lfo2Shape = it)) },
            onRate = { vm.updatePatch(patch.copy(lfo2Rate = it)) },
            onDepth = { vm.updatePatch(patch.copy(lfo2Depth = it)) },
            onTarget = { vm.updatePatch(patch.copy(lfo2Target = it)) })
    }
}

@Composable
private fun LFOColumn(
    label: String, shape: LFOShape, rate: Float, depth: Float, target: LFOTarget, color: Color,
    onShape: (LFOShape) -> Unit, onRate: (Float) -> Unit, onDepth: (Float) -> Unit, onTarget: (LFOTarget) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            LFOShape.values().forEach { s ->
                Box(Modifier.size(30.dp).background(if (shape == s) color else SpSurface, RoundedCornerShape(3.dp)).clickable { onShape(s) }, contentAlignment = Alignment.Center) {
                    Text(s.name.take(3), fontSize = 7.sp, color = SpText)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledKnob(rate, onRate, "RATE", color = color, min = 0.1f, max = 20f)
            LabeledKnob(depth, onDepth, "DEPTH", color = color)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            LFOTarget.values().forEach { t ->
                Box(Modifier.background(if (target == t) color else SpSurface, RoundedCornerShape(3.dp)).clickable { onTarget(t) }.padding(4.dp, 2.dp)) {
                    Text(t.name.take(4), fontSize = 7.sp, color = SpText)
                }
            }
        }
    }
}

@Composable
private fun ArpTab(patch: SynthPatch, vm: MainViewModel) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ArpMode.values().forEach { mode ->
                Box(Modifier.background(if (patch.arpMode == mode) SpGreen else SpSurface, RoundedCornerShape(3.dp)).clickable { vm.updatePatch(patch.copy(arpMode = mode)) }.padding(8.dp, 4.dp)) {
                    Text(mode.name, fontSize = 9.sp, color = if (patch.arpMode == mode) SpBlack else SpText)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledKnob(patch.arpRate, { vm.updatePatch(patch.copy(arpRate = it)) }, "RATE", color = SpGreen, min = 0.25f, max = 16f, displayValue = "%.2f".format(patch.arpRate))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CHORD", fontSize = 9.sp, color = SpTextDim)
            Box(Modifier.background(if (patch.chordMode) SpPurple else SpSurface, RoundedCornerShape(3.dp)).clickable { vm.updatePatch(patch.copy(chordMode = !patch.chordMode)) }.padding(8.dp, 4.dp)) {
                Text(if (patch.chordMode) "ON" else "OFF", fontSize = 9.sp, color = SpText)
            }
        }
    }
}

@Composable
private fun PatchesTab(vm: MainViewModel) {
    val patches by vm.synthPatches.collectAsState()
    val currentPatch by vm.synthPatch.collectAsState()
    LazyRow(contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(patches) { patch ->
            Box(
                Modifier.width(100.dp).height(60.dp)
                    .background(if (patch.name == currentPatch.name) SpRed else SpSurface, RoundedCornerShape(4.dp))
                    .border(0.5.dp, SpSurfaceLight, RoundedCornerShape(4.dp))
                    .clickable { vm.loadPatch(patch) },
                contentAlignment = Alignment.Center
            ) {
                Text(patch.name, fontSize = 10.sp, color = SpText, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun MiniKeyboard(onNoteOn: (Int, Float) -> Unit, onNoteOff: (Int) -> Unit) {
    val octave = remember { mutableStateOf(4) }
    val isBlack = listOf(false, true, false, true, false, false, true, false, true, false, true, false)
    val noteMidi = { whiteIdx: Int -> octave.value * 12 + listOf(0, 2, 4, 5, 7, 9, 11)[whiteIdx % 7] }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(SpDarkSurface)
            .padding(4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Octave selector
        Column(modifier = Modifier.width(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("OCT ${octave.value}", fontSize = 9.sp, color = SpTextDim)
            Row {
                Box(Modifier.size(22.dp).background(SpSurface, RoundedCornerShape(2.dp)).clickable { if (octave.value > 0) octave.value-- }, contentAlignment = Alignment.Center) { Text("−", fontSize = 12.sp, color = SpText) }
                Spacer(Modifier.width(2.dp))
                Box(Modifier.size(22.dp).background(SpSurface, RoundedCornerShape(2.dp)).clickable { if (octave.value < 8) octave.value++ }, contentAlignment = Alignment.Center) { Text("+", fontSize = 12.sp, color = SpText) }
            }
        }

        // White keys
        for (w in 0 until 14) {
            val note = noteMidi(w)
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .background(Color.White, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                    .border(0.5.dp, Color(0xFFAAAAAA), RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                    .pointerInput(note) {
                        detectTapGestures(onPress = {
                            onNoteOn(note, 0.8f)
                            try { awaitRelease() } finally { onNoteOff(note) }
                        })
                    }
            )
        }
    }
}
