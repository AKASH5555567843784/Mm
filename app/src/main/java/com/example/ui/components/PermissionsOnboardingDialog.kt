package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun PermissionsOnboardingDialog(
    hasRecordAudio: Boolean,
    hasReadContacts: Boolean,
    hasCallPhone: Boolean,
    hasNotification: Boolean,
    onRequestPermissions: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.95f))
            .padding(20.dp),
        color = DarkBackground.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(listOf(NeonMagenta, NeonViolet, NeonCyan, NeonMagenta))
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Unlock MM's Full Power",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "\"Honey, I need a few keys to run your phone seamlessly. Don't be shy!\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = NeonMagenta,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Permissions List Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PermissionRowItem(
                            icon = Icons.Default.Mic,
                            title = "Microphone & Audio Record",
                            description = "For continuous 'MM' wake-word detection & real-time bidirectional voice.",
                            isGranted = hasRecordAudio
                        )
                        PermissionRowItem(
                            icon = Icons.Default.Contacts,
                            title = "Contacts Provider",
                            description = "To find friends and family for hands-free WhatsApp and phone dialing.",
                            isGranted = hasReadContacts
                        )
                        PermissionRowItem(
                            icon = Icons.Default.Call,
                            title = "Phone Calling",
                            description = "To place direct phone calls when you say 'MM, call Mom'.",
                            isGranted = hasCallPhone
                        )
                        PermissionRowItem(
                            icon = Icons.Default.NotificationsActive,
                            title = "Foreground Notification",
                            description = "Keeps 24/7 background listener alive with pulse indicator.",
                            isGranted = hasNotification
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("grant_all_permissions_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta)
                ) {
                    Text(
                        text = "Grant Permissions & Start",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dismiss_permissions_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Continue with Limited Mode", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PermissionRowItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isGranted) AccentGreen.copy(alpha = 0.15f) else NeonViolet.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) AccentGreen else NeonCyan,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextTertiary,
                    fontSize = 10.5.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Granted",
                tint = AccentGreen,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = "REQUIRED",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NeonMagenta,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                ),
                modifier = Modifier
                    .background(NeonMagenta.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
