package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ServiceControlsSheet(
    isServiceRunning: Boolean,
    isMuted: Boolean,
    wakeWordSensitivity: Float,
    isBatteryExempt: Boolean,
    onToggleService: () -> Unit,
    onToggleMute: () -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestDisableBatteryOptimization: () -> Unit,
    onVolumePresetSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val hasMic = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val hasContacts = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    val hasCall = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED

    val allGranted = hasMic && hasContacts && hasCall

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, NeonViolet.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("service_controls_card")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Background Voice & Wake-Word",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (isServiceRunning) "Running • Say 'MM' anytime" else "Foreground service stopped",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isServiceRunning) AccentGreen else TextTertiary
                    )
                )
            }

            Switch(
                checked = isServiceRunning,
                onCheckedChange = { onToggleService() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NeonMagenta,
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = DarkSurfaceVariant
                ),
                modifier = Modifier.testTag("service_toggle_switch")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sensitivity slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wake-Word Sensitivity ('MM')",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
            Text(
                text = "${(wakeWordSensitivity * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Slider(
            value = wakeWordSensitivity,
            onValueChange = onSensitivityChanged,
            valueRange = 0.2f..0.95f,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = DarkSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sensitivity_slider")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Battery Optimization Health Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isBatteryExempt) AccentGreen.copy(alpha = 0.12f) else NeonMagenta.copy(alpha = 0.12f))
                .border(
                    1.dp,
                    if (isBatteryExempt) AccentGreen.copy(alpha = 0.4f) else NeonMagenta.copy(alpha = 0.4f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isBatteryExempt) Icons.Default.BatteryChargingFull else Icons.Default.BatterySaver,
                    contentDescription = null,
                    tint = if (isBatteryExempt) AccentGreen else NeonMagenta,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isBatteryExempt) "Battery Optimization Exempt" else "Battery Optimization Active",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = if (isBatteryExempt) "24/7 background listener is uninterrupted" else "Disable to keep MM listening 24/7",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isBatteryExempt) AccentGreen else TextTertiary
                        )
                    )
                }
            }

            if (!isBatteryExempt) {
                Button(
                    onClick = onRequestDisableBatteryOptimization,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                    modifier = Modifier.testTag("disable_battery_opt_button")
                ) {
                    Text("Exempt", fontSize = 12.sp)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mute & Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .weight(1f)
                    .testTag("mute_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isMuted) NeonMagenta else TextPrimary
                )
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isMuted) "Unmute Mic" else "Mute Mic")
            }

            if (!allGranted) {
                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("grant_permissions_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Permissions")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Intelligent Volume Adjuster Preset Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onVolumePresetSelected("volume_night") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("volume_night_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("🌙 Night Vol", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { onVolumePresetSelected("volume_auto") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("volume_auto_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonViolet)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("⚡ Auto Vol", fontSize = 12.sp)
            }
        }
    }
}
