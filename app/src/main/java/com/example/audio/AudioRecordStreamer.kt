package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioRecordStreamer(
    private val context: Context,
    private val sampleRate: Int = 16000,
    private val onAudioChunk: (ByteArray, Int) -> Unit,
    private val onUserVoiceDetected: () -> Unit = {}
) {
    companion object {
        private const val TAG = "AudioRecordStreamer"
        private const val CHUNK_SIZE = 1024 // 64ms at 16kHz 16-bit
        private const val USER_SPEECH_RMS_THRESHOLD = 0.045f
    }

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _inputAmplitude = MutableStateFlow(0f)
    val inputAmplitude: StateFlow<Float> = _inputAmplitude.asStateFlow()

    private var isMuted = false
    private var bufferSleepDelayMs = 0L

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun setBufferSleepDelay(delayMs: Long) {
        bufferSleepDelayMs = delayMs.coerceIn(0L, 200L)
        Log.d(TAG, "AudioRecordStreamer buffer sleep delay set to: ${bufferSleepDelayMs}ms")
    }

    @Synchronized
    fun startRecording(): Boolean {
        if (_isRecording.value) return true

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission not granted")
            return false
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(CHUNK_SIZE * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordJob = scope.launch {
                val buffer = ByteArray(CHUNK_SIZE)
                while (scope.isActive && _isRecording.value) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (bytesRead > 0) {
                        if (!isMuted) {
                            val copy = buffer.copyOf(bytesRead)
                            calculateAmplitudeAndVAD(copy, bytesRead)
                            onAudioChunk(copy, bytesRead)
                        } else {
                            _inputAmplitude.value = 0f
                        }
                    }
                    if (bufferSleepDelayMs > 0L) {
                        kotlinx.coroutines.delay(bufferSleepDelayMs)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord", e)
            _isRecording.value = false
            return false
        }
    }

    private fun calculateAmplitudeAndVAD(buffer: ByteArray, bytesRead: Int) {
        var sum = 0.0
        val sampleCount = bytesRead / 2
        if (sampleCount == 0) return

        for (i in 0 until bytesRead - 1 step 2) {
            val sample = ((buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)).toShort()
            val normalized = sample.toFloat() / 32768f
            sum += normalized * normalized
        }
        val rms = sqrt(sum / sampleCount).toFloat()
        val scaledAmplitude = (rms * 3.5f).coerceIn(0f, 1f)
        _inputAmplitude.value = scaledAmplitude

        // Check if user is speaking to trigger active interruption
        if (rms > USER_SPEECH_RMS_THRESHOLD) {
            onUserVoiceDetected()
        }
    }

    @Synchronized
    fun stopRecording() {
        _isRecording.value = false
        _inputAmplitude.value = 0f
        recordJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null
    }
}
