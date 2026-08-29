package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SpatialAudioOff
import androidx.compose.material.icons.filled.VoiceChat
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AssistantState
import com.example.model.SassyMood
import com.example.ui.components.CallAnnouncerCard
import com.example.ui.components.GlowingVoiceOrb
import com.example.ui.components.HybridModeStatusBar
import com.example.ui.components.LocalModelManagerSection
import com.example.ui.components.PermissionsOnboardingDialog
import com.example.ui.components.PrivacyShieldCard
import com.example.ui.components.RemotePcControlCard
import com.example.ui.components.SassyMoodControlCard
import com.example.ui.components.SassySubtitleCard
import com.example.ui.components.ServiceControlsSheet
import com.example.ui.components.ToolExecutionBadge
import com.example.ui.components.VoiceQuickChips
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.MMAssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MMAssistantScreen(
    viewModel: MMAssistantViewModel,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val assistantState by viewModel.assistantState.collectAsState()
    val sassyQuote by viewModel.sassyOneLiner.collectAsState()
    val activeTool by viewModel.activeToolCall.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val isPrivacyMode by viewModel.isPrivacyMode.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeechOutputMuted by viewModel.isSpeechOutputMuted.collectAsState()
    val isSpeechRecognizerActive by viewModel.isSpeechRecognizerActive.collectAsState()
    val isStandby by viewModel.isStandby.collectAsState()
    val wakeWordSensitivity by viewModel.wakeWordSensitivity.collectAsState()
    val isBatteryExempt by viewModel.isBatteryExempt.collectAsState()

    // Call Announcer & Remote PC states
    val callAnnouncerEnabled by viewModel.callAnnouncerEnabled.collectAsState()
    val lastCallAnnouncement by viewModel.lastCallAnnouncement.collectAsState()
    val isAnnouncingCall by viewModel.isAnnouncingCall.collectAsState()

    val pcStatus by viewModel.pcConnectionStatus.collectAsState()
    val pcName by viewModel.pcName.collectAsState()
    val pcIp by viewModel.pcTargetIp.collectAsState()
    val pcPort by viewModel.pcTargetPort.collectAsState()
    val pcLatency by viewModel.pcLastLatencyMs.collectAsState()
    val pcLastLog by viewModel.pcLastLog.collectAsState()

    val isOnline by viewModel.networkMonitor.isOnline.collectAsState()
    val networkType by viewModel.networkMonitor.networkType.collectAsState()
    val forceOfflineMode by viewModel.forceOfflineMode.collectAsState()
    val activeEngineName by viewModel.activeEngineName.collectAsState()
    val localModels by viewModel.localModels.collectAsState()
    val showPermissionsDialog by viewModel.showPermissionsDialog.collectAsState()

    val micAmplitude by viewModel.micAmplitude.collectAsState()
    val speakerAmplitude by viewModel.outputAmplitude.collectAsState()

    // Sassy Visual Mood & Theme Engine state
    val sassyMood by viewModel.currentSassyMood.collectAsState()
    val isAutoMoodDetection by viewModel.isAutoMoodDetectionEnabled.collectAsState()

    val combinedAmplitude = if (assistantState == AssistantState.SPEAKING) speakerAmplitude else micAmplitude

    val scrollState = rememberScrollState()

    val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    val hasCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    val hasNotif = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isServiceRunning) NeonCyan else Color.DarkGray
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MM",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    letterSpacing = 2.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ASSISTANT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Light,
                                    color = NeonMagenta,
                                    letterSpacing = 3.sp
                                )
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleService() },
                            modifier = Modifier.testTag("top_bar_power_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Toggle Background Service",
                                tint = if (isServiceRunning) NeonCyan else TextTertiary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkBackground
                    )
                )
            },
            containerColor = DarkBackground,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // Hybrid Mode Engine Bar (Gemini Live vs Open-Source Local GGUF)
                HybridModeStatusBar(
                    isOnline = isOnline,
                    networkType = networkType,
                    forceOfflineMode = forceOfflineMode,
                    onToggleForceOffline = { viewModel.toggleForceOfflineMode() },
                    activeEngineName = activeEngineName
                )

                // Central Glowing Voice Orb with Dynamic Sassy Mood Animations
                GlowingVoiceOrb(
                    state = assistantState,
                    amplitude = combinedAmplitude,
                    isMuted = isMuted,
                    onTap = { viewModel.onMicOrbTapped() },
                    mood = sassyMood,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Dynamic Waveform Visualizer
                WaveformVisualizer(
                    amplitude = combinedAmplitude,
                    isActive = assistantState == AssistantState.SPEAKING || assistantState == AssistantState.LISTENING
                )

                // Sassy live speech & witty personality card with Dynamic Mood Pill & border
                SassySubtitleCard(
                    quote = sassyQuote,
                    state = assistantState,
                    mood = sassyMood
                )

                // Interactive Sassy Mood & Theme State Manager
                SassyMoodControlCard(
                    currentMood = sassyMood,
                    isAutoDetectionEnabled = isAutoMoodDetection,
                    onMoodSelected = { viewModel.setSassyMood(it) },
                    onToggleAutoDetection = { viewModel.toggleAutoMoodDetection(it) },
                    onTriggerMoodSample = { viewModel.triggerMoodSample(it) }
                )

                // Persistent Privacy Shield Quick-Toggle (Mic & Speech Output Control)
                PrivacyShieldCard(
                    isPrivacyModeActive = isPrivacyMode,
                    isMicMuted = isMuted,
                    isSpeechOutputMuted = isSpeechOutputMuted,
                    isSpeechRecognizerActive = isSpeechRecognizerActive,
                    onTogglePrivacyMode = { viewModel.togglePrivacyMode() },
                    onToggleMicMute = { viewModel.toggleMute() },
                    onToggleSpeechMute = { viewModel.toggleSpeechMute() },
                    onTestWakeWord = { viewModel.testWakeWordTrigger(it) }
                )

                // Native Device Tool Call status card (if active)
                ToolExecutionBadge(activeTool = activeTool)

                // Quick Voice Command Trigger Chips (including identity checks)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Quick Voice Triggers",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextTertiary,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    VoiceQuickChips(
                        suggestions = viewModel.quickVoiceSuggestions,
                        onChipSelected = { prompt ->
                            viewModel.sendQuickPrompt(prompt)
                        }
                    )
                }

                // Intelligent Call Announcer Card (Silent Mode Dependent)
                CallAnnouncerCard(
                    isEnabled = callAnnouncerEnabled,
                    onToggleEnabled = { viewModel.toggleCallAnnouncer(it) },
                    lastAnnouncement = lastCallAnnouncement,
                    isAnnouncing = isAnnouncingCall,
                    onTestKnownCaller = { viewModel.testCallAnnouncement("Akash Upadhyay", forceSilent = true) },
                    onTestUnknownCaller = { viewModel.testCallAnnouncement(null, forceSilent = true) }
                )

                // Remote PC Companion Control Card
                RemotePcControlCard(
                    status = pcStatus,
                    pcName = pcName,
                    pcIp = pcIp,
                    pcPort = pcPort,
                    latencyMs = pcLatency,
                    lastLog = pcLastLog,
                    onPing = { viewModel.pingPc() },
                    onSaveSettings = { ip, port, name -> viewModel.updatePcSettings(ip, port, name) },
                    onExecuteAction = { cmd, params -> viewModel.executePcAction(cmd, params) },
                    pythonScriptCode = viewModel.remotePcManager.getPythonDaemonScript()
                )

                // In-App Open-Source Local GGUF Model Downloader & Storage Manager
                LocalModelManagerSection(
                    models = localModels,
                    storageUsedFormatted = viewModel.getStorageUsedFormatted(),
                    onSelectModel = { viewModel.selectGGUFModel(it) },
                    onDownloadModel = { viewModel.downloadGGUFModel(it) },
                    onDeleteModel = { viewModel.deleteGGUFModel(it) }
                )

                // Service & Wake-Word Controls
                ServiceControlsSheet(
                    isServiceRunning = isServiceRunning,
                    isMuted = isMuted,
                    wakeWordSensitivity = wakeWordSensitivity,
                    isBatteryExempt = isBatteryExempt,
                    onToggleService = { viewModel.toggleService() },
                    onToggleMute = { viewModel.toggleMute() },
                    onSensitivityChanged = { viewModel.setSensitivity(it) },
                    onRequestPermissions = onRequestPermissions,
                    onRequestDisableBatteryOptimization = { viewModel.requestDisableBatteryOptimization() },
                    onVolumePresetSelected = { viewModel.executeQuickToolDirectly(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Permissions Onboarding Dialog
        if (showPermissionsDialog && (!hasAudio || !hasContacts || !hasCall)) {
            PermissionsOnboardingDialog(
                hasRecordAudio = hasAudio,
                hasReadContacts = hasContacts,
                hasCallPhone = hasCall,
                hasNotification = hasNotif,
                onRequestPermissions = {
                    onRequestPermissions()
                    viewModel.dismissPermissionsDialog()
                },
                onDismiss = {
                    viewModel.dismissPermissionsDialog()
                }
            )
        }
    }
}
