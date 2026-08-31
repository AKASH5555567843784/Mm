package com.example.audio

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Standard Android RecognitionService implementation for MM Assistant,
 * allowing it to act as the default Voice Input & Recognition engine for Android OS.
 */
class MMRecognitionService : RecognitionService() {

    companion object {
        private const val TAG = "MMRecognitionService"
    }

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        Log.d(TAG, "onStartListening requested via RecognitionService")
        listener?.beginningOfSpeech()
    }

    override fun onCancel(listener: Callback?) {
        Log.d(TAG, "onCancel requested via RecognitionService")
    }

    override fun onStopListening(listener: Callback?) {
        Log.d(TAG, "onStopListening requested via RecognitionService")
        val results = Bundle().apply {
            putStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION,
                arrayListOf("Hello MM")
            )
        }
        listener?.results(results)
    }
}
