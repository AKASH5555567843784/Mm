package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRecordStreamer
import com.example.audio.AudioStreamPlayer
import com.example.audio.OfflineSpeechEngine
import com.example.data.local.MMAssistantDatabase
import com.example.data.repository.ConversationRepository
import com.example.gemini.GeminiLiveClient
import com.example.model.AssistantState
import com.example.model.LiveTranscript
import com.example.model.LocalGGUFModel
import com.example.model.ToolCallInfo
import com.example.network.NetworkStateMonitor
import com.example.offline.LocalInferenceEngine
import com.example.offline.LocalModelStorageManager
import com.example.service.MMAssistantForegroundService
import com.example.tools.DeviceToolManager
import com.example.util.BatteryOptimizationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MMAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val toolManager = DeviceToolManager(context)

    // Local Room Database & Conversation History Repository
    private val database = MMAssistantDatabase.getInstance(context)
    val conversationRepository = ConversationRepository(database.conversationDao())

    // Hybrid Engine & Network Monitor
    val networkMonitor = NetworkStateMonitor(context)
    val modelStorageManager = LocalModelStorageManager(context)
    val localInferenceEngine = LocalInferenceEngine(context, toolManager, modelStorageManager)

    // Call Announcer & Remote PC Ecosystem
    val callAnnouncerManager = com.example.telephony.CallAnnouncerManager.getInstance(context)
    val remotePcManager = com.example.pc.RemotePcManager.getInstance(context)

    // Offline Native Speech Recognition & TTS
    private var offlineSpeechEngine: OfflineSpeechEngine? = null

    // Manual Force Offline override toggle
    private val _forceOfflineMode = MutableStateFlow(false)
    val forceOfflineMode: StateFlow<Boolean> = _forceOfflineMode.asStateFlow()

    // Preferences & First-Launch Onboarding overlay state
    private val appPrefs = context.getSharedPreferences("mm_assistant_prefs", Context.MODE_PRIVATE)
    private val _showOnboardingOverlay = MutableStateFlow(!appPrefs.getBoolean("has_seen_first_launch_onboarding", false))
    val showOnboardingOverlay: StateFlow<Boolean> = _showOnboardingOverlay.asStateFlow()

    // Room Database Interaction Cache & Preferences DataStore
    val interactionRepository = com.example.data.local.InteractionRepository.getInstance(context)
    val settingsDataStore = com.example.data.MMSettingsDataStore.getInstance(context)

    val cachedInteractions: StateFlow<List<com.example.data.local.InteractionEntity>> = interactionRepository.recentInteractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val interactionCount: StateFlow<Int> = interactionRepository.interactionCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sassinessLevel: StateFlow<com.example.model.SassinessLevel> = settingsDataStore.sassinessLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.model.SassinessLevel.SASSY)

    val isBatteryAdaptiveEnabled: StateFlow<Boolean> = settingsDataStore.isBatteryAdaptiveWakeWordEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isInteractionCacheEnabled: StateFlow<Boolean> = settingsDataStore.isInteractionCacheEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currentBatteryOptimizationMode: StateFlow<com.example.model.BatteryOptimizationMode> = MMAssistantForegroundService.currentBatteryMode
    val currentBatteryPct: StateFlow<Int> = MMAssistantForegroundService.batteryLevel
    val isDeviceCharging: StateFlow<Boolean> = MMAssistantForegroundService.isDeviceCharging

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    // Dedicated Minimal Listening Screen Mode State
    private val _isMinimalListeningMode = MutableStateFlow(false)
    val isMinimalListeningMode: StateFlow<Boolean> = _isMinimalListeningMode.asStateFlow()

    fun toggleListeningScreenMode() {
        _isMinimalListeningMode.value = !_isMinimalListeningMode.value
    }

    fun setMinimalListeningMode(enabled: Boolean) {
        _isMinimalListeningMode.value = enabled
    }

    // Permissions onboarding state
    private val _showPermissionsDialog = MutableStateFlow(false)
    val showPermissionsDialog: StateFlow<Boolean> = _showPermissionsDialog.asStateFlow()

    val isServiceRunning: StateFlow<Boolean> = MMAssistantForegroundService.isRunning
    val isPrivacyMode: StateFlow<Boolean> = MMAssistantForegroundService.isPrivacyMode
    val isMuted: StateFlow<Boolean> = MMAssistantForegroundService.isMuted
    val isSpeechOutputMuted: StateFlow<Boolean> = MMAssistantForegroundService.isSpeechOutputMuted
    val isStandby: StateFlow<Boolean> = MMAssistantForegroundService.isStandby
    val isWakeWordListening: StateFlow<Boolean> = MMAssistantForegroundService.isWakeWordListening

    private val _lastSpeechWakePhrase = MutableStateFlow<String?>("Hey MM")
    val lastSpeechWakePhrase: StateFlow<String?> = _lastSpeechWakePhrase.asStateFlow()

    private val _speechWakeActive = MutableStateFlow(true)
    val isSpeechRecognizerActive: StateFlow<Boolean> = _speechWakeActive.asStateFlow()

    // Remote PC states
    val pcConnectionStatus: StateFlow<com.example.pc.RemotePcManager.PcConnectionStatus> = remotePcManager.status
    val pcTargetIp: StateFlow<String> = remotePcManager.targetIp
    val pcTargetPort: StateFlow<Int> = remotePcManager.targetPort
    val pcName: StateFlow<String> = remotePcManager.pcName
    val pcLastLog: StateFlow<String> = remotePcManager.lastLog
    val pcLastLatencyMs: StateFlow<Long?> = remotePcManager.lastLatencyMs

    // Call Announcer states
    val lastCallAnnouncement: StateFlow<String?> = callAnnouncerManager.lastAnnouncement
    val isAnnouncingCall: StateFlow<Boolean> = callAnnouncerManager.isAnnouncing
    val callAnnouncerEnabled: StateFlow<Boolean> = callAnnouncerManager.announcerEnabled

    private val _wakeWordSensitivity = MutableStateFlow(0.65f)
    val wakeWordSensitivity: StateFlow<Float> = _wakeWordSensitivity.asStateFlow()

    private val _localAssistantState = MutableStateFlow(AssistantState.DISCONNECTED)
    val assistantState: StateFlow<AssistantState> = _localAssistantState.asStateFlow()

    private val _localTranscripts = MutableStateFlow<List<LiveTranscript>>(emptyList())
    val transcripts: StateFlow<List<LiveTranscript>> = _localTranscripts.asStateFlow()

    private val _localActiveTool = MutableStateFlow<ToolCallInfo?>(null)
    val activeToolCall: StateFlow<ToolCallInfo?> = _localActiveTool.asStateFlow()

    private val _localSassyQuote = MutableStateFlow("Hello Boss! MM is ready to take over your phone.")
    val sassyOneLiner: StateFlow<String> = _localSassyQuote.asStateFlow()

    // Sassy Persona Intensity & Tone Settings
    private val _sassyIntensity = MutableStateFlow(
        com.example.model.SassyIntensity.fromId(appPrefs.getString("sassy_intensity", com.example.model.SassyIntensity.DEFAULT.id))
    )
    val sassyIntensity: StateFlow<com.example.model.SassyIntensity> = _sassyIntensity.asStateFlow()

    // Sassy Mood & Visual Theme State Manager
    private val _currentSassyMood = MutableStateFlow(com.example.model.SassyMood.CHARMING_SASSY)
    val currentSassyMood: StateFlow<com.example.model.SassyMood> = _currentSassyMood.asStateFlow()

    private val _isAutoMoodDetectionEnabled = MutableStateFlow(true)
    val isAutoMoodDetectionEnabled: StateFlow<Boolean> = _isAutoMoodDetectionEnabled.asStateFlow()

    private val _isBatteryExempt = MutableStateFlow(BatteryOptimizationHelper.isBatteryOptimizationIgnored(context))
    val isBatteryExempt: StateFlow<Boolean> = _isBatteryExempt.asStateFlow()

    private val _micLevel = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micLevel.asStateFlow()

    private val _speakerLevel = MutableStateFlow(0f)
    val outputAmplitude: StateFlow<Float> = _speakerLevel.asStateFlow()

    val localModels: StateFlow<List<LocalGGUFModel>> = modelStorageManager.models
    val activeEngineName: StateFlow<String> = combine(
        networkMonitor.isOnline,
        _forceOfflineMode,
        modelStorageManager.models
    ) { online, forceOffline, models ->
        if (online && !forceOffline) {
            "Gemini Live (Cloud)"
        } else {
            val selected = models.find { it.isSelected } ?: models.firstOrNull()
            selected?.name ?: "Local GGUF"
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Gemini Live (Cloud)")

    // Sassy prompt suggestions for fast voice interaction
    val quickVoiceSuggestions = listOf(
        "Hey MM",
        "Hello MM",
        "Okay MM",
        "MM Unlock My Phone",
        "Lock Phone",
        "Lock WhatsApp",
        "Lock Instagram",
        "Hide App (Stealth)",
        "Show Locked Apps",
        "Lock PC",
        "Open YouTube",
        "Who created you?",
        "Which country made you?",
        "Bye MM (Standby)",
        "Call Mom",
        "Turn on Flashlight",
        "Check Battery Status"
    )

    // Device Screen Lock & Unlock Manager (PIN, Pattern, Password, Swipe)
    val deviceLockUnlockManager = com.example.security.DeviceLockUnlockManager.getInstance(context)
    val deviceLockType: StateFlow<com.example.security.DeviceLockType> = deviceLockUnlockManager.lockType
    val deviceSavedCredential: StateFlow<String> = deviceLockUnlockManager.savedCredential
    val isAutoVoiceUnlockEnabled: StateFlow<Boolean> = deviceLockUnlockManager.isAutoVoiceUnlockEnabled
    val lastPhoneLockAction: StateFlow<String?> = deviceLockUnlockManager.lastAction
    val isAccessibilityEnabled: StateFlow<Boolean> = deviceLockUnlockManager.isAccessibilityEnabled

    // AI Intelligence, Model Architecture & Temperature Configuration
    private val _selectedAiModel = MutableStateFlow(com.example.gemini.GeminiLiveClient.DEFAULT_MODEL)
    val selectedAiModel: StateFlow<String> = _selectedAiModel.asStateFlow()

    private val _temperature = MutableStateFlow(0.3f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _isZeroFabricationEnabled = MutableStateFlow(true)
    val isZeroFabricationEnabled: StateFlow<Boolean> = _isZeroFabricationEnabled.asStateFlow()

    private val _isActionOrientedEnabled = MutableStateFlow(true)
    val isActionOrientedEnabled: StateFlow<Boolean> = _isActionOrientedEnabled.asStateFlow()

    fun updateAiModel(model: String) {
        _selectedAiModel.value = model
        MMAssistantForegroundService.activeServiceInstance?.geminiClient?.updateConfig(
            model = model,
            temp = _temperature.value,
            intensity = _sassyIntensity.value
        )
        _localSassyQuote.value = "AI Model switched to ${model.substringAfterLast("/")}, Boss."
    }

    fun updateTemperature(temp: Float) {
        val clamped = temp.coerceIn(0.0f, 1.0f)
        _temperature.value = clamped
        MMAssistantForegroundService.activeServiceInstance?.geminiClient?.updateConfig(
            model = _selectedAiModel.value,
            temp = clamped,
            intensity = _sassyIntensity.value
        )
    }

    fun setSassyIntensity(intensity: com.example.model.SassyIntensity) {
        _sassyIntensity.value = intensity
        appPrefs.edit().putString("sassy_intensity", intensity.id).apply()
        MMAssistantForegroundService.activeServiceInstance?.geminiClient?.updateConfig(
            model = _selectedAiModel.value,
            temp = _temperature.value,
            intensity = intensity
        )
        val quote = intensity.sampleQuotes.random()
        _localSassyQuote.value = quote
        addLocalTranscript(LiveTranscript.Sender.SYSTEM, "✨ Sassy persona intensity set to ${intensity.displayName} (${intensity.emoji})", isTool = true)
        offlineSpeechEngine?.speak(quote)
    }

    fun clearConversationHistory() {
        MMAssistantForegroundService.activeServiceInstance?.geminiClient?.clearTranscripts()
        viewModelScope.launch {
            conversationRepository.clearHistory()
            _localTranscripts.value = emptyList()
        }
        _localSassyQuote.value = "Conversation history wiped clean, Boss! Fresh slate ready."
        addLocalTranscript(LiveTranscript.Sender.SYSTEM, "🗑️ Conversation history and transcripts cleared.", isTool = true)
    }

    fun toggleZeroFabrication(enabled: Boolean) {
        _isZeroFabricationEnabled.value = enabled
        _localSassyQuote.value = if (enabled) "Zero-Fabrication mode activated, Boss." else "Standard mode active, Boss."
    }

    fun toggleActionOriented(enabled: Boolean) {
        _isActionOrientedEnabled.value = enabled
    }

    fun unlockPhone(overrideCredential: String? = null) {
        val result = deviceLockUnlockManager.unlockPhone(overrideCredential)
        _localSassyQuote.value = result.message
        addLocalTranscript(LiveTranscript.Sender.SYSTEM, "🔓 ${result.message}", isTool = true)
    }

    fun lockPhone() {
        val result = deviceLockUnlockManager.lockPhone()
        _localSassyQuote.value = result.message
        addLocalTranscript(LiveTranscript.Sender.SYSTEM, "🔒 ${result.message}", isTool = true)
    }

    fun saveDeviceCredentials(
        type: com.example.security.DeviceLockType,
        credential: String,
        autoVoiceUnlock: Boolean = true
    ) {
        val result = deviceLockUnlockManager.saveCredentials(type, credential, autoVoiceUnlock)
        _localSassyQuote.value = result.message
        addLocalTranscript(LiveTranscript.Sender.SYSTEM, "🔑 ${result.message}", isTool = true)
    }

    fun clearDeviceCredentials() {
        val result = deviceLockUnlockManager.clearCredentials()
        _localSassyQuote.value = result.message
        addLocalTranscript(LiveTranscript.Sender.SYSTEM, "🗑️ ${result.message}", isTool = true)
    }

    fun toggleAutoVoiceUnlock(enabled: Boolean) {
        deviceLockUnlockManager.setAutoVoiceUnlockEnabled(enabled)
    }

    fun openAccessibilitySettings() {
        deviceLockUnlockManager.openAccessibilitySettings()
    }

    fun refreshAccessibilityStatus() {
        deviceLockUnlockManager.checkAccessibilityStatus()
    }

    // App Lock & Stealth Vault Manager
    val appLockManager = com.example.security.AppLockManager.getInstance(context)
    val lockedApps: StateFlow<Set<String>> = appLockManager.lockedApps
    val hiddenApps: StateFlow<Set<String>> = appLockManager.hiddenApps
    val isAssistantHidden: StateFlow<Boolean> = appLockManager.isAssistantHidden
    val isAppLockEnabled: StateFlow<Boolean> = appLockManager.isAppLockEnabled
    val masterPin: StateFlow<String> = appLockManager.masterPin
    val lastSecurityAction: StateFlow<String?> = appLockManager.lastSecurityAction

    fun lockApp(appName: String, pin: String? = null) {
        val result = appLockManager.lockApp(appName, pin)
        _localSassyQuote.value = result.message
        addLocalTranscript(LiveTranscript.Sender.SYSTEM, "🔒 ${result.message}", isTool = true)
    }

    fun unlockApp(appName: String, pin: String? = null) {
        val result = appLockManager.unlockApp(appName, pin)
        _localSassyQuote.value = result.message
        addLocalTranscript(LiveTranscript.Sender.SYSTEM, "🔓 ${result.message}", isTool = true)
    }

    fun hideApp(appName: String) {
        val result = appLockManager.hideApp(appName)
        _localSassyQuote.value = result.message
        addLocalTranscript(LiveTranscript.Sender.SYSTEM, "👁️ ${result.message}", isTool = true)
    }

    fun unhideApp(appName: String) {
        val result = appLockManager.unhideApp(appName)
        _localSassyQuote.value = result.message
        addLocalTranscript(LiveTranscript.Sender.SYSTEM, "✨ ${result.message}", isTool = true)
    }

    fun toggleAppLockEnabled(enabled: Boolean) {
        appLockManager.setAppLockEnabled(enabled)
    }

    fun setMasterPin(pin: String): Boolean {
        return appLockManager.setMasterPin(pin)
    }

    fun toggleAssistantStealth(hide: Boolean) {
        val result = appLockManager.toggleAssistantLauncherVisibility(hide)
        _localSassyQuote.value = result.message
        addLocalTranscript(LiveTranscript.Sender.SYSTEM, "🥷 ${result.message}", isTool = true)
    }

    fun getAppDisplayName(packageName: String): String {
        return appLockManager.getAppDisplayName(packageName)
    }

    private var pollJob: Job? = null

    init {
        checkPermissionsInitial()
        initOfflineSpeechEngine()
        observePersistedConversations()
        startSyncLoop()
    }

    private fun observePersistedConversations() {
        viewModelScope.launch {
            conversationRepository.allTranscripts.collect { savedTranscripts ->
                _localTranscripts.value = savedTranscripts
            }
        }
    }

    private fun initOfflineSpeechEngine() {
        offlineSpeechEngine = OfflineSpeechEngine(
            context = context,
            onSpeechRecognized = { text ->
                handleOfflineUserInput(text)
            },
            onSpeechError = { errorMsg ->
                _localSassyQuote.value = errorMsg
                _localAssistantState.value = AssistantState.DISCONNECTED
            },
            onAmplitudeChanged = { amp ->
                if (_localAssistantState.value == AssistantState.SPEAKING || offlineSpeechEngine?.isSpeaking?.value == true) {
                    _speakerLevel.value = amp
                } else {
                    _micLevel.value = amp
                }
            },
            onSpeakingStateChanged = { speaking ->
                if (speaking) {
                    _localAssistantState.value = AssistantState.SPEAKING
                } else if (_localAssistantState.value == AssistantState.SPEAKING) {
                    _localAssistantState.value = AssistantState.STANDBY
                }
            }
        )
    }

    val isTtsSpeaking: StateFlow<Boolean> = offlineSpeechEngine?.isSpeaking ?: MutableStateFlow(false)

    fun vocalizeCurrentResponse() {
        val textToSpeak = _localSassyQuote.value
        if (textToSpeak.isNotBlank()) {
            if (offlineSpeechEngine?.isSpeaking?.value == true) {
                offlineSpeechEngine?.stopSpeaking()
            } else {
                offlineSpeechEngine?.speak(textToSpeak)
            }
        }
    }

    fun vocalizeText(text: String) {
        if (text.isNotBlank()) {
            offlineSpeechEngine?.speak(text)
        }
    }

    fun checkPermissionsInitial() {
        val hasAudio = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val hasCall = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        if (!hasAudio || !hasContacts || !hasCall) {
            _showPermissionsDialog.value = true
        }
    }

    fun dismissPermissionsDialog() {
        _showPermissionsDialog.value = false
    }

    fun dismissOnboardingOverlay() {
        _showOnboardingOverlay.value = false
        appPrefs.edit().putBoolean("has_seen_first_launch_onboarding", true).apply()
    }

    fun showOnboardingOverlay() {
        _showOnboardingOverlay.value = true
    }

    private fun startSyncLoop() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val isOnline = networkMonitor.isOnline.value && !_forceOfflineMode.value
                val service = MMAssistantForegroundService.activeServiceInstance

                if (isOnline && service != null) {
                    val newState = service.geminiClient.assistantState.value
                    if (_localAssistantState.value != newState) _localAssistantState.value = newState

                    val newTranscripts = service.geminiClient.transcripts.value
                    if (_localTranscripts.value != newTranscripts && newTranscripts.isNotEmpty()) {
                        val currentIds = _localTranscripts.value.map { it.id }.toSet()
                        val freshlyAdded = newTranscripts.filterNot { currentIds.contains(it.id) }
                        if (freshlyAdded.isNotEmpty()) {
                            conversationRepository.saveTranscripts(freshlyAdded)
                        }
                        _localTranscripts.value = newTranscripts
                    }

                    val newActiveTool = service.geminiClient.activeToolCall.value
                    if (_localActiveTool.value != newActiveTool) _localActiveTool.value = newActiveTool

                    val newQuote = service.geminiClient.sassyOneLiner.value
                    if (_localSassyQuote.value != newQuote) _localSassyQuote.value = newQuote

                    val newAmp = service.audioPlayer.liveAmplitude.value
                    if (Math.abs(_speakerLevel.value - newAmp) > 0.02f) _speakerLevel.value = newAmp
                } else if (!isOnline) {
                    // In offline mode
                    val isOfflineListening = offlineSpeechEngine?.isListening?.value ?: false
                    val isOfflineSpeaking = offlineSpeechEngine?.isSpeaking?.value ?: false

                    val offlineState = when {
                        isOfflineSpeaking -> AssistantState.SPEAKING
                        isOfflineListening -> AssistantState.LISTENING
                        _localAssistantState.value == AssistantState.THINKING -> AssistantState.THINKING
                        else -> AssistantState.DISCONNECTED
                    }
                    if (_localAssistantState.value != offlineState) {
                        _localAssistantState.value = offlineState
                    }
                }

                // Automatic Sassy Mood detection when auto mode is enabled
                if (_isAutoMoodDetectionEnabled.value) {
                    val detected = com.example.model.SassyMoodDetector.detectMood(
                        text = _localSassyQuote.value,
                        state = _localAssistantState.value,
                        activeToolName = _localActiveTool.value?.functionName
                    )
                    if (_currentSassyMood.value != detected) {
                        _currentSassyMood.value = detected
                    }
                }
                delay(120)
            }
        }
    }

    fun toggleForceOfflineMode() {
        _forceOfflineMode.value = !_forceOfflineMode.value
        val isNowOffline = _forceOfflineMode.value || !networkMonitor.isOnline.value
        if (isNowOffline) {
            _localSassyQuote.value = "Switched to Local Open-Source GGUF Mode! Ready offline."
            addLocalTranscript(LiveTranscript.Sender.SYSTEM, "⚡ Engine switched to on-device GGUF inference.")
        } else {
            _localSassyQuote.value = "Back online! Connected to Gemini Live."
            addLocalTranscript(LiveTranscript.Sender.SYSTEM, "⚡ Engine switched to Gemini Live stream.")
        }
    }

    fun selectGGUFModel(modelId: String) {
        modelStorageManager.selectModel(modelId)
        val selected = modelStorageManager.models.value.find { it.id == modelId }
        _localSassyQuote.value = "Loaded ${selected?.name ?: "model"} for offline execution."
    }

    fun downloadGGUFModel(modelId: String) {
        modelStorageManager.startModelDownload(modelId)
    }

    fun deleteGGUFModel(modelId: String) {
        modelStorageManager.deleteModel(modelId)
    }

    fun startBackgroundService() {
        MMAssistantForegroundService.startService(context)
        com.example.work.WakeWordWorkManagerScheduler.schedulePeriodicWakeWordMonitoring(context)
        com.example.work.BatteryWorkManagerScheduler.schedulePeriodicBatteryMonitoring(context)
    }

    fun stopBackgroundService() {
        MMAssistantForegroundService.stopService(context)
        com.example.work.WakeWordWorkManagerScheduler.cancelWakeWordWork(context)
    }

    fun toggleService() {
        if (isServiceRunning.value) {
            stopBackgroundService()
        } else {
            startBackgroundService()
        }
    }

    fun togglePrivacyMode(forced: Boolean? = null) {
        val target = forced ?: !isPrivacyMode.value
        val service = MMAssistantForegroundService.activeServiceInstance
        if (service != null) {
            service.togglePrivacyMode(target)
        }
        offlineSpeechEngine?.setMuted(target)
        callAnnouncerManager.setMuted(target)

        if (target) {
            _localSassyQuote.value = "🛡️ Privacy Shield Active: Microphone & Speech Output are fully off."
            addLocalTranscript(LiveTranscript.Sender.SYSTEM, "🛡️ Privacy Mode ENABLED: Mic & Assistant Speech silenced.", isTool = true)
        } else {
            _localSassyQuote.value = "🛡️ Privacy Shield Disabled: Microphone & Speech Output restored."
            addLocalTranscript(LiveTranscript.Sender.SYSTEM, "🛡️ Privacy Mode DISABLED: MM is listening for 'Hey MM'.", isTool = true)
        }
    }

    fun toggleMute() {
        val service = MMAssistantForegroundService.activeServiceInstance
        if (service != null) {
            val newMute = !isMuted.value
            service.setMuted(newMute)
        }
    }

    fun toggleSpeechMute() {
        val service = MMAssistantForegroundService.activeServiceInstance
        if (service != null) {
            val newSpeechMute = !isSpeechOutputMuted.value
            service.setSpeechMuted(newSpeechMute)
            offlineSpeechEngine?.setMuted(newSpeechMute)
            callAnnouncerManager.setMuted(newSpeechMute)
        }
    }

    fun toggleSpeechRecognizer(enabled: Boolean) {
        _speechWakeActive.value = enabled
        val service = MMAssistantForegroundService.activeServiceInstance
        if (service != null) {
            if (enabled && !isPrivacyMode.value) {
                service.speechRecognizerWakeManager.startListening()
            } else {
                service.speechRecognizerWakeManager.stopListening()
            }
        }
    }

    fun testWakeWordTrigger(phrase: String = "Hey MM") {
        _lastSpeechWakePhrase.value = phrase
        _localSassyQuote.value = "🎯 Wake-word '$phrase' triggered! Launching voice mode."
        wakeUpFromStandby()
    }

    fun setSensitivity(value: Float) {
        _wakeWordSensitivity.value = value
        MMAssistantForegroundService.activeServiceInstance?.setWakeWordSensitivity(value)
    }

    private var lastMicTapTime = 0L
    private var lastPromptSendTime = 0L
    private var lastToolExecTime = 0L
    private var lastServiceToggleTime = 0L

    fun onMicOrbTapped() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastMicTapTime < 450L) return
        lastMicTapTime = now

        val isOnline = networkMonitor.isOnline.value && !_forceOfflineMode.value
        if (isOnline) {
            val service = MMAssistantForegroundService.activeServiceInstance
            if (service != null) {
                if (service.geminiClient.assistantState.value == AssistantState.DISCONNECTED) {
                    service.geminiClient.connect()
                } else if (service.geminiClient.assistantState.value == AssistantState.SPEAKING) {
                    service.audioPlayer.interrupt()
                }
            } else {
                startBackgroundService()
            }
        } else {
            // Trigger offline voice input
            if (offlineSpeechEngine?.isListening?.value == true) {
                offlineSpeechEngine?.stopListening()
            } else if (offlineSpeechEngine?.isSpeaking?.value == true) {
                offlineSpeechEngine?.stopSpeaking()
            } else {
                offlineSpeechEngine?.startListening()
                _localSassyQuote.value = "Listening via on-device speech engine..."
            }
        }
    }

    fun sendQuickPrompt(prompt: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPromptSendTime < 450L) return
        lastPromptSendTime = now

        val isOnline = networkMonitor.isOnline.value && !_forceOfflineMode.value
        if (isOnline) {
            val service = MMAssistantForegroundService.activeServiceInstance
            if (service != null) {
                service.geminiClient.sendUserPrompt(prompt)
            } else {
                startBackgroundService()
                viewModelScope.launch {
                    delay(800)
                    MMAssistantForegroundService.activeServiceInstance?.geminiClient?.sendUserPrompt(prompt)
                }
            }
        } else {
            // Offline local inference execution
            handleOfflineUserInput(prompt)
        }
    }

    private fun handleOfflineUserInput(userPrompt: String) {
        addLocalTranscript(LiveTranscript.Sender.USER, userPrompt)
        _localAssistantState.value = AssistantState.THINKING
        _localSassyQuote.value = "Thinking with on-device model..."

        viewModelScope.launch {
            val result = localInferenceEngine.generateResponse(userPrompt)
            _localAssistantState.value = AssistantState.SPEAKING
            _localSassyQuote.value = result.textResponse

            if (result.toolResult != null) {
                _localActiveTool.value = ToolCallInfo(
                    callId = "local_${System.currentTimeMillis()}",
                    functionName = "local_tool",
                    arguments = emptyMap(),
                    status = if (result.toolResult.success) ToolCallInfo.ToolStatus.SUCCESS else ToolCallInfo.ToolStatus.FAILED,
                    resultMessage = result.toolResult.message
                )
                addLocalTranscript(LiveTranscript.Sender.SYSTEM, "⚡ ${result.toolResult.message}", isTool = true)
            }

            addLocalTranscript(LiveTranscript.Sender.MM, result.textResponse)

            // Speak response aloud via Android Text-To-Speech
            offlineSpeechEngine?.speak(result.textResponse)
        }
    }

    private fun addLocalTranscript(sender: LiveTranscript.Sender, text: String, isTool: Boolean = false) {
        val transcript = LiveTranscript(sender = sender, text = text, isToolCall = isTool)
        val current = _localTranscripts.value.toMutableList()
        current.add(transcript)
        _localTranscripts.value = current
        viewModelScope.launch(Dispatchers.IO) {
            conversationRepository.saveTranscript(
                transcript = transcript,
                sassyIntensityLevel = _sassyIntensity.value.level
            )
        }
    }

    fun executeQuickToolDirectly(toolName: String) {
        when (toolName) {
            "flashlight" -> {
                val res = toolManager.toggleFlashlight(true)
                _localSassyQuote.value = res.message
            }
            "status" -> {
                val res = toolManager.getDeviceStatus()
                _localSassyQuote.value = res.message
            }
            "youtube" -> {
                val res = toolManager.openApp(null, "YouTube")
                _localSassyQuote.value = res.message
            }
            "whatsapp" -> {
                val res = toolManager.openApp(null, "WhatsApp")
                _localSassyQuote.value = res.message
            }
            "volume_night" -> {
                val res = toolManager.adjustDeviceVolume(streamType = "all", contextMode = "night")
                _localSassyQuote.value = res.message
            }
            "volume_auto" -> {
                val res = toolManager.adjustDeviceVolume(streamType = "all", contextMode = "auto")
                _localSassyQuote.value = res.message
            }
        }
    }

    fun checkBatteryOptimizationStatus() {
        _isBatteryExempt.value = BatteryOptimizationHelper.isBatteryOptimizationIgnored(context)
    }

    // Call Announcer actions
    fun toggleCallAnnouncer(enabled: Boolean) {
        callAnnouncerManager.setAnnouncerEnabled(enabled)
        if (enabled) {
            com.example.service.CallAnnouncerService.startService(context)
        } else {
            com.example.service.CallAnnouncerService.stopService(context)
        }
    }

    fun testCallAnnouncement(contactName: String? = "Akash Upadhyay", forceSilent: Boolean = true) {
        val result = callAnnouncerManager.testAnnounce(contactName, forceSilent)
        _localSassyQuote.value = result
    }

    // Remote PC actions
    fun updatePcSettings(ip: String, port: Int, name: String) {
        remotePcManager.updateConfig(ip, port, name)
    }

    fun pingPc() {
        viewModelScope.launch {
            remotePcManager.pingPc()
        }
    }

    fun executePcAction(command: String, params: Map<String, Any?> = emptyMap()) {
        viewModelScope.launch {
            val result = remotePcManager.sendCommand(command, params)
            _localSassyQuote.value = result.message
            addLocalTranscript(LiveTranscript.Sender.SYSTEM, "💻 PC: ${result.message}", isTool = true)
        }
    }

    // Standby controls ("Bye MM" / "Hello MM")
    fun enterStandby() {
        val service = MMAssistantForegroundService.activeServiceInstance
        if (service != null) {
            val intent = android.content.Intent(context, MMAssistantForegroundService::class.java).apply {
                action = MMAssistantForegroundService.ACTION_ENTER_STANDBY
            }
            context.startService(intent)
        }
        val result = toolManager.enterStandbyMode()
        _localSassyQuote.value = result.message
        addLocalTranscript(LiveTranscript.Sender.MM, result.message)
    }

    fun wakeUpFromStandby() {
        val service = MMAssistantForegroundService.activeServiceInstance
        if (service != null) {
            service.triggerWakeWordAwakening()
        } else {
            startBackgroundService()
        }
        _localSassyQuote.value = "Hey Boss! MM is right here. What's the plan?"
    }

    fun requestDisableBatteryOptimization() {
        BatteryOptimizationHelper.requestDisableBatteryOptimization(context)
        viewModelScope.launch {
            delay(1500)
            checkBatteryOptimizationStatus()
        }
    }

    fun getStorageUsedFormatted(): String {
        return modelStorageManager.getStorageUsedFormatted()
    }

    // Sassy Mood UI State Manager controls
    fun setSassyMood(mood: com.example.model.SassyMood) {
        _isAutoMoodDetectionEnabled.value = false
        _currentSassyMood.value = mood
    }

    fun toggleAutoMoodDetection(enabled: Boolean) {
        _isAutoMoodDetectionEnabled.value = enabled
        if (enabled) {
            _currentSassyMood.value = com.example.model.SassyMoodDetector.detectMood(
                text = _localSassyQuote.value,
                state = _localAssistantState.value,
                activeToolName = _localActiveTool.value?.functionName
            )
        }
    }

    fun triggerMoodSample(mood: com.example.model.SassyMood) {
        _currentSassyMood.value = mood
        val sampleQuote = mood.sampleQuotes.random()
        _localSassyQuote.value = sampleQuote
        addLocalTranscript(LiveTranscript.Sender.MM, sampleQuote)
        offlineSpeechEngine?.speak(sampleQuote)
    }

    fun reconnect() {
        val service = MMAssistantForegroundService.activeServiceInstance
        if (service != null) {
            service.geminiClient.connect()
        } else {
            startBackgroundService()
        }
    }

    private var isFlashlightOn = false

    fun toggleFlashlight() {
        isFlashlightOn = !isFlashlightOn
        val res = toolManager.toggleFlashlight(isFlashlightOn)
        _localSassyQuote.value = res.message
    }

    // Sassiness Level, Room Cache, and Battery Optimizer Controls
    fun setSassinessLevel(level: com.example.model.SassinessLevel) {
        viewModelScope.launch {
            settingsDataStore.setSassinessLevel(level)
            _localSassyQuote.value = "Sassiness set to [${level.displayName}]: ${level.exampleQuote}"
        }
    }

    fun setBatteryAdaptiveEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setBatteryAdaptiveWakeWord(enabled)
            if (enabled) {
                com.example.work.BatteryWorkManagerScheduler.triggerImmediateCheck(context)
            }
        }
    }

    fun setInteractionCacheEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setInteractionCacheEnabled(enabled)
        }
    }

    fun clearInteractionHistory() {
        viewModelScope.launch {
            interactionRepository.clearHistory()
            _localSassyQuote.value = "Cleared all cached interaction memory from Room, Boss."
        }
    }

    fun triggerBatteryOptimizationCheck() {
        com.example.work.BatteryWorkManagerScheduler.triggerImmediateCheck(context)
    }

    fun openSettingsDialog() {
        _showSettingsDialog.value = true
    }

    fun closeSettingsDialog() {
        _showSettingsDialog.value = false
    }

    override fun onCleared() {
        pollJob?.cancel()
        offlineSpeechEngine?.shutdown()
        super.onCleared()
    }
}
