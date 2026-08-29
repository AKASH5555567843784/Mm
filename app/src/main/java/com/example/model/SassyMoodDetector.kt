package com.example.model

/**
 * Intelligent analyzer that classifies the current 'sassy' mood of Gemini model responses,
 * active tool executions, or user prompts into distinct visual UI states.
 */
object SassyMoodDetector {

    private val CHARMING_KEYWORDS = listOf(
        "handsome", "darling", "cutie", "sugar", "magic", "sparkle", "gorgeous",
        "sweetheart", "babe", "charm", "flirt", "sweet", "honey", "love", "beautiful", "✨", "😉", "💖"
    )

    private val BOSS_KEYWORDS = listOf(
        "boss", "chief", "champion", "conquer", "empire", "pro", "masterpiece",
        "king", "queen", "legend", "handled like a pro", "rule", "command", "executed with", "taken care of", "boom!", "👑", "🏆", "💎"
    )

    private val SAVAGE_KEYWORDS = listOf(
        "slow", "procrastinating", "obviously", "as if", "you're welcome", "savage",
        "roast", "burn", "drama", "excuses", "not even close", "keeping up", "wrong", "sarcastic", "try harder", "ouch", "🔥", "😏", "💅"
    )

    private val CHILL_KEYWORDS = listOf(
        "sleep", "standby", "zen", "relax", "chill", "peace", "zzz", "beauty sleep",
        "goodnight", "take it easy", "breathe", "calm", "smooth", "smooth sailing", "low-power", "💤", "🌙", "🌊"
    )

    private val GENIUS_KEYWORDS = listOf(
        "executing", "quantum", "calculat", "optimized", "telemetry", "diagnostics",
        "tool", "flashlight", "volume", "whatsapp", "gmail", "alarm", "timer", "status", "searching", "battery", "⚡", "🤖", "🧠"
    )

    /**
     * Detects the dominant sassy mood from the model's text, state, and active tool call.
     */
    fun detectMood(
        text: String,
        state: AssistantState = AssistantState.DISCONNECTED,
        activeToolName: String? = null
    ): SassyMood {
        // Priority 1: Executing active tool is always CYBER_GENIUS
        if (state == AssistantState.EXECUTING_TOOL || !activeToolName.isNullOrBlank()) {
            return SassyMood.CYBER_GENIUS
        }

        // Priority 2: Standby state maps to CHILL_ZEN
        if (state == AssistantState.STANDBY) {
            return SassyMood.CHILL_ZEN
        }

        val lower = text.lowercase()

        // Count keyword matches for each mood archetype
        val savageScore = SAVAGE_KEYWORDS.count { lower.contains(it) } * 2
        val bossScore = BOSS_KEYWORDS.count { lower.contains(it) } * 2
        val chillScore = CHILL_KEYWORDS.count { lower.contains(it) } * 2
        val charmingScore = CHARMING_KEYWORDS.count { lower.contains(it) } * 2
        val geniusScore = GENIUS_KEYWORDS.count { lower.contains(it) }

        val scores = mapOf(
            SassyMood.SAVAGE_TEASE to savageScore,
            SassyMood.WITTY_BOSS to bossScore,
            SassyMood.CHILL_ZEN to chillScore,
            SassyMood.CHARMING_SASSY to charmingScore,
            SassyMood.CYBER_GENIUS to geniusScore
        )

        val highestEntry = scores.maxByOrNull { it.value }
        return if (highestEntry != null && highestEntry.value > 0) {
            highestEntry.key
        } else {
            // Default fallback
            SassyMood.CHARMING_SASSY
        }
    }
}
