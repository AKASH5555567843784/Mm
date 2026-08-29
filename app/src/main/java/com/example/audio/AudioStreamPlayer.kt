package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs
import kotlin.math.sqrt

class AudioStreamPlayer(
    private val sampleRate: Int = 24000
) {
    companion object {
        private const val TAG = "AudioStreamPlayer"
    }

    private var audioTrack: AudioTrack? = null
    private val audioQueue = LinkedBlockingQueue<ByteArray>()
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _outputAmplitude = MutableStateFlow(0f)
    val outputAmplitude: StateFlow<Boolean> = _isPlaying // Used for indicator
    val liveAmplitude: StateFlow<Float> = _outputAmplitude.asStateFlow()

    private var isInitialized = false

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            audioQueue.clear()
            _isPlaying.value = false
            _outputAmplitude.value = 0f
        }
    }

    @Synchronized
    fun start() {
        if (isInitialized) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
            }

            audioTrack?.play()
            isInitialized = true

            playbackJob = scope.launch {
                runPlaybackLoop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioTrack", e)
        }
    }

    fun enqueuePcmChunk(pcmData: ByteArray) {
        if (_isMuted.value) return
        if (!isInitialized) start()
        audioQueue.offer(pcmData)
    }

    private fun runPlaybackLoop() {
        while (scope.isActive) {
            try {
                val chunk = audioQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                if (chunk != null && chunk.isNotEmpty()) {
                    _isPlaying.value = true
                    calculateAmplitude(chunk)
                    audioTrack?.write(chunk, 0, chunk.size)
                } else {
                    if (audioQueue.isEmpty()) {
                        _isPlaying.value = false
                        _outputAmplitude.value = 0f
                    }
                }
            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error during audio playback loop", e)
            }
        }
    }

    private fun calculateAmplitude(chunk: ByteArray) {
        var sum = 0.0
        val sampleCount = chunk.size / 2
        if (sampleCount == 0) return

        for (i in 0 until chunk.size - 1 step 2) {
            val sample = (chunk[i].toInt() and 0xFF) or (chunk[i + 1].toInt() shl 8)
            val signedSample = sample.toShort().toFloat() / 32768.0f
            sum += signedSample * signedSample
        }
        val rms = sqrt(sum / sampleCount).toFloat()
        _outputAmplitude.value = (rms * 2.5f).coerceIn(0f, 1f)
    }

    /**
     * Instantly halt playback and clear buffer for smooth interruptions.
     */
    @Synchronized
    fun interrupt() {
        audioQueue.clear()
        _isPlaying.value = false
        _outputAmplitude.value = 0f
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error interrupting AudioTrack", e)
        }
    }

    @Synchronized
    fun release() {
        playbackJob?.cancel()
        audioQueue.clear()
        _isPlaying.value = false
        _outputAmplitude.value = 0f
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack", e)
        }
        audioTrack = null
        isInitialized = false
    }
}
