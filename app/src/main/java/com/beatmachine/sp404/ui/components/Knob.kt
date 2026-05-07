package com.beatmachine.sp404.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatmachine.sp404.ui.theme.*
import kotlin.math.*

@Composable
fun Knob(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    size: Dp = 56.dp,
    color: Color = SpRed,
    min: Float = 0f,
    max: Float = 1f,
    sensitivity: Float = 0.003f
) {
    val normalized = ((value - min) / (max - min)).coerceIn(0f, 1f)
    var dragStart by remember { mutableStateOf(0f) }
    var valueAtDragStart by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragStart = it.y; valueAtDragStart = value },
                        onDrag = { change, _ ->
                            val delta = (dragStart - change.position.y) * sensitivity * (max - min)
                            onValueChange((valueAtDragStart + delta).coerceIn(min, max))
                        }
                    )
                }
        ) {
            drawKnob(normalized, color)
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 8.sp,
                color = SpTextDim,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.width(size)
            )
        }
    }
}

private fun DrawScope.drawKnob(normalized: Float, color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = size.minDimension / 2f - 4f

    // Track arc
    drawArc(
        color = Color(0xFF333333),
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        style = Stroke(width = 3f, cap = StrokeCap.Round),
        topLeft = Offset(cx - radius, cy - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
    )

    // Value arc
    drawArc(
        color = color,
        startAngle = 135f,
        sweepAngle = 270f * normalized,
        useCenter = false,
        style = Stroke(width = 3f, cap = StrokeCap.Round),
        topLeft = Offset(cx - radius, cy - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
    )

    // Body
    drawCircle(color = Color(0xFF2A2A2A), radius = radius - 5f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF1A1A1A), radius = radius - 8f, center = Offset(cx, cy))

    // Pointer line
    val angle = (135f + 270f * normalized) * PI.toFloat() / 180f
    val lineLen = radius - 10f
    val lineX = cx + lineLen * cos(angle)
    val lineY = cy + lineLen * sin(angle)
    drawLine(color = color, start = Offset(cx, cy), end = Offset(lineX, lineY), strokeWidth = 2f, cap = StrokeCap.Round)
}

@Composable
fun LabeledKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = SpRed,
    min: Float = 0f,
    max: Float = 1f,
    displayValue: String = "%.2f".format(value)
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = label, fontSize = 9.sp, color = SpTextDim, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Knob(value = value, onValueChange = onValueChange, color = color, size = 48.dp, min = min, max = max)
        Text(text = displayValue, fontSize = 8.sp, color = SpAmber, textAlign = TextAlign.Center)
    }
}
