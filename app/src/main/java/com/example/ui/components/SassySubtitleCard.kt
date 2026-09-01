package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.model.SassyMood
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalSassyMood
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SassySubtitleCard(
    quote: String,
    state: AssistantState,
    modifier: Modifier = Modifier,
    mood: SassyMood = LocalSassyMood.current,
    onVocalizeClick: (() -> Unit)? = null,
    isSpeaking: Boolean = false
) {
    val animatedBorderPrimary by animateColorAsState(
        targetValue = mood.primaryColor,
        animationSpec = tween(450),
        label = "SubBorderPrimary"
    )
    val animatedBorderSecondary by animateColorAsState(
        targetValue = mood.secondaryColor,
        animationSpec = tween(450),
        label = "SubBorderSecondary"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.85f))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        animatedBorderPrimary.copy(alpha = 0.65f),
                        animatedBorderSecondary.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
            .testTag("sassy_subtitle_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sassy badge indicator + Mood Pill Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Assistant state indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    AssistantState.SPEAKING -> mood.primaryColor
                                    AssistantState.LISTENING -> mood.secondaryColor
                                    AssistantState.THINKING -> mood.glowColor
                                    AssistantState.EXECUTING_TOOL -> mood.tertiaryColor
                                    else -> Color.Gray
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MM • ${state.label}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = mood.secondaryColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp
                        )
                    )
                }

                // Dynamic Mood Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = mood.primaryColor.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, mood.primaryColor.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("mood_pill_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = mood.emoji, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = mood.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = mood.primaryColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = quote,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { -it / 2 })
                },
                label = "QuoteAnimation"
            ) { currentQuote ->
                Text(
                    text = "\"$currentQuote\"",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 23.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mood.tagline,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (onVocalizeClick != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSpeaking) mood.primaryColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            if (isSpeaking) mood.primaryColor else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onVocalizeClick() }
                            .testTag("tts_vocalize_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Vocalize with TextToSpeech",
                                tint = if (isSpeaking) mood.primaryColor else Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSpeaking) "STOP" else "VOCALIZE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSpeaking) mood.primaryColor else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}


