package com.example.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BatteryWorkManagerScheduler {

    private const val TAG = "BatteryWorkScheduler"

    fun schedulePeriodicBatteryMonitoring(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<BatteryWakeWordWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                BatteryWakeWordWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.d(TAG, "Periodic BatteryWakeWordWorker scheduled (15 min interval)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling periodic battery worker", e)
        }
    }

    fun triggerImmediateCheck(context: Context) {
        try {
            val oneTimeRequest = OneTimeWorkRequestBuilder<BatteryWakeWordWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${BatteryWakeWordWorker.WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Triggered immediate battery check via WorkManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed triggering immediate battery check", e)
        }
    }
}
