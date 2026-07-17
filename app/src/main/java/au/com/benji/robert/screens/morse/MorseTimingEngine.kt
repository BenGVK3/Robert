package au.com.benji.robert.screens.morse

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Precision Morse timing engine following ITU-R M.1677-1 standards.
 * Dot = 1 unit, Dash = 3 units, Intra-character gap = 1 unit.
 * Inter-character gap = 3 units, Inter-word gap = 7 units.
 * Supports Farnsworth timing for character and word gaps.
 */
class MorseTimingEngine(
    private val onToneAction: (Boolean) -> Unit,
    private val onElementSent: (String) -> Unit,
    private val onCharacterComplete: () -> Unit = {},
    private val onToneStateChanged: (Boolean) -> Unit = {}
) {
    private val isRunning = AtomicBoolean(true)
    private var timingThread: Thread? = null

    private val dotPaddle = AtomicBoolean(false)
    private val dashPaddle = AtomicBoolean(false)
    private val dotLatch = AtomicBoolean(false)
    private val dashLatch = AtomicBoolean(false)
    
    private val wpm = AtomicInteger(20)
    private val farnsworthWpm = AtomicInteger(20)
    private val weighting = AtomicReference(1.0f)
    private val keyerMode = AtomicReference(KeyerMode.Straight)
    
    private val straightKeyActive = AtomicBoolean(false)
    
    // Command queue for automated playback
    private val commandQueue = LinkedBlockingQueue<TimingCommand>()
    private val isPlayingPlayback = AtomicBoolean(false)

    init {
        startTimingThread()
    }

    private fun startTimingThread() {
        timingThread = Thread({
            while (isRunning.get()) {
                val mode = keyerMode.get()
                
                if (mode == KeyerMode.Straight) {
                    handleStraightKey()
                } else {
                    val command = commandQueue.poll()
                    if (command != null) {
                        isPlayingPlayback.set(true)
                        executeCommand(command)
                    } else {
                        isPlayingPlayback.set(false)
                        handleIambicKeyer()
                    }
                }
                
                if (commandQueue.isEmpty() && !dotPaddle.get() && !dashPaddle.get() && !straightKeyActive.get()) {
                    try {
                        Thread.sleep(1)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }
        }, "MorseTimingThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun handleStraightKey() {
        if (straightKeyActive.get()) {
            val startTime = System.currentTimeMillis()
            onToneAction(true)
            onToneStateChanged(true)
            while (straightKeyActive.get() && isRunning.get()) {
                Thread.yield()
            }
            onToneAction(false)
            
            val currentWpm = wpm.get()
            val unitMs = 1200.0 / currentWpm
            val duration = System.currentTimeMillis() - startTime
            val element = if (duration < unitMs * 2.0) "." else "-"
            onElementSent(element)
            onToneStateChanged(false)
        }
    }

    private fun handleIambicKeyer() {
        val hasDot = dotPaddle.get() || dotLatch.get()
        val hasDash = dashPaddle.get() || dashLatch.get()
        
        if (!hasDot && !hasDash) {
            lastSentElement = ""
            return
        }

        val nextElement = when {
            hasDot && !hasDash -> "."
            !hasDot && hasDash -> "-"
            else -> if (lastSentElement == ".") "-" else "."
        }

        if (nextElement == ".") dotLatch.set(false) else dashLatch.set(false)
        sendElement(nextElement)
    }

    private var lastSentElement = ""

    private fun sendElement(element: String) {
        val currentWpm = wpm.get()
        val unitMs = 1200.0 / currentWpm
        val durationMs = if (element == ".") unitMs else (unitMs * 3.0 * weighting.get())
        
        onToneAction(true)
        onToneStateChanged(true)
        onElementSent(element)
        lastSentElement = element
        
        sleepNanos((durationMs * 1_000_000.0).toLong(), element)
        onToneAction(false)
        onToneStateChanged(false)
        
        // Intra-character gap (always 1 unit at current WPM)
        sleepNanos((unitMs * 1_000_000.0).toLong(), element)
    }

    private fun executeCommand(command: TimingCommand) {
        val currentWpm = wpm.get()
        val currentFarnsworthWpm = farnsworthWpm.get()
        val unitMs = 1200.0 / currentWpm
        val fUnitMs = 1200.0 / currentFarnsworthWpm

        when (command) {
            is TimingCommand.PlayElement -> {
                val durationMs = if (command.element == ".") unitMs else (unitMs * 3.0)
                onToneAction(true)
                onToneStateChanged(true)
                sleepNanos((durationMs * 1_000_000.0).toLong())
                onToneAction(false)
                onToneStateChanged(false)
                
                // Intra-character gap (always 1 unit at current WPM speed)
                sleepNanos((unitMs * 1_000_000.0).toLong())
            }
            is TimingCommand.Gap -> {
                // For inter-character (3 units) and inter-word (7 units) gaps, 
                // 1 unit is already spent in intra-character gap above.
                val actualUnits = (command.units - 1).coerceAtLeast(0)
                if (actualUnits > 0) {
                    sleepNanos((fUnitMs * actualUnits * 1_000_000.0).toLong())
                }
            }
            is TimingCommand.NotifyCharacterComplete -> {
                onCharacterComplete()
            }
        }
    }

    private fun sleepNanos(nanos: Long, elementBeingSent: String? = null) {
        val startTime = System.nanoTime()
        while (System.nanoTime() - startTime < nanos && isRunning.get()) {
            // Iambic Crossover Memory: check paddles during tone/gap.
            // ONLY latch the OPPOSITE paddle. Latching the same paddle causes duplicate elements
            // because of human response time (we can't release faster than a dot finishes).
            if (dotPaddle.get() && elementBeingSent != ".") dotLatch.set(true)
            if (dashPaddle.get() && elementBeingSent != "-") dashLatch.set(true)
            Thread.yield()
        }
    }

    fun updateConfig(settings: MorseSettings) {
        this.wpm.set(settings.wpm)
        this.farnsworthWpm.set(settings.farnsworthWpm)
        this.weighting.set(settings.weighting)
        this.keyerMode.set(settings.keyerMode)
    }

    fun setDotPaddle(pressed: Boolean) {
        dotPaddle.set(pressed)
        if (pressed) {
            // Only latch if we are NOT currently sending a dit.
            // If we ARE sending a dit, handleIambicKeyer will check dotPaddle.get() after the gap.
            // If we ARE sending a DAH, then we WANT to latch the dit for later.
            // However, it's simpler to just set the latch and let sleepNanos handle the "opposite only" rule.
            // Actually, we need to set the latch here too for the FIRST element.
            dotLatch.set(true)
        }
    }

    fun setDashPaddle(pressed: Boolean) {
        dashPaddle.set(pressed)
        if (pressed) {
            dashLatch.set(true)
        }
    }
    
    fun setStraightKey(pressed: Boolean) {
        straightKeyActive.set(pressed)
    }

    fun enqueueText(text: String) {
        commandQueue.clear()
        val trimmedText = text.uppercase().trim()
        if (trimmedText.isEmpty()) return

        // Split by whitespace to identify words
        val words = trimmedText.split(Regex("\\s+"))
        
        for (wIndex in words.indices) {
            val word = words[wIndex]
            
            for (cIndex in word.indices) {
                val char = word[cIndex]
                val code = MorseCodeMap[char] ?: continue
                
                for (element in code) {
                    commandQueue.add(TimingCommand.PlayElement(element.toString()))
                }
                
                // After every character, we add a character gap (3 units).
                // EXCEPT if it's the last character of a word and there's a word gap coming next.
                if (cIndex < word.length - 1) {
                    commandQueue.add(TimingCommand.Gap(3))
                }
                
                commandQueue.add(TimingCommand.NotifyCharacterComplete)
            }
            
            // After every word, we add a word gap (7 units).
            if (wIndex < words.size - 1) {
                commandQueue.add(TimingCommand.Gap(7))
            }
        }
    }

    fun stopPlayback() {
        commandQueue.clear()
        onToneAction(false)
        onToneStateChanged(false)
    }

    fun release() {
        isRunning.set(false)
        timingThread?.interrupt()
    }
    
    fun isPlaybackActive() = isPlayingPlayback.get() || !commandQueue.isEmpty()
}

sealed class TimingCommand {
    data class PlayElement(val element: String) : TimingCommand()
    data class Gap(val units: Int) : TimingCommand()
    object NotifyCharacterComplete : TimingCommand()
}
