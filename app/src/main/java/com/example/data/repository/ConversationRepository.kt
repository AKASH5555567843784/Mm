package com.example.data.repository

import com.example.data.local.dao.ConversationDao
import com.example.data.local.entity.ConversationMessageEntity
import com.example.model.LiveTranscript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository pattern implementation for managing conversation persistence.
 */
class ConversationRepository(
    private val conversationDao: ConversationDao
) {
    val allTranscripts: Flow<List<LiveTranscript>> = conversationDao.getAllMessages()
        .map { entities ->
            entities.map { it.toLiveTranscript() }
        }
        .flowOn(Dispatchers.IO)

    val messageCount: Flow<Int> = conversationDao.getMessageCount()
        .flowOn(Dispatchers.IO)

    suspend fun saveTranscript(
        transcript: LiveTranscript,
        toolName: String? = null,
        sassyIntensityLevel: Int? = null
    ) = withContext(Dispatchers.IO) {
        val entity = ConversationMessageEntity.fromLiveTranscript(
            transcript = transcript,
            toolName = toolName,
            sassyIntensityLevel = sassyIntensityLevel
        )
        conversationDao.insertMessage(entity)
    }

    suspend fun saveTranscripts(transcripts: List<LiveTranscript>) = withContext(Dispatchers.IO) {
        val entities = transcripts.map { ConversationMessageEntity.fromLiveTranscript(it) }
        conversationDao.insertMessages(entities)
    }

    suspend fun deleteTranscript(id: String) = withContext(Dispatchers.IO) {
        conversationDao.deleteMessageById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        conversationDao.clearAllMessages()
    }
}
