package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.model.SassyMood
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlin.math.PI
import kotlin.math.sin

/**
 * MinimalActiveListeningScreen:
 * A sleek, high-fidelity minimalist Jetpack Compose screen that displays a continuous,
 * responsive visual audio-wave animation when the assistant is actively listening
 * or responding to wake-word triggers.
 */
@Composable
fun MinimalActiveListeningScreen(
    assistantState: AssistantState,
    audioAmplitude: Float,
    sassyQuote: String,
    sassyMood: SassyMood,
    isMuted: Boolean,
    isServiceRunning: Boolean,
    onToggleMute: () -> Unit,
    onMicTap: () -> Unit,
    onSwitchToDashboard: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val isActivelyListening = assistantState == AssistantState.LISTENING ||
            assistantState == AssistantState.SPEAKING ||
            assistantState == AssistantState.THINKING

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        sassyMood.backgroundAccent.copy(alpha = 0.6f),
                        DarkBackground
                    )
                )
            )
            .testTag("minimal_active_listening_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding + 12.dp, bottom = navBarPadding + 16.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Minimal Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active status pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurface.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, sassyMood.primaryColor.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("minimal_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActivelyListening) NeonCyan else Color(0xFFFFB703)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (assistantState) {
                                AssistantState.LISTENING -> "ACTIVELY LISTENING"
                                AssistantState.SPEAKING -> "MM SPEAKING"
                                AssistantState.THINKING -> "PROCESSING..."
                                AssistantState.STANDBY -> "WAKE-WORD READY"
                                else -> "ASSISTANT READY"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Top right control buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("minimal_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Assistant Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onSwitchToDashboard,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("minimal_dashboard_toggle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Switch to Full Dashboard",
                            tint = sassyMood.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 2. Center: Visual Audio-Wave Animation & Interactive Voice Core
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Assistant Title & Mood Indicator
                Text(
                    text = "MM",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 4.sp
                    )
                )

                Text(
                    text = sassyMood.displayName.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = sassyMood.primaryColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Multi-Harmonic Visual Audio-Wave Canvas
                VisualAudioWaveAnimation(
                    amplitude = audioAmplitude,
                    isListening = isActivelyListening,
                    mood = sassyMood,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("visual_audio_wave_canvas")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Equalizer Bar Visualizer Row
                AudioEqualizerWaveRow(
                    amplitude = audioAmplitude,
                    isActive = isActivelyListening,
                    mood = sassyMood,
                    modifier = Modifier.testTag("audio_equalizer_wave_row")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Sassy Subtitle / Instruction Callout
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, sassyMood.primaryColor.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (sassyQuote.isNotBlank()) "\"$sassyQuote\"" else "\"Say 'Hey MM' or speak your request, Boss.\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isMuted) "Microphone is muted" else "Auto wake-word listening via WorkManager active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isMuted) Color(0xFFFF5964) else TextTertiary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // 3. Bottom Minimal Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute toggle
                Surface(
                    shape = CircleShape,
                    color = if (isMuted) Color(0xFFFF5964).copy(alpha = 0.2f) else DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isMuted) Color(0xFFFF5964) else TextTertiary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .clickable { onToggleMute() }
                        .testTag("minimal_mute_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isMuted) "Unmute Microphone" else "Mute Microphone",
                            tint = if (isMuted) Color(0xFFFF5964) else TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Central Mic Activation Button
                Surface(
                    shape = CircleShape,
                    color = sassyMood.primaryColor,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .clickable { onMicTap() }
                        .testTag("minimal_mic_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Tap to speak",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Switch to Dashboard
                Surface(
                    shape = CircleShape,
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, sassyMood.secondaryColor.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .clickable { onSwitchToDashboard() }
                        .testTag("minimal_expand_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Full Dashboard",
                            tint = sassyMood.secondaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * VisualAudioWaveAnimation:
 * Renders multiple flowing sine-wave layers on a full Compose Canvas that continuously
 * react to voice audio amplitude and listening state.
 */
@Composable
fun VisualAudioWaveAnimation(
    amplitude: Float,
    isListening: Boolean,
    mood: SassyMood,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VisualWaveTransition")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase2"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val effectiveAmp = if (isListening) {
            (amplitude * 1.2f + 0.15f).coerceIn(0.12f, 1.0f) * (height / 3f) * pulseScale
        } else {
            (height / 14f)
        }

        // Background Glow Ring in Center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    mood.glowColor.copy(alpha = if (isListening) 0.35f else 0.12f),
                    mood.primaryColor.copy(alpha = if (isListening) 0.18f else 0.05f),
                    Color.Transparent
                ),
                center = Offset(width / 2f, centerY),
                radius = (height * 0.7f) * (if (isListening) (0.9f + amplitude * 0.4f) else 0.75f)
            ),
            radius = height * 0.75f,
            center = Offset(width / 2f, centerY)
        )

        // Wave Layer 1 (Primary Cyan/Magenta harmonic)
        val path1 = Path()
        val path2 = Path()
        val path3 = Path()

        val points = 80
        val stepX = width / points

        for (i in 0..points) {
            val x = i * stepX
            val progress = i.toFloat() / points
            val envelope = sin(progress.toDouble() * PI).toFloat().coerceAtLeast(0f) // smooth tapering at ends

            // Layer 1
            val y1 = centerY + sin(progress.toDouble() * 4.0 * PI + phase1.toDouble()).toFloat() * effectiveAmp * envelope
            if (i == 0) path1.moveTo(x, y1) else path1.lineTo(x, y1)

            // Layer 2 (Faster frequency, inverted phase)
            val y2 = centerY + sin(progress.toDouble() * 6.0 * PI - phase2.toDouble()).toFloat() * (effectiveAmp * 0.75f) * envelope
            if (i == 0) path2.moveTo(x, y2) else path2.lineTo(x, y2)

            // Layer 3 (Lower sub-harmonic)
            val y3 = centerY + sin(progress.toDouble() * 2.5 * PI + phase1.toDouble() * 0.7).toFloat() * (effectiveAmp * 0.5f) * envelope
            if (i == 0) path3.moveTo(x, y3) else path3.lineTo(x, y3)
        }

        // Draw Layer 3
        drawPath(
            path = path3,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    mood.secondaryColor.copy(alpha = 0.35f),
                    mood.primaryColor.copy(alpha = 0.5f),
                    mood.secondaryColor.copy(alpha = 0.35f)
                )
            ),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Layer 2
        drawPath(
            path = path2,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.55f),
                    NeonViolet.copy(alpha = 0.7f),
                    NeonMagenta.copy(alpha = 0.55f)
                )
            ),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Layer 1 (Primary Accent Wave)
        drawPath(
            path = path1,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    mood.secondaryColor,
                    mood.primaryColor,
                    mood.glowColor
                )
            ),
            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * AudioEqualizerWaveRow:
 * A row of glowing vertical equalizer bars reacting dynamically to real-time audio volume.
 */
@Composable
fun AudioEqualizerWaveRow(
    amplitude: Float,
    isActive: Boolean,
    mood: SassyMood,
    modifier: Modifier = Modifier,
    barCount: Int = 24
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EqualizerAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "EqualizerPhase"
    )

    Canvas(
        modifier = modifier
            .height(32.dp)
            .width((barCount * 8).dp)
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = 3.5.dp.toPx()
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
        val spacing = if (barCount > 1) (totalWidth - (barCount * barWidth)) / (barCount - 1) else 4.dp.toPx()

        for (i in 0 until barCount) {
            val offset = (i.toDouble() / barCount) * (2 * PI)
            val waveHeightFraction = if (isActive) {
                val sineVal = (sin(phase.toDouble() + offset).toFloat() * 0.5f + 0.5f)
                (sineVal * 0.45f + amplitude * 0.85f).coerceIn(0.15f, 1.0f)
            } else {
                0.12f
            }

            val barHeight = maxHeight * waveHeightFraction
            val x = i * (barWidth + spacing)
            val y = (maxHeight - barHeight) / 2f

            if (isActive) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            mood.secondaryColor,
                            mood.primaryColor,
                            mood.glowColor
                        ),
                        startY = y,
                        endY = y + barHeight
                    ),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            } else {
                drawRoundRect(
                    color = TextTertiary.copy(alpha = 0.3f),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}
