package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Helper utility to check and request disabling battery optimizations
 * to guarantee that MMAssistantForegroundService can run uninterrupted in the background.
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptHelper"

    /**
     * Checks if the application is currently whitelisted / exempted from battery optimization.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            } else {
                true // Battery optimizations introduced in Android 6.0 (API 23)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery optimization status", e)
            false
        }
    }

    /**
     * Creates an intent to request disabling battery optimization specifically for this app.
     * Uses ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS if available, or falls back to
     * ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS.
     */
    @SuppressLint("BatteryLife")
    fun createIgnoreBatteryOptimizationIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = context.packageName
            val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (requestIntent.resolveActivity(context.packageManager) != null) {
                return requestIntent
            }

            return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Launches the battery optimization exclusion request or settings page.
     */
    fun requestDisableBatteryOptimization(context: Context): Boolean {
        return try {
            val intent = createIgnoreBatteryOptimizationIntent(context)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch battery optimization intent", e)
            try {
                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed fallback battery settings", fallbackEx)
                false
            }
        }
    }
}
