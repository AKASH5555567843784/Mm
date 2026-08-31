package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Canvas(
        modifier = modifier
            .height(36.dp)
            .width((barCount * 8).dp)
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = 4.dp.toPx()
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
        val spacing = if (barCount > 1) (totalWidth - (barCount * barWidth)) / (barCount - 1) else 4.dp.toPx()

        for (i in 0 until barCount) {
            val offset = i.toFloat() / barCount * (2 * PI).toFloat()
            val waveHeightFraction = if (isActive) {
                val sineVal = (sin(phase + offset) * 0.5f + 0.5f)
                (sineVal * 0.4f + amplitude * 0.9f).coerceIn(0.15f, 1.0f)
            } else {
                0.12f
            }

            val barHeight = maxHeight * waveHeightFraction
            val x = i * (barWidth + spacing)
            val y = (maxHeight - barHeight) / 2f

            if (isActive) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonCyan, NeonViolet, NeonMagenta),
                        startY = y,
                        endY = y + barHeight
                    ),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            } else {
                drawRoundRect(
                    color = Color.DarkGray.copy(alpha = 0.4f),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}
