package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DarkSurfaceBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.DefaultAssistantManager

/**
 * Minimalist Floating Bottom-Up Reveal Card that smoothly slides upward from the bottom
 * when MM Assistant is invoked via gesture, power button, or assist intent.
 */
@Composable
fun AssistantBottomRevealSheet(
    assistantState: AssistantState,
    sassyQuote: String,
    audioAmplitude: Float,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onReconnect: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onUnlockPhone: () -> Unit,
    onToggleFlashlight: () -> Unit,
    modifier: Modifier = Modifier,
    triggerSource: String = "ASSIST_GESTURE"
) {
    val context = LocalContext.current
    val mood = LocalSassyMood.current
    val isDefaultAssistant = DefaultAssistantManager.isDefaultAssistant(context)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 40) {
                        onDismiss()
                    }
                }
            }
            .testTag("assistant_bottom_reveal_sheet"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = DarkSurfaceCard.copy(alpha = 0.96f),
        border = BorderStroke(
            1.5.dp,
            Brush.verticalGradient(
                listOf(
                    mood.primaryColor.copy(alpha = 0.6f),
                    mood.secondaryColor.copy(alpha = 0.2f)
                )
            )
        ),
        shadowElevation = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Sleek Top Drag Handle
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
                    .clickable { onDismiss() }
            )

            // 2. Header Bar: Identity, Default Assistant Badge & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonCyan, NeonMagenta))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MM ASSISTANT",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Zero-Lie Policy",
                                tint = AccentGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = if (isDefaultAssistant) "Primary Default Digital Assistant" else "Tap to Set as Default Assistant",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDefaultAssistant) AccentGreen else mood.primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.clickable {
                                if (!isDefaultAssistant) {
                                    context.startActivity(DefaultAssistantManager.createSetDefaultAssistantIntent(context))
                                }
                            }
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("dismiss_reveal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3. Center Section: Video / Holographic Animated Visualizer
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                MMAssistantVideoStatePlayer(
                    assistantState = assistantState,
                    audioAmplitude = audioAmplitude,
                    modifier = Modifier.size(124.dp)
                )
            }

            // 4. State Indicator & Honest Response Dialogue Bubble
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assistant_speech_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when (assistantState) {
                                        AssistantState.LISTENING -> NeonCyan
                                        AssistantState.THINKING -> NeonAmber
                                        AssistantState.SPEAKING -> NeonMagenta
                                        else -> AccentGreen
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (assistantState) {
                                AssistantState.LISTENING -> "Listening for command..."
                                AssistantState.THINKING -> "Analyzing query with Zero-Lie Policy..."
                                AssistantState.SPEAKING -> "MM is responding..."
                                else -> "Ready, Boss"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = when (assistantState) {
                                    AssistantState.LISTENING -> NeonCyan
                                    AssistantState.THINKING -> NeonAmber
                                    AssistantState.SPEAKING -> NeonMagenta
                                    else -> AccentGreen
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Text Content
                    Text(
                        text = sassyQuote,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 5. Interactive Bottom Control Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute / Unmute
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color(0xFFFF453A).copy(alpha = 0.2f) else DarkSurfaceVariant)
                        .testTag("reveal_toggle_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) Color(0xFFFF453A) else NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Unlock Phone Shortcut
                IconButton(
                    onClick = onUnlockPhone,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("reveal_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhonelinkLock,
                        contentDescription = "Unlock Phone",
                        tint = AccentGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Flashlight Shortcut
                IconButton(
                    onClick = onToggleFlashlight,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("reveal_flashlight_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashlightOn,
                        contentDescription = "Flashlight",
                        tint = NeonAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Live Session Reconnect
                IconButton(
                    onClick = onReconnect,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("reveal_reconnect_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reconnect",
                        tint = NeonViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Full Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("reveal_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
