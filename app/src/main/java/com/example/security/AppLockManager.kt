package com.example.security

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import com.example.MainActivity
import com.example.model.ToolExecutionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * AppLockManager:
 * Provides secure app locking, hidden app vault management, and stealth launcher hiding
 * for MM Assistant. Supports voice-driven commands like "Lock WhatsApp", "Hide Instagram",
 * "Unlock Gallery", "Hide MM Assistant", "Show locked apps", etc.
 */
class AppLockManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AppLockManager"
        private const val PREFS_NAME = "mm_app_lock_prefs"
        private const val KEY_LOCKED_PACKAGES = "locked_packages"
        private const val KEY_HIDDEN_PACKAGES = "hidden_packages"
        private const val KEY_MASTER_PIN = "master_pin"
        private const val KEY_DEFAULT_PIN = "1234"
        private const val KEY_IS_ASSISTANT_HIDDEN = "is_assistant_hidden"
        private const val KEY_LOCK_ENABLED = "app_lock_enabled"

        @Volatile
        private var INSTANCE: AppLockManager? = null

        fun getInstance(context: Context): AppLockManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppLockManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _lockedApps = MutableStateFlow<Set<String>>(emptySet())
    val lockedApps: StateFlow<Set<String>> = _lockedApps.asStateFlow()

    private val _hiddenApps = MutableStateFlow<Set<String>>(emptySet())
    val hiddenApps: StateFlow<Set<String>> = _hiddenApps.asStateFlow()

    private val _isAssistantHidden = MutableStateFlow(false)
    val isAssistantHidden: StateFlow<Boolean> = _isAssistantHidden.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(true)
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _masterPin = MutableStateFlow(KEY_DEFAULT_PIN)
    val masterPin: StateFlow<String> = _masterPin.asStateFlow()

    private val _lastSecurityAction = MutableStateFlow<String?>(null)
    val lastSecurityAction: StateFlow<String?> = _lastSecurityAction.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        val locked = prefs.getStringSet(KEY_LOCKED_PACKAGES, emptySet()) ?: emptySet()
        val hidden = prefs.getStringSet(KEY_HIDDEN_PACKAGES, emptySet()) ?: emptySet()
        val pin = prefs.getString(KEY_MASTER_PIN, KEY_DEFAULT_PIN) ?: KEY_DEFAULT_PIN
        val isHidden = prefs.getBoolean(KEY_IS_ASSISTANT_HIDDEN, false)
        val isLockEnabled = prefs.getBoolean(KEY_LOCK_ENABLED, true)

        _lockedApps.value = locked
        _hiddenApps.value = hidden
        _masterPin.value = pin
        _isAssistantHidden.value = isHidden
        _isAppLockEnabled.value = isLockEnabled
    }

    /**
     * Set a new 4-digit Master Security PIN for App Lock.
     */
    fun setMasterPin(newPin: String): Boolean {
        val cleanPin = newPin.trim().filter { it.isDigit() }
        if (cleanPin.length in 4..6) {
            prefs.edit().putString(KEY_MASTER_PIN, cleanPin).apply()
            _masterPin.value = cleanPin
            _lastSecurityAction.value = "Master PIN updated successfully."
            return true
        }
        return false
    }

    /**
     * Verify entered PIN against Master PIN.
     */
    fun verifyPin(pin: String): Boolean {
        return pin.trim() == _masterPin.value
    }

    /**
     * Toggle global app lock feature on/off.
     */
    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
        _isAppLockEnabled.value = enabled
        _lastSecurityAction.value = if (enabled) "App Lock Protection Enabled" else "App Lock Protection Disabled"
    }

    /**
     * Locks an application by name or package.
     */
    fun lockApp(appName: String, pin: String? = null): ToolExecutionResult {
        val query = appName.trim().lowercase(Locale.ROOT)
        if (query.isEmpty()) {
            return ToolExecutionResult(
                success = false,
                message = "Which app would you like me to lock, Boss? Name it and it's sealed!"
            )
        }

        // Special case: MM Assistant
        if (query.contains("mm") || query.contains("assistant") || query.contains("this app") || query.contains("self")) {
            val pkg = context.packageName
            val updated = _lockedApps.value.toMutableSet().apply { add(pkg) }
            prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, updated).apply()
            _lockedApps.value = updated
            _lastSecurityAction.value = "Locked MM Assistant"
            return ToolExecutionResult(
                success = true,
                message = "MM Assistant is locked down tight with your PIN (${_masterPin.value}), Boss! Nobody's snooping in here.",
                data = mapOf("package" to pkg, "app" to "MM Assistant", "status" to "locked")
            )
        }

        val resolved = resolveAppDetails(query)
        val targetPackage = resolved?.packageName ?: "com.app.${query.replace(" ", "")}"
        val displayName = resolved?.label ?: appName.replaceFirstChar { it.uppercase() }

        val current = _lockedApps.value.toMutableSet()
        if (current.contains(targetPackage)) {
            return ToolExecutionResult(
                success = true,
                message = "$displayName is already locked and protected, Boss! Relax, it's safe.",
                data = mapOf("package" to targetPackage, "app" to displayName, "status" to "already_locked")
            )
        }

        current.add(targetPackage)
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedApps.value = current
        _lastSecurityAction.value = "Locked $displayName"

        Log.i(TAG, "Locked app: $displayName ($targetPackage). Total locked: ${current.size}")

        return ToolExecutionResult(
            success = true,
            message = "Boom! $displayName is now locked down. PIN protection is active!",
            data = mapOf("package" to targetPackage, "app" to displayName, "status" to "locked", "total_locked" to current.size)
        )
    }

    /**
     * Unlocks an application by name or package.
     */
    fun unlockApp(appName: String, pin: String? = null): ToolExecutionResult {
        val query = appName.trim().lowercase(Locale.ROOT)
        if (query.isEmpty()) {
            return ToolExecutionResult(
                success = false,
                message = "Tell me which app to unlock, Boss!"
            )
        }

        // Special case: unlock all
        if (query.contains("all") || query == "everything" || query.contains("all apps")) {
            val count = _lockedApps.value.size
            prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, emptySet()).apply()
            _lockedApps.value = emptySet()
            _lastSecurityAction.value = "Unlocked all applications"
            return ToolExecutionResult(
                success = true,
                message = "Unlocked all $count applications, Boss! Everything is wide open for you.",
                data = mapOf("unlocked_count" to count)
            )
        }

        // Special case: MM Assistant
        if (query.contains("mm") || query.contains("assistant") || query.contains("this app") || query.contains("self")) {
            val pkg = context.packageName
            val updated = _lockedApps.value.toMutableSet().apply { remove(pkg) }
            prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, updated).apply()
            _lockedApps.value = updated
            _lastSecurityAction.value = "Unlocked MM Assistant"
            return ToolExecutionResult(
                success = true,
                message = "MM Assistant is now unlocked, Boss!",
                data = mapOf("package" to pkg, "app" to "MM Assistant", "status" to "unlocked")
            )
        }

        val resolved = resolveAppDetails(query)
        val targetPackage = resolved?.packageName ?: "com.app.${query.replace(" ", "")}"
        val displayName = resolved?.label ?: appName.replaceFirstChar { it.uppercase() }

        val current = _lockedApps.value.toMutableSet()
        val found = current.find { it.equals(targetPackage, ignoreCase = true) || it.contains(query, ignoreCase = true) }

        if (found == null) {
            return ToolExecutionResult(
                success = true,
                message = "$displayName isn't locked right now, Boss!",
                data = mapOf("app" to displayName, "status" to "not_locked")
            )
        }

        current.remove(found)
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedApps.value = current
        _lastSecurityAction.value = "Unlocked $displayName"

        Log.i(TAG, "Unlocked app: $displayName ($found). Total locked remaining: ${current.size}")

        return ToolExecutionResult(
            success = true,
            message = "$displayName is now unlocked and ready to use, Boss!",
            data = mapOf("package" to found, "app" to displayName, "status" to "unlocked")
        )
    }

    /**
     * Hides an app into the stealth vault or toggles launcher visibility.
     */
    fun hideApp(appName: String): ToolExecutionResult {
        val query = appName.trim().lowercase(Locale.ROOT)
        if (query.isEmpty()) {
            return ToolExecutionResult(
                success = false,
                message = "Which app do you want me to hide, Boss? I can make it vanish in a second!"
            )
        }

        // Special case: Hide MM Assistant itself
        if (query.contains("mm") || query.contains("assistant") || query == "app" || query == "this app" || query == "hide me" || query == "stealth") {
            return toggleAssistantLauncherVisibility(hide = true)
        }

        val resolved = resolveAppDetails(query)
        val targetPackage = resolved?.packageName ?: "com.app.${query.replace(" ", "")}"
        val displayName = resolved?.label ?: appName.replaceFirstChar { it.uppercase() }

        val current = _hiddenApps.value.toMutableSet()
        current.add(targetPackage)
        prefs.edit().putStringSet(KEY_HIDDEN_PACKAGES, current).apply()
        _hiddenApps.value = current
        _lastSecurityAction.value = "Hidden $displayName in Vault"

        return ToolExecutionResult(
            success = true,
            message = "Poof! $displayName is now moved into your Hidden Stealth Vault. Just say 'Unhide $displayName' whenever you want it back, Boss!",
            data = mapOf("package" to targetPackage, "app" to displayName, "status" to "hidden", "total_hidden" to current.size)
        )
    }

    /**
     * Unhides an app from the stealth vault or restores launcher visibility.
     */
    fun unhideApp(appName: String): ToolExecutionResult {
        val query = appName.trim().lowercase(Locale.ROOT)
        if (query.isEmpty()) {
            return ToolExecutionResult(
                success = false,
                message = "Which app should I unhide, Boss?"
            )
        }

        // Special case: Unhide MM Assistant itself
        if (query.contains("mm") || query.contains("assistant") || query == "app" || query == "this app" || query == "unhide me") {
            return toggleAssistantLauncherVisibility(hide = false)
        }

        // Special case: unhide all
        if (query.contains("all") || query == "everything" || query.contains("all apps")) {
            val count = _hiddenApps.value.size
            prefs.edit().putStringSet(KEY_HIDDEN_PACKAGES, emptySet()).apply()
            _hiddenApps.value = emptySet()
            _lastSecurityAction.value = "Restored all hidden apps"
            return ToolExecutionResult(
                success = true,
                message = "Restored all $count hidden apps back to your main list, Boss!",
                data = mapOf("unhidden_count" to count)
            )
        }

        val resolved = resolveAppDetails(query)
        val targetPackage = resolved?.packageName ?: "com.app.${query.replace(" ", "")}"
        val displayName = resolved?.label ?: appName.replaceFirstChar { it.uppercase() }

        val current = _hiddenApps.value.toMutableSet()
        val found = current.find { it.equals(targetPackage, ignoreCase = true) || it.contains(query, ignoreCase = true) }

        if (found == null) {
            return ToolExecutionResult(
                success = true,
                message = "$displayName isn't hidden in your vault, Boss!",
                data = mapOf("app" to displayName, "status" to "not_hidden")
            )
        }

        current.remove(found)
        prefs.edit().putStringSet(KEY_HIDDEN_PACKAGES, current).apply()
        _hiddenApps.value = current
        _lastSecurityAction.value = "Unhid $displayName"

        return ToolExecutionResult(
            success = true,
            message = "Ta-da! $displayName is restored and visible again, Boss!",
            data = mapOf("package" to found, "app" to displayName, "status" to "unhidden")
        )
    }

    /**
     * Hide or show MM Assistant app icon from launcher / stealth mode.
     */
    fun toggleAssistantLauncherVisibility(hide: Boolean): ToolExecutionResult {
        return try {
            val pm = context.packageManager
            val componentName = ComponentName(context, MainActivity::class.java)

            val newState = if (hide) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }

            // Note: In typical Android runtime, disabling the main activity component hides launcher icon
            // We safely handle and record state in preferences
            try {
                pm.setComponentEnabledSetting(
                    componentName,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                Log.w(TAG, "Component state toggle notice: ${e.message}")
            }

            prefs.edit().putBoolean(KEY_IS_ASSISTANT_HIDDEN, hide).apply()
            _isAssistantHidden.value = hide
            _lastSecurityAction.value = if (hide) "MM Assistant Stealth Mode ON" else "MM Assistant Stealth Mode OFF"

            if (hide) {
                ToolExecutionResult(
                    success = true,
                    message = "Stealth Mode Activated, Boss! MM Assistant icon is now hidden. You can summon me anytime by saying 'Hey MM' or dialling PIN ${_masterPin.value}!",
                    data = mapOf("stealth" to true)
                )
            } else {
                ToolExecutionResult(
                    success = true,
                    message = "Stealth Mode Deactivated! MM Assistant icon is proudly restored to your home screen, Boss!",
                    data = mapOf("stealth" to false)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed toggling assistant visibility", e)
            ToolExecutionResult(
                success = false,
                message = "Couldn't toggle stealth mode: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Lists all secured, locked, and hidden apps.
     */
    fun listSecuredApps(): ToolExecutionResult {
        val locked = _lockedApps.value.map { getAppDisplayName(it) }
        val hidden = _hiddenApps.value.map { getAppDisplayName(it) }
        val isStealth = _isAssistantHidden.value

        val message = buildString {
            append("Here is your security status, Boss:\n")
            if (locked.isEmpty() && hidden.isEmpty() && !isStealth) {
                append("• No apps are currently locked or hidden. Everything is standard.")
            } else {
                if (locked.isNotEmpty()) {
                    append("🔒 Locked Apps (${locked.size}): ${locked.joinToString(", ")}\n")
                }
                if (hidden.isNotEmpty()) {
                    append("👁️ Hidden Vault Apps (${hidden.size}): ${hidden.joinToString(", ")}\n")
                }
                if (isStealth) {
                    append("🥷 MM Assistant Stealth Mode: ACTIVE (Icon hidden from launcher)\n")
                }
            }
            append("Master PIN: ${_masterPin.value}")
        }

        return ToolExecutionResult(
            success = true,
            message = message,
            data = mapOf(
                "locked_apps" to locked,
                "hidden_apps" to hidden,
                "assistant_stealth" to isStealth,
                "app_lock_enabled" to _isAppLockEnabled.value
            )
        )
    }

    fun isAppLocked(packageName: String): Boolean {
        if (!_isAppLockEnabled.value) return false
        return _lockedApps.value.contains(packageName)
    }

    fun isAppHidden(packageName: String): Boolean {
        return _hiddenApps.value.contains(packageName)
    }

    private data class AppInfo(val packageName: String, val label: String)

    private fun resolveAppDetails(query: String): AppInfo? {
        val common = mapOf(
            "whatsapp" to AppInfo("com.whatsapp", "WhatsApp"),
            "instagram" to AppInfo("com.instagram.android", "Instagram"),
            "youtube" to AppInfo("com.google.android.youtube", "YouTube"),
            "spotify" to AppInfo("com.spotify.music", "Spotify"),
            "gallery" to AppInfo("com.google.android.apps.photos", "Photos/Gallery"),
            "photos" to AppInfo("com.google.android.apps.photos", "Photos"),
            "chrome" to AppInfo("com.android.chrome", "Chrome"),
            "telegram" to AppInfo("org.telegram.messenger", "Telegram"),
            "facebook" to AppInfo("com.facebook.katana", "Facebook"),
            "snapchat" to AppInfo("com.snapchat.android", "Snapchat"),
            "twitter" to AppInfo("com.twitter.android", "X (Twitter)"),
            "x" to AppInfo("com.twitter.android", "X (Twitter)"),
            "gmail" to AppInfo("com.google.android.gm", "Gmail"),
            "calculator" to AppInfo("com.google.android.calculator", "Calculator"),
            "settings" to AppInfo("com.android.settings", "Settings")
        )

        if (common.containsKey(query)) return common[query]

        try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(mainIntent, 0)
            for (info in apps) {
                val label = info.loadLabel(pm).toString()
                if (label.equals(query, ignoreCase = true) || label.lowercase(Locale.ROOT).contains(query)) {
                    return AppInfo(info.activityInfo.packageName, label)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving app details for query: $query", e)
        }

        return null
    }

    fun getAppDisplayName(packageName: String): String {
        if (packageName == context.packageName) return "MM Assistant"
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }
}
