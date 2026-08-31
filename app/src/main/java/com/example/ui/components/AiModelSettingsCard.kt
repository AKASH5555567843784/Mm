package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.gemini.GeminiLiveClient
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

data class GeminiModelOption(
    val id: String,
    val displayName: String,
    val tag: String,
    val description: String
)

val GEMINI_MODELS = listOf(
    GeminiModelOption(
        id = GeminiLiveClient.DEFAULT_MODEL,
        displayName = "Gemini 2.5 Flash Native Audio",
        tag = "Real-time Live",
        description = "Ultra-low latency bidirectional native audio streaming & live voice conversations."
    ),
    GeminiModelOption(
        id = GeminiLiveClient.PRO_MODEL,
        displayName = "Gemini 2.5 Pro (Flagship)",
        tag = "High Reasoning",
        description = "Advanced reasoning, complex system instruction following, and high accuracy."
    ),
    GeminiModelOption(
        id = GeminiLiveClient.FLASH_MODEL,
        displayName = "Gemini 2.0 Flash",
        tag = "Fast Multimodal",
        description = "High-speed multimodal performance and quick response latency."
    )
)

@Composable
fun AiModelSettingsCard(
    selectedModel: String,
    temperature: Float,
    isZeroFabricationEnabled: Boolean,
    isActionOrientedEnabled: Boolean,
    onModelSelected: (String) -> Unit,
    onTemperatureChanged: (Float) -> Unit,
    onToggleZeroFabrication: (Boolean) -> Unit,
    onToggleActionOriented: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val mood = LocalSassyMood.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Core Identity & Strict Addressing Mandate
        Card(
            modifier = Modifier.fillMaxWidth().testTag("ai_identity_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, mood.primaryColor.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(mood.primaryColor, NeonMagenta))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MM Persona & Core Identity",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Strict \"Boss\" Addressing & Zero-Fabrication Mandate",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = mood.primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Surface(
                    color = DarkSurfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Strictly addresses you as 'Boss' in every turn",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zero conversational filler, pet names, or false data",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fluent Hindi, English & Hinglish command execution",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Model Architecture Selection
        Card(
            modifier = Modifier.fillMaxWidth().testTag("ai_model_selector_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = mood.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini Model Selection",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                    Text(
                        text = "AI Studio Flagship",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    )
                }

                GEMINI_MODELS.forEach { model ->
                    val isSelected = selectedModel == model.id
                    val borderColor by animateColorAsState(
                        if (isSelected) mood.primaryColor else Color.White.copy(alpha = 0.08f),
                        label = "ModelBorder"
                    )
                    val bgColor = if (isSelected) mood.primaryColor.copy(alpha = 0.12f) else DarkSurfaceVariant.copy(alpha = 0.5f)

                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModelSelected(model.id) }
                            .testTag("model_option_${model.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = model.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isSelected) TextPrimary else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) mood.primaryColor.copy(alpha = 0.25f)
                                                else Color.White.copy(alpha = 0.08f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = model.tag,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) mood.primaryColor else TextTertiary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = model.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextTertiary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(mood.primaryColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Temperature Control (Zero-Hallucination Tuning)
        Card(
            modifier = Modifier.fillMaxWidth().testTag("temperature_control_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Temperature Tuning",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (temperature <= 0.4f) AccentGreen.copy(alpha = 0.2f)
                                else mood.primaryColor.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format("%.2f", temperature),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (temperature <= 0.4f) AccentGreen else mood.primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                Text(
                    text = if (temperature <= 0.4f) {
                        "🎯 0.2 - 0.4 is optimal for Zero Hallucinations, factual accuracy, and strict tool execution."
                    } else {
                        "⚠️ Higher temperature increases creative variation but may reduce strict factual adherence."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (temperature <= 0.4f) AccentGreen else TextSecondary,
                        fontSize = 11.sp
                    )
                )

                Slider(
                    value = temperature,
                    onValueChange = onTemperatureChanged,
                    valueRange = 0.0f..1.0f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = mood.primaryColor,
                        activeTrackColor = mood.primaryColor,
                        inactiveTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("temperature_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.0 (Strict / Deterministic)", style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp))
                    Text("0.3 (Recommended)", style = MaterialTheme.typography.labelSmall.copy(color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    Text("1.0 (Creative)", style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp))
                }
            }
        }

        // Section 4: Operational Reliability Switches
        Card(
            modifier = Modifier.fillMaxWidth().testTag("reliability_switches_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = mood.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Operational Rules & Reliability",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                // Zero Fabrication Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Zero Fabrication Mode",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "If real data is unavailable, MM states \"Boss, I do not have real-time access to that data\" rather than fabricating.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Switch(
                        checked = isZeroFabricationEnabled,
                        onCheckedChange = onToggleZeroFabrication,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = mood.primaryColor,
                            uncheckedTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("zero_fabrication_switch")
                    )
                }

                // Action-Oriented Output Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Action-Oriented Output",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "Direct, production-ready solutions with structured bullet points and zero boilerplate placeholders.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Switch(
                        checked = isActionOrientedEnabled,
                        onCheckedChange = onToggleActionOriented,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = mood.primaryColor,
                            uncheckedTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("action_oriented_switch")
                    )
                }
            }
        }
    }
}
