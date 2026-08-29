package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.model.SassyMood
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SassyMoodControlCard(
    currentMood: SassyMood,
    isAutoDetectionEnabled: Boolean,
    onMoodSelected: (SassyMood) -> Unit,
    onToggleAutoDetection: (Boolean) -> Unit,
    onTriggerMoodSample: (SassyMood) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBorderPrimary by animateColorAsState(
        targetValue = currentMood.primaryColor,
        animationSpec = tween(400),
        label = "MoodCardBorderPrimary"
    )
    val animatedBorderSecondary by animateColorAsState(
        targetValue = currentMood.secondaryColor,
        animationSpec = tween(400),
        label = "MoodCardBorderSecondary"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.90f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    animatedBorderPrimary.copy(alpha = 0.5f),
                    animatedBorderSecondary.copy(alpha = 0.2f),
                    Color.Transparent
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("sassy_mood_manager_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(currentMood.primaryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Visual UI State Manager",
                            tint = currentMood.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sassy Mood & Theme Engine",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = if (isAutoDetectionEnabled) "AI auto-morphing active" else "Manual mood override",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isAutoDetectionEnabled) currentMood.secondaryColor else TextTertiary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Auto Mood Detection Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("auto_mood_switch_container")
                ) {
                    Text(
                        text = "Auto",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isAutoDetectionEnabled) currentMood.primaryColor else TextTertiary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Switch(
                        checked = isAutoDetectionEnabled,
                        onCheckedChange = onToggleAutoDetection,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = currentMood.primaryColor,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .size(width = 44.dp, height = 24.dp)
                            .testTag("auto_mood_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal Mood Selectors Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SassyMood.entries.forEach { mood ->
                    val isSelected = mood == currentMood
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) mood.primaryColor.copy(alpha = 0.25f) else DarkSurfaceVariant.copy(alpha = 0.6f),
                        animationSpec = tween(300),
                        label = "ChipBg_${mood.id}"
                    )
                    val chipBorderColor by animateColorAsState(
                        targetValue = if (isSelected) mood.primaryColor else Color.Transparent,
                        animationSpec = tween(300),
                        label = "ChipBorder_${mood.id}"
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = chipBg,
                        border = BorderStroke(1.2.dp, chipBorderColor),
                        modifier = Modifier
                            .clickable { onMoodSelected(mood) }
                            .testTag("mood_chip_${mood.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mood.emoji,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mood.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mood details & Test trigger banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = currentMood.surfaceAccent.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, currentMood.primaryColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Palette swatches
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(currentMood.primaryColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(currentMood.secondaryColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(currentMood.glowColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentMood.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = currentMood.primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentMood.tagline,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Test Sample Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = currentMood.primaryColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, currentMood.primaryColor),
                        modifier = Modifier
                            .clickable { onTriggerMoodSample(currentMood) }
                            .testTag("test_mood_sample_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Test Sample",
                                tint = currentMood.primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = currentMood.primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
