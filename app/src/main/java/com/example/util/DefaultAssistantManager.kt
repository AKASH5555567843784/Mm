package com.example.util

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Helper to check and prompt the user to set MM Assistant as the system's
 * Default Digital Assistant App (Voice Assistant / Assistant & Voice Input).
 */
object DefaultAssistantManager {

    private const val TAG = "DefaultAssistantMgr"

    /**
     * Checks if MM Assistant is currently configured as the system default assist app.
     */
    fun isDefaultAssistant(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                    return roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)
                }
            }

            val assistantSetting = Settings.Secure.getString(
                context.contentResolver,
                "assistant"
            ) ?: ""
            
            val voiceInteractionSetting = Settings.Secure.getString(
                context.contentResolver,
                "voice_interaction_service"
            ) ?: ""

            val myPackage = context.packageName
            assistantSetting.contains(myPackage) || voiceInteractionSetting.contains(myPackage)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default assistant status", e)
            false
        }
    }

    /**
     * Creates an Intent to open the system settings screen where the user can choose MM Assistant
     * as their Default Digital Assistant App.
     */
    fun createSetDefaultAssistantIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
            }
        }

        // Try standard Assist & Voice Input Settings screen
        val assistSettingsIntent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (assistSettingsIntent.resolveActivity(context.packageManager) != null) {
            return assistSettingsIntent
        }

        // Fallback to manage default apps
        val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (defaultAppsIntent.resolveActivity(context.packageManager) != null) {
            return defaultAppsIntent
        }

        // Final fallback: General settings
        return Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Creates an Intent to open Power Button / Gesture shortcuts settings (e.g. Hold power for Assistant).
     */
    fun createPowerButtonAssistantSettingsIntent(context: Context): Intent {
        val gestureIntents = listOf(
            Intent("android.settings.ASSIST_GESTURE_SETTINGS"),
            Intent("com.android.settings.GESTURE_SETTINGS"),
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in gestureIntents) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (intent.resolveActivity(context.packageManager) != null) {
                return intent
            }
        }
        return Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    }
}
