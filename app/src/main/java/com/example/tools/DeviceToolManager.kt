package com.example.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.ToolExecutionResult
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceToolManager(private val context: Context) {

    companion object {
        private const val TAG = "DeviceToolManager"
    }

    /**
     * Launch any installed application by package name or common app name.
     */
    fun openApp(packageName: String?, appName: String?): ToolExecutionResult {
        val pm = context.packageManager
        var targetPackage = packageName?.trim()

        // If package name is empty or looks like a common name, resolve by app name
        if (targetPackage.isNullOrEmpty() || !targetPackage.contains(".")) {
            val query = (appName ?: targetPackage ?: "").lowercase(Locale.ROOT)
            targetPackage = resolvePackageName(query)
        }

        if (targetPackage.isNullOrEmpty()) {
            return ToolExecutionResult(
                success = false,
                message = "I couldn't identify the application '$appName', Boss. Please specify the exact app name."
            )
        }

        return try {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val friendlyName = appName ?: targetPackage.substringAfterLast(".")
                ToolExecutionResult(
                    success = true,
                    message = "Opened $friendlyName for you! Try not to spend the whole day in it.",
                    data = mapOf("package" to targetPackage)
                )
            } else {
                // Try market or fallback
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$targetPackage")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (webIntent.resolveActivity(pm) != null) {
                    context.startActivity(webIntent)
                    ToolExecutionResult(
                        success = true,
                        message = "Looks like $targetPackage isn't installed, so I pulled it up in the Play Store for you."
                    )
                } else {
                    ToolExecutionResult(
                        success = false,
                        message = "I checked, but $targetPackage doesn't seem to be installed on your phone."
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: $targetPackage", e)
            ToolExecutionResult(
                success = false,
                message = "Oops, couldn't open that app: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun resolvePackageName(appName: String): String? {
        val lower = appName.lowercase(Locale.ROOT).trim()
        val commonMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "instagram" to "com.instagram.android",
            "whatsapp" to "com.whatsapp",
            "spotify" to "com.spotify.music",
            "calculator" to "com.google.android.calculator",
            "calc" to "com.google.android.calculator",
            "camera" to "com.android.camera",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "settings" to "com.android.settings",
            "clock" to "com.google.android.deskclock",
            "alarm" to "com.google.android.deskclock",
            "gallery" to "com.google.android.apps.photos",
            "photos" to "com.google.android.apps.photos",
            "gmail" to "com.google.android.gm",
            "email" to "com.google.android.gm",
            "calendar" to "com.google.android.calendar",
            "telegram" to "org.telegram.messenger",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "tiktok" to "com.zhiliaoapp.musically",
            "reddit" to "com.reddit.frontpage",
            "netflix" to "com.netflix.mediaclient"
        )

        if (commonMap.containsKey(lower)) {
            return commonMap[lower]
        }

        // Search installed apps
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(mainIntent, 0)
        for (info in apps) {
            val label = info.loadLabel(pm).toString().lowercase(Locale.ROOT)
            if (label == lower || label.contains(lower) || lower.contains(label)) {
                return info.activityInfo.packageName
            }
        }
        return null
    }

    /**
     * Search Contacts Provider and place a phone call or dial.
     */
    fun searchAndCallContact(contactName: String): ToolExecutionResult {
        val trimmed = contactName.trim()
        if (trimmed.isEmpty()) {
            return ToolExecutionResult(
                success = false,
                message = "Please specify which contact name or number you would like to call, Boss."
            )
        }

        val hasReadContacts = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val hasCallPhone = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        var phoneNumber: String? = null
        var resolvedName = trimmed

        if (hasReadContacts) {
            phoneNumber = findPhoneNumber(trimmed)?.also { (name, number) ->
                resolvedName = name
            }?.second
        }

        // If phone number directly given in contactName string (e.g. "+123456789")
        if (phoneNumber == null && trimmed.any { it.isDigit() }) {
            phoneNumber = trimmed.filter { it.isDigit() || it == '+' }
        }

        if (phoneNumber.isNullOrEmpty()) {
            // If contact permission is missing or not found, offer dialer intent
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return ToolExecutionResult(
                success = true,
                message = "I couldn't find a direct phone number for '$trimmed', so I opened your dialer for you."
            )
        }

        return try {
            if (hasCallPhone) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                ToolExecutionResult(
                    success = true,
                    message = "Calling $resolvedName right now. Be charming!",
                    data = mapOf("contact" to resolvedName, "phone" to phoneNumber)
                )
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                ToolExecutionResult(
                    success = true,
                    message = "I pulled up $resolvedName's number ($phoneNumber) on the dial pad. Just hit call!",
                    data = mapOf("contact" to resolvedName, "phone" to phoneNumber)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating call to $trimmed", e)
            ToolExecutionResult(
                success = false,
                message = "Couldn't place the call: ${e.localizedMessage}"
            )
        }
    }

    private fun findPhoneNumber(query: String): Pair<String, String>? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)
                return Pair(name, number)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading contacts", e)
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * Send a WhatsApp message to a contact or phone number.
     */
    fun sendWhatsAppMessage(contactName: String, message: String): ToolExecutionResult {
        val trimmedContact = contactName.trim()
        val text = message.trim()

        if (text.isEmpty()) {
            return ToolExecutionResult(
                success = false,
                message = "What did you want me to say in the message? I need some words to work with!"
            )
        }

        var phoneNumber: String? = null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            phoneNumber = findPhoneNumber(trimmedContact)?.second?.filter { it.isDigit() || it == '+' }
        }

        if (phoneNumber == null && trimmedContact.any { it.isDigit() }) {
            phoneNumber = trimmedContact.filter { it.isDigit() || it == '+' }
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (!phoneNumber.isNullOrEmpty()) {
                    val cleanPhone = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(text, "UTF-8")}")
                } else {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        `package` = "com.whatsapp"
                        putExtra(Intent.EXTRA_TEXT, text)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (sendIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(sendIntent)
                        return ToolExecutionResult(
                            success = true,
                            message = "Opened WhatsApp with your message for '$trimmedContact'. Hit send when ready!"
                        )
                    } else {
                        data = Uri.parse("https://api.whatsapp.com/send?text=${URLEncoder.encode(text, "UTF-8")}")
                    }
                }
            }

            intent.setPackage("com.whatsapp")
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // If package not found, open in browser or chooser
                intent.setPackage(null)
                context.startActivity(intent)
            }

            ToolExecutionResult(
                success = true,
                message = "Drafted your WhatsApp message for $trimmedContact: \"$text\". Go ahead and hit send!",
                data = mapOf("contact" to trimmedContact, "message" to text)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WhatsApp message", e)
            ToolExecutionResult(
                success = false,
                message = "Couldn't launch WhatsApp: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Compose or dispatch email via Gmail or standard system email clients.
     */
    fun sendGmail(recipientEmail: String, subject: String, body: String): ToolExecutionResult {
        val trimmedRecipient = recipientEmail.trim()
        val emailSubject = subject.trim().ifEmpty { "Message from MM Assistant" }
        val emailBody = body.trim()

        return try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                if (trimmedRecipient.contains("@")) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(trimmedRecipient))
                }
                putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                putExtra(Intent.EXTRA_TEXT, emailBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Prefer official Gmail package if available
            val gmailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                `package` = "com.google.android.gm"
                if (trimmedRecipient.contains("@")) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(trimmedRecipient))
                }
                putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                putExtra(Intent.EXTRA_TEXT, emailBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (gmailIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(gmailIntent)
            } else if (emailIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(emailIntent)
            } else {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(trimmedRecipient))
                    putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                    putExtra(Intent.EXTRA_TEXT, emailBody)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }

            ToolExecutionResult(
                success = true,
                message = "Drafted email to $trimmedRecipient with subject '$emailSubject'. Hit send whenever you're ready!",
                data = mapOf("recipient" to trimmedRecipient, "subject" to emailSubject)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error launching email intent", e)
            ToolExecutionResult(
                success = false,
                message = "Couldn't launch email client: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Set a timer or alarm on the device clock.
     */
    fun setAlarmOrTimer(minutes: Int, label: String?): ToolExecutionResult {
        val safeMinutes = if (minutes <= 0) 5 else minutes
        val timerLabel = label?.trim() ?: "MM Reminder"

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, safeMinutes * 60)
                putExtra(AlarmClock.EXTRA_MESSAGE, timerLabel)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolExecutionResult(
                    success = true,
                    message = "Set a timer for $safeMinutes minutes: '$timerLabel'. Don't forget it!"
                )
            } else {
                ToolExecutionResult(
                    success = false,
                    message = "Your device clock app couldn't be reached to set that timer."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting timer", e)
            ToolExecutionResult(
                success = false,
                message = "Failed to set timer: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Toggle device camera flashlight torch.
     */
    fun toggleFlashlight(enable: Boolean): ToolExecutionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager == null) {
                return ToolExecutionResult(false, "Flashlight hardware is unavailable.")
            }
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return ToolExecutionResult(false, "No flashlight found.")
            cameraManager.setTorchMode(cameraId, enable)
            val status = if (enable) "turned on" else "turned off"
            ToolExecutionResult(
                success = true,
                message = "Flashlight $status! ${if (enable) "Let there be light ✨" else "Back into the shadows."}"
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access error toggling flashlight", e)
            ToolExecutionResult(false, "Flashlight error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling flashlight", e)
            ToolExecutionResult(false, "Couldn't toggle flashlight: ${e.localizedMessage}")
        }
    }

    /**
     * Search and play music on YouTube or default player.
     */
    fun playMusic(query: String): ToolExecutionResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return ToolExecutionResult(false, "What song or artist do you want me to play?")
        }
        return try {
            val ytIntent = Intent(Intent.ACTION_SEARCH).apply {
                `package` = "com.google.android.youtube"
                putExtra("query", trimmed)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (ytIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(ytIntent)
                ToolExecutionResult(
                    success = true,
                    message = "Playing '$trimmed' on YouTube. Enjoy the tunes!"
                )
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${URLEncoder.encode(trimmed, "UTF-8")}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                ToolExecutionResult(
                    success = true,
                    message = "Searched YouTube for '$trimmed'!"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing music", e)
            ToolExecutionResult(false, "Couldn't play music: ${e.localizedMessage}")
        }
    }

    /**
     * Query device status (Battery %, WiFi, Time).
     */
    fun getDeviceStatus(): ToolExecutionResult {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val status = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } else false

        val timeFormatter = SimpleDateFormat("h:mm a, EEEE, MMM d", Locale.getDefault())
        val formattedTime = timeFormatter.format(Date())

        val batteryComment = when {
            batteryLevel in 0..20 && !isCharging -> "and your battery is sitting at $batteryLevel%—you might want to find a charger before I shut down on you!"
            batteryLevel > 80 -> "and your battery is juiced up at $batteryLevel%."
            isCharging -> "and you're actively charging at $batteryLevel%."
            else -> "with $batteryLevel% battery remaining."
        }

        return ToolExecutionResult(
            success = true,
            message = "It's $formattedTime $batteryComment All systems running smoothly!",
            data = mapOf(
                "batteryLevel" to batteryLevel,
                "isCharging" to isCharging,
                "time" to formattedTime
            )
        )
    }

    /**
     * Intelligently adjust device audio volume (media, notifications, ringtones)
     * based on level, stream type, time of day, or user context.
     */
    fun adjustDeviceVolume(
        streamType: String? = "media",
        levelPercent: Int? = null,
        contextMode: String? = null
    ): ToolExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ToolExecutionResult(false, "Audio system is unavailable on this device.")

        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY) // 0 - 23

        val stream = (streamType ?: "media").lowercase(Locale.ROOT).trim()
        val mode = contextMode?.lowercase(Locale.ROOT)?.trim()

        // Derive target percent based on context, time of day, or explicit level
        val (targetMediaPercent, targetNotificationPercent, contextExplanation) = when {
            mode?.contains("night") == true || mode?.contains("sleep") == true || mode?.contains("bed") == true -> {
                Triple(15, 10, "Night/Sleep mode active 🌙")
            }
            mode?.contains("meeting") == true || mode?.contains("work") == true || mode?.contains("office") == true || mode?.contains("quiet") == true || mode?.contains("library") == true -> {
                Triple(0, 0, "Quiet Work/Meeting mode active 🤫")
            }
            mode?.contains("gym") == true || mode?.contains("workout") == true || mode?.contains("running") == true || mode?.contains("party") == true || mode?.contains("loud") == true -> {
                Triple(95, 80, "High Energy Gym/Party mode active 💥")
            }
            mode?.contains("car") == true || mode?.contains("drive") == true || mode?.contains("driving") == true -> {
                Triple(85, 75, "Driving mode active 🚗")
            }
            // Auto time-of-day inference if no explicit level was specified and context requested
            levelPercent == null && (mode?.contains("auto") == true || mode?.contains("intelligent") == true || mode?.contains("context") == true || mode?.contains("time") == true) -> {
                when (currentHour) {
                    in 23..24, in 0..6 -> Triple(15, 10, "Late night auto-quiet mode 🌙")
                    in 7..9 -> Triple(45, 40, "Morning commute mode ☕")
                    in 10..17 -> Triple(55, 50, "Daytime productive mode 💼")
                    in 18..21 -> Triple(70, 65, "Evening chill mode 🌆")
                    else -> Triple(30, 20, "Night winding-down mode 🌌")
                }
            }
            levelPercent != null -> {
                val clamped = levelPercent.coerceIn(0, 100)
                Triple(clamped, clamped, "Set to $clamped%")
            }
            else -> {
                // Default fallback
                Triple(50, 50, "Balanced 50% sound")
            }
        }

        fun applyStreamVolume(streamFlag: Int, percent: Int): Int {
            val maxVol = audioManager.getStreamMaxVolume(streamFlag)
            val minVol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                audioManager.getStreamMinVolume(streamFlag)
            } else 0
            val targetLevel = (minVol + (maxVol - minVol) * (percent / 100f)).toInt().coerceIn(minVol, maxVol)
            try {
                audioManager.setStreamVolume(streamFlag, targetLevel, AudioManager.FLAG_SHOW_UI)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set volume for stream $streamFlag", e)
            }
            return targetLevel
        }

        val appliedDetails = mutableMapOf<String, Any>()

        val resultMsg = when (stream) {
            "notification", "notifications" -> {
                val lvl = applyStreamVolume(AudioManager.STREAM_NOTIFICATION, targetNotificationPercent)
                appliedDetails["notificationPercent"] = targetNotificationPercent
                appliedDetails["notificationLevel"] = lvl
                "Turned notification volume to $targetNotificationPercent% ($contextExplanation)."
            }
            "ring", "ringer", "call" -> {
                val lvl = applyStreamVolume(AudioManager.STREAM_RING, targetNotificationPercent)
                appliedDetails["ringPercent"] = targetNotificationPercent
                appliedDetails["ringLevel"] = lvl
                "Set ringtone volume to $targetNotificationPercent% ($contextExplanation)."
            }
            "alarm" -> {
                val lvl = applyStreamVolume(AudioManager.STREAM_ALARM, targetMediaPercent)
                appliedDetails["alarmPercent"] = targetMediaPercent
                "Alarm volume tuned to $targetMediaPercent% ($contextExplanation)."
            }
            "all", "system", "everything" -> {
                applyStreamVolume(AudioManager.STREAM_MUSIC, targetMediaPercent)
                applyStreamVolume(AudioManager.STREAM_NOTIFICATION, targetNotificationPercent)
                applyStreamVolume(AudioManager.STREAM_RING, targetNotificationPercent)
                appliedDetails["mediaPercent"] = targetMediaPercent
                appliedDetails["notificationPercent"] = targetNotificationPercent
                "Adjusted all audio channels ($contextExplanation): Media at $targetMediaPercent%, Notifications at $targetNotificationPercent%."
            }
            else -> { // Default: media
                val lvl = applyStreamVolume(AudioManager.STREAM_MUSIC, targetMediaPercent)
                appliedDetails["mediaPercent"] = targetMediaPercent
                appliedDetails["mediaLevel"] = lvl
                "Set media volume to $targetMediaPercent% ($contextExplanation)."
            }
        }

        return ToolExecutionResult(
            success = true,
            message = "$resultMsg Sassy sound check complete!",
            data = appliedDetails
        )
    }

    /**
     * Send message via Telegram app or intent.
     */
    fun sendTelegramMessage(contactOrChat: String?, message: String): ToolExecutionResult {
        val trimmedMsg = message.trim().ifEmpty { "Hey from MM Assistant!" }
        return try {
            val telegramIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = "org.telegram.messenger"
                putExtra(Intent.EXTRA_TEXT, trimmedMsg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (telegramIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(telegramIntent)
                ToolExecutionResult(
                    success = true,
                    message = "Opened Telegram with message: '$trimmedMsg'."
                )
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/share/url?url=${URLEncoder.encode(trimmedMsg, "UTF-8")}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                ToolExecutionResult(
                    success = true,
                    message = "Opened Telegram web share link with message."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Telegram", e)
            ToolExecutionResult(false, "Couldn't send Telegram message: ${e.localizedMessage}")
        }
    }

    /**
     * Control media playback (Play, Pause, Skip, Previous).
     */
    fun controlMediaPlayback(action: String): ToolExecutionResult {
        val cleanAction = action.lowercase(Locale.ROOT).trim()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ToolExecutionResult(false, "Media service unavailable.")

        val keyEventCode = when {
            cleanAction.contains("next") || cleanAction.contains("skip") -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
            cleanAction.contains("prev") || cleanAction.contains("back") -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
            cleanAction.contains("stop") -> android.view.KeyEvent.KEYCODE_MEDIA_STOP
            cleanAction.contains("play") && !cleanAction.contains("pause") -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY
            cleanAction.contains("pause") -> android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
            else -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        return try {
            val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyEventCode))
            }
            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyEventCode))
            }
            context.sendOrderedBroadcast(downIntent, null)
            context.sendOrderedBroadcast(upIntent, null)

            ToolExecutionResult(
                success = true,
                message = "Applied media action: '$cleanAction'. Groove on!"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error controlling media", e)
            ToolExecutionResult(false, "Couldn't execute media command: ${e.localizedMessage}")
        }
    }

    /**
     * Launch Wi-Fi settings or connectivity panel.
     */
    fun toggleWifi(): ToolExecutionResult {
        return try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(android.provider.Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Opened Wi-Fi settings panel for you!")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Could not open Wi-Fi panel: ${e.localizedMessage}")
        }
    }

    /**
     * Launch Bluetooth settings panel.
     */
    fun toggleBluetooth(): ToolExecutionResult {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Opened Bluetooth settings!")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Could not open Bluetooth settings: ${e.localizedMessage}")
        }
    }

    /**
     * Launch Display / Brightness settings panel.
     */
    fun adjustBrightness(): ToolExecutionResult {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Opened Display & Brightness settings for quick adjustment!")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Could not open Display settings: ${e.localizedMessage}")
        }
    }

    /**
     * Enter low-power standby mode ("Bye MM").
     * Keeps the background AudioRecord mic active for "Hello MM" re-awakening.
     */
    fun enterStandbyMode(): ToolExecutionResult {
        return ToolExecutionResult(
            success = true,
            message = "Entering low-power standby mode, Boss. I'll stay listening silently in the background—just call 'Hello MM' whenever you need me! 💤"
        )
    }

    /**
     * Control Remote PC Companion over local network.
     */
    suspend fun controlRemotePc(
        action: String,
        targetApp: String? = null,
        textToType: String? = null,
        customCommand: String? = null
    ): ToolExecutionResult {
        val pcManager = com.example.pc.RemotePcManager.getInstance(context)
        val cleanAction = action.lowercase(Locale.ROOT).trim()

        return when {
            cleanAction.contains("lock") -> pcManager.lockPc()
            cleanAction.contains("shutdown") -> pcManager.shutdownPc()
            cleanAction.contains("restart") -> pcManager.restartPc()
            cleanAction.contains("sleep") -> pcManager.sleepPc()
            cleanAction.contains("open") || !targetApp.isNullOrBlank() -> {
                val app = targetApp ?: cleanAction.substringAfter("open ").trim()
                pcManager.openApp(app)
            }
            cleanAction.contains("media") || cleanAction.contains("play") || cleanAction.contains("pause") -> {
                pcManager.controlMedia(cleanAction)
            }
            cleanAction.contains("type") || !textToType.isNullOrBlank() -> {
                val text = textToType ?: cleanAction.substringAfter("type ").trim()
                pcManager.typeText(text)
            }
            cleanAction.contains("screenshot") -> pcManager.takeScreenshot()
            cleanAction.contains("shell") || !customCommand.isNullOrBlank() -> {
                val cmd = customCommand ?: cleanAction.substringAfter("shell ").trim()
                pcManager.executeCustom(cmd)
            }
            else -> {
                pcManager.sendCommand(cleanAction)
            }
        }
    }

    /**
     * App Lock & Stealth Vault Tools
     */
    fun lockApp(appName: String, pin: String? = null): ToolExecutionResult {
        val lockManager = com.example.security.AppLockManager.getInstance(context)
        return lockManager.lockApp(appName, pin)
    }

    fun unlockApp(appName: String, pin: String? = null): ToolExecutionResult {
        val lockManager = com.example.security.AppLockManager.getInstance(context)
        return lockManager.unlockApp(appName, pin)
    }

    fun hideApp(appName: String): ToolExecutionResult {
        val lockManager = com.example.security.AppLockManager.getInstance(context)
        return lockManager.hideApp(appName)
    }

    fun unhideApp(appName: String): ToolExecutionResult {
        val lockManager = com.example.security.AppLockManager.getInstance(context)
        return lockManager.unhideApp(appName)
    }

    fun listSecuredApps(): ToolExecutionResult {
        val lockManager = com.example.security.AppLockManager.getInstance(context)
        return lockManager.listSecuredApps()
    }

    /**
     * Phone Device Lock & Unlock Automation Tools (PIN, Pattern, Password, Swipe)
     */
    fun unlockPhone(overrideCredential: String? = null): ToolExecutionResult {
        val manager = com.example.security.DeviceLockUnlockManager.getInstance(context)
        return manager.unlockPhone(overrideCredential)
    }

    fun lockPhone(): ToolExecutionResult {
        val manager = com.example.security.DeviceLockUnlockManager.getInstance(context)
        return manager.lockPhone()
    }

    fun saveDevicePassword(type: String, credential: String): ToolExecutionResult {
        val manager = com.example.security.DeviceLockUnlockManager.getInstance(context)
        val lockType = when (type.lowercase(java.util.Locale.ROOT)) {
            "pattern" -> com.example.security.DeviceLockType.PATTERN
            "password" -> com.example.security.DeviceLockType.PASSWORD
            "swipe", "none" -> com.example.security.DeviceLockType.SWIPE
            else -> com.example.security.DeviceLockType.PIN
        }
        return manager.saveCredentials(lockType, credential)
    }

    fun clearDevicePassword(): ToolExecutionResult {
        val manager = com.example.security.DeviceLockUnlockManager.getInstance(context)
        return manager.clearCredentials()
    }
}
