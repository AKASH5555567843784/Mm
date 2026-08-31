package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet

@Composable
fun CallAnnouncerCard(
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    lastAnnouncement: String?,
    isAnnouncing: Boolean,
    onTestKnownCaller: () -> Unit,
    onTestUnknownCaller: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("call_announcer_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(NeonMagenta.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = "Call Announcer",
                            tint = NeonMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Silent Mode Call Announcer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Speaks caller only when phone is Silent/Vibrate",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("toggle_call_announcer_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonMagenta,
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Logic Rules explanation card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Silent / Vibrate Mode:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "• Known: \"Boss, [Contact Name] ka call aa raha hai.\"\n• Unknown: \"Boss, kisi unknown number se call aa raha hai.\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Normal Ringing Mode:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D),
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "• MM stays completely silent & lets standard ringtone play.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            // Live status banner
            if (!lastAnnouncement.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeonViolet.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🎙️ $lastAnnouncement",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test Simulation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onTestKnownCaller,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_known_call_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Test Known Call", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onTestUnknownCaller,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_unknown_call_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonMagenta)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Test Unknown", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
