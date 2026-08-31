package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log
import com.example.ui.MMAssistOverlayActivity

/**
 * VoiceInteractionSessionService providing sessions for system-level assistant invocations
 * (Power button long press, bottom corner swipe gestures).
 */
class MMAssistantVoiceInteractionSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return MMAssistantVoiceInteractionSession(this)
    }
}

/**
 * Handles incoming system-level assist requests and launches the glowing edge overlay & bottom reveal card.
 */
class MMAssistantVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    companion object {
        private const val TAG = "MMAssistSession"
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.i(TAG, "VoiceInteractionSession onShow triggered with flags: $showFlags")

        try {
            val intent = Intent(context, MMAssistOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("trigger_source", "SYSTEM_DEFAULT_ASSISTANT_GESTURE")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch MMAssistOverlayActivity from session", e)
        }
        
        hide()
    }
}
