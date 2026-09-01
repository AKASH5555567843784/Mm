package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_interactions")
data class InteractionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userPrompt: String,
    val assistantResponse: String,
    val toolUsed: String? = null,
    val sassinessLevel: String = "SASSY"
)
