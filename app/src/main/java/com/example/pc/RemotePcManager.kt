package com.example.pc

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.ToolExecutionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Remote PC Control Ecosystem:
 * Connects MM Android Assistant to a companion PC daemon over Wi-Fi / Local Network.
 * Executes full A-to-Z PC automation:
 * - Lock / Sleep / Shutdown / Restart PC
 * - Open desktop applications (Chrome, VS Code, Spotify, Terminal, etc.)
 * - Media Play/Pause, Volume control, Next/Previous track
 * - Remote typing & text injection
 * - Remote PC screenshot capture & Shell commands
 */
class RemotePcManager(private val context: Context) {

    companion object {
        private const val TAG = "RemotePcManager"
        private const val PREFS_NAME = "mm_remote_pc_prefs"
        private const val KEY_IP = "pc_ip_address"
        private const val KEY_PORT = "pc_port"
        private const val KEY_NAME = "pc_name"
        private const val KEY_AUTO_CONNECT = "pc_auto_connect"

        @Volatile
        private var instance: RemotePcManager? = null

        fun getInstance(context: Context): RemotePcManager {
            return instance ?: synchronized(this) {
                instance ?: RemotePcManager(context.applicationContext).also { instance = it }
            }
        }
    }

    enum class PcConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val _targetIp = MutableStateFlow(prefs.getString(KEY_IP, "192.168.1.100") ?: "192.168.1.100")
    val targetIp: StateFlow<String> = _targetIp.asStateFlow()

    private val _targetPort = MutableStateFlow(prefs.getInt(KEY_PORT, 8989))
    val targetPort: StateFlow<Int> = _targetPort.asStateFlow()

    private val _pcName = MutableStateFlow(prefs.getString(KEY_NAME, "Main Workstation") ?: "Main Workstation")
    val pcName: StateFlow<String> = _pcName.asStateFlow()

    private val _status = MutableStateFlow(PcConnectionStatus.DISCONNECTED)
    val status: StateFlow<PcConnectionStatus> = _status.asStateFlow()

    private val _lastLog = MutableStateFlow("Ready to connect to PC companion daemon.")
    val lastLog: StateFlow<String> = _lastLog.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow<Long?>(null)
    val lastLatencyMs: StateFlow<Long?> = _lastLatencyMs.asStateFlow()

    fun updateConfig(ip: String, port: Int, name: String) {
        val cleanIp = ip.trim()
        val cleanPort = port.coerceIn(1024, 65535)
        val cleanName = name.trim().ifEmpty { "My PC" }

        _targetIp.value = cleanIp
        _targetPort.value = cleanPort
        _pcName.value = cleanName

        prefs.edit()
            .putString(KEY_IP, cleanIp)
            .putInt(KEY_PORT, cleanPort)
            .putString(KEY_NAME, cleanName)
            .apply()
    }

    suspend fun pingPc(): Boolean = withContext(Dispatchers.IO) {
        _status.value = PcConnectionStatus.CONNECTING
        val start = System.currentTimeMillis()
        val url = "http://${_targetIp.value}:${_targetPort.value}/ping"

        return@withContext try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - start
            _lastLatencyMs.value = latency

            if (response.isSuccessful) {
                _status.value = PcConnectionStatus.CONNECTED
                _lastLog.value = "Connected to ${_pcName.value} (${latency}ms ping)"
                true
            } else {
                _status.value = PcConnectionStatus.ERROR
                _lastLog.value = "PC Daemon replied with HTTP ${response.code}"
                false
            }
        } catch (e: Exception) {
            _status.value = PcConnectionStatus.ERROR
            _lastLatencyMs.value = null
            _lastLog.value = "Could not reach PC daemon at ${_targetIp.value}:${_targetPort.value}."
            Log.w(TAG, "Failed pinging PC at $url: ${e.message}")
            false
        }
    }

    suspend fun sendCommand(command: String, params: Map<String, Any?> = emptyMap()): ToolExecutionResult = withContext(Dispatchers.IO) {
        val url = "http://${_targetIp.value}:${_targetPort.value}/action"
        val payload = JSONObject().apply {
            put("command", command)
            put("timestamp", System.currentTimeMillis())
            val paramsObj = JSONObject()
            params.forEach { (k, v) -> paramsObj.put(k, v) }
            put("params", paramsObj)
        }

        try {
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                _status.value = PcConnectionStatus.CONNECTED
                val json = try { JSONObject(responseBody) } catch (e: Exception) { null }
                val msg = json?.optString("message") ?: "Executed '$command' on ${_pcName.value} successfully!"
                _lastLog.value = "PC [${command}]: $msg"
                ToolExecutionResult(
                    success = true,
                    message = msg,
                    data = mapOf("command" to command, "pc" to _pcName.value)
                )
            } else {
                val err = "PC rejected command '$command' (HTTP ${response.code})"
                _lastLog.value = err
                ToolExecutionResult(false, err)
            }
        } catch (e: Exception) {
            _status.value = PcConnectionStatus.ERROR
            val msg = "Couldn't send '$command' to PC at ${_targetIp.value}:${_targetPort.value}. Is the Python companion daemon running?"
            _lastLog.value = msg
            Log.e(TAG, "Error executing PC command: $command", e)
            ToolExecutionResult(false, msg)
        }
    }

    suspend fun lockPc(): ToolExecutionResult {
        return sendCommand("lock")
    }

    suspend fun shutdownPc(): ToolExecutionResult {
        return sendCommand("shutdown")
    }

    suspend fun restartPc(): ToolExecutionResult {
        return sendCommand("restart")
    }

    suspend fun sleepPc(): ToolExecutionResult {
        return sendCommand("sleep")
    }

    suspend fun openApp(appName: String): ToolExecutionResult {
        return sendCommand("open_app", mapOf("appName" to appName))
    }

    suspend fun controlMedia(action: String): ToolExecutionResult {
        return sendCommand("media", mapOf("action" to action))
    }

    suspend fun typeText(text: String): ToolExecutionResult {
        return sendCommand("type_text", mapOf("text" to text))
    }

    suspend fun takeScreenshot(): ToolExecutionResult {
        return sendCommand("screenshot")
    }

    suspend fun executeCustom(commandString: String): ToolExecutionResult {
        return sendCommand("shell", mapOf("command" to commandString))
    }

    /**
     * Pre-packaged, ready-to-run Python companion server code.
     * Users can copy this script to their PC and run `python mm_pc_daemon.py`.
     */
    fun getPythonDaemonScript(): String {
        return """
# ==========================================
#  MM Assistant - Companion PC Daemon Server
#  Run on your Windows, Mac, or Linux PC
#  Requirements: pip install flask pyautogui
# ==========================================

from flask import Flask, request, jsonify
import os, sys, platform, subprocess, time
import pyautogui

app = Flask("MMAssistantPCDaemon")
OS_NAME = platform.system().lower()

@app.route('/ping', methods=['GET'])
def ping():
    return jsonify({
        "status": "online",
        "device": platform.node(),
        "os": OS_NAME,
        "time": time.time()
    })

@app.route('/action', methods=['POST'])
def action():
    data = request.get_json(force=True) or {}
    cmd = data.get("command", "").lower().strip()
    params = data.get("params", {})
    
    print(f"[MM-PC] Received command: {cmd} with params: {params}")

    try:
        # 1. Lock Workstation
        if cmd == "lock":
            if "windows" in OS_NAME:
                os.system("rundll32.exe user32.dll,LockWorkStation")
            elif "darwin" in OS_NAME: # macOS
                os.system("pmset displaysleepnow")
            else: # Linux
                os.system("xdg-screensaver lock || gnome-screensaver-command -l")
            return jsonify({"success": True, "message": "Workstation locked successfully, Boss!"})

        # 2. Power Controls
        elif cmd == "shutdown":
            if "windows" in OS_NAME:
                os.system("shutdown /s /t 10")
            else:
                os.system("shutdown -h +1")
            return jsonify({"success": True, "message": "Initiated PC shutdown in 10 seconds!"})

        elif cmd == "restart":
            if "windows" in OS_NAME:
                os.system("shutdown /r /t 5")
            else:
                os.system("shutdown -r +1")
            return jsonify({"success": True, "message": "Restarting PC now!"})

        elif cmd == "sleep":
            if "windows" in OS_NAME:
                os.system("rundll32.exe powrprof.dll,SetSuspendState 0,1,0")
            elif "darwin" in OS_NAME:
                os.system("pmset sleepnow")
            return jsonify({"success": True, "message": "PC put to sleep."})

        # 3. Open Desktop Apps
        elif cmd == "open_app":
            app_name = params.get("appName", "").lower().strip()
            app_map = {
                "chrome": "start chrome" if "windows" in OS_NAME else "open -a 'Google Chrome'",
                "vscode": "code",
                "vs code": "code",
                "spotify": "start spotify" if "windows" in OS_NAME else "open -a Spotify",
                "notepad": "notepad",
                "terminal": "start cmd" if "windows" in OS_NAME else "open -a Terminal",
                "calculator": "calc",
                "youtube": "start https://www.youtube.com" if "windows" in OS_NAME else "open https://www.youtube.com"
            }
            exec_target = app_map.get(app_name, app_name)
            if "windows" in OS_NAME and not exec_target.startswith("start") and not exec_target.endswith(".exe"):
                exec_target = f"start {exec_target}"
            os.system(exec_target)
            return jsonify({"success": True, "message": f"Opened {app_name} on PC!"})

        # 4. Media Controls
        elif cmd == "media":
            action_type = params.get("action", "play_pause").lower()
            if action_type in ["play", "pause", "play_pause", "toggle"]:
                pyautogui.press("playpause")
            elif action_type in ["next", "skip"]:
                pyautogui.press("nexttrack")
            elif action_type in ["prev", "previous"]:
                pyautogui.press("prevtrack")
            elif action_type in ["volume_up", "volup"]:
                pyautogui.press("volumeup", presses=5)
            elif action_type in ["volume_down", "voldown"]:
                pyautogui.press("volumedown", presses=5)
            elif action_type in ["mute"]:
                pyautogui.press("volumemute")
            return jsonify({"success": True, "message": f"Media action '{action_type}' applied on PC."})

        # 5. Remote Typing
        elif cmd == "type_text":
            text_to_type = params.get("text", "")
            pyautogui.write(text_to_type, interval=0.03)
            return jsonify({"success": True, "message": f"Typed '{text_to_type}' on PC active window!"})

        # 6. Screenshot
        elif cmd == "screenshot":
            screenshot_path = os.path.expanduser("~/Desktop/MM_PC_Screenshot.png")
            img = pyautogui.screenshot()
            img.save(screenshot_path)
            return jsonify({"success": True, "message": f"PC Screenshot saved to Desktop!"})

        # 7. Shell command
        elif cmd == "shell":
            custom_cmd = params.get("command", "")
            output = subprocess.getoutput(custom_cmd)
            return jsonify({"success": True, "message": f"Output: {output[:200]}"})

        else:
            return jsonify({"success": False, "message": f"Unknown command: {cmd}"}), 400

    except Exception as e:
        print(f"Error handling command: {e}")
        return jsonify({"success": False, "message": str(e)}), 500

if __name__ == '__main__':
    port = 8989
    print(f"==================================================")
    print(f"  MM Assistant PC Companion Daemon Active!")
    print(f"  Listening on http://0.0.0.0:{port}")
    print(f"  Connect from MM Android App using this PC's IP")
    print(f"==================================================")
    app.run(host='0.0.0.0', port=port, debug=False)
        """.trimIndent()
    }
}
