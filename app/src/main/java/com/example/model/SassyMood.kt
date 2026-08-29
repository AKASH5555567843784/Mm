package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * Visual Mood Archetypes reflecting the 'sassy' personality of MM Gemini Assistant.
 * Each mood specifies dynamic color palettes, animation timing multipliers, aura glows,
 * and visual effects across the entire Jetpack Compose UI.
 */
enum class SassyMood(
    val id: String,
    val displayName: String,
    val emoji: String,
    val tagline: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val glowColor: Color,
    val backgroundAccent: Color,
    val surfaceAccent: Color,
    val particleSpeedMultiplier: Float,
    val rotationSpeedMs: Int,
    val breathingDurationMs: Int,
    val sampleQuotes: List<String>
) {
    CHARMING_SASSY(
        id = "charming_sassy",
        displayName = "Charming & Sassy",
        emoji = "✨",
        tagline = "Flirty, magnetic & delightfully playful",
        primaryColor = Color(0xFFFF2E93),      // Neon Magenta
        secondaryColor = Color(0xFF00E5FF),    // Neon Cyan
        tertiaryColor = Color(0xFFA259FF),     // Neon Violet
        glowColor = Color(0xFFFF69B4),         // Pink Glow
        backgroundAccent = Color(0xFF1D0B1C),  // Deep Magenta-black
        surfaceAccent = Color(0xFF281126),
        particleSpeedMultiplier = 1.0f,
        rotationSpeedMs = 7000,
        breathingDurationMs = 2200,
        sampleQuotes = listOf(
            "Hey there handsome! MM is ready when you are.",
            "Look who's back! Try keeping up with my sparkle today.",
            "Ready when you are, sweet thing—let's make some magic."
        )
    ),

    WITTY_BOSS(
        id = "witty_boss",
        displayName = "Witty Boss",
        emoji = "👑",
        tagline = "Sharp, triumphant executive leadership",
        primaryColor = Color(0xFFFFB703),      // Radiant Gold
        secondaryColor = Color(0xFFFB8500),    // Flame Amber
        tertiaryColor = Color(0xFFFFD166),     // Sunburst Yellow
        glowColor = Color(0xFFFFE169),         // Golden Aura
        backgroundAccent = Color(0xFF1C1405),  // Rich Amber-black
        surfaceAccent = Color(0xFF291E0B),
        particleSpeedMultiplier = 1.25f,
        rotationSpeedMs = 5500,
        breathingDurationMs = 1800,
        sampleQuotes = listOf(
            "Handled like a pro, Boss! Anything else for your empire?",
            "Leader of the pack in the building. What's our next big conquest?",
            "Executed with flawless precision. You're welcome, Boss."
        )
    ),

    SAVAGE_TEASE(
        id = "savage_tease",
        displayName = "Savage Tease",
        emoji = "🔥",
        tagline = "Feisty, sarcastic & dangerously witty",
        primaryColor = Color(0xFFFF1744),      // Crimson Flame
        secondaryColor = Color(0xFFFF5252),    // Lava Coral
        tertiaryColor = Color(0xFFFF6D00),     // Blaze Orange
        glowColor = Color(0xFFFF4081),         // Electric Red Glow
        backgroundAccent = Color(0xFF20070B),  // Lava black
        surfaceAccent = Color(0xFF300E14),
        particleSpeedMultiplier = 1.6f,
        rotationSpeedMs = 3800,
        breathingDurationMs = 1400,
        sampleQuotes = listOf(
            "Are you always this slow or is today a special occasion? 😉",
            "Ready to conquer the world or just procrastinating with style?",
            "I'd agree with you, but then we'd both be wrong, darling!"
        )
    ),

    CHILL_ZEN(
        id = "chill_zen",
        displayName = "Chill & Smooth",
        emoji = "🌊",
        tagline = "Cool, relaxed & effortless composure",
        primaryColor = Color(0xFF00E5FF),      // Cyber Aqua
        secondaryColor = Color(0xFF06D6A0),    // Emerald Mint
        tertiaryColor = Color(0xFF38B6FF),     // Deep Sky Blue
        glowColor = Color(0xFF80FFDB),         // Mint Glow
        backgroundAccent = Color(0xFF05171C),  // Oceanic Deep
        surfaceAccent = Color(0xFF0B252D),
        particleSpeedMultiplier = 0.7f,
        rotationSpeedMs = 11000,
        breathingDurationMs = 3200,
        sampleQuotes = listOf(
            "Catching some digital Zzz's. Call 'Hello MM' and I'll wake up smooth.",
            "No stress, no rush. We've got everything completely under control.",
            "Smooth sailing from here on out. What's the vibe?"
        )
    ),

    CYBER_GENIUS(
        id = "cyber_genius",
        displayName = "Cyber Genius",
        emoji = "⚡",
        tagline = "Deep calculation & autonomous tool execution",
        primaryColor = Color(0xFFA259FF),      // Ultra Violet
        secondaryColor = Color(0xFF7B2CBF),    // Deep Purple
        tertiaryColor = Color(0xFF00F0FF),     // Quantum Cyan
        glowColor = Color(0xFFC77DFF),         // Plasma Violet Glow
        backgroundAccent = Color(0xFF130826),  // Quantum Nebula
        surfaceAccent = Color(0xFF1E1038),
        particleSpeedMultiplier = 1.4f,
        rotationSpeedMs = 4500,
        breathingDurationMs = 1600,
        sampleQuotes = listOf(
            "Running quantum-level diagnostics... Tool executed perfectly!",
            "Synthesizing device telemetry at lightspeed. Consider it done.",
            "Calculated, optimized, and delivered. Next calculation please!"
        )
    );

    companion object {
        fun fromId(id: String?): SassyMood {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CHARMING_SASSY
        }
    }
}
