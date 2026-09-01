package com.example.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class InteractionRepository(private val interactionDao: InteractionDao) {

    val recentInteractions: Flow<List<InteractionEntity>> = interactionDao.getRecentInteractions()
    val interactionCount: Flow<Int> = interactionDao.getInteractionCount()

    suspend fun getRecentInteractions(): List<InteractionEntity> = withContext(Dispatchers.IO) {
        try {
            interactionDao.getRecentInteractionsSnapshot()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recent interactions", e)
            emptyList()
        }
    }

    suspend fun saveInteraction(
        userPrompt: String,
        assistantResponse: String,
        toolUsed: String? = null,
        sassinessLevel: String = "SASSY"
    ) = withContext(Dispatchers.IO) {
        try {
            val entity = InteractionEntity(
                userPrompt = userPrompt.trim(),
                assistantResponse = assistantResponse.trim(),
                toolUsed = toolUsed,
                sassinessLevel = sassinessLevel
            )
            interactionDao.insertInteraction(entity)
            interactionDao.trimOldInteractions()
            Log.d(TAG, "Cached interaction into Room (Total cap: 10)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save interaction to Room", e)
        }
    }

    suspend fun getRecentInteractionsContext(): String = withContext(Dispatchers.IO) {
        try {
            val list = interactionDao.getRecentInteractionsAscending()
            if (list.isEmpty()) return@withContext ""

            buildString {
                appendLine("=== RECENT CONVERSATION HISTORY (LAST 10 TURNS FOR CONTEXT RECALL) ===")
                list.forEachIndexed { index, item ->
                    appendLine("Turn ${index + 1}:")
                    appendLine("  Boss asked: \"${item.userPrompt}\"")
                    appendLine("  MM replied: \"${item.assistantResponse}\"")
                    if (!item.toolUsed.isNullOrEmpty()) {
                        appendLine("  Tool executed: ${item.toolUsed}")
                    }
                }
                appendLine("Directive: Use this cached history to recall context, answer follow-up questions accurately, avoid repeating questions, and maintain sharp continuity while maintaining your zero-lie policy.")
                appendLine()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading interactions context", e)
            ""
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        try {
            interactionDao.clearAllInteractions()
            Log.d(TAG, "Cleared cached interactions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing interactions", e)
        }
    }

    companion object {
        private const val TAG = "InteractionRepository"
        @Volatile
        private var INSTANCE: InteractionRepository? = null

        fun getInstance(context: Context): InteractionRepository {
            return INSTANCE ?: synchronized(this) {
                val db = MMAppDatabase.getInstance(context)
                val instance = InteractionRepository(db.interactionDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
