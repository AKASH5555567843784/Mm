package com.example.model

enum class AssistantState(val label: String) {
    DISCONNECTED("Disconnected"),
    STANDBY("Standby"),
    CONNECTING("Connecting to MM..."),
    LISTENING("Listening..."),
    THINKING("MM is thinking..."),
    SPEAKING("MM is talking..."),
    EXECUTING_TOOL("Executing device command..."),
    ERROR("Connection Error")
}

data class LiveTranscript(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isToolCall: Boolean = false
) {
    enum class Sender { USER, MM, SYSTEM }
}

data class ToolCallInfo(
    val callId: String,
    val functionName: String,
    val arguments: Map<String, Any?>,
    val status: ToolStatus = ToolStatus.PENDING,
    val resultMessage: String? = null
) {
    enum class ToolStatus { PENDING, EXECUTING, SUCCESS, FAILED }
}

data class ToolExecutionResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any?> = emptyMap()
)
