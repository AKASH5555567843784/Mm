package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ConversationDao
import com.example.data.local.entity.ConversationMessageEntity

/**
 * Room Database for local persistence of MM Assistant conversation history, logs, and state.
 */
@Database(
    entities = [ConversationMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MMAssistantDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: MMAssistantDatabase? = null

        fun getInstance(context: Context): MMAssistantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MMAssistantDatabase::class.java,
                    "mm_assistant_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
