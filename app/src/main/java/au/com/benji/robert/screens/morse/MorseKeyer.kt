package au.com.benji.robert.screens.morse

import android.content.Context

/**
 * High-level Morse keyer orchestrator.
 * Connects the UI events to the Timing and Audio engines.
 */
class MorseKeyer(
    context: Context,
    private val onElementSent: (String) -> Unit,
    private val onCharacterComplete: () -> Unit = {},
    private val onToneStateChanged: (Boolean) -> Unit = {}
) {
    private val audioEngine = MorseAudioEngine(context)
    private val timingEngine = MorseTimingEngine(
        onToneAction = { active -> audioEngine.setToneActive(active) },
        onElementSent = onElementSent,
        onCharacterComplete = onCharacterComplete,
        onToneStateChanged = onToneStateChanged
    )

    fun updateSettings(settings: MorseSettings) {
        audioEngine.updateConfig(settings.frequency, settings.sidetoneVolume)
        timingEngine.updateConfig(settings)
    }

    fun onDotPaddle(pressed: Boolean) {
        timingEngine.setDotPaddle(pressed)
    }

    fun onDashPaddle(pressed: Boolean) {
        timingEngine.setDashPaddle(pressed)
    }

    fun onStraightKey(pressed: Boolean) {
        timingEngine.setStraightKey(pressed)
    }

    fun playText(text: String) {
        timingEngine.enqueueText(text)
    }

    fun stopPlayback() {
        timingEngine.stopPlayback()
    }
    
    fun isPlaybackActive() = timingEngine.isPlaybackActive()

    fun release() {
        timingEngine.release()
        audioEngine.release()
    }
}
