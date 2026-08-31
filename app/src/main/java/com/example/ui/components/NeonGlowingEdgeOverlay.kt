package com.example.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-precision Neon-style Glowing Gradient Border that illuminates around the entire
 * perimeter of the screen like a holographic frame when MM Assistant is invoked
 * (Power button long-press, bottom-corner swipe, or 'Hello MM' wake word).
 *
 * It dynamically pulses, flows, and reacts to audio/state (Listening, Thinking, Speaking).
 */
@Composable
fun NeonGlowingEdgeOverlay(
    assistantState: AssistantState,
    audioAmplitude: Float = 0f,
    modifier: Modifier = Modifier,
    isEdgeLightingEnabled: Boolean = true
) {
    if (!isEdgeLightingEnabled) return

    val mood = LocalSassyMood.current
    val infiniteTransition = rememberInfiniteTransition(label = "EdgeGlowTransition")

    // Continuous traveling perimeter wave phase (0f..1f)
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (assistantState == AssistantState.SPEAKING) 1800 else 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FlowPhase"
    )

    // Dynamic breathing pulse for the holographic glow aura
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (assistantState) {
                    AssistantState.SPEAKING -> 400
                    AssistantState.LISTENING -> 700
                    AssistantState.THINKING -> 500
                    else -> 1200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val primaryNeon = when (assistantState) {
        AssistantState.LISTENING -> NeonCyan
        AssistantState.THINKING -> NeonAmber
        AssistantState.SPEAKING -> NeonMagenta
        AssistantState.ERROR -> Color(0xFFFF453A)
        else -> mood.primaryColor
    }

    val secondaryNeon = when (assistantState) {
        AssistantState.LISTENING -> NeonViolet
        AssistantState.THINKING -> NeonMagenta
        AssistantState.SPEAKING -> NeonCyan
        AssistantState.ERROR -> NeonAmber
        else -> mood.secondaryColor
    }

    val tertiaryNeon = when (assistantState) {
        AssistantState.SPEAKING -> AccentGreen
        AssistantState.LISTENING -> Color.White
        else -> mood.tertiaryColor
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("neon_glowing_edge_overlay")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0 || height <= 0) return@Canvas

            val strokeIntensity = (pulseScale + (audioAmplitude * 0.8f)).coerceIn(0.6f, 1.8f)

            // Dynamic Traveling Gradient Brush along screen edges
            val startOffset = Offset(
                x = width * ((cos(flowPhase * 2 * Math.PI) + 1) / 2).toFloat(),
                y = height * ((sin(flowPhase * 2 * Math.PI) + 1) / 2).toFloat()
            )
            val endOffset = Offset(
                x = width * (1f - ((cos(flowPhase * 2 * Math.PI) + 1) / 2).toFloat()),
                y = height * (1f - ((sin(flowPhase * 2 * Math.PI) + 1) / 2).toFloat())
            )

            val perimeterBrush = Brush.linearGradient(
                colors = listOf(
                    primaryNeon.copy(alpha = 0.95f),
                    secondaryNeon.copy(alpha = 0.90f),
                    tertiaryNeon.copy(alpha = 0.95f),
                    primaryNeon.copy(alpha = 0.95f)
                ),
                start = startOffset,
                end = endOffset
            )

            val softAuraBrush = Brush.linearGradient(
                colors = listOf(
                    primaryNeon.copy(alpha = 0.25f * strokeIntensity),
                    secondaryNeon.copy(alpha = 0.35f * strokeIntensity),
                    tertiaryNeon.copy(alpha = 0.25f * strokeIntensity)
                ),
                start = startOffset,
                end = endOffset
            )

            // Layer 1: Wide Soft Holographic Aura (Diffused outer glow)
            drawRect(
                brush = softAuraBrush,
                topLeft = Offset(0f, 0f),
                size = Size(width, height),
                style = Stroke(
                    width = 24.dp.toPx() * strokeIntensity,
                    join = StrokeJoin.Round
                )
            )

            // Layer 2: Medium Ambient Glow
            drawRect(
                brush = perimeterBrush,
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(width - 4.dp.toPx(), height - 4.dp.toPx()),
                style = Stroke(
                    width = 10.dp.toPx() * strokeIntensity,
                    join = StrokeJoin.Round
                ),
                alpha = 0.65f
            )

            // Layer 3: Sharp Laser-Cut Core Neon Line
            drawRect(
                brush = perimeterBrush,
                topLeft = Offset(0f, 0f),
                size = Size(width, height),
                style = Stroke(
                    width = 3.5.dp.toPx(),
                    join = StrokeJoin.Miter
                )
            )

            // Layer 4: Corner Futuristic Accents (L-Brackets)
            drawCornerAccents(
                width = width,
                height = height,
                cornerLength = 48.dp.toPx(),
                accentColor = Color.White,
                glowColor = primaryNeon,
                intensity = strokeIntensity
            )
        }
    }
}

/**
 * Draws sharp, futuristic HUD holographic corner accents on the 4 corners of the screen.
 */
private fun DrawScope.drawCornerAccents(
    width: Float,
    height: Float,
    cornerLength: Float,
    accentColor: Color,
    glowColor: Color,
    intensity: Float
) {
    val cornerStroke = 4.5.dp.toPx()
    val inset = 3.dp.toPx()

    val path = Path().apply {
        // Top-Left Corner
        moveTo(inset, inset + cornerLength)
        lineTo(inset, inset)
        lineTo(inset + cornerLength, inset)

        // Top-Right Corner
        moveTo(width - inset - cornerLength, inset)
        lineTo(width - inset, inset)
        lineTo(width - inset, inset + cornerLength)

        // Bottom-Right Corner
        moveTo(width - inset, height - inset - cornerLength)
        lineTo(width - inset, height - inset)
        lineTo(width - inset - cornerLength, height - inset)

        // Bottom-Left Corner
        moveTo(inset + cornerLength, height - inset)
        lineTo(inset, height - inset)
        lineTo(inset, height - inset - cornerLength)
    }

    // Glow backing for corner brackets
    drawPath(
        path = path,
        color = glowColor.copy(alpha = (0.7f * intensity).coerceIn(0f, 1f)),
        style = Stroke(width = cornerStroke * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Miter)
    )

    // Crisp white core for corner brackets
    drawPath(
        path = path,
        color = accentColor.copy(alpha = 0.95f),
        style = Stroke(width = cornerStroke, cap = StrokeCap.Square, join = StrokeJoin.Miter)
    )
}
