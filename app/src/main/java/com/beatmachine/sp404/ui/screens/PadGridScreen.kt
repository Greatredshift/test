package com.beatmachine.sp404.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatmachine.sp404.model.Pad
import com.beatmachine.sp404.ui.components.PadButton
import com.beatmachine.sp404.ui.theme.*
import com.beatmachine.sp404.viewmodel.MainViewModel

@Composable
fun PadGridScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val currentBank = uiState.banks[uiState.currentBankIndex]

    Row(modifier = modifier.fillMaxSize()) {
        // Bank selector (vertical A-J)
        BankSelector(
            banks = uiState.banks.map { it.name },
            selected = uiState.currentBankIndex,
            onSelect = { viewModel.selectBank(it) }
        )

        // 4x4 pad grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until 4) {
                        val padIndex = row * 4 + col
                        val pad = currentBank.pads[padIndex]
                        PadButton(
                            pad = pad,
                            isActive = uiState.activePads.contains("${uiState.currentBankIndex}_$padIndex"),
                            isRecording = uiState.recordingPad == padIndex,
                            stepActive = uiState.currentStep == padIndex,
                            onPress = { viewModel.padPressed(it) },
                            onRelease = { viewModel.padReleased(it) },
                            onLongPress = { viewModel.padLongPress(it) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }

        // Pad controls sidebar
        PadControlsSidebar(
            selectedPad = uiState.selectedPad?.let { currentBank.pads[it] },
            onVolumeChange = { viewModel.setPadVolume(it) },
            onPanChange = { viewModel.setPadPan(it) },
            onPitchChange = { viewModel.setPadPitch(it) }
        )
    }
}

@Composable
private fun BankSelector(
    banks: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .background(SpDarkSurface)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        banks.forEachIndexed { index, name ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        if (selected == index) BankColors[index] else SpSurface,
                        RoundedCornerShape(3.dp)
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected == index) SpBlack else SpTextDim
                )
            }
        }
    }
}

@Composable
private fun PadControlsSidebar(
    selectedPad: Pad?,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(SpDarkSurface)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("PAD", fontSize = 9.sp, color = SpTextDim, fontWeight = FontWeight.Bold)
        if (selectedPad != null) {
            com.beatmachine.sp404.ui.components.LabeledKnob(
                value = selectedPad.volume, onValueChange = onVolumeChange,
                label = "VOL", color = SpGreen
            )
            com.beatmachine.sp404.ui.components.LabeledKnob(
                value = (selectedPad.pan + 1f) / 2f,
                onValueChange = { onPanChange(it * 2f - 1f) },
                label = "PAN", color = SpCyan,
                displayValue = "%.0f".format(selectedPad.pan * 100f)
            )
            com.beatmachine.sp404.ui.components.LabeledKnob(
                value = (selectedPad.pitch + 24f) / 48f,
                onValueChange = { onPitchChange(it * 48f - 24f) },
                label = "PITCH", color = SpPurple,
                displayValue = "%+.0f".format(selectedPad.pitch)
            )
        } else {
            Text("─", fontSize = 14.sp, color = SpTextDisabled)
        }
    }
}
