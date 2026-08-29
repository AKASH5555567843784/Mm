package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.model.SassyMood

val LocalSassyMood = compositionLocalOf { SassyMood.CHARMING_SASSY }

@Composable
fun MMAssistantTheme(
    sassyMood: SassyMood = SassyMood.CHARMING_SASSY,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Smoothly animate core theme colors as Gemini's sassy mood shifts
    val animatedPrimary by animateColorAsState(
        targetValue = sassyMood.primaryColor,
        animationSpec = tween(durationMillis = 500),
        label = "ThemePrimaryAnim"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = sassyMood.secondaryColor,
        animationSpec = tween(durationMillis = 500),
        label = "ThemeSecondaryAnim"
    )
    val animatedTertiary by animateColorAsState(
        targetValue = sassyMood.tertiaryColor,
        animationSpec = tween(durationMillis = 500),
        label = "ThemeTertiaryAnim"
    )
    val animatedBackground by animateColorAsState(
        targetValue = sassyMood.backgroundAccent,
        animationSpec = tween(durationMillis = 600),
        label = "ThemeBgAnim"
    )
    val animatedSurface by animateColorAsState(
        targetValue = sassyMood.surfaceAccent,
        animationSpec = tween(durationMillis = 600),
        label = "ThemeSurfaceAnim"
    )

    val dynamicColorScheme = darkColorScheme(
        primary = animatedPrimary,
        onPrimary = Color.White,
        primaryContainer = animatedPrimary.copy(alpha = 0.25f),
        onPrimaryContainer = Color.White,
        secondary = animatedSecondary,
        onSecondary = Color.Black,
        secondaryContainer = animatedSecondary.copy(alpha = 0.25f),
        onSecondaryContainer = Color.White,
        tertiary = animatedTertiary,
        onTertiary = Color.White,
        tertiaryContainer = animatedTertiary.copy(alpha = 0.25f),
        onTertiaryContainer = Color.White,
        background = animatedBackground,
        onBackground = TextPrimary,
        surface = animatedSurface,
        onSurface = TextPrimary,
        surfaceVariant = animatedSurface.copy(alpha = 0.85f),
        onSurfaceVariant = TextSecondary,
        outline = animatedPrimary.copy(alpha = 0.35f)
    )

    CompositionLocalProvider(LocalSassyMood provides sassyMood) {
        MaterialTheme(
            colorScheme = dynamicColorScheme,
            typography = Typography,
            content = content
        )
    }
}

