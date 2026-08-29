package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.telephony.CallAnnouncerManager

class IncomingCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "IncomingCallReceiver"
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Log.d(TAG, "Phone state changed: $state, number: $incomingNumber")

            if (state == TelephonyManager.EXTRA_STATE_RINGING && lastState != TelephonyManager.EXTRA_STATE_RINGING) {
                // Incoming call is ringing!
                val callAnnouncer = CallAnnouncerManager.getInstance(context)
                callAnnouncer.onIncomingCall(incomingNumber)
            } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK || state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call answered or finished, stop announcement
                val callAnnouncer = CallAnnouncerManager.getInstance(context)
                callAnnouncer.stopSpeaking()
            }

            lastState = state
        }
    }
}
