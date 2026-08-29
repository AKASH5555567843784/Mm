package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRecordStreamer
import com.example.audio.AudioStreamPlayer
import com.example.audio.OfflineSpeechEngine
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MMAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val toolManager = DeviceToolManager(context)

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

    private val _localSassyQuote = MutableStateFlow("Hey handsome! MM is ready to take over your phone.")
    val sassyOneLiner: StateFlow<String> = _localSassyQuote.asStateFlow()

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
        "Hello MM",
        "Bye MM (Standby)",
        "Lock PC",
        "Open VS Code on PC",
        "Who created you?",
        "Which country made you?",
        "Open YouTube",
        "Set Night Mode Volume",
        "Telegram message Boss",
        "Call Mom",
        "Turn on Flashlight",
        "Check Battery Status"
    )

    private var pollJob: Job? = null

    init {
        checkPermissionsInitial()
        initOfflineSpeechEngine()
        startSyncLoop()
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
                _micLevel.value = amp
            }
        )
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

    private fun startSyncLoop() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                val isOnline = networkMonitor.isOnline.value && !_forceOfflineMode.value
                val service = MMAssistantForegroundService.activeServiceInstance

                if (isOnline && service != null) {
                    _localAssistantState.value = service.geminiClient.assistantState.value
                    _localTranscripts.value = service.geminiClient.transcripts.value
                    _localActiveTool.value = service.geminiClient.activeToolCall.value
                    _localSassyQuote.value = service.geminiClient.sassyOneLiner.value
                    _speakerLevel.value = service.audioPlayer.liveAmplitude.value
                } else if (!isOnline) {
                    // In offline mode
                    val isOfflineListening = offlineSpeechEngine?.isListening?.value ?: false
                    val isOfflineSpeaking = offlineSpeechEngine?.isSpeaking?.value ?: false

                    _localAssistantState.value = when {
                        isOfflineSpeaking -> AssistantState.SPEAKING
                        isOfflineListening -> AssistantState.LISTENING
                        _localAssistantState.value == AssistantState.THINKING -> AssistantState.THINKING
                        else -> AssistantState.DISCONNECTED
                    }
                }

                // Automatic Sassy Mood detection when auto mode is enabled
                if (_isAutoMoodDetectionEnabled.value) {
                    val detected = com.example.model.SassyMoodDetector.detectMood(
                        text = _localSassyQuote.value,
                        state = _localAssistantState.value,
                        activeToolName = _localActiveTool.value?.toolName
                    )
                    _currentSassyMood.value = detected
                }
                delay(80)
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
    }

    fun stopBackgroundService() {
        MMAssistantForegroundService.stopService(context)
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

    fun onMicOrbTapped() {
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
        val current = _localTranscripts.value.toMutableList()
        current.add(LiveTranscript(sender = sender, text = text, isToolCall = isTool))
        _localTranscripts.value = current
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
                activeToolName = _localActiveTool.value?.toolName
            )
        }
    }

    fun triggerMoodSample(mood: com.example.model.SassyMood) {
        _currentSassyMood.value = mood
        val sampleQuote = mood.sampleQuotes.random()
        _localSassyQuote.value = sampleQuote
        addLocalTranscript(LiveTranscript.Sender.MM, sampleQuote)
    }

    override fun onCleared() {
        pollJob?.cancel()
        offlineSpeechEngine?.shutdown()
        super.onCleared()
    }
}
