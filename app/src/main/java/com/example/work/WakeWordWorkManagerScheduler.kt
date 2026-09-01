package com.example.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * WakeWordWorkManagerScheduler:
 * Central manager to schedule, trigger, and cancel WorkManager tasks
 * for hands-free wake-word detection and assistant auto-activation.
 */
object WakeWordWorkManagerScheduler {

    private const val TAG = "WakeWordWorkScheduler"

    /**
     * Schedules periodic background monitoring to keep wake-word detection resilient
     * and recover if system kills background processes.
     */
    fun schedulePeriodicWakeWordMonitoring(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<WakeWordBackgroundWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // Flex window
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WakeWordBackgroundWorker.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
            Log.d(TAG, "Periodic WakeWordBackgroundWorker scheduled successfully (15m interval)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling periodic wake-word worker", e)
        }
    }

    /**
     * Immediately triggers background wake-word activation and assistant awakening without manual input.
     */
    fun triggerImmediateWakeWordActivation(context: Context, activateAssistant: Boolean = false) {
        try {
            val inputData = Data.Builder()
                .putBoolean(WakeWordBackgroundWorker.KEY_TRIGGER_WAKE, activateAssistant)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<WakeWordBackgroundWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WakeWordBackgroundWorker.ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Triggered immediate wake-word work request (activateAssistant=$activateAssistant)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed triggering immediate wake-word work request", e)
        }
    }

    /**
     * Cancels all scheduled background wake-word work tasks.
     */
    fun cancelWakeWordWork(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WakeWordBackgroundWorker.PERIODIC_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(WakeWordBackgroundWorker.ONE_TIME_WORK_NAME)
            Log.d(TAG, "Cancelled all wake-word WorkManager tasks")
        } catch (e: Exception) {
            Log.e(TAG, "Failed cancelling wake-word WorkManager tasks", e)
        }
    }
}
