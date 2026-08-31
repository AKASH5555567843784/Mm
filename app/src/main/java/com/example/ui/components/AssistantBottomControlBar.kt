package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VoiceChat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.model.SassyMood
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AssistantBottomControlBar(
    assistantState: AssistantState,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onMicTap: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    mood: SassyMood = LocalSassyMood.current
) {
    val isInteracting = assistantState == AssistantState.LISTENING || assistantState == AssistantState.SPEAKING
    val infiniteTransition = rememberInfiniteTransition(label = "BottomBarPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isInteracting) 1.12f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isInteracting) 900 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicBtnPulse"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = mood.primaryColor.copy(alpha = 0.35f),
        animationSpec = tween(400),
        label = "BottomBarBorder"
    )

    Surface(
        color = DarkSurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    animatedBorderColor,
                    Color.Transparent
                )
            )
        ),
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("assistant_bottom_control_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Mute / Unmute Toggle Button
            val muteBgColor by animateColorAsState(
                targetValue = if (isMuted) Color(0xFF42151B) else DarkSurfaceVariant.copy(alpha = 0.7f),
                animationSpec = tween(300),
                label = "MuteBgColor"
            )
            val muteIconColor by animateColorAsState(
                targetValue = if (isMuted) Color(0xFFFF4D6D) else TextSecondary,
                animationSpec = tween(300),
                label = "MuteIconColor"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White),
                        onClick = onToggleMute
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("mute_toggle_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(muteBgColor)
                        .border(
                            width = 1.2.dp,
                            color = if (isMuted) Color(0xFFFF4D6D) else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Unmute Microphone" else "Mute Microphone",
                        tint = muteIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isMuted) "Muted" else "Mic On",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isMuted) Color(0xFFFF4D6D) else TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                )
            }

            // CENTER: Prominent Beautifully Styled Glowing Action/Mic Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.testTag("center_mic_action_container")
            ) {
                // Outer subtle radiant aura ring
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    mood.glowColor.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Main circular action button
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = mood.primaryColor, spotColor = mood.glowColor)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    mood.primaryColor,
                                    mood.secondaryColor
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = onMicTap
                        )
                        .testTag("center_voice_action_button"),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (assistantState) {
                        AssistantState.SPEAKING -> Icons.Default.Stop
                        AssistantState.LISTENING -> Icons.Default.VoiceChat
                        else -> Icons.Default.Mic
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Interact with MM Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // RIGHT: Settings Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White),
                        onClick = onOpenSettings
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("open_settings_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant.copy(alpha = 0.7f))
                        .border(
                            width = 1.dp,
                            color = mood.primaryColor.copy(alpha = 0.25f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
