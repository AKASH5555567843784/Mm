package com.example.telephony

import android.content.Context
import android.database.Cursor
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Intelligent Call Announcer with Silent Mode Dependency:
 * - If Phone is on Silent or Vibrate:
 *   - Known Contact: "Boss, [Contact Name] ka call aa raha hai."
 *   - Unknown Number: "Boss, kisi unknown number se call aa raha hai."
 * - If Phone is NOT on Silent (Normal Ringing Mode):
 *   - Remains completely silent, allowing standard phone ringtone to play uninterrupted.
 */
class CallAnnouncerManager(private val context: Context) {

    companion object {
        private const val TAG = "CallAnnouncerManager"
        const val UNKNOWN_CALLER_MESSAGE = "Boss, kisi unknown number se call aa raha hai."

        @Volatile
        private var instance: CallAnnouncerManager? = null

        fun getInstance(context: Context): CallAnnouncerManager {
            return instance ?: synchronized(this) {
                instance ?: CallAnnouncerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _lastAnnouncement = MutableStateFlow<String?>(null)
    val lastAnnouncement: StateFlow<String?> = _lastAnnouncement.asStateFlow()

    private val _isAnnouncing = MutableStateFlow(false)
    val isAnnouncing: StateFlow<Boolean> = _isAnnouncing.asStateFlow()

    private val _announcerEnabled = MutableStateFlow(true)
    val announcerEnabled: StateFlow<Boolean> = _announcerEnabled.asStateFlow()

    private var isMuted = false

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stopSpeaking()
        }
    }

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("hi", "IN"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Fallback to English (India) or default
                        tts?.setLanguage(Locale("en", "IN"))
                    }
                    tts?.setPitch(1.15f)
                    tts?.setSpeechRate(1.0f)
                    isTtsReady = true

                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isAnnouncing.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isAnnouncing.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isAnnouncing.value = false
                        }
                    })
                    Log.d(TAG, "Call Announcer TTS initialized successfully.")
                } else {
                    Log.e(TAG, "Failed to initialize Call Announcer TTS.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up TTS engine", e)
        }
    }

    fun setAnnouncerEnabled(enabled: Boolean) {
        _announcerEnabled.value = enabled
    }

    /**
     * Inspects current device ringer mode:
     * Returns true if device is SILENT or VIBRATE.
     */
    fun isDeviceInSilentOrVibrateMode(): Boolean {
        val mode = audioManager.ringerMode
        return mode == AudioManager.RINGER_MODE_SILENT || mode == AudioManager.RINGER_MODE_VIBRATE
    }

    /**
     * Resolves caller name from phone number against device ContactsContract.
     */
    fun resolveCallerName(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return it.getString(nameIndex)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact for $phoneNumber", e)
            null
        }
    }

    /**
     * Handles incoming call ringing event:
     * - Checks if phone is on Silent/Vibrate mode.
     * - If yes: Formulates message ("Boss, [Name] ka call aa raha hai." or unknown) and announces via TTS.
     * - If no (Normal Ringer): Stays completely silent!
     */
    fun onIncomingCall(phoneNumber: String?): Boolean {
        if (!_announcerEnabled.value || isMuted) {
            Log.d(TAG, "Call announcer is disabled or muted.")
            return false
        }

        val isSilent = isDeviceInSilentOrVibrateMode()
        if (!isSilent) {
            Log.d(TAG, "Device is in NORMAL ringer mode. MM will stay silent and let ringtone play.")
            _lastAnnouncement.value = "Phone is ringing normally; MM stayed silent."
            return false
        }

        Log.i(TAG, "Device is in SILENT/VIBRATE mode! Initiating MM call announcement...")

        val callerName = resolveCallerName(phoneNumber)
        val announcement = if (!callerName.isNullOrBlank()) {
            "Boss, $callerName ka call aa raha hai."
        } else {
            UNKNOWN_CALLER_MESSAGE
        }

        speakAnnouncement(announcement)
        return true
    }

    /**
     * Speak announcement out loud via TextToSpeech on an audible audio stream.
     */
    fun speakAnnouncement(message: String) {
        _lastAnnouncement.value = message
        scope.launch {
            if (!isTtsReady || tts == null) {
                initTts()
            }

            try {
                val params = android.os.Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }

                tts?.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    params,
                    "mm_call_announcement_${System.currentTimeMillis()}"
                )
                Log.i(TAG, "Announced: $message")
            } catch (e: Exception) {
                Log.e(TAG, "Error speaking call announcement", e)
            }
        }
    }

    /**
     * Helper for manual / UI / testing triggers.
     */
    fun testAnnounce(simulatedContactName: String? = "Akash Upadhyay", forceSilent: Boolean = true): String {
        val announcement = if (!simulatedContactName.isNullOrBlank()) {
            "Boss, $simulatedContactName ka call aa raha hai."
        } else {
            UNKNOWN_CALLER_MESSAGE
        }

        if (forceSilent || isDeviceInSilentOrVibrateMode()) {
            speakAnnouncement(announcement)
            return "Announced: $announcement"
        } else {
            _lastAnnouncement.value = "Phone is in normal ring mode; announcement suppressed."
            return "Device is in normal mode; MM stayed silent."
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isAnnouncing.value = false
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }
}
