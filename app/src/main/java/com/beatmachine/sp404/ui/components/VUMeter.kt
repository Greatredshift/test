package com.beatmachine.sp404.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beatmachine.sp404.ui.theme.*
import kotlin.math.log10
import kotlin.math.max

@Composable
fun VUMeter(
    levelL: Float,
    levelR: Float,
    modifier: Modifier = Modifier
) {
    val animL by animateFloatAsState(levelL.coerceIn(0f, 1f), label = "vuL")
    val animR by animateFloatAsState(levelR.coerceIn(0f, 1f), label = "vuR")

    Canvas(modifier = modifier.width(16.dp).height(80.dp)) {
        val barW = size.width / 2f - 1f
        val barH = size.height

        drawMeterBar(animL, 0f, barW, barH)
        drawMeterBar(animR, barW + 2f, barW, barH)
    }
}

private fun vuColor(level: Float): Color = when {
    level > 0.9f -> SpRedBright
    level > 0.7f -> SpAmber
    else -> SpGreen
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMeterBar(
    level: Float, x: Float, w: Float, h: Float
) {
    // Background
    drawRect(color = Color(0xFF111111), topLeft = Offset(x, 0f), size = Size(w, h))
    // Fill
    val fillH = h * level
    drawRect(
        color = vuColor(level),
        topLeft = Offset(x, h - fillH),
        size = Size(w, fillH)
    )
    // Peak segments
    val segments = 20
    for (i in 0 until segments) {
        val y = i * (h / segments)
        drawLine(color = Color(0xFF0D0D0D), start = Offset(x, y), end = Offset(x + w, y), strokeWidth = 1f)
    }
}
