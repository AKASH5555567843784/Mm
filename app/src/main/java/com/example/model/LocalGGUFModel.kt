package com.example.model

/**
 * Metadata for open-source GGUF on-device local models.
 */
data class LocalGGUFModel(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val parameterCount: String,
    val quantization: String,
    val downloadUrl: String,
    val fileName: String,
    val isDownloaded: Boolean = false,
    val isSelected: Boolean = false,
    val downloadProgress: Float = 0f,
    val isDownloading: Boolean = false
)
