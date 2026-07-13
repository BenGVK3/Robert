package au.com.benji.robert.screens.morse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class MorseViewModel(application: Application) : AndroidViewModel(application) {
    
    private val keyer = MorseKeyer(
        context = application,
        onElementSent = { element ->
            viewModelScope.launch(Dispatchers.Main) {
                handleElementSent(element)
            }
        },
        onCharacterComplete = {
            // Future auto-spacing logic
        },
        onToneStateChanged = { active ->
            viewModelScope.launch(Dispatchers.Main) {
                _isKeyBusy.value = active
                if (!active) {
                    _lastElementSent.value = ""
                    // Start decoding timer only when the engine becomes idle
                    if (_currentSymbolBuffer.value.isNotEmpty()) {
                        startDecodingTimer()
                    }
                } else {
                    // Cancel any pending decode if a new press starts
                    decodeJob?.cancel()
                }
            }
        }
    )

    private val _settings = MutableStateFlow(MorseSettings())
    val settings = _settings.asStateFlow()

    private val _currentSection = MutableStateFlow(MorseSection.Menu)
    val currentSection = _currentSection.asStateFlow()

    private val _diagnostics = MutableStateFlow<List<KeyerDiagnosticEntry>>(emptyList())
    val diagnostics = _diagnostics.asStateFlow()

    private val _latencyLog = MutableStateFlow<List<String>>(emptyList())
    val latencyLog = _latencyLog.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentText = MutableStateFlow("")
    val currentText = _currentText.asStateFlow()

    private val _decodedText = MutableStateFlow("")
    val decodedText = _decodedText.asStateFlow()

    private val _progress = MutableStateFlow(MorseProgress())
    val progress = _progress.asStateFlow()

    private val _currentSymbolBuffer = MutableStateFlow("")
    val currentSymbolBuffer = _currentSymbolBuffer.asStateFlow()

    private val _isKeyBusy = MutableStateFlow(false)
    val isKeyBusy = _isKeyBusy.asStateFlow()

    private val _lastElementSent = MutableStateFlow("")
    val lastElementSent = _lastElementSent.asStateFlow()

    private val _trainerTarget = MutableStateFlow("")
    val trainerTarget = _trainerTarget.asStateFlow()

    private val _trainerFeedback = MutableStateFlow<TrainerFeedback?>(null)
    val trainerFeedback = _trainerFeedback.asStateFlow()

    private val _messages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isDecoding = MutableStateFlow(false)
    val isDecoding = _isDecoding.asStateFlow()

    private var decodeJob: Job? = null
    private val lastElementTime = AtomicLong(0)

    private val lessons = listOf(
        "K M", "K M R", "K M R S", "K M R S U", "K M R S U A", "K M R S U A P",
        "K M R S U A P T", "K M R S U A P T L", "K M R S U A P T L O", "K M R S U A P T L O W"
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                _isPlaying.value = keyer.isPlaybackActive()
                delay(100)
            }
        }
    }

    fun setSection(section: MorseSection) {
        _currentSection.value = section
        stopAll()
        if (section == MorseSection.Trainer) {
            startLesson(_progress.value.currentLessonIndex)
        } else if (section == MorseSection.Simulator) {
            startSimulator()
        }
    }

    private fun stopAll() {
        keyer.stopPlayback()
        decodeJob?.cancel()
        _isPlaying.value = false
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        _trainerFeedback.value = null
        _isKeyBusy.value = false
        _lastElementSent.value = ""
    }

    fun updateSettings(newSettings: MorseSettings) {
        _settings.value = newSettings
        keyer.updateSettings(newSettings)
    }

    fun generateNewExercise(type: ExerciseType) {
        val text = generateExercise(type)
        _currentText.value = text
        playText(text)
    }

    fun playText(text: String) {
        keyer.playText(text)
    }

    fun stopPlayback() {
        keyer.stopPlayback()
    }

    // --- Keyer Events ---

    fun onKeyDown(isRightPaddle: Boolean) {
        val config = _settings.value
        val isDash = if (config.paddleOrientation == PaddleOrientation.LeftDitRightDah) isRightPaddle else !isRightPaddle

        if (config.keyerMode == KeyerMode.Straight) {
            keyer.onStraightKey(true)
        } else {
            if (isDash) keyer.onDashPaddle(true) else keyer.onDotPaddle(true)
        }
    }

    fun onKeyUp(isRightPaddle: Boolean) {
        val config = _settings.value
        val isDash = if (config.paddleOrientation == PaddleOrientation.LeftDitRightDah) isRightPaddle else !isRightPaddle

        if (config.keyerMode == KeyerMode.Straight) {
            keyer.onStraightKey(false)
        } else {
            if (isDash) keyer.onDashPaddle(false) else keyer.onDotPaddle(false)
        }
    }

    private fun handleElementSent(element: String) {
        decodeJob?.cancel() // Immediate cancellation
        _lastElementSent.value = element
        _currentSymbolBuffer.value += element
        lastElementTime.set(System.currentTimeMillis())
    }

    private fun startDecodingTimer() {
        decodeJob?.cancel()
        decodeJob = viewModelScope.launch {
            val settings = _settings.value
            val unitMs = 1200 / settings.farnsworthWpm
            
            // 1. Wait for Inter-Character Gap (3 units total)
            // For Iambic: 1 unit is already consumed by the engine's mandatory gap. Wait 2 more.
            // For Straight: Tone just ended. Wait 3 full units.
            val remainingCharGap = if (settings.keyerMode == KeyerMode.Straight) 3.0 else 2.0
            delay((unitMs * remainingCharGap).toLong())
            
            val buffer = _currentSymbolBuffer.value
            if (buffer.isNotEmpty()) {
                val char = MorseCodeMap.entries.find { it.value == buffer }?.key
                if (char != null) {
                    _decodedText.value += char
                    if (_currentSection.value == MorseSection.Trainer) checkTrainerProgress()
                } else {
                    _decodedText.value += "?"
                }
                _currentSymbolBuffer.value = ""
            }
            
            // 2. Wait for Inter-Word Gap (7 units total, 4 more from character gap)
            delay((unitMs * 4.0).toLong())
            if (_decodedText.value.isNotEmpty() && !_decodedText.value.endsWith(" ")) {
                _decodedText.value += " "
            }
        }
    }

    // --- Utility ---

    private fun generateExercise(type: ExerciseType): String {
        return when (type) {
            ExerciseType.Characters -> (1..5).map { ('A'..'Z').random() }.joinToString("")
            ExerciseType.Numbers -> (1..5).map { ('0'..'9').random() }.joinToString("")
            ExerciseType.Punctuation -> (1..5).map { ".,/?=+-()!@".random() }.joinToString("")
            ExerciseType.Mixed -> (1..5).map { (('A'..'Z') + ('0'..'9')).random() }.joinToString("")
            ExerciseType.Callsigns -> {
                val prefixes = listOf("VK", "W", "G", "F", "JA", "PY", "DL", "I", "VE", "ZL")
                val suffix = (1..(2..3).random()).map { ('A'..'Z').random() }.joinToString("")
                "${prefixes.random()}${(0..9).random()}$suffix"
            }
            ExerciseType.Words -> listOf("THE", "QUICK", "BROWN", "FOX", "JUMPS", "OVER", "LAZY", "DOG").random()
            ExerciseType.Phrases -> listOf("UR RST 599", "QTH MELBOURNE", "NAME BEN", "HW CPY", "73 SK").random()
            ExerciseType.CQ -> "CQ CQ CQ DE VK2SIM K"
        }
    }

    // --- Trainer ---

    fun startLesson(index: Int) {
        if (index in lessons.indices) {
            val lessonContent = lessons[index]
            _progress.value = _progress.value.copy(currentLessonIndex = index)
            val chars = lessonContent.split(" ")
            _trainerTarget.value = (1..3).map { chars.random() }.joinToString("")
            _decodedText.value = ""
            _trainerFeedback.value = null
        }
    }

    fun resetTrainerExercise() {
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        _trainerFeedback.value = null
        decodeJob?.cancel()
    }

    private fun checkTrainerProgress() {
        val current = _decodedText.value.trim().uppercase()
        val target = _trainerTarget.value.uppercase()
        
        if (current.length >= target.length) {
            val isCorrect = current == target
            _trainerFeedback.value = TrainerFeedback(
                isCorrect = isCorrect,
                expected = target,
                received = current,
                expectedCode = target.map { MorseCodeMap[it] ?: "" }.joinToString(" "),
                receivedCode = current.map { MorseCodeMap[it] ?: "" }.joinToString(" "),
                message = if (isCorrect) "EXCELLENT!" else "ERROR DETECTED"
            )

            if (isCorrect) {
                updateProgress(true)
                viewModelScope.launch {
                    delay(1500)
                    startLesson(_progress.value.currentLessonIndex)
                }
            } else {
                updateProgress(false)
            }
        }
    }

    private fun updateProgress(success: Boolean) {
        val p = _progress.value
        val newAttempts = p.attempts + 1
        val newMastered = p.charactersMastered.toMutableSet()
        if (success) _trainerTarget.value.forEach { newMastered.add(it) }
        
        val newAccuracy = (p.totalAccuracy * (newAttempts - 1) + (if (success) 100f else 0f)) / newAttempts
        var newLessonsCompleted = p.lessonsCompleted
        if (newAccuracy > 90f && p.currentLessonIndex == p.lessonsCompleted && newAttempts % 5 == 0) {
            newLessonsCompleted++
        }

        _progress.value = p.copy(
            attempts = newAttempts,
            charactersMastered = newMastered,
            totalAccuracy = newAccuracy,
            lessonsCompleted = newLessonsCompleted
        )
    }

    // --- Simulator ---

    private var simulatorOp: String? = null
    private var simStage = 0

    fun startSimulator() {
        _messages.value = emptyList()
        simStage = 1
        simulatorOp = generateExercise(ExerciseType.Callsigns)
        val initialMsg = "CQ CQ CQ DE $simulatorOp $simulatorOp K"
        _messages.value += (simulatorOp!! to initialMsg)
        playText(initialMsg)
    }

    fun submitSentText() {
        if (_decodedText.value.isNotEmpty()) {
            val userText = _decodedText.value.trim().uppercase()
            _messages.value += ("You" to userText)
            _decodedText.value = ""
            processSimulatorResponse()
        }
    }

    private fun processSimulatorResponse() {
        val op = simulatorOp ?: return
        viewModelScope.launch {
            delay(1500)
            val response = when (simStage) {
                1 -> { simStage = 2; "TU UR 599 BK" }
                2 -> { simStage = 3; "FB 73 SK" }
                else -> "QRZ?"
            }
            _messages.value += (op to response)
            playText(response)
        }
    }

    fun clearSentText() {
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        decodeJob?.cancel()
    }

    fun toggleDecoding() {
        _isDecoding.value = !_isDecoding.value
    }

    override fun onCleared() {
        super.onCleared()
        keyer.release()
    }
}

data class TrainerFeedback(
    val isCorrect: Boolean,
    val expected: String,
    val received: String,
    val expectedCode: String,
    val receivedCode: String,
    val message: String
)
