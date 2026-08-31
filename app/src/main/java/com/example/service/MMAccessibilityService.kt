package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * MMAccessibilityService:
 * Enables MM Assistant to automate device lock and unlock operations with user-saved PIN,
 * Pattern, Password, or Swipe credentials upon voice trigger (e.g. "MM unlock my phone", "MM lock phone").
 */
class MMAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MMAccessibilityService"

        @Volatile
        var instance: MMAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "MMAccessibilityService connected and ready for device lock/unlock automation")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing if needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "MMAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "MMAccessibilityService destroyed")
    }

    /**
     * Locks the phone screen immediately via accessibility global action.
     */
    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val result = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            Log.i(TAG, "performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) returned: $result")
            result
        } else {
            Log.w(TAG, "Lock screen global action requires API 28+")
            false
        }
    }

    /**
     * Presses Home button via global action.
     */
    fun pressHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Performs a vertical swipe up gesture to dismiss initial lockscreen slide and reveal PIN/Pattern pad.
     */
    fun swipeUpToUnlock(onComplete: (() -> Unit)? = null): Boolean {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        val startX = width * 0.5f
        val startY = height * 0.85f
        val endX = width * 0.5f
        val endY = height * 0.20f

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i(TAG, "Swipe up completed successfully")
                mainHandler.postDelayed({ onComplete?.invoke() }, 250)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Swipe up gesture cancelled")
                onComplete?.invoke()
            }
        }, null)
    }

    /**
     * Dispatches multi-point pattern drawing gesture across a standard 3x3 lockscreen pattern grid.
     * @param points list of point indices from 1 to 9 (1=top-left, 2=top-center, ... 9=bottom-right)
     */
    fun drawPatternGesture(points: List<Int>, onComplete: (() -> Unit)? = null): Boolean {
        if (points.size < 2) return false

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        // Standard Android pattern lock is located in the middle-to-lower portion of the screen
        val patternSize = width * 0.70f
        val left = (width - patternSize) / 2f
        val top = height * 0.48f

        val colStep = patternSize / 2f
        val rowStep = patternSize / 2f

        // Map point index 1..9 to coordinate
        fun getPointCoord(point: Int): Pair<Float, Float> {
            val idx = (point - 1).coerceIn(0, 8)
            val row = idx / 3
            val col = idx % 3
            return Pair(left + col * colStep, top + row * rowStep)
        }

        val path = Path()
        val firstCoord = getPointCoord(points.first())
        path.moveTo(firstCoord.first, firstCoord.second)

        for (i in 1 until points.size) {
            val coord = getPointCoord(points[i])
            path.lineTo(coord.first, coord.second)
        }

        val duration = (points.size * 180L).coerceAtLeast(400L)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i(TAG, "Pattern gesture completed successfully for ${points.size} points")
                onComplete?.invoke()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Pattern gesture cancelled")
                onComplete?.invoke()
            }
        }, null)
    }

    /**
     * Enters PIN digits by tapping standard Android keypad coordinates or filling password field.
     */
    fun enterPinGesture(pin: String, onComplete: (() -> Unit)? = null): Boolean {
        val cleanDigits = pin.filter { it.isDigit() }
        if (cleanDigits.isEmpty()) return false

        // First attempt: Try finding editable node in current active window
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val editText = findFirstEditableNode(rootNode)
            if (editText != null) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, cleanDigits)
                }
                val setOk = editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                if (setOk) {
                    Log.i(TAG, "PIN set via Accessibility ACTION_SET_TEXT successfully")
                    onComplete?.invoke()
                    return true
                }
            }
        }

        // Second attempt: Sequential keypad taps
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        val keypadWidth = width * 0.76f
        val left = (width - keypadWidth) / 2f
        val top = height * 0.50f
        val colStep = keypadWidth / 2f
        val rowStep = height * 0.09f

        fun getDigitCoord(digit: Char): Pair<Float, Float> {
            return when (digit) {
                '1' -> Pair(left, top)
                '2' -> Pair(left + colStep, top)
                '3' -> Pair(left + 2 * colStep, top)
                '4' -> Pair(left, top + rowStep)
                '5' -> Pair(left + colStep, top + rowStep)
                '6' -> Pair(left + 2 * colStep, top + rowStep)
                '7' -> Pair(left, top + 2 * rowStep)
                '8' -> Pair(left + colStep, top + 2 * rowStep)
                '9' -> Pair(left + 2 * colStep, top + 2 * rowStep)
                '0' -> Pair(left + colStep, top + 3 * rowStep)
                else -> Pair(left + colStep, top)
            }
        }

        fun tapNextDigit(index: Int) {
            if (index >= cleanDigits.length) {
                onComplete?.invoke()
                return
            }

            val digit = cleanDigits[index]
            val coord = getDigitCoord(digit)
            val path = Path().apply {
                moveTo(coord.first, coord.second)
            }

            val stroke = GestureDescription.StrokeDescription(path, 0, 50)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    mainHandler.postDelayed({ tapNextDigit(index + 1) }, 120)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    mainHandler.postDelayed({ tapNextDigit(index + 1) }, 120)
                }
            }, null)
        }

        tapNextDigit(0)
        return true
    }

    /**
     * Enters alphanumeric password into lock screen field.
     */
    fun enterPasswordText(password: String, onComplete: (() -> Unit)? = null): Boolean {
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val editText = findFirstEditableNode(rootNode)
            if (editText != null) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password)
                }
                val result = editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                if (result) {
                    Log.i(TAG, "Password set on lockscreen input field successfully")
                    onComplete?.invoke()
                    return true
                }
            }
        }

        onComplete?.invoke()
        return true
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableNode(child)
            if (found != null) return found
        }
        return null
    }
}
