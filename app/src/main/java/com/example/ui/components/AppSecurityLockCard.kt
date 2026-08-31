package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * AppSecurityLockCard:
 * Full visual management UI for App Lock, Master PIN security, Hidden Apps Vault,
 * and Stealth Mode in MM Assistant.
 */
@Composable
fun AppSecurityLockCard(
    isAppLockEnabled: Boolean,
    lockedApps: Set<String>,
    hiddenApps: Set<String>,
    isAssistantHidden: Boolean,
    masterPin: String,
    lastSecurityAction: String?,
    onToggleAppLock: (Boolean) -> Unit,
    onLockApp: (String) -> Unit,
    onUnlockApp: (String) -> Unit,
    onHideApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onSetMasterPin: (String) -> Unit,
    onToggleAssistantStealth: (Boolean) -> Unit,
    getDisplayName: (String) -> String,
    modifier: Modifier = Modifier
) {
    val mood = LocalSassyMood.current
    var isEditingPin by remember { mutableStateOf(false) }
    var enteredNewPin by remember { mutableStateOf("") }
    var customAppInput by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_security_lock_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, mood.primaryColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        mood.primaryColor.copy(alpha = 0.35f),
                                        mood.secondaryColor.copy(alpha = 0.20f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "App Lock & Stealth",
                            tint = mood.primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "App Lock & Stealth Vault",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Voice-activated security & app hiding",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = isAppLockEnabled,
                    onCheckedChange = onToggleAppLock,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = mood.primaryColor,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("app_lock_global_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Last Security Feedback Banner
            if (lastSecurityAction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(mood.primaryColor.copy(alpha = 0.12f))
                        .border(1.dp, mood.primaryColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "⚡ $lastSecurityAction",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = mood.primaryColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Master PIN Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "PIN",
                                tint = mood.secondaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Master Security PIN",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Current PIN: •••• ($masterPin)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        TextButton(
                            onClick = { isEditingPin = !isEditingPin },
                            modifier = Modifier.testTag("edit_pin_button")
                        ) {
                            Text(
                                text = if (isEditingPin) "Cancel" else "Change",
                                color = mood.secondaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    AnimatedVisibility(visible = isEditingPin) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            OutlinedTextField(
                                value = enteredNewPin,
                                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) enteredNewPin = it },
                                label = { Text("Enter New 4-Digit PIN") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mood.primaryColor,
                                    unfocusedBorderColor = TextTertiary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_pin_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (enteredNewPin.length >= 4) {
                                        onSetMasterPin(enteredNewPin)
                                        enteredNewPin = ""
                                        isEditingPin = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = mood.primaryColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_pin_button")
                            ) {
                                Text("Save Master PIN", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // MM Assistant Stealth Launcher Mode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isAssistantHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Stealth",
                            tint = if (isAssistantHidden) mood.primaryColor else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MM Assistant Stealth Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = if (isAssistantHidden) "Icon hidden from launcher (Wake with 'Hey MM')" else "Icon visible on home screen",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isAssistantHidden) mood.primaryColor else TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Switch(
                        checked = isAssistantHidden,
                        onCheckedChange = onToggleAssistantStealth,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = mood.primaryColor,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("assistant_stealth_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick App Lock / Hide Adder
            Text(
                text = "Quick Secure App Action",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customAppInput,
                    onValueChange = { customAppInput = it },
                    placeholder = { Text("App Name (e.g. WhatsApp, Gallery)", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = mood.primaryColor,
                        unfocusedBorderColor = TextTertiary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_app_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (customAppInput.isNotBlank()) {
                            onLockApp(customAppInput.trim())
                            customAppInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = mood.primaryColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("quick_lock_btn")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = {
                        if (customAppInput.isNotBlank()) {
                            onHideApp(customAppInput.trim())
                            customAppInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = mood.secondaryColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("quick_hide_btn")
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = "Hide", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hide", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Locked Apps List
            Text(
                text = "Currently Locked Apps (${lockedApps.size})",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (lockedApps.isEmpty()) {
                Text(
                    text = "No apps locked yet. Say 'Hey MM, Lock WhatsApp' or type an app name above.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextTertiary,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lockedApps.forEach { pkg ->
                        val name = getDisplayName(pkg)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = mood.primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            IconButton(
                                onClick = { onUnlockApp(name) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Unlock",
                                    tint = mood.secondaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hidden Apps Vault List
            Text(
                text = "Hidden Stealth Vault (${hiddenApps.size})",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (hiddenApps.isEmpty()) {
                Text(
                    text = "No apps hidden in vault. Say 'Hey MM, Hide Instagram' to make apps vanish!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextTertiary,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    hiddenApps.forEach { pkg ->
                        val name = getDisplayName(pkg)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Hidden",
                                    tint = mood.secondaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            IconButton(
                                onClick = { onUnhideApp(name) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Unhide",
                                    tint = mood.primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
