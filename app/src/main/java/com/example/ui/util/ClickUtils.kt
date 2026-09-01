package com.example.ui.util

import android.os.SystemClock
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

/**
 * Global Debouncer to prevent rapid duplicate tap events across the UI.
 */
object ClickDebouncer {
    private var lastClickTime = 0L
    private const val DEFAULT_DEBOUNCE_MS = 400L

    /**
     * Executes the given action only if [debounceMs] has elapsed since the last accepted click.
     * Returns true if the action was allowed, false if debounced/suppressed.
     */
    @Synchronized
    fun canClick(debounceMs: Long = DEFAULT_DEBOUNCE_MS): Boolean {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime >= debounceMs) {
            lastClickTime = currentTime
            return true
        }
        return false
    }

    /**
     * Wraps a lambda with global debouncing.
     */
    fun debounce(debounceMs: Long = DEFAULT_DEBOUNCE_MS, action: () -> Unit): () -> Unit {
        return {
            if (canClick(debounceMs)) {
                action()
            }
        }
    }
}

/**
 * Custom Modifier extension that guarantees a single click per touch and suppresses duplicate taps.
 */
fun Modifier.singleClickable(
    debounceMs: Long = 400L,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickedTime by remember { mutableLongStateOf(0L) }

    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role
    ) {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickedTime >= debounceMs && ClickDebouncer.canClick(debounceMs)) {
            lastClickedTime = currentTime
            onClick()
        }
    }
}

/**
 * Custom Modifier extension with custom interaction source & indication for single/debounced clicks.
 */
fun Modifier.singleClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    debounceMs: Long = 400L,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickedTime by remember { mutableLongStateOf(0L) }

    this.clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role
    ) {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickedTime >= debounceMs && ClickDebouncer.canClick(debounceMs)) {
            lastClickedTime = currentTime
            onClick()
        }
    }
}
