package com.beatmachine.sp404.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.beatmachine.sp404.ui.components.TransportBar
import com.beatmachine.sp404.ui.theme.SpBlack
import com.beatmachine.sp404.viewmodel.MainViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatmachine.sp404.ui.theme.*

enum class MainTab { PADS, SYNTH, EFFECTS, PATTERN }

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val vuLevel by viewModel.vuLevel.collectAsState()
    var currentTab by remember { mutableStateOf(MainTab.PADS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBlack)
    ) {
        // Transport bar across the top
        TransportBar(
            bpm = uiState.bpm,
            onBpmChange = { viewModel.setBpm(it) },
            isPlaying = uiState.isPlaying,
            isRecording = uiState.isRecording,
            onPlayStop = { viewModel.togglePlayStop() },
            onRecord = { viewModel.toggleRecord() },
            vuL = vuLevel.first,
            vuR = vuLevel.second
        )

        // Tab navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(SpDarkSurface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.values().forEach { tab ->
                val isSelected = currentTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isSelected) SpRed else Color.Transparent)
                        .clickable { currentTab = tab },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) SpText else SpTextDim
                    )
                }
            }
        }

        // Screen content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentTab) {
                MainTab.PADS -> PadGridScreen(viewModel, Modifier.fillMaxSize())
                MainTab.SYNTH -> SynthScreen(viewModel, Modifier.fillMaxSize())
                MainTab.EFFECTS -> EffectsScreen(viewModel, Modifier.fillMaxSize())
                MainTab.PATTERN -> PatternScreen(viewModel, Modifier.fillMaxSize())
            }
        }
    }
}
