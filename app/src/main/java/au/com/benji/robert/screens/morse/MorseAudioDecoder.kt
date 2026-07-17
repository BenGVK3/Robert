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
 * Advanced Morse Audio Decoder.
 * Uses adaptive thresholding and frequency isolation to decode Morse from audio.
 */
class MorseAudioDecoder(
    private val onDecodedChar: (Char) -> Unit,
    private val onVisualizerData: (FloatArray) -> Unit,
    initialWpm: Int = 20
) {
    private val TAG = "MorseAudioDecoder"
    private val sampleRate = 44100
    private val fftSize = 1024
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(fftSize * 2)

    private var audioRecord: AudioRecord? = null
    private val isRunning = AtomicBoolean(false)
    private var processingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Signal processing state
    private var noiseFloor = 1000.0f
    private var signalThreshold = 5000.0f
    private var lastState = false
    private var stateStartTime = System.currentTimeMillis()
    private var symbolBuffer = StringBuilder()
    
    // Auto-speed tracking (WPM)
    private var currentUnitMs = 1200.0 / initialWpm
    private val historyLength = 8
    private val ditHistory = mutableListOf<Long>()

    fun updateWpm(wpm: Int) {
        currentUnitMs = 1200.0 / wpm.coerceAtLeast(5)
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
        val buffer = ShortArray(fftSize)
        val real = FloatArray(fftSize)
        val imag = FloatArray(fftSize)
        
        while (isRunning.get()) {
            val read = audioRecord?.read(buffer, 0, fftSize) ?: 0
            if (read > 0) {
                // 1. Prepare for FFT
                for (i in 0 until fftSize) {
                    real[i] = if (i < read) buffer[i].toFloat() else 0f
                    imag[i] = 0f
                }

                // 2. Windowing (Hanning) to reduce leakage
                for (i in 0 until fftSize) {
                    val window = 0.5 * (1 - cos(2.0 * PI * i / (fftSize - 1)))
                    real[i] *= window.toFloat()
                }

                fft(real, imag)

                // 3. Magnitude & Spectrum for UI
                val magnitudes = FloatArray(fftSize / 2)
                var peakMag = 0f
                for (i in 0 until fftSize / 2) {
                    magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
                    if (magnitudes[i] > peakMag) peakMag = magnitudes[i]
                }

                // Smooth mapping to 64 visualizer bins (standard spectrum feel)
                val visBins = FloatArray(64)
                val step = (fftSize / 2) / 64
                for (i in 0 until 64) {
                    var maxInBin = 0f
                    for (j in 0 until step) {
                        maxInBin = max(maxInBin, magnitudes[i * step + j])
                    }
                    // Logarithmic-like scaling for the "analyzer" look
                    visBins[i] = (ln(maxInBin + 1f) / 12f).coerceIn(0.05f, 1f)
                }
                onVisualizerData(visBins)

                // 4. Adaptive Signal Detection
                noiseFloor = noiseFloor * 0.98f + peakMag * 0.02f
                signalThreshold = max(noiseFloor * 3.5f, 8000.0f)
                
                handleStateChange(peakMag > signalThreshold)
            }
            delay(16) // ~60fps processing
        }
    }

    private fun handleStateChange(currentState: Boolean) {
        val now = System.currentTimeMillis()
        val duration = now - stateStartTime

        if (currentState != lastState) {
            if (!currentState) {
                // Signal Ended: Dit or Dah?
                if (duration > currentUnitMs * 0.3) {
                    val element = if (duration < currentUnitMs * 2.0) "." else "-"
                    symbolBuffer.append(element)
                    
                    // Adaptive Speed: If it's a dit, track it
                    if (element == ".") {
                        ditHistory.add(duration)
                        if (ditHistory.size > historyLength) ditHistory.removeAt(0)
                        val avgDit = ditHistory.average()
                        if (avgDit > 20) currentUnitMs = currentUnitMs * 0.8 + avgDit * 0.2
                    }
                }
            } else {
                // Signal Started: Letter or Word Gap?
                if (duration > currentUnitMs * 5.0) {
                    decodeBuffer()
                    onDecodedChar(' ')
                } else if (duration > currentUnitMs * 1.8) {
                    decodeBuffer()
                }
            }
            lastState = currentState
            stateStartTime = now
        } else {
            // Timeout to force decode last char
            if (!currentState && duration > currentUnitMs * 10.0 && symbolBuffer.isNotEmpty()) {
                decodeBuffer()
            }
        }
    }

    private fun decodeBuffer() {
        val code = symbolBuffer.toString()
        if (code.isNotEmpty()) {
            val char = MorseCodeMap.entries.find { it.value == code }?.key
            if (char != null) onDecodedChar(char)
            symbolBuffer.setLength(0)
        }
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
