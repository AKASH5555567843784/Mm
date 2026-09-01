package com.example.model

/**
 * Dynamic polling and sensitivity modes for wake-word audio sampling
 * based on WorkManager battery level and power state monitoring.
 */
enum class BatteryOptimizationMode(
    val displayName: String,
    val pollingDelayMs: Long,
    val sensitivity: Float
) {
    HIGH_PERFORMANCE(
        displayName = "High Performance (Real-time)",
        pollingDelayMs = 0L,
        sensitivity = 0.65f
    ),
    BALANCED_SAVER(
        displayName = "Balanced Saver (25ms Duty Cycle)",
        pollingDelayMs = 25L,
        sensitivity = 0.60f
    ),
    ULTRA_BATTERY_SAVER(
        displayName = "Ultra Battery Saver (60ms Duty Cycle)",
        pollingDelayMs = 60L,
        sensitivity = 0.55f
    )
}
