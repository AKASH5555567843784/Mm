package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.AudioRecordStreamer
import com.example.audio.AudioStreamPlayer
import com.example.audio.SpeechRecognizerWakeWordManager
import com.example.audio.WakeWordDetector
import com.example.gemini.GeminiLiveClient
import com.example.model.AssistantState
import com.example.receiver.MMAssistantWakeWordReceiver
import com.example.tools.DeviceToolManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MMAssistantForegroundService : Service() {

    companion object {
        private const val TAG = "MMAssistantService"
        const val CHANNEL_ID = "mm_assistant_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.mm.action.START"
        const val ACTION_STOP = "com.example.mm.action.STOP"
        const val ACTION_TOGGLE_MUTE = "com.example.mm.action.TOGGLE_MUTE"
        const val ACTION_TOGGLE_PRIVACY = "com.example.mm.action.TOGGLE_PRIVACY"
        const val ACTION_TOGGLE_SPEECH_MUTE = "com.example.mm.action.TOGGLE_SPEECH_MUTE"
        const val ACTION_TRIGGER_WAKE = "com.example.mm.action.TRIGGER_WAKE"
        const val ACTION_ENTER_STANDBY = "com.example.mm.action.ENTER_STANDBY"
        const val ACTION_WAKE_WORD_DETECTED = MMAssistantWakeWordReceiver.ACTION_WAKE_WORD_DETECTED

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isPrivacyMode = MutableStateFlow(false)
        val isPrivacyMode: StateFlow<Boolean> = _isPrivacyMode.asStateFlow()

        private val _isMuted = MutableStateFlow(false)
        val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

        private val _isSpeechOutputMuted = MutableStateFlow(false)
        val isSpeechOutputMuted: StateFlow<Boolean> = _isSpeechOutputMuted.asStateFlow()

        private val _isStandby = MutableStateFlow(false)
        val isStandby: StateFlow<Boolean> = _isStandby.asStateFlow()

        private val _isWakeWordListening = MutableStateFlow(true)
        val isWakeWordListening: StateFlow<Boolean> = _isWakeWordListening.asStateFlow()

        var activeServiceInstance: MMAssistantForegroundService? = null
            private set

        fun startService(context: Context) {
            try {
                val intent = Intent(context, MMAssistantForegroundService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MMAssistantForegroundService", e)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, MMAssistantForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop MMAssistantForegroundService", e)
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var toolManager: DeviceToolManager
    lateinit var audioPlayer: AudioStreamPlayer
        private set
    lateinit var geminiClient: GeminiLiveClient
        private set
    private lateinit var audioRecorder: AudioRecordStreamer
    private lateinit var wakeWordDetector: WakeWordDetector
    lateinit var speechRecognizerWakeManager: SpeechRecognizerWakeWordManager
        private set

    inner class LocalBinder : Binder() {
        fun getService(): MMAssistantForegroundService = this@MMAssistantForegroundService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        activeServiceInstance = this
        toolManager = DeviceToolManager(applicationContext)

        audioPlayer = AudioStreamPlayer(sampleRate = 24000)

        geminiClient = GeminiLiveClient(
            deviceToolManager = toolManager,
            onAudioOutputChunk = { pcmChunk ->
                if (!_isSpeechOutputMuted.value && !_isPrivacyMode.value) {
                    audioPlayer.enqueuePcmChunk(pcmChunk)
                }
            },
            onInterrupted = {
                audioPlayer.interrupt()
            }
        )

        wakeWordDetector = WakeWordDetector(
            sensitivity = 0.65f,
            onWakeWordDetected = {
                if (!_isPrivacyMode.value && !_isMuted.value) {
                    onWakeWordSignatureDetected()
                }
            }
        )

        speechRecognizerWakeManager = SpeechRecognizerWakeWordManager(
            context = applicationContext,
            onWakeWordDetected = { phrase ->
                if (!_isPrivacyMode.value && !_isMuted.value) {
                    Log.i(TAG, "SpeechRecognizer triggered voice mode with wake phrase: '$phrase'")
                    triggerWakeWordAwakening()
                }
            }
        )

        audioRecorder = AudioRecordStreamer(
            context = applicationContext,
            sampleRate = 16000,
            onAudioChunk = { buffer, bytesRead ->
                if (!_isPrivacyMode.value && !_isMuted.value) {
                    // Persistent AudioRecord buffer feeds wake-word acoustic signature detector
                    wakeWordDetector.processAudioChunk(buffer, bytesRead)
                    // If Gemini Live is active/connected, stream audio live
                    if (geminiClient.assistantState.value != AssistantState.DISCONNECTED) {
                        geminiClient.sendAudioChunk(buffer, bytesRead)
                    }
                }
            },
            onUserVoiceDetected = {
                // If MM is currently speaking and user starts talking, interrupt MM immediately
                if (geminiClient.assistantState.value == AssistantState.SPEAKING || audioPlayer.isPlaying.value) {
                    audioPlayer.interrupt()
                }
            }
        )

        createNotificationChannel()
        acquireWakeLock()

        // Observe assistant state changes to dynamically update notification visual pulse indicator
        scope.launch {
            geminiClient.assistantState.collect {
                if (_isRunning.value) {
                    updateNotification()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundServiceInternal()
            }
            ACTION_STOP -> {
                stopForegroundServiceInternal()
            }
            ACTION_TOGGLE_PRIVACY -> {
                togglePrivacyMode()
            }
            ACTION_TOGGLE_MUTE -> {
                val newMute = !_isMuted.value
                setMuted(newMute)
            }
            ACTION_TOGGLE_SPEECH_MUTE -> {
                val newSpeechMute = !_isSpeechOutputMuted.value
                setSpeechMuted(newSpeechMute)
            }
            ACTION_TRIGGER_WAKE -> {
                _isStandby.value = false
                triggerWakeWordAwakening()
            }
            ACTION_ENTER_STANDBY -> {
                _isStandby.value = true
                audioPlayer.interrupt()
                updateNotification()
                Log.d(TAG, "Entered low-power standby mode. Background microphone listener remains active.")
            }
            else -> {
                _isStandby.value = false
                startForegroundServiceInternal()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceInternal() {
        _isRunning.value = true
        try {
            val notification = buildNotification()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call startForeground with notification", e)
        }

        if (!_isPrivacyMode.value) {
            audioRecorder.startRecording()
            speechRecognizerWakeManager.startListening()
        }
        audioPlayer.start()
        geminiClient.connect()
    }

    private fun stopForegroundServiceInternal() {
        _isRunning.value = false
        speechRecognizerWakeManager.stopListening()
        audioRecorder.stopRecording()
        audioPlayer.release()
        geminiClient.disconnect()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Triggered when the persistent AudioRecord buffer detects the 'MM' wake-word signature.
     * Fires a Broadcast Intent to launch or resume the Gemini Live session.
     */
    private fun onWakeWordSignatureDetected() {
        Log.i(TAG, "Wake-word 'MM' signature detected in persistent AudioRecord buffer. Broadcasting intent...")

        // 1. Broadcast Intent to notify registered receivers and launch/resume session
        val broadcastIntent = Intent(ACTION_WAKE_WORD_DETECTED).apply {
            setPackage(packageName)
            putExtra(MMAssistantWakeWordReceiver.EXTRA_TRIGGER_SOURCE, "AudioRecord_Persistent_Buffer")
        }
        sendBroadcast(broadcastIntent)

        // 2. Direct fast-path activation to avoid any IPC latency
        triggerWakeWordAwakening()
    }

    fun triggerWakeWordAwakening() {
        vibratePhone()
        scope.launch {
            if (geminiClient.assistantState.value == AssistantState.DISCONNECTED) {
                geminiClient.connect()
            }
            // Bring MainActivity to foreground
            val activityIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("WAKE_TRIGGERED", true)
            }
            startActivity(activityIntent)
            updateNotification()
        }
    }

    private fun vibratePhone() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 120), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(120)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration error", e)
        }
    }

    fun setWakeWordSensitivity(sensitivity: Float) {
        wakeWordDetector.setSensitivity(sensitivity)
    }

    fun togglePrivacyMode(forcedState: Boolean? = null) {
        val target = forcedState ?: !_isPrivacyMode.value
        _isPrivacyMode.value = target
        _isMuted.value = target
        _isSpeechOutputMuted.value = target

        audioRecorder.setMuted(target)
        audioPlayer.setMuted(target)
        if (target) {
            audioPlayer.interrupt()
        }
        speechRecognizerWakeManager.setPrivacyMode(target)

        updateNotification()
        Log.i(TAG, "Privacy Mode updated -> active: $target (Mic Muted: $target, Speech Output Muted: $target)")
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        audioRecorder.setMuted(muted)
        if (muted && _isSpeechOutputMuted.value) {
            _isPrivacyMode.value = true
        } else if (!muted) {
            _isPrivacyMode.value = false
        }
        updateNotification()
    }

    fun setSpeechMuted(muted: Boolean) {
        _isSpeechOutputMuted.value = muted
        audioPlayer.setMuted(muted)
        if (muted) {
            audioPlayer.interrupt()
        }
        if (muted && _isMuted.value) {
            _isPrivacyMode.value = true
        } else if (!muted) {
            _isPrivacyMode.value = false
        }
        updateNotification()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MM Assistant Background Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps MM Voice Assistant active in the background for instant wake-word activation"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val privacyIntent = Intent(this, MMAssistantForegroundService::class.java).apply {
            action = ACTION_TOGGLE_PRIVACY
        }
        val pendingPrivacy = PendingIntent.getService(
            this, 1, privacyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, MMAssistantForegroundService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val pendingMute = PendingIntent.getService(
            this, 2, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MMAssistantForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wakeIntent = Intent(this, MMAssistantForegroundService::class.java).apply {
            action = ACTION_TRIGGER_WAKE
        }
        val pendingWake = PendingIntent.getService(
            this, 4, wakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val currentState = geminiClient.assistantState.value
        val isPrivacyActive = _isPrivacyMode.value
        val isMicMutedState = _isMuted.value

        // Determine visual pulse indicator assets, color-shifting background, and texts
        val (titleText, statusBadgeText, pulsePillText, subtextText, pulseBgRes, iconRes, statusTextColor) = when {
            isPrivacyActive -> Tuple7(
                "MM Assistant",
                "🛡️ Privacy Shield Active (Mic & Speech Off)",
                "PRIVACY LOCKED",
                "Microphone & speech output are fully paused. Tap Privacy to resume listening.",
                R.drawable.bg_pulse_muted,
                R.drawable.ic_notification_listening,
                0xFFFF5252.toInt()
            )
            isMicMutedState -> Tuple7(
                "MM Assistant",
                "🔴 Microphone Muted",
                "MUTED",
                "Microphone is muted. Tap Unmute or open app to resume listening.",
                R.drawable.bg_pulse_muted,
                R.drawable.ic_notification_listening,
                0xFFFF5252.toInt()
            )
            currentState == AssistantState.SPEAKING -> Tuple7(
                "MM Assistant",
                "🔊 MM is Speaking Live...",
                "SPEAKING",
                "MM is responding with real-time Gemini voice stream.",
                R.drawable.bg_pulse_speaking,
                R.drawable.ic_notification_speaking,
                0xFFFF007F.toInt()
            )
            currentState == AssistantState.THINKING -> Tuple7(
                "MM Assistant",
                "⚡ 🟣 Processing Audio Input...",
                "PULSING • PROCESSING",
                "⚡ Gemini Live is analyzing your voice input in real time...",
                R.drawable.bg_pulse_processing,
                R.drawable.ic_notification_processing,
                0xFFBA55D3.toInt()
            )
            currentState == AssistantState.EXECUTING_TOOL -> Tuple7(
                "MM Assistant",
                "⚙️ Executing Device Action...",
                "ACTION RUNNING",
                "Performing phone command with intelligent device tools.",
                R.drawable.bg_pulse_processing,
                R.drawable.ic_notification_processing,
                0xFF8A2BE2.toInt()
            )
            currentState == AssistantState.CONNECTING -> Tuple7(
                "MM Assistant",
                "🟡 Connecting Gemini Live...",
                "CONNECTING",
                "Establishing real-time bidirectional audio stream...",
                R.drawable.bg_pulse_listening,
                R.drawable.ic_notification_listening,
                0xFFFFD700.toInt()
            )
            else -> Tuple7(
                "MM Assistant",
                "🟢 Listening for 'Hey MM' / 'MM'",
                "PULSE ACTIVE",
                "Say \"Hey MM\" or tap Talk Now to launch live voice conversation.",
                R.drawable.bg_pulse_listening,
                R.drawable.ic_notification_listening,
                0xFF00E5FF.toInt()
            )
        }

        // Custom RemoteViews layout for Compact notification
        val compactRemoteViews = RemoteViews(packageName, R.layout.notification_mm_assistant_compact).apply {
            setTextViewText(R.id.notif_title, titleText)
            setTextViewText(R.id.notif_status_badge, statusBadgeText)
            setTextColor(R.id.notif_status_badge, statusTextColor)
            setImageViewResource(R.id.pulse_indicator_icon, iconRes)
            setInt(R.id.pulse_indicator_container, "setBackgroundResource", pulseBgRes)

            val privacyBtnText = if (isPrivacyActive) "🛡️ Privacy Off" else "🛡️ Privacy"
            setTextViewText(R.id.btn_compact_privacy, privacyBtnText)

            setOnClickPendingIntent(R.id.btn_compact_privacy, pendingPrivacy)
            setOnClickPendingIntent(R.id.btn_compact_talk, pendingWake)
        }

        // Custom RemoteViews layout for Expanded notification
        val expandedRemoteViews = RemoteViews(packageName, R.layout.notification_mm_assistant_expanded).apply {
            setTextViewText(R.id.expanded_notif_title, titleText)
            setTextViewText(R.id.expanded_status_text, statusBadgeText)
            setTextColor(R.id.expanded_status_text, statusTextColor)
            setTextViewText(R.id.expanded_pulse_pill, pulsePillText)
            setTextViewText(R.id.expanded_subtext, subtextText)
            setImageViewResource(R.id.expanded_pulse_icon, iconRes)
            setInt(R.id.expanded_pulse_container, "setBackgroundResource", pulseBgRes)

            val privacyActionText = if (isPrivacyActive) "🛡️ Unprotect" else "🛡️ Privacy"
            setTextViewText(R.id.btn_action_privacy, privacyActionText)

            val muteLabel = if (isMicMutedState) "🎙️ Unmute" else "🔇 Mic Off"
            setTextViewText(R.id.btn_action_mute, muteLabel)

            setOnClickPendingIntent(R.id.btn_action_talk, pendingWake)
            setOnClickPendingIntent(R.id.btn_action_privacy, pendingPrivacy)
            setOnClickPendingIntent(R.id.btn_action_mute, pendingMute)
            setOnClickPendingIntent(R.id.btn_action_stop, pendingStop)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(titleText)
            .setContentText(statusBadgeText)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compactRemoteViews)
            .setCustomBigContentView(expandedRemoteViews)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MMAssistant::WakeLock"
            )?.apply {
                acquire(24 * 60 * 60 * 1000L) // 24 hours
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
        wakeLock = null
    }

    override fun onDestroy() {
        activeServiceInstance = null
        speechRecognizerWakeManager.destroy()
        stopForegroundServiceInternal()
        super.onDestroy()
    }
}

private data class Tuple7<A, B, C, D, E, F, G>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G
)

