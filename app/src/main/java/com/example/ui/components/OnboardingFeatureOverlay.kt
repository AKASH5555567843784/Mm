package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SassyMood
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * OnboardingFeatureOverlay:
 * Brief, non-intrusive interactive overlay explaining:
 * 1. Voice-Trigger Gestures (Orb tap, "Hey MM" wake word, direct tool commands)
 * 2. 'Sassy' Personality Mode Toggles (Mood switcher, dynamic themes & automatic detection)
 */
@Composable
fun OnboardingFeatureOverlay(
    isVisible: Boolean,
    currentMood: SassyMood,
    onMoodSelected: (SassyMood) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val activeMood = LocalSassyMood.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    activeMood.primaryColor,
                                    activeMood.secondaryColor.copy(alpha = 0.6f),
                                    NeonCyan.copy(alpha = 0.3f)
                                )
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
                    .shadow(24.dp, RoundedCornerShape(26.dp), spotColor = activeMood.primaryColor)
                    .testTag("onboarding_overlay_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(activeMood.primaryColor, activeMood.secondaryColor)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Welcome",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Meet MM Assistant",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                )
                                Text(
                                    text = "Quick guide to gestures & sassy moods",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .testTag("dismiss_onboarding_top_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Navigation (Gestures vs Sassy Modes)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkSurface,
                        contentColor = activeMood.primaryColor,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = activeMood.primaryColor
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.TouchApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Voice & Gestures",
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("onboarding_tab_gestures")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Face,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Sassy Personas",
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("onboarding_tab_sassy")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Content based on tab with smooth scroll
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (selectedTab == 0) {
                            VoiceGesturesGuide(moodColor = activeMood.primaryColor)
                        } else {
                            SassyModesGuide(
                                currentMood = currentMood,
                                onMoodSelected = onMoodSelected
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedTab == 0) {
                            Button(
                                onClick = { selectedTab = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("onboarding_next_tab_btn")
                            ) {
                                Text("Next: Personas ➔", color = TextPrimary, fontSize = 12.5.sp)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeMood.primaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_got_it_btn")
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Got it",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Got It, Let's Go!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceGesturesGuide(moodColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OnboardingItemRow(
            icon = Icons.Default.TouchApp,
            iconColor = NeonCyan,
            title = "Tap Central Orb to Speak",
            description = "Tap the glowing pulsating voice orb at any time to instantly start speaking. Tap again to pause or stop."
        )

        OnboardingItemRow(
            icon = Icons.Default.Mic,
            iconColor = NeonMagenta,
            title = "Say \"Hey MM\" Wake Word",
            description = "Hands-free 24/7 activation! Just say \"Hey MM\" or \"MM\" even with your screen in standby."
        )

        OnboardingItemRow(
            icon = Icons.Default.PhonelinkLock,
            iconColor = Color(0xFF10B981),
            title = "Unlock & Lock Phone Hands-Free",
            description = "Say \"Hey MM, unlock my phone\" (auto-enters your PIN/Pattern) or \"Lock phone\" to sleep the screen."
        )

        OnboardingItemRow(
            icon = Icons.Default.AutoAwesome,
            iconColor = Color(0xFFA855F7),
            title = "Hindi & English Multi-Language",
            description = "Talk naturally in Hindi or English! Say \"Torch jalao\", \"Phone unlock karo\", or \"Turn on flashlight\"—MM obeys Boss in any language."
        )

        OnboardingItemRow(
            icon = Icons.Default.GraphicEq,
            iconColor = Color(0xFFF59E0B),
            title = "Quick Voice Chips",
            description = "Tap any suggestion chip below the orb for instant actions like app locking, torch, contacts, or jokes."
        )
    }
}

@Composable
private fun SassyModesGuide(
    currentMood: SassyMood,
    onMoodSelected: (SassyMood) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(currentMood.primaryColor.copy(alpha = 0.12f))
                .border(1.dp, currentMood.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentMood.emoji,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Active Persona: ${currentMood.displayName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = currentMood.primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${currentMood.tagline}\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                )
            }
        }

        Text(
            text = "Tap a persona to switch MM's attitude & theme:",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextTertiary,
                fontSize = 11.sp
            )
        )

        // Mood selection chips
        val allMoods = SassyMood.entries
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until allMoods.size step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val mood1 = allMoods[i]
                    val isSelected1 = mood1 == currentMood
                    FilterChip(
                        selected = isSelected1,
                        onClick = { onMoodSelected(mood1) },
                        label = {
                            Text(
                                text = "${mood1.emoji} ${mood1.displayName}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = mood1.primaryColor,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_mood_chip_${mood1.name}")
                    )

                    if (i + 1 < allMoods.size) {
                        val mood2 = allMoods[i + 1]
                        val isSelected2 = mood2 == currentMood
                        FilterChip(
                            selected = isSelected2,
                            onClick = { onMoodSelected(mood2) },
                            label = {
                                Text(
                                    text = "${mood2.emoji} ${mood2.displayName}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected2) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = mood2.primaryColor,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_mood_chip_${mood2.name}")
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "💡 Pro-Tip: Say \"MM, be more savage\" or \"MM, switch to boss mode\" to change personas with your voice!",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 10.5.sp
            )
        )
    }
}

@Composable
private fun OnboardingItemRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            )
        }
    }
}
