package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.model.SassyMood
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * WaveformVisualizer:
 * Animated multi-bar waveform combined with a reactive pulse wave glow effect.
 * Reacts dynamically with live audio amplitude when the assistant is 'speaking' or 'listening'.
 */
@Composable
fun WaveformVisualizer(
    amplitude: Float,
    isActive: Boolean = true,
    state: AssistantState = AssistantState.DISCONNECTED,
    modifier: Modifier = Modifier,
    barCount: Int = 22,
    mood: SassyMood = LocalSassyMood.current
) {
    val isSpeaking = state == AssistantState.SPEAKING || (isActive && amplitude > 0.05f)
    val isListening = state == AssistantState.LISTENING || (isActive && !isSpeaking)
    val isEffectActive = isActive || state == AssistantState.SPEAKING || state == AssistantState.LISTENING

    val infiniteTransition = rememberInfiniteTransition(label = "WaveformPulseAnim")

    // Continuous wave phase motion
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 850 else 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // Pulse expanding ripple animation
    val pulseExpansion by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 600 else 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseExpansion"
    )

    // Secondary pulse harmonic
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 600 else 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // State-aware dynamic color scheme
    val targetPrimary = when {
        state == AssistantState.SPEAKING -> mood.primaryColor
        state == AssistantState.LISTENING -> mood.secondaryColor
        isEffectActive -> NeonCyan
        else -> Color.Gray
    }
    val targetSecondary = when {
        state == AssistantState.SPEAKING -> mood.secondaryColor
        state == AssistantState.LISTENING -> NeonViolet
        isEffectActive -> NeonMagenta
        else -> Color.DarkGray
    }
    val targetTertiary = when {
        state == AssistantState.SPEAKING -> mood.glowColor
        state == AssistantState.LISTENING -> NeonCyan
        isEffectActive -> NeonViolet
        else -> Color(0xFF333333)
    }

    val primaryColor by animateColorAsState(targetValue = targetPrimary, animationSpec = tween(300), label = "WavePrimary")
    val secondaryColor by animateColorAsState(targetValue = targetSecondary, animationSpec = tween(300), label = "WaveSecondary")
    val tertiaryColor by animateColorAsState(targetValue = targetTertiary, animationSpec = tween(300), label = "WaveTertiary")

    Box(
        modifier = modifier
            .height(52.dp)
            .width((barCount * 10).coerceAtLeast(200).dp)
            .padding(horizontal = 8.dp)
            .testTag("waveform_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val maxHeight = size.height
            val centerY = maxHeight / 2f
            val barWidth = 3.5.dp.toPx()
            val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            val spacing = if (barCount > 1) (totalWidth - (barCount * barWidth)) / (barCount - 1) else 4.dp.toPx()

            val effectiveAmp = if (isEffectActive) amplitude.coerceIn(0.12f, 1.0f) else 0.05f

            // 1. Background Pulse Aura Wave (ambient reactive glow behind bars)
            if (isEffectActive) {
                val glowRadiusX = totalWidth * 0.45f * pulseExpansion
                val glowRadiusY = maxHeight * 0.75f * (0.8f + effectiveAmp * 0.4f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.22f * pulseAlpha),
                            secondaryColor.copy(alpha = 0.10f * pulseAlpha),
                            Color.Transparent
                        ),
                        center = Offset(totalWidth / 2f, centerY),
                        radius = glowRadiusX
                    ),
                    topLeft = Offset(totalWidth / 2f - glowRadiusX, centerY - glowRadiusY),
                    size = Size(glowRadiusX * 2, glowRadiusY * 2)
                )

                // Horizontal flowing sine pulse filament
                val path = Path()
                val steps = 40
                for (s in 0..steps) {
                    val x = (s.toFloat() / steps) * totalWidth
                    val relX = (s.toFloat() / steps) * 2 * PI.toFloat()
                    val waveAmp = (sin(relX * 2f + wavePhase) * (maxHeight * 0.22f * effectiveAmp))
                    val y = centerY + waveAmp
                    if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = tertiaryColor.copy(alpha = 0.35f * pulseAlpha),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 2. Multi-Bar Dynamic Frequency Waveform
            for (i in 0 until barCount) {
                val normalizedIndex = i.toFloat() / (barCount - 1).coerceAtLeast(1)
                val centerDistance = kotlin.math.abs(normalizedIndex - 0.5f) * 2f // 0 at center, 1 at ends
                val centerWeight = 1.0f - (centerDistance * 0.45f) // Center bars are naturally taller

                val barOffset = i.toFloat() * 0.45f
                val waveHeightFraction = if (isEffectActive) {
                    val primarySine = sin(wavePhase + barOffset) * 0.5f + 0.5f
                    val secondarySine = cos(wavePhase * 1.5f + barOffset * 0.8f) * 0.3f
                    val harmonic = (primarySine + secondarySine).coerceIn(0f, 1f)

                    val dynamicHeight = (harmonic * 0.45f + effectiveAmp * 0.85f) * centerWeight
                    dynamicHeight.coerceIn(0.12f, 1.0f)
                } else {
                    0.10f * centerWeight
                }

                val barHeight = (maxHeight * waveHeightFraction).coerceAtLeast(3.dp.toPx())
                val x = i * (barWidth + spacing)
                val y = centerY - (barHeight / 2f)

                if (isEffectActive) {
                    val barColors = if (isSpeaking) {
                        listOf(primaryColor, secondaryColor, tertiaryColor)
                    } else {
                        listOf(secondaryColor, primaryColor, secondaryColor)
                    }

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = barColors,
                            startY = y,
                            endY = y + barHeight
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                } else {
                    drawRoundRect(
                        color = Color.DarkGray.copy(alpha = 0.35f),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }
    }
}

