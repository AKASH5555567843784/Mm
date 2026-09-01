package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import com.example.ui.components.AssistantBottomRevealSheet
import com.example.ui.components.NeonGlowingEdgeOverlay
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AssistantState
import com.example.ui.components.AssistantBottomControlBar
import com.example.ui.components.GlowingVoiceOrb
import com.example.ui.components.MMAssistantSettingsModal
import com.example.ui.components.OnboardingFeatureOverlay
import com.example.ui.components.PermissionsOnboardingDialog
import com.example.ui.components.SassySubtitleCard
import com.example.ui.components.ToolExecutionBadge
import com.example.ui.components.VoiceQuickChips
import com.example.ui.components.WakeWordStatusIndicator
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.NeonCyan
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
    val isWakeWordListening by viewModel.isWakeWordListening.collectAsState()
    val isSpeechRecognizerActive by viewModel.isSpeechRecognizerActive.collectAsState()
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
    val forceOfflineMode by viewModel.forceOfflineMode.collectAsState()
    val activeEngineName by viewModel.activeEngineName.collectAsState()
    val localModels by viewModel.localModels.collectAsState()
    val showPermissionsDialog by viewModel.showPermissionsDialog.collectAsState()
    val showOnboardingOverlay by viewModel.showOnboardingOverlay.collectAsState()

    val micAmplitude by viewModel.micAmplitude.collectAsState()
    val speakerAmplitude by viewModel.outputAmplitude.collectAsState()

    // Sassy Visual Mood & Theme Engine state
    val sassyMood by viewModel.currentSassyMood.collectAsState()
    val isAutoMoodDetection by viewModel.isAutoMoodDetectionEnabled.collectAsState()

    // Device Screen Lock & Unlock states (PIN, Pattern, Password, Swipe)
    val deviceLockType by viewModel.deviceLockType.collectAsState()
    val deviceSavedCredential by viewModel.deviceSavedCredential.collectAsState()
    val isAutoVoiceUnlockEnabled by viewModel.isAutoVoiceUnlockEnabled.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val lastPhoneLockAction by viewModel.lastPhoneLockAction.collectAsState()

    // App Lock & Stealth states
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val lockedApps by viewModel.lockedApps.collectAsState()
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val isAssistantHidden by viewModel.isAssistantHidden.collectAsState()
    val masterPin by viewModel.masterPin.collectAsState()
    val lastSecurityAction by viewModel.lastSecurityAction.collectAsState()

    // AI Intelligence Configuration states
    val selectedAiModel by viewModel.selectedAiModel.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val isZeroFabricationEnabled by viewModel.isZeroFabricationEnabled.collectAsState()
    val isActionOrientedEnabled by viewModel.isActionOrientedEnabled.collectAsState()
    val sassyIntensity by viewModel.sassyIntensity.collectAsState()
    val transcripts by viewModel.transcripts.collectAsState()

    var isSettingsOpen by remember { mutableStateOf(false) }
    var showRevealPreview by remember { mutableStateOf(false) }

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
                            // Service state indicator dot
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
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
                                    color = sassyMood.primaryColor,
                                    letterSpacing = 3.sp
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        // Engine pill badge (Tappable to toggle Cloud / Local GGUF)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurface.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, sassyMood.primaryColor.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clickable { viewModel.toggleForceOfflineMode() }
                                .testTag("top_engine_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (forceOfflineMode || !isOnline) Icons.Default.Memory else Icons.Default.Cloud,
                                    contentDescription = "Active Engine",
                                    tint = if (forceOfflineMode || !isOnline) Color(0xFFFFB703) else sassyMood.secondaryColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (forceOfflineMode || !isOnline) "GGUF" else "GEMINI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick Guide & Onboarding Overlay Trigger
                        IconButton(
                            onClick = { viewModel.showOnboardingOverlay() },
                            modifier = Modifier.testTag("top_bar_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Voice & Sassy Mood Guide",
                                tint = sassyMood.primaryColor.copy(alpha = 0.85f)
                            )
                        }

                        // Background Service Quick Toggle
                        IconButton(
                            onClick = { viewModel.toggleService() },
                            modifier = Modifier.testTag("top_bar_power_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Toggle Background Service",
                                tint = if (isServiceRunning) sassyMood.secondaryColor else TextTertiary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkBackground
                    )
                )
            },
            bottomBar = {
                // Fixed Modern Bottom Control Bar (Mute, Mic Action, Settings)
                AssistantBottomControlBar(
                    assistantState = assistantState,
                    isMuted = isMuted,
                    onToggleMute = { viewModel.toggleMute() },
                    onMicTap = { viewModel.onMicOrbTapped() },
                    onOpenSettings = { isSettingsOpen = true },
                    mood = sassyMood
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
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Persistent Real-Time Wake-Word Listening Status Indicator
                    WakeWordStatusIndicator(
                        isServiceRunning = isServiceRunning,
                        isPrivacyMode = isPrivacyMode,
                        isMuted = isMuted,
                        isWakeWordListening = isWakeWordListening,
                        onToggleListening = { viewModel.togglePrivacyMode() },
                        onTestWakeTrigger = { phrase -> viewModel.testWakeWordTrigger(phrase) },
                        mood = sassyMood,
                        sensitivity = wakeWordSensitivity,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Dynamic Tool Execution Badge (shown cleanly when a device tool is executing)
                    AnimatedVisibility(
                        visible = activeTool != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ToolExecutionBadge(
                            activeTool = activeTool,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Central Glowing Voice & Sassy Mood Orb
                    GlowingVoiceOrb(
                        state = assistantState,
                        amplitude = combinedAmplitude,
                        isMuted = isMuted,
                        onTap = { viewModel.onMicOrbTapped() },
                        mood = sassyMood,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Dynamic Reactive Waveform & Pulse Visualizer
                    WaveformVisualizer(
                        amplitude = combinedAmplitude,
                        isActive = assistantState == AssistantState.SPEAKING || assistantState == AssistantState.LISTENING,
                        state = assistantState,
                        mood = sassyMood
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sassy Subtitle & Live Speech Card with dynamic mood pill & TTS vocalize action
                    SassySubtitleCard(
                        quote = sassyQuote,
                        state = assistantState,
                        mood = sassyMood,
                        onVocalizeClick = { viewModel.vocalizeCurrentResponse() },
                        isSpeaking = assistantState == AssistantState.SPEAKING
                    )
                }

                // Bottom Section: Quick Voice Triggers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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

                        Text(
                            text = "⚡ Preview Edge Glow & Card",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = sassyMood.primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showRevealPreview = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("preview_edge_glow_button")
                        )
                    }

                    VoiceQuickChips(
                        suggestions = viewModel.quickVoiceSuggestions,
                        onChipSelected = { prompt ->
                            viewModel.sendQuickPrompt(prompt)
                        }
                    )
                }
            }
        }

        // Dedicated Settings Modal / BottomSheet
        if (isSettingsOpen) {
            MMAssistantSettingsModal(
                onDismiss = { isSettingsOpen = false },
                sassyIntensity = sassyIntensity,
                onIntensitySelected = { viewModel.setSassyIntensity(it) },
                transcripts = transcripts,
                onClearHistory = { viewModel.clearConversationHistory() },
                selectedAiModel = selectedAiModel,
                temperature = temperature,
                isZeroFabricationEnabled = isZeroFabricationEnabled,
                isActionOrientedEnabled = isActionOrientedEnabled,
                onSelectAiModel = { viewModel.updateAiModel(it) },
                onTemperatureChanged = { viewModel.updateTemperature(it) },
                onToggleZeroFabrication = { viewModel.toggleZeroFabrication(it) },
                onToggleActionOriented = { viewModel.toggleActionOriented(it) },
                currentMood = sassyMood,
                isAutoMoodDetection = isAutoMoodDetection,
                onMoodSelected = { viewModel.setSassyMood(it) },
                onToggleAutoDetection = { viewModel.toggleAutoMoodDetection(it) },
                onTriggerMoodSample = { viewModel.triggerMoodSample(it) },
                // Phone Device Lock & Unlock (PIN, Pattern, Password, Swipe)
                deviceLockType = deviceLockType,
                deviceSavedCredential = deviceSavedCredential,
                isAutoVoiceUnlockEnabled = isAutoVoiceUnlockEnabled,
                isAccessibilityEnabled = isAccessibilityEnabled,
                lastPhoneLockAction = lastPhoneLockAction,
                onSaveDeviceCredentials = { type, cred, auto -> viewModel.saveDeviceCredentials(type, cred, auto) },
                onClearDeviceCredentials = { viewModel.clearDeviceCredentials() },
                onToggleAutoVoiceUnlock = { viewModel.toggleAutoVoiceUnlock(it) },
                onUnlockPhone = { viewModel.unlockPhone() },
                onLockPhone = { viewModel.lockPhone() },
                onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                // App Lock & Stealth
                isAppLockEnabled = isAppLockEnabled,
                lockedApps = lockedApps,
                hiddenApps = hiddenApps,
                isAssistantHidden = isAssistantHidden,
                masterPin = masterPin,
                lastSecurityAction = lastSecurityAction,
                onToggleAppLock = { viewModel.toggleAppLockEnabled(it) },
                onLockApp = { viewModel.lockApp(it) },
                onUnlockApp = { viewModel.unlockApp(it) },
                onHideApp = { viewModel.hideApp(it) },
                onUnhideApp = { viewModel.unhideApp(it) },
                onSetMasterPin = { viewModel.setMasterPin(it) },
                onToggleAssistantStealth = { viewModel.toggleAssistantStealth(it) },
                getDisplayName = { viewModel.getAppDisplayName(it) },
                // Privacy & Audio
                isPrivacyMode = isPrivacyMode,
                isMuted = isMuted,
                isSpeechOutputMuted = isSpeechOutputMuted,
                isSpeechRecognizerActive = isSpeechRecognizerActive,
                onTogglePrivacyMode = { viewModel.togglePrivacyMode() },
                onToggleMute = { viewModel.toggleMute() },
                onToggleSpeechMute = { viewModel.toggleSpeechMute() },
                onTestWakeWord = { viewModel.testWakeWordTrigger(it) },
                callAnnouncerEnabled = callAnnouncerEnabled,
                onToggleCallAnnouncer = { viewModel.toggleCallAnnouncer(it) },
                lastCallAnnouncement = lastCallAnnouncement,
                isAnnouncingCall = isAnnouncingCall,
                onTestKnownCaller = { viewModel.testCallAnnouncement("Akash Upadhyay", forceSilent = true) },
                onTestUnknownCaller = { viewModel.testCallAnnouncement(null, forceSilent = true) },
                pcStatus = pcStatus,
                pcName = pcName,
                pcIp = pcIp,
                pcPort = pcPort,
                pcLatency = pcLatency,
                pcLastLog = pcLastLog,
                onPingPc = { viewModel.pingPc() },
                onSavePcSettings = { ip, port, name -> viewModel.updatePcSettings(ip, port, name) },
                onExecutePcAction = { cmd, params -> viewModel.executePcAction(cmd, params) },
                pythonScriptCode = viewModel.remotePcManager.getPythonDaemonScript(),
                localModels = localModels,
                storageUsedFormatted = viewModel.getStorageUsedFormatted(),
                onSelectModel = { viewModel.selectGGUFModel(it) },
                onDownloadModel = { viewModel.downloadGGUFModel(it) },
                onDeleteModel = { viewModel.deleteGGUFModel(it) },
                isServiceRunning = isServiceRunning,
                wakeWordSensitivity = wakeWordSensitivity,
                isBatteryExempt = isBatteryExempt,
                onToggleService = { viewModel.toggleService() },
                onSensitivityChanged = { viewModel.setSensitivity(it) },
                onRequestPermissions = onRequestPermissions,
                onRequestDisableBatteryOptimization = { viewModel.requestDisableBatteryOptimization() },
                onVolumePresetSelected = { viewModel.executeQuickToolDirectly(it) }
            )
        }

        // First-Launch Non-Intrusive Onboarding Overlay (Voice-Trigger Gestures & Sassy Personas)
        OnboardingFeatureOverlay(
            isVisible = showOnboardingOverlay,
            currentMood = sassyMood,
            onMoodSelected = { viewModel.setSassyMood(it) },
            onDismiss = { viewModel.dismissOnboardingOverlay() }
        )

        // Holographic Neon Perimeter Edge Lighting (Active on listening, thinking, speaking or trigger preview)
        NeonGlowingEdgeOverlay(
            assistantState = assistantState,
            audioAmplitude = combinedAmplitude,
            isEdgeLightingEnabled = showRevealPreview || assistantState == AssistantState.LISTENING || assistantState == AssistantState.THINKING || assistantState == AssistantState.SPEAKING
        )

        // Minimalist Bottom-Up Reveal Interactive Sheet Preview
        if (showRevealPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showRevealPreview = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    AssistantBottomRevealSheet(
                        assistantState = assistantState,
                        sassyQuote = sassyQuote,
                        audioAmplitude = combinedAmplitude,
                        isMuted = isMuted,
                        onToggleMute = { viewModel.toggleMute() },
                        onReconnect = { viewModel.reconnect() },
                        onDismiss = { showRevealPreview = false },
                        onOpenSettings = {
                            showRevealPreview = false
                            isSettingsOpen = true
                        },
                        onUnlockPhone = { viewModel.unlockPhone() },
                        onToggleFlashlight = { viewModel.toggleFlashlight() },
                        triggerSource = "IN_APP_PREVIEW"
                    )
                }
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
