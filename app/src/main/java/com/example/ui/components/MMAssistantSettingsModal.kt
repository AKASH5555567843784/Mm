package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhonelinkLock
import com.example.model.LiveTranscript
import com.example.model.LocalGGUFModel
import com.example.model.SassyIntensity
import com.example.model.SassyMood
import com.example.pc.RemotePcManager
import com.example.security.DeviceLockType
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch

enum class SettingsCategory(
    val title: String,
    val icon: ImageVector
) {
    PERSONA_MEMORY("Persona & History", Icons.Default.LocalFireDepartment),
    AI_INTELLIGENCE("AI Model & Engine", Icons.Default.AutoAwesome),
    MOOD_THEME("Mood & Theme", Icons.Default.Palette),
    DEVICE_UNLOCK("Phone Unlock", Icons.Default.PhonelinkLock),
    APP_LOCK("App Lock & Stealth", Icons.Default.Security),
    PRIVACY_MIC("Privacy & Audio", Icons.Default.Tune),
    CALL_ANNOUNCER("Call Announcer", Icons.Default.PhoneInTalk),
    REMOTE_PC("Remote PC", Icons.Default.Computer),
    OFFLINE_MODELS("Local Models", Icons.Default.Memory),
    SERVICE_SYSTEM("System Controls", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MMAssistantSettingsModal(
    onDismiss: () -> Unit,
    // Sassy Persona Intensity & History
    sassyIntensity: SassyIntensity = SassyIntensity.CLASSIC_SASSY,
    onIntensitySelected: (SassyIntensity) -> Unit = {},
    transcripts: List<LiveTranscript> = emptyList(),
    onClearHistory: () -> Unit = {},
    // AI Model & Intelligence Configuration
    selectedAiModel: String = com.example.gemini.GeminiLiveClient.DEFAULT_MODEL,
    temperature: Float = 0.3f,
    isZeroFabricationEnabled: Boolean = true,
    isActionOrientedEnabled: Boolean = true,
    onSelectAiModel: (String) -> Unit = {},
    onTemperatureChanged: (Float) -> Unit = {},
    onToggleZeroFabrication: (Boolean) -> Unit = {},
    onToggleActionOriented: (Boolean) -> Unit = {},
    // Sassy Mood
    currentMood: SassyMood,
    isAutoMoodDetection: Boolean,
    onMoodSelected: (SassyMood) -> Unit,
    onToggleAutoDetection: (Boolean) -> Unit,
    onTriggerMoodSample: (SassyMood) -> Unit,
    // Phone Device Lock & Unlock (PIN, Pattern, Password, Swipe)
    deviceLockType: DeviceLockType = DeviceLockType.PIN,
    deviceSavedCredential: String = "",
    isAutoVoiceUnlockEnabled: Boolean = true,
    isAccessibilityEnabled: Boolean = false,
    lastPhoneLockAction: String? = null,
    onSaveDeviceCredentials: (DeviceLockType, String, Boolean) -> Unit = { _, _, _ -> },
    onClearDeviceCredentials: () -> Unit = {},
    onToggleAutoVoiceUnlock: (Boolean) -> Unit = {},
    onUnlockPhone: () -> Unit = {},
    onLockPhone: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    // App Lock & Stealth
    isAppLockEnabled: Boolean = true,
    lockedApps: Set<String> = emptySet(),
    hiddenApps: Set<String> = emptySet(),
    isAssistantHidden: Boolean = false,
    masterPin: String = "1234",
    lastSecurityAction: String? = null,
    onToggleAppLock: (Boolean) -> Unit = {},
    onLockApp: (String) -> Unit = {},
    onUnlockApp: (String) -> Unit = {},
    onHideApp: (String) -> Unit = {},
    onUnhideApp: (String) -> Unit = {},
    onSetMasterPin: (String) -> Unit = {},
    onToggleAssistantStealth: (Boolean) -> Unit = {},
    getDisplayName: (String) -> String = { it },
    // Privacy & Mic
    isPrivacyMode: Boolean,
    isMuted: Boolean,
    isSpeechOutputMuted: Boolean,
    isSpeechRecognizerActive: Boolean,
    onTogglePrivacyMode: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeechMute: () -> Unit,
    onTestWakeWord: (String) -> Unit,
    // Call Announcer
    callAnnouncerEnabled: Boolean,
    onToggleCallAnnouncer: (Boolean) -> Unit,
    lastCallAnnouncement: String?,
    isAnnouncingCall: Boolean,
    onTestKnownCaller: () -> Unit,
    onTestUnknownCaller: () -> Unit,
    // Remote PC
    pcStatus: RemotePcManager.PcConnectionStatus,
    pcName: String,
    pcIp: String,
    pcPort: Int,
    pcLatency: Long?,
    pcLastLog: String,
    onPingPc: () -> Unit,
    onSavePcSettings: (String, Int, String) -> Unit,
    onExecutePcAction: (String, Map<String, Any?>) -> Unit,
    pythonScriptCode: String,
    // Local Models
    localModels: List<LocalGGUFModel>,
    storageUsedFormatted: String,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    // Service Controls
    isServiceRunning: Boolean,
    wakeWordSensitivity: Float,
    isBatteryExempt: Boolean,
    onToggleService: () -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestDisableBatteryOptimization: () -> Unit,
    onVolumePresetSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val mood = LocalSassyMood.current
    val scope = rememberCoroutineScope()
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null,
        modifier = modifier.testTag("settings_modal_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .navigationBarsPadding()
        ) {
            // Header Bar
            Surface(
                color = DarkSurface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, mood.primaryColor.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(mood.primaryColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = mood.primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Assistant Settings",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = "Preferences, Privacy & Companion Tools",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Categories Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = DarkSurface.copy(alpha = 0.7f),
                contentColor = mood.primaryColor,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (selectedCategoryIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                            color = mood.primaryColor,
                            height = 3.dp
                        )
                    }
                },
                divider = {}
            ) {
                SettingsCategory.entries.forEachIndexed { index, category ->
                    val isSelected = selectedCategoryIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedCategoryIndex = index },
                        modifier = Modifier.testTag("settings_tab_${category.name.lowercase()}"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) mood.primaryColor else TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) TextPrimary else TextTertiary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    )
                }
            }

            // Category Content Container
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (SettingsCategory.entries[selectedCategoryIndex]) {
                    SettingsCategory.PERSONA_MEMORY -> {
                        SassyPersonaAndHistoryCard(
                            currentIntensity = sassyIntensity,
                            onIntensitySelected = onIntensitySelected,
                            transcripts = transcripts,
                            onClearHistory = onClearHistory,
                            currentMood = currentMood
                        )
                        SassyMoodControlCard(
                            currentMood = currentMood,
                            isAutoDetectionEnabled = isAutoMoodDetection,
                            onMoodSelected = onMoodSelected,
                            onToggleAutoDetection = onToggleAutoDetection,
                            onTriggerMoodSample = onTriggerMoodSample
                        )
                    }

                    SettingsCategory.AI_INTELLIGENCE -> {
                        SassyPersonaAndHistoryCard(
                            currentIntensity = sassyIntensity,
                            onIntensitySelected = onIntensitySelected,
                            transcripts = transcripts,
                            onClearHistory = onClearHistory,
                            currentMood = currentMood
                        )
                        AiModelSettingsCard(
                            selectedModel = selectedAiModel,
                            temperature = temperature,
                            isZeroFabricationEnabled = isZeroFabricationEnabled,
                            isActionOrientedEnabled = isActionOrientedEnabled,
                            onModelSelected = onSelectAiModel,
                            onTemperatureChanged = onTemperatureChanged,
                            onToggleZeroFabrication = onToggleZeroFabrication,
                            onToggleActionOriented = onToggleActionOriented
                        )
                    }

                    SettingsCategory.MOOD_THEME -> {
                        SassyMoodControlCard(
                            currentMood = currentMood,
                            isAutoDetectionEnabled = isAutoMoodDetection,
                            onMoodSelected = onMoodSelected,
                            onToggleAutoDetection = onToggleAutoDetection,
                            onTriggerMoodSample = onTriggerMoodSample
                        )
                        SassyPersonaAndHistoryCard(
                            currentIntensity = sassyIntensity,
                            onIntensitySelected = onIntensitySelected,
                            transcripts = transcripts,
                            onClearHistory = onClearHistory,
                            currentMood = currentMood
                        )
                    }

                    SettingsCategory.DEVICE_UNLOCK -> {
                        DeviceLockUnlockCard(
                            currentLockType = deviceLockType,
                            savedCredential = deviceSavedCredential,
                            isAutoVoiceUnlockEnabled = isAutoVoiceUnlockEnabled,
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            lastAction = lastPhoneLockAction,
                            onSaveCredentials = onSaveDeviceCredentials,
                            onClearCredentials = onClearDeviceCredentials,
                            onToggleAutoVoiceUnlock = onToggleAutoVoiceUnlock,
                            onUnlockPhone = onUnlockPhone,
                            onLockPhone = onLockPhone,
                            onOpenAccessibilitySettings = onOpenAccessibilitySettings
                        )
                    }

                    SettingsCategory.APP_LOCK -> {
                        AppSecurityLockCard(
                            isAppLockEnabled = isAppLockEnabled,
                            lockedApps = lockedApps,
                            hiddenApps = hiddenApps,
                            isAssistantHidden = isAssistantHidden,
                            masterPin = masterPin,
                            lastSecurityAction = lastSecurityAction,
                            onToggleAppLock = onToggleAppLock,
                            onLockApp = onLockApp,
                            onUnlockApp = onUnlockApp,
                            onHideApp = onHideApp,
                            onUnhideApp = onUnhideApp,
                            onSetMasterPin = onSetMasterPin,
                            onToggleAssistantStealth = onToggleAssistantStealth,
                            getDisplayName = getDisplayName
                        )
                    }

                    SettingsCategory.PRIVACY_MIC -> {
                        PrivacyShieldCard(
                            isPrivacyModeActive = isPrivacyMode,
                            isMicMuted = isMuted,
                            isSpeechOutputMuted = isSpeechOutputMuted,
                            isSpeechRecognizerActive = isSpeechRecognizerActive,
                            onTogglePrivacyMode = onTogglePrivacyMode,
                            onToggleMicMute = onToggleMute,
                            onToggleSpeechMute = onToggleSpeechMute,
                            onTestWakeWord = onTestWakeWord
                        )
                    }

                    SettingsCategory.CALL_ANNOUNCER -> {
                        CallAnnouncerCard(
                            isEnabled = callAnnouncerEnabled,
                            onToggleEnabled = onToggleCallAnnouncer,
                            lastAnnouncement = lastCallAnnouncement,
                            isAnnouncing = isAnnouncingCall,
                            onTestKnownCaller = onTestKnownCaller,
                            onTestUnknownCaller = onTestUnknownCaller
                        )
                    }

                    SettingsCategory.REMOTE_PC -> {
                        RemotePcControlCard(
                            status = pcStatus,
                            pcName = pcName,
                            pcIp = pcIp,
                            pcPort = pcPort,
                            latencyMs = pcLatency,
                            lastLog = pcLastLog,
                            onPing = onPingPc,
                            onSaveSettings = onSavePcSettings,
                            onExecuteAction = onExecutePcAction,
                            pythonScriptCode = pythonScriptCode
                        )
                    }

                    SettingsCategory.OFFLINE_MODELS -> {
                        LocalModelManagerSection(
                            models = localModels,
                            storageUsedFormatted = storageUsedFormatted,
                            onSelectModel = onSelectModel,
                            onDownloadModel = onDownloadModel,
                            onDeleteModel = onDeleteModel
                        )
                    }

                    SettingsCategory.SERVICE_SYSTEM -> {
                        ServiceControlsSheet(
                            isServiceRunning = isServiceRunning,
                            isMuted = isMuted,
                            wakeWordSensitivity = wakeWordSensitivity,
                            isBatteryExempt = isBatteryExempt,
                            onToggleService = onToggleService,
                            onToggleMute = onToggleMute,
                            onSensitivityChanged = onSensitivityChanged,
                            onRequestPermissions = onRequestPermissions,
                            onRequestDisableBatteryOptimization = onRequestDisableBatteryOptimization,
                            onVolumePresetSelected = onVolumePresetSelected
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
