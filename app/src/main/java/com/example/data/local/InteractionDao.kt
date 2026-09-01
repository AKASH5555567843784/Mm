package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionDao {

    @Query("SELECT * FROM cached_interactions ORDER BY timestamp DESC LIMIT 10")
    fun getRecentInteractions(): Flow<List<InteractionEntity>>

    @Query("SELECT * FROM cached_interactions ORDER BY timestamp ASC LIMIT 10")
    suspend fun getRecentInteractionsAscending(): List<InteractionEntity>

    @Query("SELECT * FROM cached_interactions ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentInteractionsSnapshot(): List<InteractionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: InteractionEntity): Long

    @Query("DELETE FROM cached_interactions WHERE id NOT IN (SELECT id FROM cached_interactions ORDER BY timestamp DESC LIMIT 10)")
    suspend fun trimOldInteractions()

    @Query("DELETE FROM cached_interactions")
    suspend fun clearAllInteractions()

    @Query("SELECT COUNT(*) FROM cached_interactions")
    fun getInteractionCount(): Flow<Int>
}
