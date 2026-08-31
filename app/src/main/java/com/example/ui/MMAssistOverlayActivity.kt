package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.model.AssistantState
import com.example.service.MMAssistantForegroundService
import com.example.ui.components.AssistantBottomRevealSheet
import com.example.ui.components.MMAssistantSettingsModal
import com.example.ui.components.NeonGlowingEdgeOverlay
import com.example.ui.theme.MMAssistantTheme
import com.example.viewmodel.MMAssistantViewModel

/**
 * System-wide Default Digital Assistant Overlay Activity.
 * Intercepts android.intent.action.ASSIST / VOICE_ASSIST when triggered via:
 * - Physical Power / Side button long-press
 * - Bottom corner swipe up navigation gesture
 * - Hardware assistant shortcuts
 *
 * Renders the Neon Glowing Edge perimeter lighting and the Bottom-Up Reveal floating card.
 */
class MMAssistOverlayActivity : ComponentActivity() {

    private val viewModel: MMAssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow display over lock screen and wake device if locked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Ensure background foreground service is running and awaken assistant
        MMAssistantForegroundService.startService(this)
        MMAssistantForegroundService.activeServiceInstance?.triggerWakeWordAwakening()

        val triggerSource = intent?.getStringExtra("trigger_source") ?: "SYSTEM_ASSIST_INTENT"

        setContent {
            val sassyMood by viewModel.currentSassyMood.collectAsState()
            val assistantState by viewModel.assistantState.collectAsState()
            val sassyQuote by viewModel.sassyOneLiner.collectAsState()
            val micAmplitude by viewModel.micAmplitude.collectAsState()
            val speakerAmplitude by viewModel.outputAmplitude.collectAsState()
            val isMuted by viewModel.isMuted.collectAsState()
            val isSpeechOutputMuted by viewModel.isSpeechOutputMuted.collectAsState()
            val isSpeechRecognizerActive by viewModel.isSpeechRecognizerActive.collectAsState()
            val isPrivacyMode by viewModel.isPrivacyMode.collectAsState()

            val selectedAiModel by viewModel.selectedAiModel.collectAsState()
            val temperature by viewModel.temperature.collectAsState()
            val isZeroFabricationEnabled by viewModel.isZeroFabricationEnabled.collectAsState()
            val isActionOrientedEnabled by viewModel.isActionOrientedEnabled.collectAsState()

            val isAutoMoodDetection by viewModel.isAutoMoodDetectionEnabled.collectAsState()
            val deviceLockType by viewModel.deviceLockType.collectAsState()
            val deviceSavedCredential by viewModel.deviceSavedCredential.collectAsState()
            val isAutoVoiceUnlockEnabled by viewModel.isAutoVoiceUnlockEnabled.collectAsState()
            val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
            val lastPhoneLockAction by viewModel.lastPhoneLockAction.collectAsState()

            val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
            val lockedApps by viewModel.lockedApps.collectAsState()
            val hiddenApps by viewModel.hiddenApps.collectAsState()
            val isAssistantHidden by viewModel.isAssistantHidden.collectAsState()
            val masterPin by viewModel.masterPin.collectAsState()
            val lastSecurityAction by viewModel.lastSecurityAction.collectAsState()

            val callAnnouncerEnabled by viewModel.callAnnouncerEnabled.collectAsState()
            val lastCallAnnouncement by viewModel.lastCallAnnouncement.collectAsState()
            val isAnnouncingCall by viewModel.isAnnouncingCall.collectAsState()

            val pcStatus by viewModel.pcConnectionStatus.collectAsState()
            val pcName by viewModel.pcName.collectAsState()
            val pcIp by viewModel.pcTargetIp.collectAsState()
            val pcPort by viewModel.pcTargetPort.collectAsState()
            val pcLatency by viewModel.pcLastLatencyMs.collectAsState()
            val pcLastLog by viewModel.pcLastLog.collectAsState()

            val localModels by viewModel.localModels.collectAsState()
            val isServiceRunning by viewModel.isServiceRunning.collectAsState()
            val wakeWordSensitivity by viewModel.wakeWordSensitivity.collectAsState()
            val isBatteryExempt by viewModel.isBatteryExempt.collectAsState()

            var isSettingsOpen by remember { mutableStateOf(false) }

            val combinedAmplitude = if (assistantState == AssistantState.SPEAKING) speakerAmplitude else micAmplitude

            MMAssistantTheme(sassyMood = sassyMood) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            finish()
                        }
                ) {
                    // 1. Sleek Neon Glowing Edge Animation around screen perimeter
                    NeonGlowingEdgeOverlay(
                        assistantState = assistantState,
                        audioAmplitude = combinedAmplitude,
                        isEdgeLightingEnabled = true
                    )

                    // 2. Bottom-Up Sliding Minimalist Interactive Card
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            AssistantBottomRevealSheet(
                                assistantState = assistantState,
                                sassyQuote = sassyQuote,
                                audioAmplitude = combinedAmplitude,
                                isMuted = isMuted,
                                onToggleMute = { viewModel.toggleMute() },
                                onReconnect = { viewModel.reconnect() },
                                onDismiss = { finish() },
                                onOpenSettings = { isSettingsOpen = true },
                                onUnlockPhone = { viewModel.unlockPhone() },
                                onToggleFlashlight = { viewModel.toggleFlashlight() },
                                triggerSource = triggerSource
                            )
                        }
                    }

                    // Settings Dialog
                    if (isSettingsOpen) {
                        MMAssistantSettingsModal(
                            onDismiss = { isSettingsOpen = false },
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
                            onRequestPermissions = {},
                            onRequestDisableBatteryOptimization = { viewModel.requestDisableBatteryOptimization() },
                            onVolumePresetSelected = { viewModel.executeQuickToolDirectly(it) }
                        )
                    }
                }
            }
        }
    }
}
