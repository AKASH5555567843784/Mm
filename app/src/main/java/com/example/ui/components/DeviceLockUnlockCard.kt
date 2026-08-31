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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.DeviceLockType
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * DeviceLockUnlockCard:
 * Full visual setup and interactive management for MM Assistant's phone lock & unlock feature.
 * Supports PIN, Pattern (with visual 3x3 pattern dot grid), Password, and Swipe.
 */
@Composable
fun DeviceLockUnlockCard(
    currentLockType: DeviceLockType,
    savedCredential: String,
    isAutoVoiceUnlockEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    lastAction: String?,
    onSaveCredentials: (DeviceLockType, String, Boolean) -> Unit,
    onClearCredentials: () -> Unit,
    onToggleAutoVoiceUnlock: (Boolean) -> Unit,
    onUnlockPhone: () -> Unit,
    onLockPhone: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mood = LocalSassyMood.current
    var selectedType by remember(currentLockType) { mutableStateOf(currentLockType) }
    var inputCredential by remember(savedCredential, currentLockType) { mutableStateOf(savedCredential) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Pattern interactive state (list of points 1..9)
    val patternPoints = remember { mutableStateListOf<Int>() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_lock_unlock_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, mood.primaryColor.copy(alpha = 0.30f))
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        mood.primaryColor.copy(alpha = 0.40f),
                                        mood.secondaryColor.copy(alpha = 0.25f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhonelinkLock,
                            contentDescription = "Device Lock Unlock",
                            tint = mood.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Phone Lock & Unlock",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "MM auto-enters your phone password & unlocks",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = isAutoVoiceUnlockEnabled,
                    onCheckedChange = onToggleAutoVoiceUnlock,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = mood.primaryColor,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("device_unlock_voice_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Accessibility Service Warning / Status
            if (!isAccessibilityEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚠️ Accessibility Permission Recommended",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Enables MM to automatically draw patterns & enter PIN on your lock screen.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenAccessibilitySettings,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("enable_accessibility_btn")
                        ) {
                            Text("Enable", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Last Action Feedback Pill
            if (lastAction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(mood.primaryColor.copy(alpha = 0.10f))
                        .border(1.dp, mood.primaryColor.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚡ $lastAction",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = mood.primaryColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Step 1: Select Lock Type
            Text(
                text = "1. Choose Your Phone's Lock Screen Type",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DeviceLockType.values().forEach { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedType = type
                            if (type == currentLockType) {
                                inputCredential = savedCredential
                            } else if (type == DeviceLockType.SWIPE) {
                                inputCredential = "SWIPE"
                            }
                        },
                        label = {
                            Text(
                                text = when (type) {
                                    DeviceLockType.PIN -> "PIN"
                                    DeviceLockType.PATTERN -> "Pattern"
                                    DeviceLockType.PASSWORD -> "Password"
                                    DeviceLockType.SWIPE -> "Swipe"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (type) {
                                    DeviceLockType.PIN -> Icons.Default.Numbers
                                    DeviceLockType.PATTERN -> Icons.Default.GridView
                                    DeviceLockType.PASSWORD -> Icons.Default.Password
                                    DeviceLockType.SWIPE -> Icons.Default.Swipe
                                },
                                contentDescription = type.label,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = mood.primaryColor,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                            labelColor = TextSecondary,
                            iconColor = TextSecondary
                        ),
                        modifier = Modifier.weight(1f).testTag("lock_type_chip_${type.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step 2: Input Credential according to type
            Text(
                text = "2. Enter & Save Phone Password",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            when (selectedType) {
                DeviceLockType.PIN -> {
                    Column {
                        OutlinedTextField(
                            value = inputCredential,
                            onValueChange = {
                                if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                                    inputCredential = it
                                }
                            },
                            label = { Text("Phone PIN (4–8 digits)") },
                            placeholder = { Text("e.g. 1234 or 5892") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle PIN Visibility",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = mood.primaryColor,
                                unfocusedBorderColor = TextTertiary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_pin_input")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 MM Assistant will type these digits onto your lock screen.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                        )
                    }
                }

                DeviceLockType.PATTERN -> {
                    Column {
                        // Interactive 3x3 Pattern Matrix
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Tap 3x3 dots in order to draw your pattern:",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // 3 rows of 3 dots
                                for (row in 0..2) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        for (col in 0..2) {
                                            val point = row * 3 + col + 1
                                            val isSelected = patternPoints.contains(point)
                                            val order = patternPoints.indexOf(point) + 1

                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) mood.primaryColor else DarkSurface
                                                    )
                                                    .border(
                                                        2.dp,
                                                        if (isSelected) mood.secondaryColor else TextTertiary,
                                                        CircleShape
                                                    )
                                                    .clickable {
                                                        if (patternPoints.contains(point)) {
                                                            patternPoints.remove(point)
                                                        } else {
                                                            patternPoints.add(point)
                                                        }
                                                        inputCredential = patternPoints.joinToString("-")
                                                    }
                                                    .testTag("pattern_dot_$point"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Text(
                                                        text = "$order",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                } else {
                                                    Text(
                                                        text = "$point",
                                                        color = TextTertiary,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (inputCredential.isNotEmpty()) "Pattern: $inputCredential" else "No dots selected yet",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (inputCredential.isNotEmpty()) mood.primaryColor else TextTertiary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )

                                    TextButton(
                                        onClick = {
                                            patternPoints.clear()
                                            inputCredential = ""
                                        },
                                        modifier = Modifier.testTag("clear_pattern_dots_btn")
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp), tint = TextSecondary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reset Dots", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                DeviceLockType.PASSWORD -> {
                    Column {
                        OutlinedTextField(
                            value = inputCredential,
                            onValueChange = { inputCredential = it },
                            label = { Text("Phone Alphanumeric Password") },
                            placeholder = { Text("e.g. MyPassword123") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Password Visibility",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = mood.primaryColor,
                                unfocusedBorderColor = TextTertiary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_password_input")
                        )
                    }
                }

                DeviceLockType.SWIPE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Swipe to unlock mode is selected. MM Assistant will swipe up to wake and unlock your device without a password.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save & Clear Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val cred = if (selectedType == DeviceLockType.SWIPE) "SWIPE" else inputCredential
                        onSaveCredentials(selectedType, cred, isAutoVoiceUnlockEnabled)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = mood.primaryColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_device_lock_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to MM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (savedCredential.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            onClearCredentials()
                            inputCredential = ""
                            patternPoints.clear()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("clear_device_lock_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step 3: Test Now Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Instant Device Controls",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onUnlockPhone,
                            colors = ButtonDefaults.buttonColors(containerColor = mood.secondaryColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_unlock_phone_btn")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Unlock", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Unlock", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onLockPhone,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, mood.primaryColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_lock_phone_btn")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock", tint = mood.primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Screen", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Voice Command Quick Guide
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(mood.primaryColor.copy(alpha = 0.08f))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "🗣️ Voice Commands You Can Say:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = mood.primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• \"Hey MM, unlock my phone\"\n• \"Hey MM, lock phone\"\n• \"Hey MM, phone unlock karo\"\n• \"Hey MM, save my phone PIN as 1234\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    )
                }
            }
        }
    }
}
