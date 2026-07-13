package au.com.benji.robert.screens.morse

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Precision Morse timing engine.
 * Handles Iambic Mode A/B, Straight Key, and automatic spacing.
 * Operates on a dedicated high-priority thread for consistent timing.
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
    private val keyerMode = AtomicReference(KeyerMode.IambicB)
    
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
            
            // Logic to determine if it was a dot or dash for decoding
            val currentWpm = wpm.get()
            val unitMs = 1200L / currentWpm
            val duration = System.currentTimeMillis() - startTime
            val element = if (duration < unitMs * 2) "." else "-"
            onElementSent(element)

            onToneStateChanged(false)
        }
    }

    private fun handleIambicKeyer() {
        val hasDot = dotPaddle.get() || dotLatch.get()
        val hasDash = dashPaddle.get() || dashLatch.get()
        
        if (!hasDot && !hasDash) {
            lastSentElement = "" // Reset toggle memory when idle for crisp character starts
            return
        }

        val nextElement = when {
            hasDot && !hasDash -> "."
            !hasDot && hasDash -> "-"
            else -> if (lastSentElement == ".") "-" else "."
        }

        // Consume the latch for the element we are sending
        if (nextElement == ".") dotLatch.set(false) else dashLatch.set(false)
        
        sendElement(nextElement)
    }

    private var lastSentElement = ""

    private fun sendElement(element: String) {
        val currentWpm = wpm.get()
        val unitMs = 1200L / currentWpm
        val durationMs = if (element == ".") unitMs else (unitMs * 3 * weighting.get()).toLong()
        
        onToneAction(true)
        onToneStateChanged(true)
        onElementSent(element)
        lastSentElement = element
        
        val startTime = System.nanoTime()
        val durationNano = durationMs * 1_000_000L
        
        while (System.nanoTime() - startTime < durationNano && isRunning.get()) {
            // Iambic Crossover Memory: Latch the opposite paddle while the current one is playing.
            // This ensures crisp transitions during squeeze keying without causing same-paddle repeats.
            if (element == "." && dashPaddle.get()) dashLatch.set(true)
            if (element == "-" && dotPaddle.get()) dotLatch.set(true)
            Thread.yield()
        }
        
        onToneAction(false)
        
        val gapStartTime = System.nanoTime()
        val gapDurationNano = unitMs * 1_000_000L
        
        while (System.nanoTime() - gapStartTime < gapDurationNano && isRunning.get()) {
            Thread.yield()
        }

        // Notify that the full element cycle (tone + gap) is complete
        onToneStateChanged(false)
    }

    private fun executeCommand(command: TimingCommand) {
        when (command) {
            is TimingCommand.PlayElement -> {
                val currentWpm = wpm.get()
                val unitMs = 1200L / currentWpm
                val durationMs = if (command.element == ".") unitMs else (unitMs * 3).toLong()
                
                onToneAction(true)
                onToneStateChanged(true)
                onElementSent(command.element)
                
                val startTime = System.nanoTime()
                val durationNano = durationMs * 1_000_000L
                while (System.nanoTime() - startTime < durationNano && isRunning.get()) {
                    Thread.yield()
                }
                onToneAction(false)
                onToneStateChanged(false)
                
                val gapStartTime = System.nanoTime()
                val gapDurationNano = unitMs * 1_000_000L
                while (System.nanoTime() - gapStartTime < gapDurationNano && isRunning.get()) {
                    Thread.yield()
                }
            }
            is TimingCommand.Gap -> {
                val fWpm = farnsworthWpm.get()
                val unitMs = 1200L / fWpm
                val durationMs = unitMs * command.units
                
                val startTime = System.nanoTime()
                val durationNano = durationMs * 1_000_000L
                while (System.nanoTime() - startTime < durationNano && isRunning.get()) {
                    Thread.yield()
                }
            }
            is TimingCommand.NotifyCharacterComplete -> {
                onCharacterComplete()
            }
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
        if (pressed) dotLatch.set(true)
    }

    fun setDashPaddle(pressed: Boolean) {
        dashPaddle.set(pressed)
        if (pressed) dashLatch.set(true)
    }
    
    fun setStraightKey(pressed: Boolean) {
        straightKeyActive.set(pressed)
    }

    fun enqueueText(text: String) {
        commandQueue.clear()
        for (char in text.uppercase()) {
            if (char == ' ') {
                commandQueue.add(TimingCommand.Gap(7))
                continue
            }
            val code = MorseCodeMap[char] ?: continue
            for (element in code) {
                commandQueue.add(TimingCommand.PlayElement(element.toString()))
            }
            commandQueue.add(TimingCommand.Gap(3))
            commandQueue.add(TimingCommand.NotifyCharacterComplete)
        }
    }

    fun stopPlayback() {
        commandQueue.clear()
        onToneAction(false)
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
