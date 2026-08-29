package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MainActivity
import com.example.service.MMAssistantForegroundService

/**
 * BroadcastReceiver that responds to the wake-word trigger broadcast intent
 * ("com.example.mm.action.WAKE_WORD_DETECTED") emitted when the persistent AudioRecord buffer
 * detects the 'MM' wake-word acoustic signature.
 *
 * It launches or resumes the Gemini Live session and brings the assistant interface into focus.
 */
class MMAssistantWakeWordReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MMWakeWordReceiver"
        const val ACTION_WAKE_WORD_DETECTED = "com.example.mm.action.WAKE_WORD_DETECTED"
        const val EXTRA_TRIGGER_SOURCE = "trigger_source"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "Received broadcast intent action: $action")

        if (action == ACTION_WAKE_WORD_DETECTED) {
            val source = intent.getStringExtra(EXTRA_TRIGGER_SOURCE) ?: "audio_record_buffer"
            Log.i(TAG, "Wake-word 'MM' signature confirmed from $source. Launching/resuming Gemini Live session.")

            // 1. Ensure Foreground Service instance has live session awakened
            MMAssistantForegroundService.activeServiceInstance?.triggerWakeWordAwakening()

            // 2. Launch or bring MainActivity to the foreground
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("WAKE_TRIGGERED", true)
                putExtra("TRIGGER_SOURCE", source)
            }
            context.startActivity(launchIntent)
        }
    }
}
