package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.telephony.CallAnnouncerManager
import java.util.concurrent.Executor

/**
 * CallAnnouncer Service:
 * Hooks directly into Android's TelephonyManager to detect incoming calls in real-time
 * and uses Text-To-Speech (TTS) to announce the caller specifically when the device is
 * in Silent or Vibrate mode.
 */
class CallAnnouncerService : Service() {

    companion object {
        private const val TAG = "CallAnnouncerService"
        const val CHANNEL_ID = "mm_call_announcer_channel"
        const val NOTIFICATION_ID = 1002

        const val ACTION_START = "com.example.mm.action.START_CALL_ANNOUNCER"
        const val ACTION_STOP = "com.example.mm.action.STOP_CALL_ANNOUNCER"

        fun startService(context: Context) {
            try {
                val intent = Intent(context, CallAnnouncerService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start CallAnnouncerService", e)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, CallAnnouncerService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop CallAnnouncerService", e)
            }
        }
    }

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var callAnnouncerManager: CallAnnouncerManager

    // Modern Android S (API 31+) Telephony Callback
    private var telephonyCallback: TelephonyCallback? = null

    // Legacy PhoneStateListener for API < 31
    private var legacyPhoneStateListener: PhoneStateListener? = null

    private var lastCallState = TelephonyManager.CALL_STATE_IDLE

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        callAnnouncerManager = CallAnnouncerManager.getInstance(applicationContext)

        createNotificationChannel()
        registerTelephonyListener()
        Log.i(TAG, "CallAnnouncerService created and TelephonyManager listener registered.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForegroundServiceInternal()
            return START_NOT_STICKY
        }

        try {
            startForeground(NOTIFICATION_ID, buildForegroundNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground in CallAnnouncerService", e)
        }
        return START_STICKY
    }

    private fun registerTelephonyListener() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallStateChange(state, null)
                    }
                }
                telephonyCallback = callback
                val mainExecutor: Executor = mainExecutor
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
                Log.d(TAG, "Registered modern TelephonyCallback.")
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallStateChange(state, phoneNumber)
                    }
                }
                legacyPhoneStateListener = listener
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                Log.d(TAG, "Registered legacy PhoneStateListener.")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing READ_PHONE_STATE permission to attach telephony listener directly: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering telephony listener", e)
        }
    }

    private fun unregisterTelephonyListener() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let {
                    telephonyManager.unregisterTelephonyCallback(it)
                }
                telephonyCallback = null
            } else {
                legacyPhoneStateListener?.let {
                    @Suppress("DEPRECATION")
                    telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                }
                legacyPhoneStateListener = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering telephony listener", e)
        }
    }

    /**
     * Inspects call state changes and announces caller if device is in Silent/Vibrate mode.
     */
    fun handleCallStateChange(state: Int, incomingNumber: String?) {
        Log.d(TAG, "TelephonyManager Call State: $state (Previous: $lastCallState)")

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                if (lastCallState != TelephonyManager.CALL_STATE_RINGING) {
                    callAnnouncerManager.onIncomingCall(incomingNumber)
                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK,
            TelephonyManager.CALL_STATE_IDLE -> {
                callAnnouncerManager.stopSpeaking()
            }
        }

        lastCallState = state
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_listening)
            .setContentTitle("MM Call Announcer Active")
            .setContentText("Monitoring incoming calls (Audible when Silent/Vibrate is active)")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MM Call Announcer",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Monitors incoming phone calls and announces callers when phone is silent"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundServiceInternal() {
        unregisterTelephonyListener()
        callAnnouncerManager.stopSpeaking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        unregisterTelephonyListener()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
