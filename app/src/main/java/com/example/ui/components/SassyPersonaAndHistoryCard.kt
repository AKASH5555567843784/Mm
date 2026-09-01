package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LiveTranscript
import com.example.model.SassyIntensity
import com.example.model.SassyMood
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SassyPersonaAndHistoryCard(
    currentIntensity: SassyIntensity,
    onIntensitySelected: (SassyIntensity) -> Unit,
    transcripts: List<LiveTranscript>,
    onClearHistory: () -> Unit,
    currentMood: SassyMood,
    modifier: Modifier = Modifier
) {
    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    var isHistoryExpanded by remember { mutableStateOf(false) }

    val animatedBorderColor by animateColorAsState(
        targetValue = currentIntensity.accentColor,
        animationSpec = tween(400),
        label = "PersonaIntensityBorder"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.92f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    animatedBorderColor.copy(alpha = 0.55f),
                    currentMood.primaryColor.copy(alpha = 0.25f),
                    Color.Transparent
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("sassy_persona_history_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Section
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
                            .background(currentIntensity.accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Sassy Intensity",
                            tint = currentIntensity.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Persona Intensity & Memory",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = "Adjust AI wit level or wipe conversation history",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Level Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = currentIntensity.accentColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, currentIntensity.accentColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentIntensity.emoji} Level ${currentIntensity.level}/4",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = currentIntensity.accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Intensity Level Segmented Chips
            Text(
                text = "SASSY INTENSITY LEVEL",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextTertiary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SassyIntensity.entries.forEach { intensity ->
                    val isSelected = intensity == currentIntensity
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) intensity.accentColor.copy(alpha = 0.25f) else DarkSurfaceVariant.copy(alpha = 0.5f),
                        animationSpec = tween(250),
                        label = "IntensityChipBg_${intensity.id}"
                    )
                    val chipBorder by animateColorAsState(
                        targetValue = if (isSelected) intensity.accentColor else Color.Transparent,
                        animationSpec = tween(250),
                        label = "IntensityChipBorder_${intensity.id}"
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = chipBg,
                        border = BorderStroke(1.2.dp, chipBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onIntensitySelected(intensity) }
                            .testTag("intensity_chip_${intensity.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = intensity.emoji,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = intensity.shortLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Stepper Slider
            Slider(
                value = currentIntensity.level.toFloat(),
                onValueChange = { value ->
                    val targetLevel = value.toInt().coerceIn(1, 4)
                    val selected = SassyIntensity.fromLevel(targetLevel)
                    if (selected != currentIntensity) {
                        onIntensitySelected(selected)
                    }
                },
                valueRange = 1f..4f,
                steps = 2,
                colors = SliderDefaults.colors(
                    thumbColor = currentIntensity.accentColor,
                    activeTrackColor = currentIntensity.accentColor,
                    inactiveTrackColor = DarkSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sassy_intensity_slider")
            )

            // Current Intensity Info Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = currentIntensity.accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, currentIntensity.accentColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentIntensity.emoji} ${currentIntensity.displayName}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = currentIntensity.accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        Text(
                            text = currentIntensity.tagline,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentIntensity.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary.copy(alpha = 0.9f),
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Conversation History Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Conversation History",
                        tint = currentMood.primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Conversation Memory",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = if (transcripts.isEmpty()) "No active conversation turns" else "${transcripts.size} messages in session memory",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Clear History Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (transcripts.isNotEmpty()) Color(0xFFFF1744).copy(alpha = 0.18f) else DarkSurfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (transcripts.isNotEmpty()) Color(0xFFFF1744).copy(alpha = 0.6f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .clickable(enabled = transcripts.isNotEmpty()) {
                            showClearConfirmationDialog = true
                        }
                        .testTag("clear_history_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = if (transcripts.isNotEmpty()) Color(0xFFFF5252) else TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear History",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (transcripts.isNotEmpty()) Color(0xFFFF5252) else TextTertiary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Optional expand/preview recent transcripts
            if (transcripts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isHistoryExpanded = !isHistoryExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHistoryExpanded) "Hide session transcripts" else "View recent transcripts (${transcripts.takeLast(3).size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = currentMood.secondaryColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                    Icon(
                        imageVector = if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle History View",
                        tint = currentMood.secondaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isHistoryExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        transcripts.takeLast(5).forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = when (item.sender) {
                                        LiveTranscript.Sender.USER -> "Boss:"
                                        LiveTranscript.Sender.MM -> "MM:"
                                        LiveTranscript.Sender.SYSTEM -> "SYS:"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = when (item.sender) {
                                            LiveTranscript.Sender.USER -> Color(0xFF00E5FF)
                                            LiveTranscript.Sender.MM -> currentMood.primaryColor
                                            LiveTranscript.Sender.SYSTEM -> Color(0xFFFFB703)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextPrimary,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Clear Confirmation Alert Dialog
    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFF1744),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Clear Conversation History?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            },
            text = {
                Text(
                    text = "This will permanently wipe all ${transcripts.size} conversation turns and tool execution logs from active assistant memory.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showClearConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF1744)
                    ),
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearConfirmationDialog = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("clear_history_dialog")
        )
    }
}
