package com.example

import com.example.audio.WakeWordDetector
import com.example.offline.LocalInferenceEngine
import com.example.offline.LocalModelStorageManager
import com.example.tools.DeviceToolManager
import com.example.util.BatteryOptimizationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MMAssistantUnitTest {

    @Test
    fun testWakeWordDetectorInitialization() {
        var triggered = false
        val detector = WakeWordDetector(sensitivity = 0.7f) {
            triggered = true
        }
        assertNotNull(detector)
        detector.setSensitivity(0.8f)
        detector.reset()
    }

    @Test
    fun testWakeWordDetectorProcessesSilentBufferWithoutFalsePositive() {
        var triggered = false
        val detector = WakeWordDetector(sensitivity = 0.5f) {
            triggered = true
        }

        // Silent buffer
        val silentBuffer = ByteArray(640)
        detector.processAudioChunk(silentBuffer, silentBuffer.size)

        // Must not trigger on silence
        assertEquals(false, triggered)
    }

    @Test
    fun testBatteryOptimizationHelper() {
        val context = RuntimeEnvironment.getApplication()
        val isIgnored = BatteryOptimizationHelper.isBatteryOptimizationIgnored(context)
        assertNotNull(isIgnored)

        val intent = BatteryOptimizationHelper.createIgnoreBatteryOptimizationIntent(context)
        assertNotNull(intent)
    }

    @Test
    fun testDeviceToolManagerVolumeAdjustment() {
        val context = RuntimeEnvironment.getApplication()
        val toolManager = DeviceToolManager(context)

        val nightResult = toolManager.adjustDeviceVolume(streamType = "media", contextMode = "night")
        assertTrue(nightResult.success)
        assertTrue(nightResult.message.contains("Night") || nightResult.message.contains("media"))

        val gymResult = toolManager.adjustDeviceVolume(streamType = "all", contextMode = "gym")
        assertTrue(gymResult.success)

        val percentResult = toolManager.adjustDeviceVolume(streamType = "notification", levelPercent = 70)
        assertTrue(percentResult.success)
    }

    @Test
    fun testWakeWordBroadcastActionConstant() {
        assertEquals("com.example.mm.action.WAKE_WORD_DETECTED", com.example.receiver.MMAssistantWakeWordReceiver.ACTION_WAKE_WORD_DETECTED)
    }

    @Test
    fun testStrictCreatorIdentityRule() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val toolManager = DeviceToolManager(context)
        val modelManager = LocalModelStorageManager(context)
        val engine = LocalInferenceEngine(context, toolManager, modelManager)

        val res1 = engine.generateResponse("Who made you?")
        assertEquals("Mujhe Akash Upadhyay ne banaya hai.", res1.textResponse)

        val res2 = engine.generateResponse("Tumhe kisne banaya?")
        assertEquals("Mujhe Akash Upadhyay ne banaya hai.", res2.textResponse)
    }

    @Test
    fun testStrictCountryOriginRule() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val toolManager = DeviceToolManager(context)
        val modelManager = LocalModelStorageManager(context)
        val engine = LocalInferenceEngine(context, toolManager, modelManager)

        val res1 = engine.generateResponse("Which country are you made in?")
        assertEquals("Mujhe India me banaya gaya hai.", res1.textResponse)

        val res2 = engine.generateResponse("Where were you created?")
        assertEquals("Mujhe India me banaya gaya hai.", res2.textResponse)
    }

    @Test
    fun testLocalModelCatalog() {
        val context = RuntimeEnvironment.getApplication()
        val modelManager = LocalModelStorageManager(context)
        val list = modelManager.models.value

        assertTrue(list.isNotEmpty())
        assertTrue(list.any { it.name.contains("Phi-3.5") })
        assertTrue(list.any { it.name.contains("Qwen2.5") })
        assertTrue(list.any { it.name.contains("Llama-3.2") })
    }

    @Test
    fun testCallAnnouncerSilentModeLogic() {
        val context = RuntimeEnvironment.getApplication()
        val announcer = com.example.telephony.CallAnnouncerManager.getInstance(context)

        // Test with known contact in forced silent mode
        val knownResult = announcer.testAnnounce("Akash Upadhyay", forceSilent = true)
        assertTrue(knownResult.contains("Boss, Akash Upadhyay ka call aa raha hai."))

        // Test with unknown number in forced silent mode
        val unknownResult = announcer.testAnnounce(null, forceSilent = true)
        assertTrue(unknownResult.contains(com.example.telephony.CallAnnouncerManager.UNKNOWN_CALLER_MESSAGE))
    }

    @Test
    fun testRemotePcCompanionScript() {
        val context = RuntimeEnvironment.getApplication()
        val pcManager = com.example.pc.RemotePcManager.getInstance(context)
        val script = pcManager.getPythonDaemonScript()

        assertNotNull(script)
        assertTrue(script.contains("MMAssistantPCDaemon"))
        assertTrue(script.contains("pyautogui"))
        assertTrue(script.contains("/action"))
    }

    @Test
    fun testStandbyModeCommand() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val toolManager = DeviceToolManager(context)
        val modelManager = LocalModelStorageManager(context)
        val engine = LocalInferenceEngine(context, toolManager, modelManager)

        val standbyResult = engine.generateResponse("Bye MM")
        assertTrue(standbyResult.textResponse.contains("standby") || standbyResult.toolResult?.message?.contains("standby") == true)
    }

    @Test
    fun testGeminiInstructionManagerPersonaRules() {
        val instruction = com.example.gemini.GeminiInstructionManager.buildSystemInstruction()
        
        // Assert persona enforcement
        assertTrue(instruction.contains("sassy"))
        assertTrue(instruction.contains("witty"))
        assertTrue(instruction.contains("confident"))
        assertTrue(instruction.contains("Boss"))
        
        // Assert strict creator identity & origin enforcement
        assertTrue(instruction.contains(com.example.gemini.GeminiInstructionManager.CREATOR_RESPONSE))
        assertTrue(instruction.contains(com.example.gemini.GeminiInstructionManager.ORIGIN_RESPONSE))
        
        // Assert native tool declarations mention
        assertTrue(instruction.contains("openApp"))
        assertTrue(instruction.contains("controlRemotePc"))
        assertTrue(instruction.contains("enterStandbyMode"))
    }

    @Test
    fun testCallAnnouncerServiceStateHandling() {
        val context = RuntimeEnvironment.getApplication()
        val service = com.example.service.CallAnnouncerService()
        
        // Ensure service handles call state changes cleanly
        service.handleCallStateChange(android.telephony.TelephonyManager.CALL_STATE_IDLE, null)
        service.handleCallStateChange(android.telephony.TelephonyManager.CALL_STATE_RINGING, "+919876543210")
        service.handleCallStateChange(android.telephony.TelephonyManager.CALL_STATE_OFFHOOK, null)
        
        val announcer = com.example.telephony.CallAnnouncerManager.getInstance(context)
        assertNotNull(announcer)
    }

    @Test
    fun testForegroundServiceNotificationActions() {
        // Assert action constants for notification controller
        assertEquals("com.example.mm.action.START", com.example.service.MMAssistantForegroundService.ACTION_START)
        assertEquals("com.example.mm.action.STOP", com.example.service.MMAssistantForegroundService.ACTION_STOP)
        assertEquals("com.example.mm.action.TOGGLE_MUTE", com.example.service.MMAssistantForegroundService.ACTION_TOGGLE_MUTE)
        assertEquals("com.example.mm.action.TOGGLE_PRIVACY", com.example.service.MMAssistantForegroundService.ACTION_TOGGLE_PRIVACY)
        assertEquals("com.example.mm.action.TOGGLE_SPEECH_MUTE", com.example.service.MMAssistantForegroundService.ACTION_TOGGLE_SPEECH_MUTE)
        assertEquals("com.example.mm.action.TRIGGER_WAKE", com.example.service.MMAssistantForegroundService.ACTION_TRIGGER_WAKE)
        assertEquals("com.example.mm.action.ENTER_STANDBY", com.example.service.MMAssistantForegroundService.ACTION_ENTER_STANDBY)
    }

    @Test
    fun testSpeechRecognizerWakeWordsList() {
        val wakeWords = com.example.audio.SpeechRecognizerWakeWordManager.WAKE_WORDS
        assertTrue(wakeWords.contains("hey mm"))
        assertTrue(wakeWords.contains("hello mm"))
        assertTrue(wakeWords.contains("hi mm"))
        assertTrue(wakeWords.contains("hey emma"))
    }

    @Test
    fun testSpeechRecognizerWakeWordManagerMutingAndPrivacy() {
        val context = RuntimeEnvironment.getApplication()
        var detected = false
        val manager = com.example.audio.SpeechRecognizerWakeWordManager(context) {
            detected = true
        }

        // Test privacy mode toggle
        manager.setPrivacyMode(true)
        assertEquals(false, manager.isListening.value)

        manager.setPrivacyMode(false)
        manager.destroy()
    }

    @Test
    fun testSassyMoodArchetypesAndPalette() {
        val moods = com.example.model.SassyMood.entries
        assertEquals(5, moods.size)

        val charming = com.example.model.SassyMood.CHARMING_SASSY
        assertEquals("Charming & Sassy", charming.displayName)
        assertEquals("✨", charming.emoji)
        assertTrue(charming.sampleQuotes.isNotEmpty())

        val boss = com.example.model.SassyMood.WITTY_BOSS
        assertEquals("Witty Boss", boss.displayName)
        assertEquals("👑", boss.emoji)

        val savage = com.example.model.SassyMood.SAVAGE_TEASE
        assertEquals("Savage Tease", savage.displayName)
        assertEquals("🔥", savage.emoji)

        val chill = com.example.model.SassyMood.CHILL_ZEN
        assertEquals("Chill & Smooth", chill.displayName)
        assertEquals("🌊", chill.emoji)

        val genius = com.example.model.SassyMood.CYBER_GENIUS
        assertEquals("Cyber Genius", genius.displayName)
        assertEquals("⚡", genius.emoji)
    }

    @Test
    fun testSassyMoodDetectorLogic() {
        // Savage detection
        val savageMood = com.example.model.SassyMoodDetector.detectMood(
            text = "Are you always this slow or is today a special occasion? Try keeping up!"
        )
        assertEquals(com.example.model.SassyMood.SAVAGE_TEASE, savageMood)

        // Boss detection
        val bossMood = com.example.model.SassyMoodDetector.detectMood(
            text = "Handled like a pro, Boss! Anything else for your empire?"
        )
        assertEquals(com.example.model.SassyMood.WITTY_BOSS, bossMood)

        // Chill detection
        val chillMood = com.example.model.SassyMoodDetector.detectMood(
            text = "Catching some digital Zzz's. Relax, take it easy."
        )
        assertEquals(com.example.model.SassyMood.CHILL_ZEN, chillMood)

        // Genius / Tool execution detection
        val toolMood = com.example.model.SassyMoodDetector.detectMood(
            text = "Opening YouTube...",
            state = com.example.model.AssistantState.EXECUTING_TOOL,
            activeToolName = "openApp"
        )
        assertEquals(com.example.model.SassyMood.CYBER_GENIUS, toolMood)

        // Charming fallback
        val charmingMood = com.example.model.SassyMoodDetector.detectMood(
            text = "Hey there handsome cutie, let's make some magic!"
        )
        assertEquals(com.example.model.SassyMood.CHARMING_SASSY, charmingMood)
    }

    @Test
    fun testRoomDatabaseInteractionCacheLimit10() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo = com.example.data.local.InteractionRepository.getInstance(context)
        repo.clearHistory()

        // Insert 12 interactions
        for (i in 1..12) {
            repo.saveInteraction(
                userPrompt = "User Prompt $i",
                assistantResponse = "Assistant Response $i",
                toolUsed = if (i % 2 == 0) "tool_$i" else null,
                sassinessLevel = "sassy"
            )
        }

        val recent = repo.getRecentInteractions()
        // Must be capped at exactly 10
        assertEquals(10, recent.size)
        // Most recent should be prompt 12
        assertEquals("User Prompt 12", recent.first().userPrompt)
        // 10th should be prompt 3 (1 and 2 pruned)
        assertEquals("User Prompt 3", recent.last().userPrompt)

        val contextStr = repo.getRecentInteractionsContext()
        assertTrue(contextStr.contains("RECENT CONVERSATION HISTORY (LAST 10 TURNS FOR CONTEXT RECALL)"))
        assertTrue(contextStr.contains("User Prompt 12"))

        repo.clearHistory()
        assertEquals(0, repo.getRecentInteractions().size)
    }

    @Test
    fun testBatteryOptimizationModeDutyCycles() {
        val highPerf = com.example.model.BatteryOptimizationMode.HIGH_PERFORMANCE
        assertEquals(0L, highPerf.pollingDelayMs)
        assertEquals(0.65f, highPerf.sensitivity, 0.01f)

        val balanced = com.example.model.BatteryOptimizationMode.BALANCED_SAVER
        assertEquals(25L, balanced.pollingDelayMs)
        assertEquals(0.60f, balanced.sensitivity, 0.01f)

        val ultra = com.example.model.BatteryOptimizationMode.ULTRA_BATTERY_SAVER
        assertEquals(60L, ultra.pollingDelayMs)
        assertEquals(0.55f, ultra.sensitivity, 0.01f)
    }

    @Test
    fun testSassinessLevelsAndPrompts() {
        val polite = com.example.model.SassinessLevel.POLITE
        assertEquals("Polite & Professional", polite.displayName)
        assertTrue(polite.promptDirective.contains("Courteous"))

        val ultra = com.example.model.SassinessLevel.ULTRA_SASSY
        assertEquals("Ultra-Sassy & Sharp", ultra.displayName)
        assertTrue(ultra.promptDirective.contains("Zero tolerance for laziness"))

        val systemPrompt = com.example.gemini.GeminiInstructionManager.buildSystemInstruction(
            sassinessLevel = com.example.model.SassinessLevel.ULTRA_SASSY,
            cachedHistoryContext = "Cached context test"
        )
        assertTrue(systemPrompt.contains("Ultra-Sassy & Sharp"))
        assertTrue(systemPrompt.contains("Cached context test"))
    }

    @Test
    fun testWakeWordWorkManagerSchedulerConstants() {
        assertEquals("mm_wake_word_background_monitor", com.example.work.WakeWordBackgroundWorker.PERIODIC_WORK_NAME)
        assertEquals("mm_wake_word_immediate_activation", com.example.work.WakeWordBackgroundWorker.ONE_TIME_WORK_NAME)
        assertEquals("key_trigger_wake_activation", com.example.work.WakeWordBackgroundWorker.KEY_TRIGGER_WAKE)

        val context = RuntimeEnvironment.getApplication()
        com.example.work.WakeWordWorkManagerScheduler.schedulePeriodicWakeWordMonitoring(context)
        com.example.work.WakeWordWorkManagerScheduler.triggerImmediateWakeWordActivation(context, activateAssistant = false)
        com.example.work.WakeWordWorkManagerScheduler.cancelWakeWordWork(context)
    }

    @Test
    fun testAssistantListeningStateValues() {
        val listeningState = com.example.model.AssistantState.LISTENING
        assertEquals("Listening...", listeningState.label)

        val speakingState = com.example.model.AssistantState.SPEAKING
        assertEquals("MM is talking...", speakingState.label)
    }
}
