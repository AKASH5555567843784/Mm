package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.example.model.LocalGGUFModel
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun HybridModeStatusBar(
    isOnline: Boolean,
    networkType: String,
    forceOfflineMode: Boolean,
    onToggleForceOffline: () -> Unit,
    activeEngineName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hybrid_status_bar"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard.copy(alpha = 0.85f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOnline && !forceOfflineMode) NeonCyan.copy(alpha = 0.35f) else NeonViolet.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOnline && !forceOfflineMode) AccentGreen.copy(alpha = 0.15f)
                            else NeonViolet.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOnline && !forceOfflineMode) Icons.Default.CloudDone else Icons.Default.Memory,
                        contentDescription = null,
                        tint = if (isOnline && !forceOfflineMode) AccentGreen else NeonViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isOnline && !forceOfflineMode) "Online: Gemini Live (3.1/2.0)" else "Offline: Local Open-Source GGUF",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }
                    Text(
                        text = if (isOnline && !forceOfflineMode) "Fast bi-directional audio stream ($networkType)" else "Engine: $activeEngineName",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isOnline && !forceOfflineMode) AccentGreen else NeonViolet,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = onToggleForceOffline,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (forceOfflineMode) NeonViolet else TextSecondary
                ),
                modifier = Modifier.testTag("toggle_offline_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (forceOfflineMode) "Local GGUF" else "Auto-Hybrid",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun LocalModelManagerSection(
    models: List<LocalGGUFModel>,
    storageUsedFormatted: String,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("local_model_manager_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard.copy(alpha = 0.9f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = NeonViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Local Open-Source GGUF Models",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Storage used: $storageUsedFormatted • Offline on-device LLM runtime",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "MM seamlessly runs open-source GGUF weights on-device when internet is unavailable. Download or select a preferred model below:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextTertiary,
                            fontSize = 11.5.sp
                        )
                    )

                    models.forEach { model ->
                        LocalModelItem(
                            model = model,
                            onSelect = { onSelectModel(model.id) },
                            onDownload = { onDownloadModel(model.id) },
                            onDelete = { onDeleteModel(model.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalModelItem(
    model: LocalGGUFModel,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val isSelected = model.isSelected

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NeonViolet.copy(alpha = 0.12f) else DarkSurface)
            .border(
                1.dp,
                if (isSelected) NeonViolet.copy(alpha = 0.7f) else DarkSurface.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = model.isDownloaded) { onSelect() }
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) NeonViolet else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = model.parameterCount,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Text(
                            text = "${model.quantization} • ${model.formattedSize}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                }

                // Action button: Download, Downloading, or Delete
                when {
                    model.isDownloading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${(model.downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan)
                            )
                        }
                    }
                    model.isDownloaded -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier
                                        .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete model",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownload,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("download_model_${model.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Get", fontSize = 11.sp)
                        }
                    }
                }
            }

            if (model.isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { model.downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = NeonCyan,
                    trackColor = DarkSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = model.description,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextTertiary,
                    fontSize = 10.5.sp
                )
            )
        }
    }
}
