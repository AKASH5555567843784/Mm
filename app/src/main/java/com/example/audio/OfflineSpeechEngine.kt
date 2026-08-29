package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Provides offline native Android speech recognition and Text-To-Speech (TTS)
 * for open-source local inference when disconnected from Gemini Live.
 */
class OfflineSpeechEngine(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val onSpeechError: (String) -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit
) {

    companion object {
        private const val TAG = "OfflineSpeechEngine"
        private const val UTTERANCE_ID = "MM_OFFLINE_TTS"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var isMuted = false

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stopSpeaking()
        }
    }

    init {
        initTTS()
    }

    private fun initTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                textToSpeech?.language = Locale.US
                textToSpeech?.setPitch(1.15f) // Slightly higher, youthful, vibrant pitch for MM
                textToSpeech?.setSpeechRate(1.05f) // Snappy pace
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        onAmplitudeChanged(0f)
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        onAmplitudeChanged(0f)
                    }
                })
                Log.d(TAG, "Native Android TTS initialized successfully.")
            } else {
                Log.e(TAG, "Failed initializing Native Android TTS.")
            }
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onSpeechError("Speech recognition is not available on this device.")
            return
        }

        try {
            stopListening()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        Log.d(TAG, "Offline SpeechRecognizer ready for speech.")
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        val norm = ((rmsdB + 2f) / 10f).coerceIn(0f, 1f)
                        onAmplitudeChanged(norm)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        onAmplitudeChanged(0f)
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        onAmplitudeChanged(0f)
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "I didn't quite catch that, speak up!"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timed out waiting for voice input."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error occurred."
                            else -> "Voice recognition error code: $error"
                        }
                        Log.w(TAG, msg)
                        onSpeechError(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        onAmplitudeChanged(0f)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val recognizedText = matches?.firstOrNull() ?: ""
                        if (recognizedText.isNotBlank()) {
                            onSpeechRecognized(recognizedText)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting SpeechRecognizer", e)
            _isListening.value = false
            onSpeechError("Error: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            _isListening.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping SpeechRecognizer", e)
        }
    }

    fun speak(text: String) {
        if (isMuted) return
        if (!isTtsReady || textToSpeech == null) {
            Log.w(TAG, "TTS is not ready yet.")
            return
        }
        try {
            _isSpeaking.value = true
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
            }
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text via TTS", e)
            _isSpeaking.value = false
        }
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        stopListening()
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
