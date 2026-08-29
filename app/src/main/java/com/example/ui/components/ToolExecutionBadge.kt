package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ToolCallInfo
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary

@Composable
fun ToolExecutionBadge(
    activeTool: ToolCallInfo?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = activeTool != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        if (activeTool != null) {
            val icon = when (activeTool.functionName) {
                "openApp" -> Icons.Default.PhoneAndroid
                "searchAndCallContact" -> Icons.Default.Call
                "sendWhatsAppMessage" -> Icons.Default.Chat
                "setAlarmOrTimer" -> Icons.Default.Alarm
                "toggleFlashlight" -> Icons.Default.FlashlightOn
                "playMusic" -> Icons.Default.MusicNote
                else -> Icons.Default.SmartToy
            }

            val statusColor = when (activeTool.status) {
                ToolCallInfo.ToolStatus.EXECUTING -> AccentAmber
                ToolCallInfo.ToolStatus.SUCCESS -> AccentGreen
                ToolCallInfo.ToolStatus.FAILED -> Color.Red
                else -> NeonCyan
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("tool_execution_badge")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = activeTool.functionName,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Native Tool: ${activeTool.functionName} (${activeTool.status.name.lowercase()})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
