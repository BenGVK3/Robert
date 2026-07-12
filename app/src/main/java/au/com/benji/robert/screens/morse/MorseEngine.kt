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

class MorseEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "MorseEngine"
    private var audioTrack: AudioTrack? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private var wpm = 20
    private var farnsworthWpm = 20
    @Volatile private var frequency = 600
    @Volatile private var sidetoneVolume = 0.8f
    @Volatile private var playbackVolume = 0.8f

    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
    
    private val isRunning = AtomicBoolean(true)
    private val sidetoneActive = AtomicBoolean(false)
    private val playbackActive = AtomicBoolean(false)
    
    @Volatile
    private var playbackQueue = mutableListOf<MorseElement>()
    private val playbackLock = Any()

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
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack?.play()
    }

    private fun startAudioThread() {
        audioThread = Thread({
            val bufferSamples = 128 // Further reduced for even lower latency
            val samples = ShortArray(bufferSamples)
            var phase = 0.0
            var currentEnvelope = 0.0f
            val attackStep = 1.0f / (sampleRate * 0.002f) // 2ms attack for snappier response
            val releaseStep = 1.0f / (sampleRate * 0.002f) // 2ms release

            while (isRunning.get()) {
                val sidetone = sidetoneActive.get()
                val playing = playbackActive.get()
                val targetEnvelope = if (sidetone || playing) 1.0f else 0.0f

                for (i in 0 until bufferSamples) {
                    // Update envelope for smooth ramp
                    if (currentEnvelope < targetEnvelope) {
                        currentEnvelope = (currentEnvelope + attackStep).coerceAtMost(targetEnvelope)
                    } else if (currentEnvelope > targetEnvelope) {
                        currentEnvelope = (currentEnvelope - releaseStep).coerceAtLeast(targetEnvelope)
                    }

                    if (currentEnvelope > 0) {
                        val angle = 2.0 * PI * phase
                        val vol = if (playing) playbackVolume else sidetoneVolume
                        samples[i] = (sin(angle) * currentEnvelope * vol * Short.MAX_VALUE).toInt().toShort()
                        
                        phase += frequency.toDouble() / sampleRate
                        if (phase > 1.0) phase -= 1.0
                    } else {
                        samples[i] = 0
                        phase = 0.0 
                    }
                }
                audioTrack?.write(samples, 0, bufferSamples)
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
            val dotDuration = 1200 / wpm
            val charSpace = 1200 / farnsworthWpm * 3
            val wordSpace = 1200 / farnsworthWpm * 7

            for (char in text.uppercase()) {
                if (!isActive) break
                if (char == ' ') {
                    delay(wordSpace.toLong())
                    continue
                }

                val code = MorseCodeMap[char] ?: continue
                for (i in code.indices) {
                    val symbol = code[i]
                    val duration = if (symbol == '.') dotDuration else dotDuration * 3
                    
                    playbackActive.set(true)
                    delay(duration.toLong())
                    playbackActive.set(false)
                    
                    if (i < code.length - 1) {
                        delay(dotDuration.toLong())
                    }
                }
                delay(charSpace.toLong())
            }
            onComplete()
        }
    }

    fun startSidetone() {
        if (!requestFocus()) return
        sidetoneActive.set(true)
    }

    fun stopSidetone() {
        sidetoneActive.set(false)
    }

    fun stop() {
        playbackActive.set(false)
        sidetoneActive.set(false)
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
            audioThread?.join(500)
        } catch (e: Exception) {
            Log.e(TAG, "Error joining audio thread", e)
        }
        audioTrack?.stop()
        audioTrack?.release()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}

sealed class MorseElement {
    data class Tone(val durationMs: Int) : MorseElement()
    data class Silence(val durationMs: Int) : MorseElement()
}
