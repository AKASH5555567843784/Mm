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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.model.SassyMood
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LocalSassyMood
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlowingVoiceOrb(
    state: AssistantState,
    amplitude: Float,
    isMuted: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    mood: SassyMood = LocalSassyMood.current
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    // Dynamic rotation speed adjusted by the current sassy mood
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(mood.rotationSpeedMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Counter rotation for secondary particle ring
    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween((mood.rotationSpeedMs * 1.4f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbCounterRotation"
    )

    // Dynamic breathing pulse duration based on mood
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(mood.breathingDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingPulse"
    )

    // Dynamic scale driven by audio amplitude, assistant state, and mood multiplier
    val dynamicScale = remember(amplitude, state, breathingPulse, mood) {
        val audioBoost = amplitude * 0.45f * mood.particleSpeedMultiplier
        when (state) {
            AssistantState.SPEAKING -> (1.05f + audioBoost).coerceIn(1.0f, 1.45f)
            AssistantState.LISTENING -> (breathingPulse + audioBoost * 0.7f).coerceIn(0.95f, 1.35f)
            AssistantState.THINKING -> (breathingPulse * 1.03f)
            AssistantState.EXECUTING_TOOL -> 1.18f
            AssistantState.STANDBY -> 0.92f
            else -> 0.96f
        }
    }

    // Dynamic colors with smooth transitions
    val targetPrimary = when {
        isMuted -> Color.Gray
        state == AssistantState.SPEAKING -> mood.primaryColor
        state == AssistantState.LISTENING -> mood.secondaryColor
        state == AssistantState.THINKING -> mood.tertiaryColor
        state == AssistantState.EXECUTING_TOOL -> mood.primaryColor
        state == AssistantState.STANDBY -> mood.secondaryColor.copy(alpha = 0.5f)
        else -> mood.tertiaryColor.copy(alpha = 0.6f)
    }

    val targetGlow = when {
        isMuted -> Color.DarkGray
        state == AssistantState.SPEAKING -> mood.glowColor
        state == AssistantState.LISTENING -> mood.secondaryColor.copy(alpha = 0.85f)
        state == AssistantState.THINKING -> mood.glowColor
        state == AssistantState.EXECUTING_TOOL -> mood.glowColor
        state == AssistantState.STANDBY -> mood.glowColor.copy(alpha = 0.3f)
        else -> mood.glowColor.copy(alpha = 0.4f)
    }

    val primaryColor by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 400),
        label = "OrbPrimaryColor"
    )
    val glowColor by animateColorAsState(
        targetValue = targetGlow,
        animationSpec = tween(durationMillis = 400),
        label = "OrbGlowColor"
    )

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("voice_orb_container"),
        contentAlignment = Alignment.Center
    ) {
        // Multi-layered Canvas for glowing radiant halos and orbital particles
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(dynamicScale)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2.6f

            // 1. Outer radiant mood aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.55f),
                        glowColor.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.6f
                ),
                radius = radius * 1.6f,
                center = center
            )

            // 2. Dynamic primary orbital ring
            val ringRadius = radius * 1.15f
            drawCircle(
                color = primaryColor.copy(alpha = 0.40f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // 3. Secondary outer orbital ring for Savage / Genius / Boss moods
            val outerRingRadius = radius * 1.32f
            drawCircle(
                color = mood.secondaryColor.copy(alpha = 0.22f),
                radius = outerRingRadius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // 4. Primary Orbital particle nodes orbiting around orb
            val numParticles = when (mood) {
                SassyMood.SAVAGE_TEASE, SassyMood.CYBER_GENIUS -> 8
                SassyMood.WITTY_BOSS -> 7
                SassyMood.CHARMING_SASSY -> 6
                SassyMood.CHILL_ZEN -> 4
            }

            for (i in 0 until numParticles) {
                val angleRad = Math.toRadians((rotationAngle + i * (360.0 / numParticles))).toFloat()
                val particleOffset = Offset(
                    x = center.x + ringRadius * cos(angleRad),
                    y = center.y + ringRadius * sin(angleRad)
                )
                drawCircle(
                    color = if (i % 2 == 0) primaryColor else glowColor,
                    radius = 4.5f + (amplitude * 6f),
                    center = particleOffset
                )
            }

            // 5. Outer secondary sparkles rotating in reverse
            for (i in 0 until 4) {
                val angleRad = Math.toRadians((counterRotationAngle + i * 90.0)).toFloat()
                val outerOffset = Offset(
                    x = center.x + outerRingRadius * cos(angleRad),
                    y = center.y + outerRingRadius * sin(angleRad)
                )
                drawCircle(
                    color = mood.tertiaryColor.copy(alpha = 0.75f),
                    radius = 3.0f + (amplitude * 4f),
                    center = outerOffset
                )
            }

            // 6. Inner core luminous sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        glowColor.copy(alpha = 0.85f),
                        DarkSurface
                    ),
                    center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        // Center Action / State Icon Button
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(DarkSurface.copy(alpha = 0.60f))
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
                state == AssistantState.LISTENING -> Icons.Default.Mic
                else -> Icons.Default.Mic
            }
            Icon(
                imageVector = icon,
                contentDescription = "MM Voice Assistant status: ${state.label}, Mood: ${mood.displayName}",
                tint = if (isMuted) Color.Gray else Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

