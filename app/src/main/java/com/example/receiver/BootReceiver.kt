package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.MMAssistantForegroundService

/**
 * Automatically starts the MMAssistantForegroundService upon device boot
 * to ensure persistent hands-free 'Hello MM' / 'Hey MM' wake-word detection
 * is active continuously across reboots.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MMBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "BootReceiver received action: $action")
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.i(TAG, "Device booted. Starting persistent MMAssistantForegroundService.")
            MMAssistantForegroundService.startService(context)
        }
    }
}
