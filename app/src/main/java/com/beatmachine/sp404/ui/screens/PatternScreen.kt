package com.beatmachine.sp404.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import com.beatmachine.sp404.model.Pattern
import com.beatmachine.sp404.ui.components.LabeledKnob
import com.beatmachine.sp404.ui.theme.*
import com.beatmachine.sp404.viewmodel.MainViewModel

@Composable
fun PatternScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val pattern = uiState.patterns[uiState.currentPatternIndex]
    val currentStep = uiState.currentStep
    val currentBank = uiState.banks[uiState.currentBankIndex]

    Row(modifier = modifier.fillMaxSize()) {
        // Pattern list
        Column(
            modifier = Modifier
                .width(90.dp)
                .fillMaxHeight()
                .background(SpDarkSurface)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text("PATTERNS", fontSize = 8.sp, color = SpTextDim, modifier = Modifier.padding(bottom = 4.dp))
            uiState.patterns.forEachIndexed { i, pat ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (i == uiState.currentPatternIndex) SpRed else SpSurface,
                            RoundedCornerShape(3.dp)
                        )
                        .clickable { viewModel.selectPattern(i) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(pat.name, fontSize = 9.sp, color = SpText, maxLines = 1)
                }
            }
        }

        // Main sequencer
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().background(SpBlack).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Pattern controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(pattern.name, fontSize = 12.sp, color = SpText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                // Steps selector
                listOf(8, 16, 32).forEach { steps ->
                    Box(
                        modifier = Modifier
                            .background(if (pattern.length == steps) SpCyan else SpSurface, RoundedCornerShape(3.dp))
                            .clickable { viewModel.setPatternLength(steps) }
                            .padding(8.dp, 4.dp)
                    ) { Text("$steps", fontSize = 9.sp, color = SpText) }
                }
                // Swing knob
                LabeledKnob(
                    value = pattern.swing,
                    onValueChange = { viewModel.setPatternSwing(it) },
                    label = "SWING",
                    color = SpOrange,
                    modifier = Modifier.width(60.dp)
                )
            }

            // Step sequencer grid — one row per pad
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                currentBank.pads.forEachIndexed { padIdx, pad ->
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pad label
                        Text(
                            text = pad.name.take(4),
                            fontSize = 7.sp,
                            color = Color(pad.color.hex),
                            modifier = Modifier.width(28.dp),
                            maxLines = 1,
                            textAlign = TextAlign.End
                        )

                        // Steps
                        val sequence = pattern.padSequences[padIdx]
                        for (stepIdx in 0 until pattern.length) {
                            val step = sequence.steps[stepIdx]
                            val isCurrentStep = stepIdx == currentStep && uiState.isPlaying
                            val isBeat = stepIdx % 4 == 0
                            StepButton(
                                active = step.active,
                                isCurrent = isCurrentStep,
                                isBeat = isBeat,
                                padColor = Color(pad.color.hex),
                                velocity = step.velocity,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onToggle = { viewModel.toggleStep(padIdx, stepIdx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepButton(
    active: Boolean,
    isCurrent: Boolean,
    isBeat: Boolean,
    padColor: Color,
    velocity: Float,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val bgColor = when {
        isCurrent && active -> padColor
        isCurrent -> SpSurface.copy(alpha = 0.8f)
        active -> padColor.copy(alpha = 0.7f * velocity)
        isBeat -> Color(0xFF222222)
        else -> Color(0xFF161616)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(2.dp))
            .border(
                width = if (isCurrent) 1.dp else 0.5.dp,
                color = if (isCurrent) padColor else if (isBeat) SpSurface else Color(0xFF222222),
                shape = RoundedCornerShape(2.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggle() })
            }
    )
}
