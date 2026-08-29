package com.example.audio

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

class WakeWordDetector(
    private var sensitivity: Float = 0.65f,
    private val onWakeWordDetected: () -> Unit
) {
    companion object {
        private const val TAG = "WakeWordDetector"
        private const val FRAME_SIZE = 320 // 20ms at 16kHz
    }

    private var recentEnergyHistory = FloatArray(30) // ~600ms history
    private var historyIndex = 0
    private var lastTriggerTimestamp = 0L
    private val cooldownMs = 2000L // Prevent double trigger within 2s

    fun setSensitivity(value: Float) {
        sensitivity = value.coerceIn(0.1f, 1.0f)
    }

    /**
     * Process raw 16-bit 16kHz PCM audio buffer chunk.
     */
    fun processAudioChunk(buffer: ByteArray, bytesRead: Int) {
        if (bytesRead < FRAME_SIZE * 2) return

        var frameSum = 0.0
        val sampleCount = bytesRead / 2

        var zeroCrossings = 0
        var prevSign = false

        for (i in 0 until bytesRead - 1 step 2) {
            val sample = ((buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)).toShort()
            val normalized = sample.toFloat() / 32768f
            frameSum += normalized * normalized

            val currentSign = sample >= 0
            if (i > 0 && currentSign != prevSign) {
                zeroCrossings++
            }
            prevSign = currentSign
        }

        val rms = sqrt(frameSum / sampleCount).toFloat()
        recentEnergyHistory[historyIndex] = rms
        historyIndex = (historyIndex + 1) % recentEnergyHistory.size

        // Zero-crossing rate: "MM" has low-to-mid frequency nasal characteristics (lower ZCR compared to sibilants 's', 'sh')
        val zcr = zeroCrossings.toFloat() / sampleCount

        // Check if recent history has the "M - M" dual acoustic pulse pattern
        val now = System.currentTimeMillis()
        if (now - lastTriggerTimestamp > cooldownMs) {
            if (evaluateWakePattern(zcr)) {
                lastTriggerTimestamp = now
                Log.d(TAG, "Wake-word 'MM' detected! Triggering assistant.")
                onWakeWordDetected()
            }
        }
    }

    private fun evaluateWakePattern(zcr: Float): Boolean {
        // Base energy threshold adjusted by sensitivity
        val energyThreshold = (1.0f - sensitivity * 0.7f) * 0.08f

        // Count frames above threshold in recent history
        var activeFrames = 0
        var peak1 = 0f
        var peak2 = 0f

        val half = recentEnergyHistory.size / 2
        for (i in 0 until half) {
            val idx = (historyIndex + i) % recentEnergyHistory.size
            if (recentEnergyHistory[idx] > peak1) peak1 = recentEnergyHistory[idx]
        }
        for (i in half until recentEnergyHistory.size) {
            val idx = (historyIndex + i) % recentEnergyHistory.size
            if (recentEnergyHistory[idx] > peak2) peak2 = recentEnergyHistory[idx]
        }

        // Distinct dual-burst ("M" - "M") presence and energy exceeding threshold
        val bothPeaksSufficient = peak1 > energyThreshold && peak2 > energyThreshold
        val notPureNoise = zcr in 0.03f..0.45f

        return bothPeaksSufficient && notPureNoise
    }

    fun reset() {
        recentEnergyHistory.fill(0f)
        historyIndex = 0
    }
}
