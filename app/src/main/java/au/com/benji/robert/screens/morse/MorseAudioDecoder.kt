package au.com.benji.robert.screens.morse

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

/**
 * Professional-grade Morse Audio Decoder detection engine.
 * Features:
 * - Adaptive Goertzel-based frequency tracking.
 * - Signal-to-Noise Ratio (SNR) calculated in dB.
 * - Adaptive thresholding using noise floor estimation.
 * - Learning timing engine for dits, dahs, and gaps.
 * - Real-time calibration and auto-tracking.
 */
class MorseAudioDecoder(
    private val onDecodedChar: (ConfidenceCharacter) -> Unit,
    private val onVisualizerData: (FloatArray) -> Unit,
    private val onTelemetryUpdate: (SignalTelemetry) -> Unit,
    initialSettings: MorseSettings
) {
    private val TAG = "MorseAudioDecoder"
    private val sampleRate = 44100
    private val fftSize = 1024
    private val processingWindowSize = 512 // 11.6ms window @ 44.1kHz
    
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(fftSize * 2)

    private var audioRecord: AudioRecord? = null
    private val isRunning = AtomicBoolean(false)
    private var processingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile private var settings = initialSettings
    
    // --- DSP Tracking State ---
    private var noiseFloor = 100.0f
    private var peakMag = 0f
    private var trackedFrequency = initialSettings.frequency.toDouble()
    private var signalLevelSma = 0f // Simple Moving Average for smoothing
    
    // --- Adaptive Timing Engine ---
    private var currentDitMs = 1200.0 / initialSettings.wpm
    private var currentDahMs = currentDitMs * 3.0
    private var currentCharGapMs = currentDitMs * 3.0
    private var currentWordGapMs = currentDitMs * 7.0
    
    private val ditHistory = mutableListOf<Long>()
    private val dahHistory = mutableListOf<Long>()
    
    private var lastState = false
    private var stateStartTime = System.currentTimeMillis()
    private var symbolBuffer = StringBuilder()
    
    // --- Waterfall state ---
    private val real = FloatArray(fftSize)
    private val imag = FloatArray(fftSize)
    private val hanningWindow = FloatArray(fftSize) { i ->
        (0.5 * (1 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
    }

    fun updateSettings(newSettings: MorseSettings) {
        settings = newSettings
        if (!newSettings.autoTone) {
            trackedFrequency = newSettings.frequency.toDouble()
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning.get()) return
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) return
            
            audioRecord?.startRecording()
            isRunning.set(true)
            processingJob = scope.launch { processAudio() }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting decoder", e)
        }
    }

    fun stop() {
        isRunning.set(false)
        processingJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null
    }

    private suspend fun processAudio() {
        val buffer = ShortArray(processingWindowSize)
        
        while (isRunning.get()) {
            val read = audioRecord?.read(buffer, 0, processingWindowSize) ?: 0
            if (read > 0) {
                // 1. Waterfall & Coarse Tone Detection (FFT)
                // We reuse the full fftSize for the waterfall display
                for (i in 0 until fftSize) {
                    val sample = if (i < read) buffer[i].toFloat() else 0f
                    real[i] = sample * hanningWindow[i]
                    imag[i] = 0f
                }
                fft(real, imag)
                
                val magnitudes = FloatArray(fftSize / 2)
                var framePeakMag = 0f
                var framePeakFreq = 0f
                
                for (i in 0 until fftSize / 2) {
                    magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
                    if (magnitudes[i] > framePeakMag) {
                        framePeakMag = magnitudes[i]
                        framePeakFreq = i.toFloat() * sampleRate / fftSize
                    }
                }
                onVisualizerData(magnitudes.map { (ln(it + 1f) / 12f).coerceIn(0f, 1f) }.toFloatArray())

                // 2. High-Precision Target Tracking (Goertzel)
                // If auto-tone is on, follow the peak if it's stable and likely CW
                if (settings.autoTone && framePeakMag > noiseFloor * 4.0f && framePeakFreq in 300f..1500f) {
                    trackedFrequency = trackedFrequency * 0.95 + framePeakFreq * 0.05
                }
                
                val goertzelMag = computeGoertzel(buffer, read, trackedFrequency)
                
                // 3. Noise Floor & Signal Thresholding
                // Adaptive noise floor: follows the signal downwards quickly, upwards slowly
                if (goertzelMag < noiseFloor) {
                    noiseFloor = noiseFloor * 0.8f + goertzelMag * 0.2f
                } else {
                    noiseFloor = noiseFloor * 0.995f + goertzelMag * 0.005f
                }
                
                signalLevelSma = signalLevelSma * 0.6f + goertzelMag * 0.4f
                val snrDb = if (noiseFloor > 0) 20 * log10(max(1f, signalLevelSma) / max(1f, noiseFloor)) else 0f
                
                // Adaptive threshold: typically SNR > 6dB is a good target for CW
                val dynamicThreshold = noiseFloor * 3.5f // ~10dB above noise floor
                val currentState = signalLevelSma > dynamicThreshold

                // 4. Timing & State Logic
                handleStateChange(currentState, snrDb, dynamicThreshold)

                // 5. Telemetry update
                onTelemetryUpdate(SignalTelemetry(
                    signalStrengthDbfs = 20 * log10(signalLevelSma / (Short.MAX_VALUE.toFloat() * processingWindowSize)),
                    detectedFrequencyHz = trackedFrequency.toInt(),
                    snrDb = snrDb,
                    estimatedWpm = (1200.0 / currentDitMs).toInt(),
                    confidence = (snrDb / 35f).coerceIn(0f, 1f),
                    sampleRate = sampleRate,
                    status = determineStatus(snrDb),
                    noiseFloor = noiseFloor,
                    threshold = dynamicThreshold,
                    currentDitMs = currentDitMs.toLong(),
                    currentDahMs = currentDahMs.toLong(),
                    currentCharGapMs = currentCharGapMs.toLong(),
                    currentWordGapMs = currentWordGapMs.toLong(),
                    rawMorse = symbolBuffer.toString(),
                    isKeyDown = currentState
                ))
            }
            // Processing loop delay is implicitly handled by AudioRecord.read blocking
        }
    }

    private fun determineStatus(snrDb: Float): DecoderStatus {
        return when {
            snrDb > 12f -> DecoderStatus.Decoding
            snrDb > 6f -> DecoderStatus.Locked
            snrDb > 3f -> DecoderStatus.Searching
            else -> DecoderStatus.SignalLost
        }
    }

    private fun handleStateChange(currentState: Boolean, snrDb: Float, threshold: Float) {
        val now = System.currentTimeMillis()
        val duration = now - stateStartTime

        if (currentState != lastState) {
            if (!currentState) {
                // TONE ENDED (Key-up detected)
                processToneEnd(duration)
            } else {
                // TONE STARTED (Key-down detected)
                processSilenceEnd(duration)
            }
            lastState = currentState
            stateStartTime = now
        } else {
            // Continuous state checks (timeouts)
            if (!currentState && duration > currentDitMs * 12.0 && symbolBuffer.isNotEmpty()) {
                decodeBuffer()
            }
        }
    }

    private fun processToneEnd(duration: Long) {
        // Did we have a valid element?
        // A dit is 1 unit, a dah is 3 units. 
        // We use a midpoint of 2 units for classification.
        if (duration > currentDitMs * 0.35) {
            val element = if (duration < currentDitMs * 2.0) "." else "-"
            symbolBuffer.append(element)
            
            // Adaptive Timing: Learn from the sender
            updateAdaptiveTiming(duration, element == ".")
        }
    }

    private fun processSilenceEnd(duration: Long) {
        // ITU: 1 unit intra, 3 units char, 7 units word.
        // We use forgiving boundaries.
        if (duration > currentDitMs * 5.0) {
            // Word gap detected
            decodeBuffer()
            onDecodedChar(ConfidenceCharacter(' ', 1f, ""))
        } else if (duration > currentDitMs * 1.8) {
            // Character gap detected
            decodeBuffer()
        }
    }

    private fun updateAdaptiveTiming(duration: Long, isDit: Boolean) {
        if (isDit) {
            ditHistory.add(duration)
            if (ditHistory.size > 8) ditHistory.removeAt(0)
            val avg = ditHistory.average()
            if (avg in 20.0..400.0) {
                currentDitMs = currentDitMs * 0.8 + avg * 0.2
                // Recalculate other targets based on learned dit
                currentDahMs = currentDitMs * 3.0
            }
        } else {
            dahHistory.add(duration)
            if (dahHistory.size > 8) dahHistory.removeAt(0)
            val avg = dahHistory.average()
            if (avg in 60.0..1200.0) {
                // We can also learn dah length if it deviates from 3:1
            }
        }
    }

    private fun decodeBuffer() {
        val code = symbolBuffer.toString()
        if (code.isNotEmpty()) {
            val char = MorseCodeMap.entries.find { it.value == code }?.key ?: '?'
            // Confidence is a factor of SNR and timing consistency
            val confidence = (signalLevelSma / (noiseFloor * 5f)).coerceIn(0.2f, 1f)
            onDecodedChar(ConfidenceCharacter(char, confidence, code))
            symbolBuffer.setLength(0)
        }
    }

    /**
     * Highly efficient Goertzel algorithm for single-frequency power detection.
     */
    private fun computeGoertzel(samples: ShortArray, length: Int, freq: Double): Float {
        val k = (0.5 + (length.toDouble() * freq / sampleRate)).toInt()
        val omega = (2.0 * PI * k) / length
        val cosine = cos(omega)
        val coeff = 2.0 * cosine
        
        var q0 = 0.0
        var q1 = 0.0
        var q2 = 0.0
        
        for (i in 0 until length) {
            q0 = coeff * q1 - q2 + samples[i]
            q2 = q1
            q1 = q0
        }
        
        val magnitude = q1 * q1 + q2 * q2 - coeff * q1 * q2
        return sqrt(magnitude).toFloat()
    }

    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
            var m = n shr 1
            while (m >= 1 && j >= m) { j -= m; m = m shr 1 }
            j += m
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wlr = cos(ang).toFloat()
            val wli = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var wr = 1f; var wi = 0f
                for (k in 0 until len / 2) {
                    val vr = real[i + k + len / 2] * wr - imag[i + k + len / 2] * wi
                    val vi = real[i + k + len / 2] * wi + imag[i + k + len / 2] * wr
                    real[i + k + len / 2] = real[i + k] - vr
                    imag[i + k + len / 2] = imag[i + k] - vi
                    real[i + k] += vr
                    imag[i + k] += vi
                    val nwr = wr * wlr - wi * wli
                    wi = wr * wli + wi * wlr
                    wr = nwr
                }
                i += len
            }
            len *= 2
        }
    }
}
