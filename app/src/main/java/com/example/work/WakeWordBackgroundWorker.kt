package com.example.work

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.receiver.MMAssistantWakeWordReceiver
import com.example.service.MMAssistantForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WakeWordBackgroundWorker:
 * Robust, battery-efficient background worker powered by Android's WorkManager
 * to maintain resilient wake-word listening without manual user intervention.
 *
 * Responsibilities:
 * 1. Verifies microphone runtime permission.
 * 2. Checks privacy mode and active state.
 * 3. Restores and maintains active wake-word detector state in background.
 * 4. Dispatches immediate activation broadcast if wake-word trigger is signaled.
 */
class WakeWordBackgroundWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Executing WakeWordBackgroundWorker background verification...")

        try {
            // 1. Verify Microphone Permission
            val hasAudioPermission = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasAudioPermission) {
                Log.w(TAG, "Microphone permission is not granted. Cannot run wake-word listening.")
                return@withContext Result.failure()
            }

            // 2. Check if Privacy Mode is active in Foreground Service
            val isPrivacyActive = MMAssistantForegroundService.isPrivacyMode.value
            if (isPrivacyActive) {
                Log.d(TAG, "Privacy mode is active. Background wake-word listening is suspended.")
                return@withContext Result.success()
            }

            // 3. Ensure foreground listening service is running
            val isServiceActive = MMAssistantForegroundService.isRunning.value
            if (!isServiceActive) {
                Log.i(TAG, "Foreground service inactive. Starting service to restore wake-word detection...")
                MMAssistantForegroundService.startService(appContext)
            } else {
                // Ensure wake-word detection component inside the active service is listening
                val activeService = MMAssistantForegroundService.activeServiceInstance
                if (activeService != null && !MMAssistantForegroundService.isWakeWordListening.value) {
                    Log.i(TAG, "Active service found with paused wake-word. Resuming listening...")
                    activeService.togglePrivacyMode(false)
                }
            }

            // 4. Handle any trigger activation input parameter passed by work request
            val shouldTriggerWake = inputData.getBoolean(KEY_TRIGGER_WAKE, false)
            if (shouldTriggerWake) {
                Log.i(TAG, "Triggering instant assistant wake-up from WorkManager input data.")
                val wakeIntent = Intent(appContext, MMAssistantWakeWordReceiver::class.java).apply {
                    action = MMAssistantWakeWordReceiver.ACTION_WAKE_WORD_DETECTED
                    putExtra(MMAssistantWakeWordReceiver.EXTRA_TRIGGER_SOURCE, "WorkManager_Background_Worker")
                }
                appContext.sendBroadcast(wakeIntent)
            }

            Log.d(TAG, "WakeWordBackgroundWorker completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during WakeWordBackgroundWorker execution", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "WakeWordBgWorker"
        const val PERIODIC_WORK_NAME = "mm_wake_word_background_monitor"
        const val ONE_TIME_WORK_NAME = "mm_wake_word_immediate_activation"
        const val KEY_TRIGGER_WAKE = "key_trigger_wake_activation"
    }
}
