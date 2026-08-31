package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pc.RemotePcManager
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemotePcControlCard(
    status: RemotePcManager.PcConnectionStatus,
    pcName: String,
    pcIp: String,
    pcPort: Int,
    latencyMs: Long?,
    lastLog: String,
    onPing: () -> Unit,
    onSaveSettings: (ip: String, port: Int, name: String) -> Unit,
    onExecuteAction: (command: String, params: Map<String, Any?>) -> Unit,
    pythonScriptCode: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showConfigDialog by remember { mutableStateOf(false) }
    var showScriptDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var textToType by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("remote_pc_control_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
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
                            .size(38.dp)
                            .background(NeonCyan.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = "Remote PC",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pcName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Status Dot
                            val (dotColor, statusText) = when (status) {
                                RemotePcManager.PcConnectionStatus.CONNECTED -> AccentGreen to "${latencyMs ?: 12}ms"
                                RemotePcManager.PcConnectionStatus.CONNECTING -> Color(0xFFFFB74D) to "Connecting..."
                                RemotePcManager.PcConnectionStatus.ERROR -> Color(0xFFFF5252) to "Offline"
                                RemotePcManager.PcConnectionStatus.DISCONNECTED -> Color.Gray to "Ready"
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = dotColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "$pcIp:$pcPort (Companion Daemon)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPing,
                        modifier = Modifier.size(32.dp).testTag("ping_pc_button")
                    ) {
                        if (status == RemotePcManager.PcConnectionStatus.CONNECTING) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Ping PC", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.size(32.dp).testTag("edit_pc_settings_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit PC Settings", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Grid / Chips
            Text(
                text = "Autonomous PC Commands",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Lock PC
                PcQuickActionButton(
                    icon = Icons.Default.Lock,
                    label = "Lock PC",
                    tint = NeonMagenta,
                    onClick = { onExecuteAction("lock", emptyMap()) }
                )

                // Sleep PC
                PcQuickActionButton(
                    icon = Icons.Default.PowerSettingsNew,
                    label = "Sleep",
                    tint = Color(0xFFFFB74D),
                    onClick = { onExecuteAction("sleep", emptyMap()) }
                )

                // Media Play/Pause
                PcQuickActionButton(
                    icon = Icons.Default.PlayArrow,
                    label = "Play / Pause",
                    tint = AccentGreen,
                    onClick = { onExecuteAction("media", mapOf("action" to "play_pause")) }
                )

                // Next Track
                PcQuickActionButton(
                    icon = Icons.Default.FastForward,
                    label = "Next Track",
                    tint = AccentGreen,
                    onClick = { onExecuteAction("media", mapOf("action" to "next")) }
                )

                // Vol Up
                PcQuickActionButton(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Vol +",
                    tint = NeonCyan,
                    onClick = { onExecuteAction("media", mapOf("action" to "volume_up")) }
                )

                // Vol Down
                PcQuickActionButton(
                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                    label = "Vol -",
                    tint = NeonCyan,
                    onClick = { onExecuteAction("media", mapOf("action" to "volume_down")) }
                )

                // Open Chrome
                PcQuickActionButton(
                    icon = Icons.Default.OpenInBrowser,
                    label = "Chrome",
                    tint = NeonCyan,
                    onClick = { onExecuteAction("open_app", mapOf("appName" to "chrome")) }
                )

                // Open VS Code
                PcQuickActionButton(
                    icon = Icons.Default.Code,
                    label = "VS Code",
                    tint = NeonViolet,
                    onClick = { onExecuteAction("open_app", mapOf("appName" to "vscode")) }
                )

                // Remote Typing
                PcQuickActionButton(
                    icon = Icons.Default.Keyboard,
                    label = "Type Text",
                    tint = NeonMagenta,
                    onClick = { showTypeDialog = true }
                )

                // Screenshot
                PcQuickActionButton(
                    icon = Icons.Default.CameraAlt,
                    label = "Screenshot",
                    tint = Color(0xFF00E5FF),
                    onClick = { onExecuteAction("screenshot", emptyMap()) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Last Log / Feedback Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lastLog,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = { showScriptDialog = true },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Get PC Script",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Edit PC Config Dialog
    if (showConfigDialog) {
        var tempName by remember { mutableStateOf(pcName) }
        var tempIp by remember { mutableStateOf(pcIp) }
        var tempPort by remember { mutableStateOf(pcPort.toString()) }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("PC Companion Settings", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Workstation Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempIp,
                        onValueChange = { tempIp = it },
                        label = { Text("PC IP Address (e.g. 192.168.1.100)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempPort,
                        onValueChange = { tempPort = it },
                        label = { Text("Port (Default: 8989)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val portInt = tempPort.toIntOrNull() ?: 8989
                        onSaveSettings(tempIp, portInt, tempName)
                        showConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("Save & Connect", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }

    // Type text dialog
    if (showTypeDialog) {
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            title = { Text("Remote Type Text on PC", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = textToType,
                    onValueChange = { textToType = it },
                    label = { Text("Text to type in active PC window") },
                    placeholder = { Text("e.g. Hello Boss, MM is in control!") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonMagenta,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textToType.isNotEmpty()) {
                            onExecuteAction("type_text", mapOf("text" to textToType))
                            showTypeDialog = false
                            textToType = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta, contentColor = Color.White)
                ) {
                    Text("Type on PC", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTypeDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }

    // Python Companion Script Exporter Dialog
    if (showScriptDialog) {
        AlertDialog(
            onDismissRequest = { showScriptDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PC Daemon Server (Python)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("MM PC Companion Script", pythonScriptCode)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied Python script to clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "1. Install: pip install flask pyautogui\n2. Run: python mm_pc_daemon.py on your PC",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = pythonScriptCode,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = NeonCyan.copy(alpha = 0.9f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showScriptDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }
}

@Composable
private fun PcQuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = tint.copy(alpha = 0.14f),
            contentColor = tint
        ),
        modifier = Modifier.height(34.dp).testTag("pc_btn_${label.lowercase().replace(" ", "_")}")
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
