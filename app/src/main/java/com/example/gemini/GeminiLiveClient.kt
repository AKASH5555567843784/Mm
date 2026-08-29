package com.example.gemini

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.AssistantState
import com.example.model.LiveTranscript
import com.example.model.ToolCallInfo
import com.example.model.ToolExecutionResult
import com.example.tools.DeviceToolManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(
    private val deviceToolManager: DeviceToolManager,
    private val onAudioOutputChunk: (ByteArray) -> Unit,
    private val onInterrupted: () -> Unit
) {
    companion object {
        private const val TAG = "GeminiLiveClient"
        private const val LIVE_WS_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
        private const val MODEL_NAME = "models/gemini-2.0-flash-exp"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var isConnecting = false

    private val _assistantState = MutableStateFlow(AssistantState.DISCONNECTED)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _transcripts = MutableStateFlow<List<LiveTranscript>>(emptyList())
    val transcripts: StateFlow<List<LiveTranscript>> = _transcripts.asStateFlow()

    private val _activeToolCall = MutableStateFlow<ToolCallInfo?>(null)
    val activeToolCall: StateFlow<ToolCallInfo?> = _activeToolCall.asStateFlow()

    private val _sassyOneLiner = MutableStateFlow(GeminiInstructionManager.SASSY_GREETINGS.first())
    val sassyOneLiner: StateFlow<String> = _sassyOneLiner.asStateFlow()

    private val systemPrompt = GeminiInstructionManager.buildSystemInstruction()

    fun connect() {
        if (isConnected || isConnecting) return
        isConnecting = true
        _assistantState.value = AssistantState.CONNECTING

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured in secrets.")
            _assistantState.value = AssistantState.ERROR
            isConnecting = false
            addTranscript(LiveTranscript.Sender.SYSTEM, "Please add your Gemini API key in AI Studio Secrets to connect live voice.")
            return
        }

        val url = "$LIVE_WS_URL?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Gemini Live WebSocket opened!")
                isConnected = true
                isConnecting = false
                _assistantState.value = AssistantState.LISTENING
                sendSetupMessage(webSocket)
                _sassyOneLiner.value = "I'm listening, sugar. What can MM do for you?"
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleServerMessage(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Gemini Live WebSocket closing: $code / $reason")
                isConnected = false
                _assistantState.value = AssistantState.DISCONNECTED
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Gemini Live WebSocket closed: $code / $reason")
                isConnected = false
                isConnecting = false
                _assistantState.value = AssistantState.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Gemini Live WebSocket failed", t)
                isConnected = false
                isConnecting = false
                _assistantState.value = AssistantState.ERROR
                _sassyOneLiner.value = "Network hiccup! Tap reconnect and let's get back to it."
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val setupJson = JSONObject().apply {
                val setupObj = JSONObject().apply {
                    put("model", MODEL_NAME)

                    // Generation config with Audio modality and Aoede voice
                    val genConfig = JSONObject().apply {
                        put("responseModalities", JSONArray().put("AUDIO"))
                        val speechConfig = JSONObject().apply {
                            val voiceConfig = JSONObject().apply {
                                val prebuilt = JSONObject().apply {
                                    put("voiceName", "Aoede") // Aoede / Kore is a confident, expressive female voice
                                }
                                put("prebuiltVoiceConfig", prebuilt)
                            }
                            put("voiceConfig", voiceConfig)
                        }
                        put("speechConfig", speechConfig)
                    }
                    put("generationConfig", genConfig)

                    // System instructions
                    val systemInstruction = JSONObject().apply {
                        val parts = JSONArray().put(JSONObject().put("text", systemPrompt))
                        put("parts", parts)
                    }
                    put("systemInstruction", systemInstruction)

                    // Native Tools definition
                    val toolsArray = JSONArray()
                    val toolsObj = JSONObject().apply {
                        put("functionDeclarations", buildToolDeclarations())
                    }
                    toolsArray.put(toolsObj)
                    put("tools", toolsArray)
                }
                put("setup", setupObj)
            }

            ws.send(setupJson.toString())
            Log.d(TAG, "Sent setup message to Gemini Live")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create setup message", e)
        }
    }

    private fun buildToolDeclarations(): JSONArray {
        val list = JSONArray()

        // 1. openApp
        list.put(JSONObject().apply {
            put("name", "openApp")
            put("description", "Open or launch any installed Android app like YouTube, Instagram, WhatsApp, Spotify, Calculator, Camera, Maps, etc.")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("appName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Common name of the app to launch (e.g. YouTube, Instagram, WhatsApp, Spotify, Calculator)")
                    })
                    put("packageName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional Android package name (e.g. com.google.android.youtube)")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("appName"))
            }
            put("parameters", params)
        })

        // 2. searchAndCallContact
        list.put(JSONObject().apply {
            put("name", "searchAndCallContact")
            put("description", "Find contact from phone book and place a phone call")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("contactName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of person to call from contacts (e.g. Mom, Alex, John)")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("contactName"))
            }
            put("parameters", params)
        })

        // 3. sendWhatsAppMessage
        list.put(JSONObject().apply {
            put("name", "sendWhatsAppMessage")
            put("description", "Send a WhatsApp text message to a contact or phone number")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("contactName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name or phone number of the recipient")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The message text to send")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("contactName").put("message"))
            }
            put("parameters", params)
        })

        // 3b. sendGmail
        list.put(JSONObject().apply {
            put("name", "sendGmail")
            put("description", "Compose or send an email via Gmail or system email client")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("recipientEmail", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Email address or contact name of the recipient")
                    })
                    put("subject", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Subject line of the email")
                    })
                    put("body", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Body content of the email")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("recipientEmail").put("body"))
            }
            put("parameters", params)
        })

        // 4. setAlarmOrTimer
        list.put(JSONObject().apply {
            put("name", "setAlarmOrTimer")
            put("description", "Set a timer or reminder on the phone")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("minutes", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Number of minutes for the timer")
                    })
                    put("label", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Label or reason for timer")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("minutes"))
            }
            put("parameters", params)
        })

        // 5. toggleFlashlight
        list.put(JSONObject().apply {
            put("name", "toggleFlashlight")
            put("description", "Turn the device camera flashlight torch on or off")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("enable", JSONObject().apply {
                        put("type", "BOOLEAN")
                        put("description", "True to turn on flashlight, False to turn off")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("enable"))
            }
            put("parameters", params)
        })

        // 6. playMusic
        list.put(JSONObject().apply {
            put("name", "playMusic")
            put("description", "Play or search for songs/artists on YouTube or music app")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Song, genre, or artist to play")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("query"))
            }
            put("parameters", params)
        })

        // 7. getDeviceStatus
        list.put(JSONObject().apply {
            put("name", "getDeviceStatus")
            put("description", "Get the device battery level, charging status, current time and date")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
            put("parameters", params)
        })

        // 8. adjustDeviceVolume
        list.put(JSONObject().apply {
            put("name", "adjustDeviceVolume")
            put("description", "Intelligently adjust the media, notification, ringtone, or all audio volumes on the phone based on explicit percentage (0-100), time of day, or user context (e.g. night, meeting, gym, car, party, auto)")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("streamType", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Target audio stream: 'media', 'notification', 'ring', 'alarm', or 'all'")
                    })
                    put("levelPercent", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Target volume percentage between 0 and 100")
                    })
                    put("contextMode", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Contextual mode or time descriptor: 'night', 'sleep', 'meeting', 'quiet', 'work', 'gym', 'workout', 'party', 'car', 'driving', 'auto', or 'time'")
                    })
                }
                put("properties", props)
            }
            put("parameters", params)
        })

        // 9. sendTelegramMessage
        list.put(JSONObject().apply {
            put("name", "sendTelegramMessage")
            put("description", "Send a message via Telegram")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("contactOrChat", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Contact name or chat recipient")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The message text to send")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("message"))
            }
            put("parameters", params)
        })

        // 10. controlMediaPlayback
        list.put(JSONObject().apply {
            put("name", "controlMediaPlayback")
            put("description", "Control audio/video playback (play, pause, next/skip, prev, stop)")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("action", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Action to perform: 'play', 'pause', 'next', 'prev', 'stop', or 'toggle'")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("action"))
            }
            put("parameters", params)
        })

        // 11. toggleWifi
        list.put(JSONObject().apply {
            put("name", "toggleWifi")
            put("description", "Open the Wi-Fi settings or connectivity panel")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
            put("parameters", params)
        })

        // 12. toggleBluetooth
        list.put(JSONObject().apply {
            put("name", "toggleBluetooth")
            put("description", "Open Bluetooth settings")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
            put("parameters", params)
        })

        // 13. adjustBrightness
        list.put(JSONObject().apply {
            put("name", "adjustBrightness")
            put("description", "Open Display & Brightness settings")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
            put("parameters", params)
        })

        // 14. enterStandbyMode
        list.put(JSONObject().apply {
            put("name", "enterStandbyMode")
            put("description", "Enter low-power standby mode when user says 'Bye MM', 'Bye', 'Go to sleep', etc. Keeps microphone active in background for 'Hello MM'.")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
            put("parameters", params)
        })

        // 15. controlRemotePc
        list.put(JSONObject().apply {
            put("name", "controlRemotePc")
            put("description", "Control connected Desktop PC over Wi-Fi (lock, shutdown, restart, sleep, open_app, media, type_text, screenshot, shell)")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("action", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "PC command: 'lock', 'shutdown', 'restart', 'sleep', 'open_app', 'media', 'type_text', 'screenshot', 'shell'")
                    })
                    put("targetApp", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Desktop app to open (e.g. 'chrome', 'vscode', 'spotify', 'terminal')")
                    })
                    put("textToType", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Text to type remotely onto PC screen")
                    })
                    put("customCommand", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Shell command to run on PC")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("action"))
            }
            put("parameters", params)
        })

        return list
    }

    /**
     * Send real-time 16kHz 16-bit PCM audio chunk to Gemini Live.
     */
    fun sendAudioChunk(pcmData: ByteArray, bytesRead: Int) {
        if (!isConnected || webSocket == null) return

        try {
            val base64Audio = Base64.encodeToString(pcmData, 0, bytesRead, Base64.NO_WRAP)
            val realtimeMessage = JSONObject().apply {
                val realtimeInput = JSONObject().apply {
                    val mediaChunks = JSONArray().put(JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64Audio)
                    })
                    put("mediaChunks", mediaChunks)
                }
                put("realtimeInput", realtimeInput)
            }
            webSocket?.send(realtimeMessage.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending audio chunk", e)
        }
    }

    /**
     * Handle incoming WebSocket JSON payload from Gemini Live.
     */
    private fun handleServerMessage(jsonString: String) {
        try {
            val json = JSONObject(jsonString)

            // 1. Check for serverContent
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")

                // Check for interruption
                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d(TAG, "Gemini Live was interrupted by user!")
                    onInterrupted()
                    _assistantState.value = AssistantState.LISTENING
                    return
                }

                // Parse model turns
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts") ?: JSONArray()

                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)

                        // Check for audio PCM chunks (24kHz)
                        if (part.has("inlineData")) {
                            val inlineData = part.getJSONObject("inlineData")
                            val base64Data = inlineData.optString("data")
                            if (base64Data.isNotEmpty()) {
                                val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                _assistantState.value = AssistantState.SPEAKING
                                onAudioOutputChunk(audioBytes)
                            }
                        }

                        // Check for text transcript
                        if (part.has("text")) {
                            val text = part.getString("text").trim()
                            if (text.isNotEmpty()) {
                                _sassyOneLiner.value = text
                                addTranscript(LiveTranscript.Sender.MM, text)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    _assistantState.value = AssistantState.LISTENING
                }
            }

            // 2. Check for toolCall
            if (json.has("toolCall")) {
                val toolCall = json.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls") ?: JSONArray()

                for (i in 0 until functionCalls.length()) {
                    val callObj = functionCalls.getJSONObject(i)
                    val callId = callObj.optString("id", java.util.UUID.randomUUID().toString())
                    val name = callObj.optString("name")
                    val argsObj = callObj.optJSONObject("args") ?: JSONObject()

                    val argsMap = mutableMapOf<String, Any?>()
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.get(key)
                    }

                    executeNativeTool(callId, name, argsMap)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message", e)
        }
    }

    private fun executeNativeTool(callId: String, name: String, args: Map<String, Any?>) {
        scope.launch {
            _assistantState.value = AssistantState.EXECUTING_TOOL
            _activeToolCall.value = ToolCallInfo(callId, name, args, ToolCallInfo.ToolStatus.EXECUTING)

            val result: ToolExecutionResult = when (name) {
                "openApp" -> {
                    val appName = args["appName"]?.toString()
                    val packageName = args["packageName"]?.toString()
                    deviceToolManager.openApp(packageName, appName)
                }
                "searchAndCallContact" -> {
                    val contactName = args["contactName"]?.toString() ?: ""
                    deviceToolManager.searchAndCallContact(contactName)
                }
                "sendWhatsAppMessage" -> {
                    val contactName = args["contactName"]?.toString() ?: ""
                    val message = args["message"]?.toString() ?: ""
                    deviceToolManager.sendWhatsAppMessage(contactName, message)
                }
                "sendGmail" -> {
                    val recipient = args["recipientEmail"]?.toString() ?: ""
                    val subject = args["subject"]?.toString() ?: "Message from MM Assistant"
                    val body = args["body"]?.toString() ?: ""
                    deviceToolManager.sendGmail(recipient, subject, body)
                }
                "setAlarmOrTimer" -> {
                    val minutes = (args["minutes"] as? Number)?.toInt() ?: 5
                    val label = args["label"]?.toString()
                    deviceToolManager.setAlarmOrTimer(minutes, label)
                }
                "toggleFlashlight" -> {
                    val enable = (args["enable"] as? Boolean) ?: true
                    deviceToolManager.toggleFlashlight(enable)
                }
                "playMusic" -> {
                    val query = args["query"]?.toString() ?: ""
                    deviceToolManager.playMusic(query)
                }
                "getDeviceStatus" -> {
                    deviceToolManager.getDeviceStatus()
                }
                "adjustDeviceVolume" -> {
                    val streamType = args["streamType"]?.toString()
                    val levelPercent = (args["levelPercent"] as? Number)?.toInt()
                    val contextMode = args["contextMode"]?.toString()
                    deviceToolManager.adjustDeviceVolume(streamType, levelPercent, contextMode)
                }
                "sendTelegramMessage" -> {
                    val contactOrChat = args["contactOrChat"]?.toString()
                    val message = args["message"]?.toString() ?: ""
                    deviceToolManager.sendTelegramMessage(contactOrChat, message)
                }
                "controlMediaPlayback" -> {
                    val action = args["action"]?.toString() ?: "play_pause"
                    deviceToolManager.controlMediaPlayback(action)
                }
                "toggleWifi" -> {
                    deviceToolManager.toggleWifi()
                }
                "toggleBluetooth" -> {
                    deviceToolManager.toggleBluetooth()
                }
                "adjustBrightness" -> {
                    deviceToolManager.adjustBrightness()
                }
                "enterStandbyMode" -> {
                    deviceToolManager.enterStandbyMode()
                }
                "controlRemotePc" -> {
                    val action = args["action"]?.toString() ?: "ping"
                    val targetApp = args["targetApp"]?.toString()
                    val textToType = args["textToType"]?.toString()
                    val customCommand = args["customCommand"]?.toString()
                    deviceToolManager.controlRemotePc(action, targetApp, textToType, customCommand)
                }
                else -> {
                    ToolExecutionResult(false, "Unknown tool: $name")
                }
            }

            _activeToolCall.value = ToolCallInfo(
                callId, name, args,
                if (result.success) ToolCallInfo.ToolStatus.SUCCESS else ToolCallInfo.ToolStatus.FAILED,
                result.message
            )
            addTranscript(LiveTranscript.Sender.SYSTEM, "⚡ MM executed $name: ${result.message}", isTool = true)
            _sassyOneLiner.value = result.message

            // Send toolResponse back to Gemini Live
            sendToolResponse(callId, name, result)

            delay(1500)
            _assistantState.value = AssistantState.LISTENING
        }
    }

    private fun sendToolResponse(callId: String, functionName: String, result: ToolExecutionResult) {
        if (!isConnected || webSocket == null) return

        try {
            val responseMsg = JSONObject().apply {
                val toolResponse = JSONObject().apply {
                    val functionResponses = JSONArray().put(JSONObject().apply {
                        put("id", callId)
                        val responseObj = JSONObject().apply {
                            val outputObj = JSONObject().apply {
                                put("result", result.message)
                                put("success", result.success)
                                result.data.forEach { (k, v) -> put(k, v) }
                            }
                            put("output", outputObj)
                        }
                        put("response", responseObj)
                    })
                    put("functionResponses", functionResponses)
                }
                put("toolResponse", toolResponse)
            }
            webSocket?.send(responseMsg.toString())
            Log.d(TAG, "Sent toolResponse for $functionName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending tool response", e)
        }
    }

    /**
     * Send direct user text/voice trigger (e.g. from quick chips).
     */
    fun sendUserPrompt(prompt: String) {
        addTranscript(LiveTranscript.Sender.USER, prompt)
        if (!isConnected) {
            connect()
        }

        try {
            val clientContentMsg = JSONObject().apply {
                val clientContent = JSONObject().apply {
                    val turns = JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        val parts = JSONArray().put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put("parts", parts)
                    })
                    put("turns", turns)
                    put("turnComplete", true)
                }
                put("clientContent", clientContent)
            }
            webSocket?.send(clientContentMsg.toString())
            _assistantState.value = AssistantState.THINKING
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending user prompt", e)
        }
    }

    private fun addTranscript(sender: LiveTranscript.Sender, text: String, isTool: Boolean = false) {
        val current = _transcripts.value.toMutableList()
        current.add(LiveTranscript(sender = sender, text = text, isToolCall = isTool))
        if (current.size > 50) current.removeAt(0)
        _transcripts.value = current
    }

    fun disconnect() {
        isConnected = false
        isConnecting = false
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _assistantState.value = AssistantState.DISCONNECTED
    }
}
