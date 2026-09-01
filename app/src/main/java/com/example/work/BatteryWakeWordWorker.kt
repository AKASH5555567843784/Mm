package com.example.work

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.MMSettingsDataStore
import com.example.model.BatteryOptimizationMode
import com.example.service.MMAssistantForegroundService
import kotlinx.coroutines.flow.first

class BatteryWakeWordWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Executing BatteryWakeWordWorker check...")

        try {
            val settings = MMSettingsDataStore.getInstance(appContext)
            val isAdaptiveEnabled = settings.isBatteryAdaptiveWakeWordEnabled.first()

            if (!isAdaptiveEnabled) {
                Log.d(TAG, "Battery adaptive wake-word is disabled in settings. Skipping adjustment.")
                return Result.success()
            }

            // 1. Read battery status
            val batteryStatusIntent: Intent? = appContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            val level = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
            val scale = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val batteryPct = if (scale > 0 && level >= 0) (level * 100) / scale else 50

            val status = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isPowerSaveMode = powerManager?.isPowerSaveMode ?: false

            Log.d(TAG, "Battery: $batteryPct%, Charging: $isCharging, PowerSave: $isPowerSaveMode")

            // 2. Determine Wake-Word Optimization Mode
            val optimizationMode = when {
                isCharging -> BatteryOptimizationMode.HIGH_PERFORMANCE
                isPowerSaveMode || batteryPct < 20 -> BatteryOptimizationMode.ULTRA_BATTERY_SAVER
                batteryPct < 45 -> BatteryOptimizationMode.BALANCED_SAVER
                else -> BatteryOptimizationMode.HIGH_PERFORMANCE
            }

            Log.d(TAG, "Applying Wake-Word Mode: ${optimizationMode.name} (Battery: $batteryPct%)")

            // 3. Apply dynamically to foreground service
            MMAssistantForegroundService.applyBatteryOptimization(appContext, optimizationMode, batteryPct, isCharging)

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in BatteryWakeWordWorker", e)
            return Result.retry()
        }
    }

    companion object {
        const val TAG = "BatteryWakeWordWorker"
        const val WORK_NAME = "mm_battery_wakeword_monitor"
    }
}
