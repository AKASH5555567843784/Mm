package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.LiveTranscript

/**
 * Room Database Entity for persisting conversation history and assistant interactions.
 */
@Entity(tableName = "conversation_history")
data class ConversationMessageEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER", "MM", "SYSTEM"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isToolCall: Boolean = false,
    val toolName: String? = null,
    val sassyIntensityLevel: Int? = null
) {
    fun toLiveTranscript(): LiveTranscript {
        val senderEnum = when (sender.uppercase()) {
            "USER" -> LiveTranscript.Sender.USER
            "MM" -> LiveTranscript.Sender.MM
            else -> LiveTranscript.Sender.SYSTEM
        }
        return LiveTranscript(
            id = id,
            sender = senderEnum,
            text = text,
            timestamp = timestamp,
            isToolCall = isToolCall
        )
    }

    companion object {
        fun fromLiveTranscript(
            transcript: LiveTranscript,
            toolName: String? = null,
            sassyIntensityLevel: Int? = null
        ): ConversationMessageEntity {
            return ConversationMessageEntity(
                id = transcript.id,
                sender = transcript.sender.name,
                text = transcript.text,
                timestamp = transcript.timestamp,
                isToolCall = transcript.isToolCall,
                toolName = toolName,
                sassyIntensityLevel = sassyIntensityLevel
            )
        }
    }
}
