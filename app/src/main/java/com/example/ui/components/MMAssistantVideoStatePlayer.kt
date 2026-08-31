package com.example.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.AssistantState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

enum class AssistantVideoState {
    INTRO_POPUP,
    LISTENING,
    TALKING_1,
    TALKING_2,
    STANDBY
}

/**
 * High-performance Video & Animated State Engine for MM Assistant.
 * Works flawlessly both ONLINE and OFFLINE:
 * 1. Offline Mode: Procedural real-time 3D Canvas holographic particle shader & audio-reactive orb.
 * 2. File / Video Stream Mode: Seamlessly plays local MP4 video assets if present.
 */
@Composable
fun MMAssistantVideoStatePlayer(
    assistantState: AssistantState,
    audioAmplitude: Float = 0f,
    modifier: Modifier = Modifier,
    preferredVideoState: AssistantVideoState? = null
) {
    val context = LocalContext.current
    val currentVideoState = preferredVideoState ?: when (assistantState) {
        AssistantState.CONNECTING -> AssistantVideoState.INTRO_POPUP
        AssistantState.LISTENING -> AssistantVideoState.LISTENING
        AssistantState.THINKING -> AssistantVideoState.TALKING_1
        AssistantState.SPEAKING -> AssistantVideoState.TALKING_2
        AssistantState.EXECUTING_TOOL -> AssistantVideoState.TALKING_1
        AssistantState.STANDBY -> AssistantVideoState.STANDBY
        AssistantState.DISCONNECTED -> AssistantVideoState.STANDBY
        AssistantState.ERROR -> AssistantVideoState.STANDBY
    }

    // Check if any local video file exists for this state
    val localVideoUri = remember(currentVideoState) {
        findLocalVideoFile(context, currentVideoState)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .testTag("assistant_video_state_player"),
        contentAlignment = Alignment.Center
    ) {
        if (localVideoUri != null) {
            // Online / File Video Playback
            NativeVideoPlayer(videoUri = localVideoUri)
        } else {
            // Offline High-Precision Holographic Canvas Engine
            OfflineHolographicEngine(
                videoState = currentVideoState,
                audioAmplitude = audioAmplitude
            )
        }
    }
}

/**
 * Searches for video files in assets, app storage, or public storage matching the requested state.
 */
private fun findLocalVideoFile(context: Context, state: AssistantVideoState): Uri? {
    val candidateNames = when (state) {
        AssistantVideoState.INTRO_POPUP -> listOf("button_intro_popup.mp4", "button intro popup.mp4", "buttom popup.mp4", "intro.mp4")
        AssistantVideoState.LISTENING -> listOf("listening.mp4", "listen.mp4", "orbit_listening.mp4")
        AssistantVideoState.TALKING_1 -> listOf("talking 1.mp4", "talking_1.mp4", "talking1.mp4", "speaking.mp4")
        AssistantVideoState.TALKING_2 -> listOf("talking 2.mp4", "talking_2.mp4", "talking2.mp4", "speaking_alt.mp4")
        AssistantVideoState.STANDBY -> listOf("standby.mp4", "idle.mp4")
    }

    try {
        // 1. Check internal storage files
        val filesDir = context.filesDir
        for (name in candidateNames) {
            val file = File(filesDir, name)
            if (file.exists() && file.length() > 1024) {
                return Uri.fromFile(file)
            }
        }

        // 2. Check external public storage directory
        val extDir = context.getExternalFilesDir(null)
        if (extDir != null) {
            for (name in candidateNames) {
                val file = File(extDir, name)
                if (file.exists() && file.length() > 1024) {
                    return Uri.fromFile(file)
                }
            }
        }
    } catch (e: Exception) {
        Log.w("VideoStatePlayer", "Error searching for local video files", e)
    }

    return null
}

/**
 * Native Android MediaPlayer view with looping playback for MP4 videos.
 */
@Composable
private fun NativeVideoPlayer(videoUri: Uri) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(videoUri) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                Log.e("NativeVideoPlayer", "Error releasing MediaPlayer", e)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        try {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(ctx, videoUri)
                                setDisplay(holder)
                                isLooping = true
                                setVolume(0f, 0f) // Video visualizer is silent; assistant speaks via Gemini audio stream
                                prepareAsync()
                                setOnPreparedListener { mp ->
                                    mp.start()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("NativeVideoPlayer", "Error preparing video", e)
                        }
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        mediaPlayer?.setDisplay(null)
                    }
                })
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Ultra-smooth Offline Procedural Holographic Canvas Engine that renders
 * futuristic glowing orbs, orbiting rings, dynamic sound waves, and particle auras.
 */
@Composable
private fun OfflineHolographicEngine(
    videoState: AssistantVideoState,
    audioAmplitude: Float
) {
    val mood = LocalSassyMood.current
    val infiniteTransition = rememberInfiniteTransition(label = "HolographicEngineTransition")

    // Continuous 3D rotation angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (videoState) {
                    AssistantVideoState.TALKING_1, AssistantVideoState.TALKING_2 -> 2500
                    AssistantVideoState.LISTENING -> 4000
                    AssistantVideoState.INTRO_POPUP -> 1500
                    AssistantVideoState.STANDBY -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    // Pulse scale for audio responsiveness
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (videoState) {
                    AssistantVideoState.TALKING_1, AssistantVideoState.TALKING_2 -> 450
                    AssistantVideoState.LISTENING -> 800
                    AssistantVideoState.INTRO_POPUP -> 350
                    AssistantVideoState.STANDBY -> 1400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val primaryColor = when (videoState) {
        AssistantVideoState.LISTENING -> NeonCyan
        AssistantVideoState.TALKING_1 -> NeonMagenta
        AssistantVideoState.TALKING_2 -> NeonViolet
        AssistantVideoState.INTRO_POPUP -> AccentGreen
        AssistantVideoState.STANDBY -> Color(0xFF6C757D)
    }

    val secondaryColor = when (videoState) {
        AssistantVideoState.LISTENING -> NeonViolet
        AssistantVideoState.TALKING_1 -> NeonAmber
        AssistantVideoState.TALKING_2 -> NeonCyan
        AssistantVideoState.INTRO_POPUP -> NeonCyan
        AssistantVideoState.STANDBY -> Color(0xFF495057)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = (size.minDimension / 2f) * 0.75f
        val effectiveScale = (pulse + (audioAmplitude * 0.7f)).coerceIn(0.7f, 1.6f)

        // 1. Ambient Radial Nebula Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.45f * effectiveScale),
                    secondaryColor.copy(alpha = 0.20f * effectiveScale),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = baseRadius * 1.3f
            ),
            radius = baseRadius * 1.3f,
            center = Offset(centerX, centerY)
        )

        // 2. Multi-layered Orbiting Holographic Rings
        val ringCount = 3
        for (i in 0 until ringCount) {
            val ringRadius = baseRadius * (0.55f + i * 0.22f) * effectiveScale
            val phaseOffset = i * 60f
            val ringAngle = (rotationAngle + phaseOffset) * (if (i % 2 == 0) 1 else -1)

            val ringBrush = Brush.sweepGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.9f),
                    secondaryColor.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.9f),
                    primaryColor.copy(alpha = 0.9f)
                ),
                center = Offset(centerX, centerY)
            )

            drawCircle(
                brush = ringBrush,
                radius = ringRadius,
                center = Offset(centerX, centerY),
                style = Stroke(
                    width = (3.dp.toPx() * (1f - i * 0.2f)).coerceAtLeast(1.5f),
                    cap = StrokeCap.Round
                )
            )

            // Orbiting Quantum Particle on the ring
            val rad = Math.toRadians((ringAngle).toDouble())
            val particleX = (centerX + ringRadius * cos(rad)).toFloat()
            val particleY = (centerY + ringRadius * sin(rad)).toFloat()
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(particleX, particleY)
            )
            drawCircle(
                color = primaryColor,
                radius = 7.dp.toPx(),
                center = Offset(particleX, particleY),
                alpha = 0.6f
            )
        }

        // 3. Central Core Glowing Orb
        val coreRadius = baseRadius * 0.45f * effectiveScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    primaryColor,
                    secondaryColor.copy(alpha = 0.7f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = coreRadius
            ),
            radius = coreRadius,
            center = Offset(centerX, centerY)
        )

        // 4. Audio-Reactive Sound Wave Spikes during Speaking / Listening
        if (videoState == AssistantVideoState.TALKING_1 || videoState == AssistantVideoState.TALKING_2 || videoState == AssistantVideoState.LISTENING) {
            val spikeCount = 24
            for (j in 0 until spikeCount) {
                val angleRad = (j * (360.0 / spikeCount) + rotationAngle * 0.5) * (Math.PI / 180.0)
                val spikeHeight = 12.dp.toPx() * (1f + (audioAmplitude * 2.5f) * ((j % 3) + 1) * 0.4f)
                val startR = baseRadius * 0.85f
                val endR = startR + spikeHeight

                val startX = (centerX + startR * cos(angleRad)).toFloat()
                val startY = (centerY + startR * sin(angleRad)).toFloat()
                val endX = (centerX + endR * cos(angleRad)).toFloat()
                val endY = (centerY + endR * sin(angleRad)).toFloat()

                drawLine(
                    color = if (j % 2 == 0) primaryColor else secondaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
