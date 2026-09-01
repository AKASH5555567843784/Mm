package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ConversationMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for querying and persisting MM Assistant conversation messages.
 */
@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversation_history ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ConversationMessageEntity>>

    @Query("SELECT * FROM conversation_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int): Flow<List<ConversationMessageEntity>>

    @Query("SELECT COUNT(*) FROM conversation_history")
    fun getMessageCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ConversationMessageEntity>)

    @Query("DELETE FROM conversation_history WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM conversation_history")
    suspend fun clearAllMessages()
}
