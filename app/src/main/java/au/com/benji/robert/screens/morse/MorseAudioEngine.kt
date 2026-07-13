package au.com.benji.robert.screens.morse

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

/**
 * High-performance audio engine for Morse sidetone generation.
 * Uses a dedicated high-priority thread and AudioTrack in low-latency mode.
 */
class MorseAudioEngine(context: Context) {
    private val TAG = "MorseAudioEngine"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioTrack: AudioTrack? = null

    @Volatile private var frequency = 600
    @Volatile private var volume = 0.8f
    
    private val sampleRate = 44100
    private val bufferSamples = 64 // Small buffer for low latency
    
    private val isRunning = AtomicBoolean(true)
    private val toneActive = AtomicBoolean(false)
    
    private var audioThread: Thread? = null

    init {
        initAudio()
        startAudioThread()
    }

    private fun initAudio() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate, 
            AudioFormat.CHANNEL_OUT_MONO, 
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        }
        
        audioTrack = builder.build()
        audioTrack?.play()
    }

    private fun startAudioThread() {
        audioThread = Thread({
            val samples = ShortArray(bufferSamples)
            var phase = 0.0
            var currentEnvelope = 0.0f
            
            // 2ms ramps to eliminate clicks while remaining "instant"
            val rampTimeSeconds = 0.002f
            val envelopeStep = 1.0f / (sampleRate * rampTimeSeconds)

            while (isRunning.get()) {
                val isActive = toneActive.get()
                val targetEnvelope = if (isActive) 1.0f else 0.0f
                
                val freq = frequency.toDouble()
                val vol = volume
                val maxAmp = (vol * Short.MAX_VALUE).toDouble()
                val phaseStep = freq / sampleRate

                for (i in 0 until bufferSamples) {
                    // Smooth linear amplitude ramp
                    if (currentEnvelope < targetEnvelope) {
                        currentEnvelope = (currentEnvelope + envelopeStep).coerceAtMost(targetEnvelope)
                    } else if (currentEnvelope > targetEnvelope) {
                        currentEnvelope = (currentEnvelope - envelopeStep).coerceAtLeast(targetEnvelope)
                    }

                    if (currentEnvelope > 0) {
                        samples[i] = (sin(2.0 * PI * phase) * currentEnvelope * maxAmp).toInt().toShort()
                    } else {
                        samples[i] = 0
                    }

                    phase += phaseStep
                    if (phase >= 1.0) phase -= 1.0
                }
                
                audioTrack?.write(samples, 0, bufferSamples, AudioTrack.WRITE_BLOCKING)
            }
        }, "MorseAudioThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun setToneActive(active: Boolean) {
        toneActive.set(active)
    }

    fun updateConfig(frequency: Int, volume: Float) {
        this.frequency = frequency
        this.volume = volume
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
        audioTrack = null
    }
}
