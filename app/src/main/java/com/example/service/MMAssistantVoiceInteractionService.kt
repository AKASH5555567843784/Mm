package com.example.service

import android.service.voice.VoiceInteractionService
import android.util.Log

/**
 * System VoiceInteractionService enabling MM Assistant to register as the primary
 * Default Digital Assistant in Android settings.
 */
class MMAssistantVoiceInteractionService : VoiceInteractionService() {

    companion object {
        private const val TAG = "MMAssistantVoiceService"
    }

    override fun onReady() {
        super.onReady()
        Log.i(TAG, "MMAssistantVoiceInteractionService is ready and active as default voice service.")
    }

    override fun onShutdown() {
        super.onShutdown()
        Log.i(TAG, "MMAssistantVoiceInteractionService shutdown.")
    }
}
