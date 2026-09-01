package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SassyMood
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Persistent visual status indicator showing real-time wake-word listening state,
 * pulsing radar animations, supported keywords, and instant voice triggers.
 */
@Composable
fun WakeWordStatusIndicator(
    isServiceRunning: Boolean,
    isPrivacyMode: Boolean,
    isMuted: Boolean,
    isWakeWordListening: Boolean,
    onToggleListening: () -> Unit,
    onTestWakeTrigger: (String) -> Unit,
    mood: SassyMood,
    sensitivity: Float = 0.65f,
    modifier: Modifier = Modifier
) {
    var isPhraseListExpanded by remember { mutableStateOf(false) }

    // Listening state calculation
    val isActivelyListening = isServiceRunning && !isPrivacyMode && !isMuted && isWakeWordListening

    // Infinite pulsing animation for active listening radar ring
    val infiniteTransition = rememberInfiniteTransition(label = "WakeWordPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Dynamic state colors
    val activeColor = when {
        isPrivacyMode -> Color(0xFFFF5252) // Red / Coral
        isMuted -> Color(0xFFFFB703) // Amber
        !isServiceRunning -> TextTertiary // Muted Gray
        isActivelyListening -> Color(0xFF00E676) // Vivid Emerald Green / Neon Cyan
        else -> mood.primaryColor
    }

    val stateBadgeBg by animateColorAsState(
        targetValue = activeColor.copy(alpha = 0.16f),
        animationSpec = tween(300),
        label = "StateBadgeBg"
    )

    val stateBadgeBorder by animateColorAsState(
        targetValue = activeColor.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "StateBadgeBorder"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.94f)
        ),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    activeColor.copy(alpha = 0.5f),
                    mood.primaryColor.copy(alpha = 0.25f),
                    activeColor.copy(alpha = 0.1f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("wake_word_status_indicator")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Main Top Status Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleListening() }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Pulsing Radar Ring / Status Icon
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActivelyListening) {
                            // Pulsing radar wave
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(activeColor.copy(alpha = pulseAlpha))
                            )
                        }

                        // Center Icon Bubble
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(activeColor.copy(alpha = 0.22f))
                                .border(1.dp, activeColor.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isPrivacyMode -> Icons.Default.Security
                                    isMuted -> Icons.Default.MicOff
                                    !isServiceRunning -> Icons.Default.PowerSettingsNew
                                    else -> Icons.Default.Hearing
                                },
                                contentDescription = "Wake-Word Status",
                                tint = activeColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Status Labels
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Live Status Pill Dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isPrivacyMode -> "PRIVACY SHIELD ACTIVE"
                                    isMuted -> "MICROPHONE MUTED"
                                    !isServiceRunning -> "STANDBY (TAP TO START)"
                                    isActivelyListening -> "ACTIVELY LISTENING FOR WAKE-WORD"
                                    else -> "WAKE-WORD PAUSED"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = activeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.8.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = when {
                                isPrivacyMode -> "Microphone silenced. Tap to disable privacy mode."
                                isMuted -> "Tap to unmute and enable hands-free activation."
                                !isServiceRunning -> "Tap to activate continuous background listening."
                                isActivelyListening -> "Say \"Hey MM\", \"Hello MM\", or \"Okay MM\""
                                else -> "Continuous background voice trigger is standby."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Action / Expand Trigger
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = stateBadgeBg,
                    border = BorderStroke(1.dp, stateBadgeBorder),
                    modifier = Modifier
                        .clickable { isPhraseListExpanded = !isPhraseListExpanded }
                        .testTag("wake_word_phrases_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isActivelyListening) "Active" else "Details",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = activeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (isPhraseListExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Wake Phrases",
                            tint = activeColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Expandable Phrases & Sensitivity Info
            AnimatedVisibility(
                visible = isPhraseListExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    // Divider Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SUPPORTED TRIGGER PHRASES (TAP TO TEST)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextTertiary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        )

                        Text(
                            text = "Sens: ${(sensitivity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = mood.secondaryColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Phrase Chips
                    val wakeChips = listOf(
                        "Hey MM",
                        "Hello MM",
                        "Okay MM",
                        "Hi MM",
                        "Suno MM",
                        "MM"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        wakeChips.take(3).forEach { phrase ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, mood.primaryColor.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onTestWakeTrigger(phrase) }
                                    .testTag("test_wake_chip_$phrase")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = mood.primaryColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = phrase,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        wakeChips.drop(3).forEach { phrase ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, mood.secondaryColor.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onTestWakeTrigger(phrase) }
                                    .testTag("test_wake_chip_$phrase")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = mood.secondaryColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = phrase,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
