package au.com.benji.robert.screens.morse

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

/**
 * Professional Morse Audio Engine.
 * Optimized for zero-latency gating and pure sine-wave generation.
 * Eliminates distortion by maintaining phase continuity and using smooth envelopes.
 */
class MorseEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "MorseEngine"
    private var audioTrack: AudioTrack? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    @Volatile private var wpm = 20
    @Volatile private var farnsworthWpm = 20
    @Volatile private var frequency = 600
    @Volatile private var sidetoneVolume = 0.8f
    @Volatile private var playbackVolume = 0.8f

    private val sampleRate = 44100
    private val bufferSamples = 256 // Slightly larger for stability against underruns
    
    private val isRunning = AtomicBoolean(true)
    private val gateActive = AtomicBoolean(false)
    private val playbackActive = AtomicBoolean(false)

    private var audioThread: Thread? = null

    private val audioFocusRequest: AudioFocusRequest? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stop()
                    }
                }
                .build()
        } else null
    }

    init {
        initAudio()
        startAudioThread()
    }

    private fun initAudio() {
        // Abandon any previous focus if we're re-initializing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }

        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            // Use a reasonably sized buffer to avoid underruns on Android's scheduling
            .setBufferSizeInBytes(minBufferSize.coerceAtLeast(bufferSamples * 8))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            
        audioTrack?.play()
    }

    private fun startAudioThread() {
        audioThread = Thread({
            val samples = ShortArray(bufferSamples)
            var phase = 0.0
            var currentEnvelope = 0.0f
            
            // Professional 5ms ramps to eliminate clicks/pops
            val rampTimeSeconds = 0.005f
            val envelopeStep = 1.0f / (sampleRate * rampTimeSeconds)

            while (isRunning.get()) {
                val gActive = gateActive.get()
                val pActive = playbackActive.get()
                val targetEnvelope = if (gActive || pActive) 1.0f else 0.0f
                
                // Copy volatiles to locals for this buffer
                val freq = frequency.toDouble()
                val vol = if (pActive) playbackVolume else sidetoneVolume
                val maxAmp = (vol * Short.MAX_VALUE).toDouble()
                val phaseStep = freq / sampleRate

                for (i in 0 until bufferSamples) {
                    // Smooth linear amplitude ramp
                    if (currentEnvelope < targetEnvelope) {
                        currentEnvelope = (currentEnvelope + envelopeStep).coerceAtMost(targetEnvelope)
                    } else if (currentEnvelope > targetEnvelope) {
                        currentEnvelope = (currentEnvelope - envelopeStep).coerceAtLeast(targetEnvelope)
                    }

                    // Pure Sine Wave Generation
                    // We maintain phase continuity even when silent to avoid any pops on start
                    val sampleValue = if (currentEnvelope > 0) {
                        (sin(2.0 * PI * phase) * currentEnvelope * maxAmp).toInt().toShort()
                    } else {
                        0.toShort()
                    }
                    
                    samples[i] = sampleValue
                    
                    phase += phaseStep
                    if (phase >= 1.0) phase -= 1.0
                }
                
                // Blocking write is more stable for dedicated threads
                // It ensures we stay in sync with the hardware clock
                val result = audioTrack?.write(samples, 0, bufferSamples, AudioTrack.WRITE_BLOCKING)
                if (result != null && result < 0) {
                    Log.e(TAG, "AudioTrack write error: $result")
                }
            }
        }, "MorseAudioThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun requestFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) } == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    fun updateConfig(settings: MorseSettings) {
        this.wpm = settings.wpm
        this.farnsworthWpm = settings.farnsworthWpm
        this.frequency = settings.frequency
        this.sidetoneVolume = settings.sidetoneVolume
        this.playbackVolume = settings.volume
    }

    fun playText(text: String, onComplete: () -> Unit = {}) {
        if (!requestFocus()) return
        
        scope.launch(Dispatchers.Default) {
            playbackActive.set(false)
            val unit = 1200 / wpm
            val farnsworthCharGap = (1200 / farnsworthWpm) * 3
            val farnsworthWordGap = (1200 / farnsworthWpm) * 7

            for (char in text.uppercase()) {
                if (!isActive) break
                if (char == ' ') {
                    delay(farnsworthWordGap.toLong())
                    continue
                }

                val code = MorseCodeMap[char] ?: continue
                for (i in code.indices) {
                    val symbol = code[i]
                    val duration = if (symbol == '.') unit else unit * 3
                    
                    playbackActive.set(true)
                    delay(duration.toLong())
                    playbackActive.set(false)
                    
                    if (i < code.length - 1) {
                        delay(unit.toLong())
                    }
                }
                delay(farnsworthCharGap.toLong())
            }
            onComplete()
        }
    }

    fun startSidetone() {
        if (!requestFocus()) return
        gateActive.set(true)
    }

    fun stopSidetone() {
        gateActive.set(false)
    }

    fun stop() {
        playbackActive.set(false)
        gateActive.set(false)
    }

    fun generateExercise(type: ExerciseType): String {
        return when (type) {
            ExerciseType.Characters -> (1..5).map { ('A'..'Z').random() }.joinToString("")
            ExerciseType.Numbers -> (1..5).map { ('0'..'9').random() }.joinToString("")
            ExerciseType.Punctuation -> (1..5).map { ".,/?=+-()!@".random() }.joinToString("")
            ExerciseType.Mixed -> (1..5).map { (('A'..'Z') + ('0'..'9')).random() }.joinToString("")
            ExerciseType.Callsigns -> {
                val prefixes = listOf("VK", "W", "G", "F", "JA", "PY", "DL", "I", "VE", "ZL")
                val prefix = prefixes.random()
                val digit = (0..9).random()
                val suffix = (1..(2..3).random()).map { ('A'..'Z').random() }.joinToString("")
                "$prefix$digit$suffix"
            }
            ExerciseType.Words -> listOf("THE", "QUICK", "BROWN", "FOX", "JUMPS", "OVER", "LAZY", "DOG", "RADIO", "AMATEUR", "HAM", "MORSE").random()
            ExerciseType.Phrases -> listOf(
                "UR RST 599 599 BK",
                "QTH IS MELBOURNE MELBOURNE BK",
                "NAME IS BEN BEN BK",
                "HW CPY? BK",
                "TU FOR QSO 73 SK"
            ).random()
            ExerciseType.CQ -> "CQ CQ CQ DE VK2SIM VK2SIM K"
        }
    }

    fun release() {
        isRunning.set(false)
        try {
            audioThread?.join(1000)
        } catch (e: Exception) {
            Log.e(TAG, "Error joining audio thread", e)
        }
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
