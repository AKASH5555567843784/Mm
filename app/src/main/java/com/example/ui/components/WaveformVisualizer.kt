package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")

    Row(
        modifier = modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val animDuration = 400 + (i % 5) * 120
            val waveHeight by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(animDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BarHeight$i"
            )

            val dynamicHeightFraction = if (isActive) {
                (waveHeight * 0.4f + amplitude * 0.9f).coerceIn(0.15f, 1.0f)
            } else {
                0.12f
            }

            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    NeonCyan,
                    NeonViolet,
                    NeonMagenta
                )
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((36 * dynamicHeightFraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) barBrush else Brush.verticalGradient(listOf(Color.DarkGray, Color.Black)))
            )
        }
    }
}
