package com.beatmachine.sp404.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatmachine.sp404.model.Pad
import com.beatmachine.sp404.model.PadColor
import com.beatmachine.sp404.ui.theme.*

@Composable
fun PadButton(
    pad: Pad,
    isActive: Boolean,
    isRecording: Boolean,
    stepActive: Boolean,
    onPress: (Pad) -> Unit,
    onRelease: (Pad) -> Unit,
    onLongPress: (Pad) -> Unit,
    modifier: Modifier = Modifier
) {
    val padColor = Color(pad.color.hex)
    val bgColor by animateColorAsState(
        when {
            isRecording -> SpRed
            isActive -> padColor.copy(alpha = 1f)
            stepActive -> padColor.copy(alpha = 0.6f)
            pad.isEmpty -> SpSurface
            else -> padColor.copy(alpha = 0.25f)
        },
        label = "padBg"
    )
    val scale by animateFloatAsState(if (isActive) 0.93f else 1f, label = "padScale")
    val textColor = if (bgColor.luminance() > 0.3f) SpBlack else SpText

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(
                width = if (stepActive) 1.5.dp else 0.5.dp,
                color = if (stepActive) padColor else Color(0xFF444444),
                shape = RoundedCornerShape(6.dp)
            )
            .pointerInput(pad) {
                detectTapGestures(
                    onPress = { _ ->
                        onPress(pad)
                        try { awaitRelease() } finally { onRelease(pad) }
                    },
                    onLongPress = { onLongPress(pad) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
            Text(
                text = pad.name.take(6),
                fontSize = 8.sp,
                color = if (pad.isEmpty) SpTextDisabled else textColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!pad.isEmpty) {
                Text(
                    text = pad.sampleName.take(8).ifEmpty { if (pad.useSynth) "SYNTH" else "" },
                    fontSize = 6.sp,
                    color = textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (pad.isEmpty) {
                Text("─", fontSize = 10.sp, color = SpTextDisabled)
            }
        }
        // Mute indicator
        if (pad.isMuted) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            Text("M", modifier = Modifier.align(Alignment.TopEnd).padding(2.dp), fontSize = 7.sp, color = SpAmber, fontWeight = FontWeight.Bold)
        }
    }
}
