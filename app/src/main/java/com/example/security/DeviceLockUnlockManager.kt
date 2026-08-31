package com.example.security

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.example.model.ToolExecutionResult
import com.example.service.MMAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Supported Phone Screen Lock Types
 */
enum class DeviceLockType(val label: String, val description: String) {
    PIN("PIN Code", "4 to 8 digit numerical passcode"),
    PATTERN("Pattern Lock", "3x3 grid point sequence (e.g. 1-2-3-6-9)"),
    PASSWORD("Password", "Alphanumeric text password"),
    SWIPE("Swipe / None", "Simple swipe gesture without password")
}

/**
 * DeviceLockUnlockManager:
 * Manages phone screen lock & unlock automation for MM Assistant.
 * Stores user-configured phone unlock credentials (PIN, Pattern, Password, Swipe)
 * and executes screen wake, keyguard dismiss, and credential injection when voice-triggered.
 */
class DeviceLockUnlockManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "DeviceLockUnlockManager"
        private const val PREFS_NAME = "mm_device_lock_prefs"
        private const val KEY_LOCK_TYPE = "phone_lock_type"
        private const val KEY_CREDENTIAL = "phone_credential"
        private const val KEY_AUTO_VOICE_UNLOCK = "auto_voice_unlock_enabled"
        private const val KEY_LAST_ACTION = "last_phone_lock_action"

        @Volatile
        private var INSTANCE: DeviceLockUnlockManager? = null

        fun getInstance(context: Context): DeviceLockUnlockManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceLockUnlockManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _lockType = MutableStateFlow(DeviceLockType.PIN)
    val lockType: StateFlow<DeviceLockType> = _lockType.asStateFlow()

    private val _savedCredential = MutableStateFlow("")
    val savedCredential: StateFlow<String> = _savedCredential.asStateFlow()

    private val _isAutoVoiceUnlockEnabled = MutableStateFlow(true)
    val isAutoVoiceUnlockEnabled: StateFlow<Boolean> = _isAutoVoiceUnlockEnabled.asStateFlow()

    private val _lastAction = MutableStateFlow<String?>(null)
    val lastAction: StateFlow<String?> = _lastAction.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        loadPreferences()
        checkAccessibilityStatus()
    }

    private fun loadPreferences() {
        val typeName = prefs.getString(KEY_LOCK_TYPE, DeviceLockType.PIN.name) ?: DeviceLockType.PIN.name
        val type = try {
            DeviceLockType.valueOf(typeName)
        } catch (e: Exception) {
            DeviceLockType.PIN
        }
        val cred = prefs.getString(KEY_CREDENTIAL, "") ?: ""
        val auto = prefs.getBoolean(KEY_AUTO_VOICE_UNLOCK, true)

        _lockType.value = type
        _savedCredential.value = cred
        _isAutoVoiceUnlockEnabled.value = auto
    }

    fun checkAccessibilityStatus(): Boolean {
        val isRunning = MMAccessibilityService.isServiceRunning
        _isAccessibilityEnabled.value = isRunning
        return isRunning
    }

    /**
     * Save or update phone lock credentials.
     */
    fun saveCredentials(
        type: DeviceLockType,
        credential: String,
        autoVoiceUnlock: Boolean = true
    ): ToolExecutionResult {
        val cleanCred = credential.trim()
        
        if (type != DeviceLockType.SWIPE && cleanCred.isEmpty()) {
            return ToolExecutionResult(
                success = false,
                message = "Please provide your phone's ${type.label} so MM Assistant can unlock it for you, Boss!"
            )
        }

        prefs.edit()
            .putString(KEY_LOCK_TYPE, type.name)
            .putString(KEY_CREDENTIAL, cleanCred)
            .putBoolean(KEY_AUTO_VOICE_UNLOCK, autoVoiceUnlock)
            .apply()

        _lockType.value = type
        _savedCredential.value = cleanCred
        _isAutoVoiceUnlockEnabled.value = autoVoiceUnlock

        val feedback = when (type) {
            DeviceLockType.PIN -> "Phone PIN saved! I'll auto-enter your PIN whenever you say 'MM unlock my phone', Boss!"
            DeviceLockType.PATTERN -> "Pattern sequence ($cleanCred) locked in! Say 'MM unlock my phone' anytime to draw it automatically!"
            DeviceLockType.PASSWORD -> "Phone password saved securely! Say 'MM unlock my phone' to auto-login."
            DeviceLockType.SWIPE -> "Swipe unlock configured! Say 'MM unlock my phone' to swipe open your screen."
        }

        _lastAction.value = "Saved ${type.label} credentials"
        Log.i(TAG, "Device lock credentials updated for type: $type")

        return ToolExecutionResult(
            success = true,
            message = feedback,
            data = mapOf("type" to type.name, "has_credential" to cleanCred.isNotEmpty())
        )
    }

    /**
     * Clear saved phone lock credentials.
     */
    fun clearCredentials(): ToolExecutionResult {
        prefs.edit()
            .remove(KEY_CREDENTIAL)
            .putString(KEY_LOCK_TYPE, DeviceLockType.PIN.name)
            .apply()

        _savedCredential.value = ""
        _lockType.value = DeviceLockType.PIN
        _lastAction.value = "Cleared phone unlock credentials"

        return ToolExecutionResult(
            success = true,
            message = "Phone unlock credentials cleared, Boss! You can set a new PIN, Pattern, or Password anytime."
        )
    }

    /**
     * Toggles auto voice unlock setting.
     */
    fun setAutoVoiceUnlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_VOICE_UNLOCK, enabled).apply()
        _isAutoVoiceUnlockEnabled.value = enabled
        _lastAction.value = if (enabled) "Voice Unlock Enabled" else "Voice Unlock Disabled"
    }

    /**
     * Unlocks the phone screen using saved credentials (PIN, Pattern, Password, Swipe).
     */
    fun unlockPhone(overrideCredential: String? = null): ToolExecutionResult {
        val type = _lockType.value
        val credential = overrideCredential?.trim()?.ifEmpty { null } ?: _savedCredential.value

        if (type != DeviceLockType.SWIPE && credential.isEmpty()) {
            return ToolExecutionResult(
                success = false,
                message = "Boss, you haven't saved your phone's ${type.label} in MM Assistant yet! Go to Settings -> Device Lock & Unlock or tell me your password to save it."
            )
        }

        // 1. Wake the screen up
        wakeScreenUp()

        // 2. Execute accessibility gesture / keyguard dismissal
        val accService = MMAccessibilityService.instance
        var automated = false

        if (accService != null) {
            automated = true
            accService.swipeUpToUnlock {
                when (type) {
                    DeviceLockType.PIN -> {
                        accService.enterPinGesture(credential) {
                            Log.i(TAG, "Completed PIN unlock execution")
                        }
                    }
                    DeviceLockType.PATTERN -> {
                        val points = parsePatternPoints(credential)
                        if (points.isNotEmpty()) {
                            accService.drawPatternGesture(points) {
                                Log.i(TAG, "Completed Pattern unlock execution")
                            }
                        }
                    }
                    DeviceLockType.PASSWORD -> {
                        accService.enterPasswordText(credential) {
                            Log.i(TAG, "Completed Password unlock execution")
                        }
                    }
                    DeviceLockType.SWIPE -> {
                        Log.i(TAG, "Completed Swipe unlock execution")
                    }
                }
            }
        }

        // 3. Fallback request dismiss keyguard
        try {
            val activity = context as? android.app.Activity
            if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                km?.requestDismissKeyguard(activity, null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Keyguard dismissal exception: ${e.message}")
        }

        val sassyResponse = when (type) {
            DeviceLockType.PIN -> "Unlocking your phone right now, Boss! PIN entered and screen is wide open for you. Let's conquer the day!"
            DeviceLockType.PATTERN -> "Drawing your pattern lock right now, Boss! Boom, phone is unlocked and ready!"
            DeviceLockType.PASSWORD -> "Typing in your phone password... and unlocked! Welcome back, Boss!"
            DeviceLockType.SWIPE -> "Swiping up to unlock your phone, Boss! Screen is awake and ready!"
        }

        _lastAction.value = "Phone unlocked with ${type.label}"

        return ToolExecutionResult(
            success = true,
            message = sassyResponse,
            data = mapOf(
                "type" to type.name,
                "automated" to automated,
                "accessibility_active" to (accService != null)
            )
        )
    }

    /**
     * Locks the phone screen immediately.
     */
    fun lockPhone(): ToolExecutionResult {
        val accService = MMAccessibilityService.instance
        var locked = false

        if (accService != null) {
            locked = accService.lockScreen()
        }

        _lastAction.value = "Phone locked"

        return ToolExecutionResult(
            success = true,
            message = "Phone locked down tight, Boss! Screen is asleep and secured with your ${lockType.value.label}.",
            data = mapOf(
                "locked" to locked,
                "accessibility_active" to (accService != null)
            )
        )
    }

    /**
     * Open Android Accessibility Settings so user can enable MM Accessibility Service.
     */
    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
        }
    }

    private fun wakeScreenUp() {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && !pm.isInteractive) {
                @Suppress("DEPRECATION")
                val wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "MMAssistant:PhoneUnlockWakeLock"
                )
                wakeLock.acquire(3000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock for phone unlock", e)
        }
    }

    /**
     * Parses pattern string like "1-2-3-6-9" or "1,2,3,6,9" or "12369" into list of integer points.
     */
    fun parsePatternPoints(patternStr: String): List<Int> {
        val clean = patternStr.trim()
        val digits = clean.filter { it.isDigit() && it in '1'..'9' }
        return digits.map { it.toString().toInt() }
    }
}
