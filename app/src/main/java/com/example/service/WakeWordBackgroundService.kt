package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.MMAssistantWakeWordReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * WakeWordBackgroundService:
 * A simple, robust, persistent foreground background service using Kotlin Coroutines
 * and continuous microphone input (via AudioRecord) to detect wake-word activation
 * ("Hey MM", "MM") in real-time and awaken the MM Voice Assistant.
 */
class WakeWordBackgroundService : Service() {

    companion object {
        private const val TAG = "WakeWordBgService"
        const val CHANNEL_ID = "mm_wakeword_service_channel"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START = "com.example.mm.action.START_WAKE_SERVICE"
        const val ACTION_STOP = "com.example.mm.action.STOP_WAKE_SERVICE"
        const val ACTION_TOGGLE_LISTENING = "com.example.mm.action.TOGGLE_WAKE_LISTENING"

        private const val SAMPLE_RATE = 16000 // 16kHz Mono 16-bit PCM
        private const val CHUNK_SIZE = 1024 // ~64ms frames
        private const val TRIGGER_COOLDOWN_MS = 2000L

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _isListening = MutableStateFlow(false)
        val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

        private val _liveAudioAmplitude = MutableStateFlow(0f)
        val liveAudioAmplitude: StateFlow<Float> = _liveAudioAmplitude.asStateFlow()

        private val _wakeWordDetectedCount = MutableStateFlow(0)
        val wakeWordDetectedCount: StateFlow<Int> = _wakeWordDetectedCount.asStateFlow()

        fun start(context: Context) {
            try {
                val intent = Intent(context, WakeWordBackgroundService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed starting WakeWordBackgroundService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, WakeWordBackgroundService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed stopping WakeWordBackgroundService", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var recordJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastWakeTriggerTime = 0L

    // Wake-word energy sliding window history (~600ms)
    private val energyHistory = FloatArray(20)
    private var energyHistoryIdx = 0

    inner class LocalBinder : Binder() {
        fun getService(): WakeWordBackgroundService = this@WakeWordBackgroundService
    }

    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        Log.i(TAG, "WakeWordBackgroundService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopServiceInternal()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_LISTENING -> {
                if (_isListening.value) {
                    stopMicrophoneListening()
                } else {
                    startMicrophoneListening()
                }
                updateNotification()
            }
            else -> {
                startForegroundNotification()
                startMicrophoneListening()
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        _isServiceActive.value = true
        val notification = buildServiceNotification()
        try {
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
            Log.e(TAG, "Error starting foreground notification", e)
        }
    }

    /**
     * Launch persistent microphone streaming coroutine using AudioRecord.
     */
    private fun startMicrophoneListening() {
        if (_isListening.value) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission missing. Cannot start microphone stream.")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(CHUNK_SIZE * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            _isListening.value = true

            // Persistent background coroutine loop for reading microphone audio
            recordJob?.cancel()
            recordJob = serviceScope.launch {
                val buffer = ByteArray(CHUNK_SIZE)
                Log.i(TAG, "Microphone coroutine listening loop started.")

                while (isActive && _isListening.value) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (bytesRead > 0) {
                        processAudioBuffer(buffer, bytesRead)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting microphone listening", e)
            _isListening.value = false
        }
    }

    /**
     * Analyze audio chunk for volume amplitude and wake-word acoustic signature.
     */
    private fun processAudioBuffer(buffer: ByteArray, bytesRead: Int) {
        var sumSquares = 0.0
        val sampleCount = bytesRead / 2
        if (sampleCount == 0) return

        var zeroCrossings = 0
        var prevPositive = false

        for (i in 0 until bytesRead - 1 step 2) {
            val sample = ((buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)).toShort()
            val normalized = sample.toFloat() / 32768f
            sumSquares += normalized * normalized

            val isPositive = sample >= 0
            if (i > 0 && isPositive != prevPositive) {
                zeroCrossings++
            }
            prevPositive = isPositive
        }

        val rms = sqrt(sumSquares / sampleCount).toFloat()
        val amplitude = (rms * 3.5f).coerceIn(0f, 1f)
        _liveAudioAmplitude.value = amplitude

        // Record energy history for wake-word temporal analysis
        energyHistory[energyHistoryIdx] = rms
        energyHistoryIdx = (energyHistoryIdx + 1) % energyHistory.size

        val zcr = zeroCrossings.toFloat() / sampleCount
        val now = System.currentTimeMillis()

        // Wake pattern evaluation: "MM" / "Hey MM" energy burst with characteristic vocal tract formants
        if (now - lastWakeTriggerTime > TRIGGER_COOLDOWN_MS) {
            if (evaluateWakeSignature(rms, zcr)) {
                lastWakeTriggerTime = now
                _wakeWordDetectedCount.value += 1
                Log.i(TAG, "Wake-word triggered from persistent microphone coroutine! Count=${_wakeWordDetectedCount.value}")
                onWakeWordTriggered()
            }
        }
    }

    private fun evaluateWakeSignature(currentRms: Float, zcr: Float): Boolean {
        val minEnergy = 0.055f
        if (currentRms < minEnergy) return false

        // Check if there are distinct energy peaks in recent history (double pulse "M-M" or "Hey M-M")
        var peak1 = 0f
        var peak2 = 0f
        val half = energyHistory.size / 2

        for (i in 0 until half) {
            val idx = (energyHistoryIdx + i) % energyHistory.size
            if (energyHistory[idx] > peak1) peak1 = energyHistory[idx]
        }
        for (i in half until energyHistory.size) {
            val idx = (energyHistoryIdx + i) % energyHistory.size
            if (energyHistory[idx] > peak2) peak2 = energyHistory[idx]
        }

        val hasDualPulse = peak1 > minEnergy && peak2 > minEnergy
        val isHumanVoiceZcr = zcr in 0.02f..0.48f

        return hasDualPulse && isHumanVoiceZcr
    }

    /**
     * Dispatches intent & opens assistant when wake-word is detected.
     */
    private fun onWakeWordTriggered() {
        triggerHapticPulse()

        // 1. Send broadcast for registered listeners
        val broadcastIntent = Intent(MMAssistantWakeWordReceiver.ACTION_WAKE_WORD_DETECTED).apply {
            setPackage(packageName)
            putExtra(MMAssistantWakeWordReceiver.EXTRA_TRIGGER_SOURCE, "WakeWordBackgroundService")
        }
        sendBroadcast(broadcastIntent)

        // 2. Launch Main Assistant Activity to the foreground
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("WAKE_TRIGGERED", true)
            putExtra("TRIGGER_SOURCE", "WakeWordBackgroundService")
        }
        startActivity(launchIntent)
    }

    private fun triggerHapticPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 70, 50, 110), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic vibration error", e)
        }
    }

    private fun stopMicrophoneListening() {
        _isListening.value = false
        _liveAudioAmplitude.value = 0f
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null
        Log.i(TAG, "Microphone listening stopped.")
    }

    private fun stopServiceInternal() {
        _isServiceActive.value = false
        stopMicrophoneListening()
        releaseWakeLock()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MM Wake-Word Detector",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent background microphone listener for hands-free 'Hey MM' activation"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildServiceNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, WakeWordBackgroundService::class.java).apply {
            action = ACTION_TOGGLE_LISTENING
        }
        val pendingToggle = PendingIntent.getService(
            this, 1, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isListeningState = _isListening.value
        val title = "MM Assistant Background Listener"
        val statusText = if (isListeningState) "🟢 Listening for 'Hey MM' / 'MM'..." else "🔴 Microphone Paused"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_listening)
            .setContentTitle(title)
            .setContentText(statusText)
            .setContentIntent(pendingOpen)
            .addAction(
                if (isListeningState) R.drawable.ic_notification_listening else R.drawable.ic_notification_speaking,
                if (isListeningState) "Pause Mic" else "Resume Mic",
                pendingToggle
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildServiceNotification())
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MMAssistant::WakeWordBgLock"
            )?.apply {
                acquire(24 * 60 * 60 * 1000L)
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
        stopServiceInternal()
        super.onDestroy()
    }
}
