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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.model.SassyMood
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LocalSassyMood
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * GlowingVoiceOrb:
 * High-quality, Canvas-based organic living plasma orb.
 * Reacts in real-time to audio input/output intensity and dynamically adapts its
 * color palette, orbital geometry, harmonic waves, and particle fields to the Sassy Personality State.
 */
@Composable
fun GlowingVoiceOrb(
    state: AssistantState,
    amplitude: Float,
    isMuted: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    mood: SassyMood = LocalSassyMood.current
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbCanvasTransitions")

    // Continuous smooth rotation for orbital elements and harmonic phase
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(mood.rotationSpeedMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbContinuousRotation"
    )

    // Counter rotation for holographic tick rings and inverse harmonics
    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween((mood.rotationSpeedMs * 1.35f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbCounterRotation"
    )

    // Organic harmonic wave phase
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(mood.breathingDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HarmonicWavePhase"
    )

    // Subtle breathing pulse for idle / standby states
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(mood.breathingDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreathingPulse"
    )

    // Dynamic scale driven by real-time audio amplitude, assistant state, and mood multiplier
    val dynamicScale = remember(amplitude, state, breathingPulse, mood) {
        val audioBoost = amplitude * 0.50f * mood.particleSpeedMultiplier
        when (state) {
            AssistantState.SPEAKING -> (1.05f + audioBoost).coerceIn(1.0f, 1.50f)
            AssistantState.LISTENING -> (breathingPulse + audioBoost * 0.75f).coerceIn(0.95f, 1.40f)
            AssistantState.THINKING -> (breathingPulse * 1.04f)
            AssistantState.EXECUTING_TOOL -> 1.20f
            AssistantState.STANDBY -> 0.90f
            else -> 0.95f
        }
    }

    // Dynamic color interpolation across personality states
    val targetPrimary = when {
        isMuted -> Color(0xFF6B7280)
        state == AssistantState.SPEAKING -> mood.primaryColor
        state == AssistantState.LISTENING -> mood.secondaryColor
        state == AssistantState.THINKING -> mood.tertiaryColor
        state == AssistantState.EXECUTING_TOOL -> mood.primaryColor
        state == AssistantState.STANDBY -> mood.secondaryColor.copy(alpha = 0.6f)
        else -> mood.primaryColor.copy(alpha = 0.75f)
    }

    val targetSecondary = when {
        isMuted -> Color(0xFF4B5563)
        state == AssistantState.SPEAKING -> mood.secondaryColor
        state == AssistantState.LISTENING -> mood.tertiaryColor
        state == AssistantState.THINKING -> mood.primaryColor
        state == AssistantState.EXECUTING_TOOL -> mood.secondaryColor
        state == AssistantState.STANDBY -> mood.tertiaryColor.copy(alpha = 0.5f)
        else -> mood.secondaryColor.copy(alpha = 0.65f)
    }

    val targetGlow = when {
        isMuted -> Color(0xFF374151)
        state == AssistantState.SPEAKING -> mood.glowColor
        state == AssistantState.LISTENING -> mood.secondaryColor.copy(alpha = 0.90f)
        state == AssistantState.THINKING -> mood.glowColor
        state == AssistantState.EXECUTING_TOOL -> mood.glowColor
        state == AssistantState.STANDBY -> mood.glowColor.copy(alpha = 0.35f)
        else -> mood.glowColor.copy(alpha = 0.5f)
    }

    val primaryColor by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 350),
        label = "OrbPrimaryColor"
    )
    val secondaryColor by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(durationMillis = 350),
        label = "OrbSecondaryColor"
    )
    val glowColor by animateColorAsState(
        targetValue = targetGlow,
        animationSpec = tween(durationMillis = 350),
        label = "OrbGlowColor"
    )

    Box(
        modifier = modifier
            .size(260.dp)
            .testTag("voice_orb_container"),
        contentAlignment = Alignment.Center
    ) {
        // High-Quality Custom Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(dynamicScale)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 2.75f
            val audioReactiveMultiplier = (amplitude * 1.5f).coerceIn(0f, 1f)

            // 1. Layer 1: Ambient Diffuse Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (state == AssistantState.SPEAKING) 0.65f else 0.40f),
                        secondaryColor.copy(alpha = 0.22f),
                        glowColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.85f
                ),
                radius = baseRadius * 1.85f,
                center = center
            )

            // 2. Layer 2: Real-time Reactive Soundwave Rays / Filaments (Pulsing when speaking/listening)
            val numFilaments = 24
            val filamentBaseRadius = baseRadius * 1.08f
            val filamentMaxExtension = baseRadius * (0.20f + audioReactiveMultiplier * 0.45f)

            for (i in 0 until numFilaments) {
                val filamentAngle = (i * (360f / numFilaments) + rotationAngle * 0.4f) * (PI.toFloat() / 180f)
                val harmonicOffset = sin(i * 1.2f + wavePhase * 2f) * (audioReactiveMultiplier * 14f)
                val currentLength = filamentBaseRadius + (harmonicOffset.coerceAtLeast(0f)) + (audioReactiveMultiplier * filamentMaxExtension)

                val start = Offset(
                    center.x + filamentBaseRadius * cos(filamentAngle),
                    center.y + filamentBaseRadius * sin(filamentAngle)
                )
                val end = Offset(
                    center.x + currentLength * cos(filamentAngle),
                    center.y + currentLength * sin(filamentAngle)
                )

                val filamentAlpha = (0.25f + (audioReactiveMultiplier * 0.65f)).coerceIn(0.1f, 0.9f)
                drawLine(
                    color = if (i % 2 == 0) primaryColor.copy(alpha = filamentAlpha) else secondaryColor.copy(alpha = filamentAlpha * 0.8f),
                    start = start,
                    end = end,
                    strokeWidth = 2.2f + (audioReactiveMultiplier * 2.0f),
                    cap = StrokeCap.Round
                )
            }

            // 3. Layer 3: Dynamic Harmonic Liquid Plasma Membrane (Clockwise)
            drawFluidPlasmaLayer(
                center = center,
                baseRadius = baseRadius,
                wavePhase = wavePhase,
                amplitude = audioReactiveMultiplier,
                color = primaryColor.copy(alpha = 0.50f),
                pointsCount = 10,
                frequency = 3,
                deformationScale = 12f
            )

            // 4. Layer 4: Counter-rotating Harmonic Plasma Membrane (Secondary accent)
            drawFluidPlasmaLayer(
                center = center,
                baseRadius = baseRadius * 0.94f,
                wavePhase = -wavePhase * 1.2f,
                amplitude = audioReactiveMultiplier,
                color = secondaryColor.copy(alpha = 0.40f),
                pointsCount = 8,
                frequency = 4,
                deformationScale = 9f
            )

            // 5. Layer 5: Inner Core Glowing Nucleus
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor.copy(alpha = 0.90f),
                        glowColor.copy(alpha = 0.80f),
                        DarkSurface
                    ),
                    center = Offset(center.x - baseRadius * 0.22f, center.y - baseRadius * 0.22f),
                    radius = baseRadius * 0.88f
                ),
                radius = baseRadius * 0.88f,
                center = center
            )

            // 6. Layer 6: Holographic Segmented Arc Ring (High-Tech Gyro)
            val ringRadius = baseRadius * 1.25f
            drawCircle(
                color = primaryColor.copy(alpha = 0.35f),
                radius = ringRadius,
                center = center,
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f, 6f, 14f), rotationAngle * 2f)
                )
            )

            // 7. Layer 7: Secondary Outer Constellation Ring
            val outerConstellationRadius = baseRadius * 1.48f
            drawCircle(
                color = glowColor.copy(alpha = 0.20f),
                radius = outerConstellationRadius,
                center = center,
                style = Stroke(
                    width = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 20f), counterRotationAngle)
                )
            )

            // 8. Layer 8: Orbiting Sassy Mood Star-Nodes & Photons
            val numParticles = when (mood) {
                SassyMood.SAVAGE_TEASE, SassyMood.CYBER_GENIUS -> 8
                SassyMood.WITTY_BOSS -> 7
                SassyMood.CHARMING_SASSY -> 6
                SassyMood.CHILL_ZEN -> 5
            }

            for (i in 0 until numParticles) {
                val angleRad = (rotationAngle + i * (360f / numParticles)) * (PI.toFloat() / 180f)
                val orbitalRadius = ringRadius + (sin(angleRad * 2f + wavePhase) * (4f + audioReactiveMultiplier * 8f))
                val particleOffset = Offset(
                    x = center.x + orbitalRadius * cos(angleRad),
                    y = center.y + orbitalRadius * sin(angleRad)
                )

                // Draw luminous photon particle
                val pRadius = 3.5f + (audioReactiveMultiplier * 5.5f) + (i % 2) * 1.5f
                drawCircle(
                    color = Color.White,
                    radius = pRadius * 0.5f,
                    center = particleOffset
                )
                drawCircle(
                    color = if (i % 2 == 0) primaryColor else glowColor,
                    radius = pRadius,
                    center = particleOffset
                )
            }

            // 9. Layer 9: Outer Counter-Orbiting Stardust Sparkles
            for (i in 0 until 4) {
                val angleRad = (counterRotationAngle + i * 90f) * (PI.toFloat() / 180f)
                val stardustOffset = Offset(
                    x = center.x + outerConstellationRadius * cos(angleRad),
                    y = center.y + outerConstellationRadius * sin(angleRad)
                )
                drawCircle(
                    color = mood.tertiaryColor.copy(alpha = 0.85f),
                    radius = 2.5f + (audioReactiveMultiplier * 4.0f),
                    center = stardustOffset
                )
            }
        }

        // Center Interactive Glassmorphism Icon Hub
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(DarkSurface.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap
                )
                .testTag("orb_touch_target"),
            contentAlignment = Alignment.Center
        ) {
            val icon = when {
                isMuted -> Icons.Default.MicOff
                state == AssistantState.SPEAKING -> Icons.Default.GraphicEq
                state == AssistantState.THINKING -> Icons.Default.Psychology
                state == AssistantState.EXECUTING_TOOL -> Icons.Default.SmartToy
                state == AssistantState.STANDBY -> Icons.Default.Lock
                state == AssistantState.LISTENING -> Icons.Default.Mic
                else -> Icons.Default.Mic
            }
            Icon(
                imageVector = icon,
                contentDescription = "MM Voice Assistant status: ${state.label}, Mood: ${mood.displayName}",
                tint = if (isMuted) Color.Gray else Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

/**
 * Draws an organic undulating fluid plasma membrane using harmonic sine wave deformation.
 */
private fun DrawScope.drawFluidPlasmaLayer(
    center: Offset,
    baseRadius: Float,
    wavePhase: Float,
    amplitude: Float,
    color: Color,
    pointsCount: Int,
    frequency: Int,
    deformationScale: Float
) {
    val path = Path()
    val totalDeformation = deformationScale * (0.6f + amplitude * 1.8f)

    for (i in 0 until pointsCount) {
        val angle = (i * (360f / pointsCount)) * (PI.toFloat() / 180f)
        val radiusMod = baseRadius + sin(angle * frequency + wavePhase) * totalDeformation

        val x = center.x + radiusMod * cos(angle)
        val y = center.y + radiusMod * sin(angle)

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(
        path = path,
        color = color
    )
}
