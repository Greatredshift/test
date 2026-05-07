package com.beatmachine.sp404.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatmachine.sp404.ui.theme.*

@Composable
fun TransportBar(
    bpm: Double,
    onBpmChange: (Double) -> Unit,
    isPlaying: Boolean,
    isRecording: Boolean,
    onPlayStop: () -> Unit,
    onRecord: () -> Unit,
    vuL: Float,
    vuR: Float,
    modifier: Modifier = Modifier
) {
    var bpmDragStart by remember { mutableStateOf(0f) }
    var bpmAtDragStart by remember { mutableStateOf(bpm) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(SpDarkSurface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Logo
        Text(
            "SP-404",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = SpRed
        )

        // BPM Display (draggable)
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(36.dp)
                .background(SpBlack, RoundedCornerShape(4.dp))
                .border(1.dp, SpSurface, RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { bpmDragStart = it.x; bpmAtDragStart = bpm },
                        onDrag = { change, _ ->
                            val delta = (change.position.x - bpmDragStart) * 0.3
                            onBpmChange((bpmAtDragStart + delta).coerceIn(40.0, 300.0))
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BPM", fontSize = 7.sp, color = SpTextDim)
                Text("%.1f".format(bpm), fontSize = 16.sp, color = SpAmber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // Play button
        TransportButton(
            onClick = onPlayStop,
            color = if (isPlaying) SpGreen else SpSurfaceLight
        ) {
            if (isPlaying) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = SpBlack, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = SpGreen, modifier = Modifier.size(20.dp))
            }
        }

        // Record button
        TransportButton(
            onClick = onRecord,
            color = if (isRecording) SpRed else SpSurfaceLight
        ) {
            Icon(Icons.Default.FiberManualRecord, contentDescription = "Rec",
                tint = if (isRecording) SpText else SpRed,
                modifier = Modifier.size(16.dp))
        }

        Spacer(Modifier.weight(1f))

        // VU meters
        VUMeter(levelL = vuL, levelR = vuR, modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun TransportButton(onClick: () -> Unit, color: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
