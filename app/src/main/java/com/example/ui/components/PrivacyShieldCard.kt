package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta

/**
 * Persistent Privacy Shield UI Component:
 * Allows user to instantly toggle off the microphone AND the assistant's speech output for privacy.
 * Displays real-time hardware status badges and background SpeechRecognizer 'Hey MM' listener state.
 */
@Composable
fun PrivacyShieldCard(
    isPrivacyModeActive: Boolean,
    isMicMuted: Boolean,
    isSpeechOutputMuted: Boolean,
    isSpeechRecognizerActive: Boolean,
    onTogglePrivacyMode: () -> Unit,
    onToggleMicMute: () -> Unit,
    onToggleSpeechMute: () -> Unit,
    onTestWakeWord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isPrivacyModeActive) Color(0xFFFF5252) else NeonCyan.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 300),
        label = "privacy_border_anim"
    )

    val backgroundBrush = if (isPrivacyModeActive) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF2C1014),
                Color(0xFF1B0C10)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                DarkSurface,
                Color(0xFF140D2B)
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .testTag("privacy_shield_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row: Title, Shield Icon & Main Master Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPrivacyModeActive) Color(0xFFFF5252).copy(alpha = 0.25f)
                                    else NeonCyan.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPrivacyModeActive) Icons.Default.PrivacyTip else Icons.Default.Security,
                                contentDescription = "Privacy Shield",
                                tint = if (isPrivacyModeActive) Color(0xFFFF5252) else NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = if (isPrivacyModeActive) "🛡️ Privacy Shield: LOCKED" else "🛡️ Privacy Shield",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isPrivacyModeActive) Color(0xFFFF8A80) else Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isPrivacyModeActive) "Mic & Speech output are completely OFF" else "Tap switch to silence mic & voice output",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPrivacyModeActive) Color(0xFFFFCDD2) else Color(0xFFC7BBE6),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Master Privacy Shield Switch
                    Switch(
                        checked = isPrivacyModeActive,
                        onCheckedChange = { onTogglePrivacyMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFF5252),
                            uncheckedThumbColor = NeonCyan,
                            uncheckedTrackColor = Color(0xFF2E2248)
                        ),
                        modifier = Modifier.testTag("privacy_shield_master_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Status & Sub-Toggles Row (Mic, Speech, Wake-Word)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Microphone Toggle Badge
                    PrivacyStatusChip(
                        icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        title = "Microphone",
                        statusText = if (isMicMuted) "MUTED" else "ACTIVE",
                        isActive = !isMicMuted,
                        onClick = onToggleMicMute,
                        modifier = Modifier.weight(1f)
                    )

                    // 2. Speech & TTS Output Toggle Badge
                    PrivacyStatusChip(
                        icon = if (isSpeechOutputMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        title = "Voice Speech",
                        statusText = if (isSpeechOutputMuted) "MUTED" else "ACTIVE",
                        isActive = !isSpeechOutputMuted,
                        onClick = onToggleSpeechMute,
                        modifier = Modifier.weight(1f)
                    )

                    // 3. 'Hey MM' Speech Recognizer Badge
                    PrivacyStatusChip(
                        icon = Icons.Default.Hearing,
                        title = "'Hey MM' Wake",
                        statusText = if (isPrivacyModeActive) "PAUSED" else if (isSpeechRecognizerActive) "LISTENING" else "STANDBY",
                        isActive = !isPrivacyModeActive && isSpeechRecognizerActive,
                        onClick = { onTestWakeWord("Hey MM") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Expandable info / Quick Test Wake Bar
                AnimatedVisibility(visible = !isPrivacyModeActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1F1638))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👂 Wake triggers: \"Hey MM\", \"Hello MM\", \"MM\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonMagenta.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .clickable { onTestWakeWord("Hey MM") }
                                    .testTag("test_wake_word_button")
                            ) {
                                Text(
                                    text = "Test Wake",
                                    color = NeonMagenta,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyStatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    statusText: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipBg = if (isActive) Color(0xFF1B2A38) else Color(0xFF381B20)
    val chipBorder = if (isActive) NeonCyan.copy(alpha = 0.4f) else Color(0xFFFF5252).copy(alpha = 0.5f)
    val tintColor = if (isActive) NeonCyan else Color(0xFFFF5252)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, chipBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = chipBg
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tintColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE0E0E0),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = tintColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
