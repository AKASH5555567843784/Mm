package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.receiver.MMAssistantWakeWordReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * SpeechRecognizerWakeWordManager:
 * Continuous background listening engine utilizing Android's native SpeechRecognizer
 * to detect specific wake-words like 'Hey MM', 'Hello MM', 'MM', 'Hi MM', and 'Ok MM'
 * and trigger the assistant's live voice mode.
 */
class SpeechRecognizerWakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "SpeechRecognizerWake"
        private const val RESTART_DELAY_MS = 350L
        private const val COOLDOWN_MS = 2500L

        // Wake word trigger keywords & variations
        val WAKE_WORDS = listOf(
            "hey mm",
            "hello mm",
            "hi mm",
            "ok mm",
            "hey m m",
            "hello m m",
            "hey em em",
            "hey emma",
            "hey mama",
            "hey aim",
            "mm assistant",
            "hey mm assistant",
            "hello mm assistant"
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningActive = false
    private var isPrivacyMode = false
    private var isDestroyed = false
    private var lastTriggerTime = 0L

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastRecognizedPhrase = MutableStateFlow<String?>(null)
    val lastRecognizedPhrase: StateFlow<String?> = _lastRecognizedPhrase.asStateFlow()

    private val _wakeWordTriggerCount = MutableStateFlow(0)
    val wakeWordTriggerCount: StateFlow<Int> = _wakeWordTriggerCount.asStateFlow()

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "SpeechRecognizer ready for speech.")
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech input began.")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Audio RMS level
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech input ended. Scheduling restart...")
            _isListening.value = false
            scheduleRestart()
        }

        override fun onError(error: Int) {
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No recognition match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Error code $error"
            }
            Log.d(TAG, "SpeechRecognizer error: $errorMsg ($error)")
            _isListening.value = false
            scheduleRestart()
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.let { processRecognizedPhrases(it) }
            scheduleRestart()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.let { processRecognizedPhrases(it) }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Start continuous background SpeechRecognizer listening.
     */
    fun startListening() {
        if (isPrivacyMode || isDestroyed) return
        isListeningActive = true

        mainHandler.post {
            ensureRecognizerInitialized()
            startListeningInternal()
        }
    }

    /**
     * Stop background SpeechRecognizer listening.
     */
    fun stopListening() {
        isListeningActive = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognizer", e)
            }
            _isListening.value = false
        }
    }

    /**
     * Set privacy mode. When active, all listening is halted immediately.
     */
    fun setPrivacyMode(enabled: Boolean) {
        isPrivacyMode = enabled
        if (enabled) {
            stopListening()
            Log.i(TAG, "Privacy Mode enabled: SpeechRecognizer halted.")
        } else {
            Log.i(TAG, "Privacy Mode disabled: Resuming SpeechRecognizer.")
            startListening()
        }
    }

    private fun ensureRecognizerInitialized() {
        if (speechRecognizer == null && !isDestroyed) {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(recognitionListener)
                    }
                    Log.i(TAG, "Created background SpeechRecognizer instance.")
                } else {
                    Log.w(TAG, "Speech recognition is not available on this device.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create SpeechRecognizer", e)
            }
        }
    }

    private fun startListeningInternal() {
        if (!isListeningActive || isPrivacyMode || isDestroyed) return

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra("android.speech.extra.DICTATION_MODE", true)
            }
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting SpeechRecognizer listening", e)
            _isListening.value = false
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        if (!isListeningActive || isPrivacyMode || isDestroyed) return

        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isListeningActive && !isPrivacyMode && !isDestroyed) {
                try {
                    speechRecognizer?.cancel()
                } catch (e: Exception) {
                    Log.w(TAG, "Cancel before restart error: ${e.message}")
                }
                startListeningInternal()
            }
        }, RESTART_DELAY_MS)
    }

    /**
     * Inspects recognized phrases from SpeechRecognizer for wake words.
     */
    fun processRecognizedPhrases(phrases: List<String>): Boolean {
        for (rawPhrase in phrases) {
            val normalized = rawPhrase.lowercase(Locale.ROOT).trim()
            _lastRecognizedPhrase.value = rawPhrase

            val matchedWakeWord = checkPhraseForWakeWord(normalized)
            if (matchedWakeWord != null) {
                val now = System.currentTimeMillis()
                if (now - lastTriggerTime > COOLDOWN_MS) {
                    lastTriggerTime = now
                    _wakeWordTriggerCount.value += 1
                    Log.i(TAG, "🎯 Wake-word detected by SpeechRecognizer: '$matchedWakeWord' (Full phrase: '$rawPhrase')")

                    // Dispatch via callback
                    onWakeWordDetected(matchedWakeWord)

                    // Also emit system broadcast
                    val broadcastIntent = Intent(MMAssistantWakeWordReceiver.ACTION_WAKE_WORD_DETECTED).apply {
                        putExtra(MMAssistantWakeWordReceiver.EXTRA_TRIGGER_SOURCE, "speech_recognizer:$matchedWakeWord")
                    }
                    context.sendBroadcast(broadcastIntent)

                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks if a phrase contains wake words or standalone triggers.
     */
    fun checkPhraseForWakeWord(phrase: String): String? {
        // Direct matching against defined wake words
        for (wakeWord in WAKE_WORDS) {
            if (phrase.contains(wakeWord)) {
                return wakeWord
            }
        }

        // Check standalone 'mm' token surrounded by word boundaries or at end of phrase
        val tokens = phrase.split("\\s+".toRegex())
        for (i in tokens.indices) {
            val token = tokens[i].trim('.', ',', '!', '?')
            if (token == "mm" || token == "m.m.") {
                return "mm"
            }
            if (i > 0 && (tokens[i - 1] == "hey" || tokens[i - 1] == "hello" || tokens[i - 1] == "hi") && (token == "mm" || token == "m" || token == "em")) {
                return "${tokens[i - 1]} mm"
            }
        }

        return null
    }

    /**
     * Destroy SpeechRecognizer resources.
     */
    fun destroy() {
        isDestroyed = true
        isListeningActive = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying SpeechRecognizer", e)
            }
            speechRecognizer = null
            _isListening.value = false
        }
    }
}
