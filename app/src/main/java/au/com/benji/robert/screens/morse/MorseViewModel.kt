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

    private val _receiveStats = MutableStateFlow(ReceiveStats())
    val receiveStats = _receiveStats.asStateFlow()

    private val _receiveFeedback = MutableStateFlow<ReceiveFeedback?>(null)
    val receiveFeedback = _receiveFeedback.asStateFlow()

    private val _currentExerciseType = MutableStateFlow(ExerciseType.Beginner)
    val currentExerciseType = _currentExerciseType.asStateFlow()

    private val _isDecoding = MutableStateFlow(false)
    val isDecoding = _isDecoding.asStateFlow()

    private val _trainerMode = MutableStateFlow<TrainerMode?>(null)
    val trainerMode = _trainerMode.asStateFlow()

    private var decodeJob: Job? = null
    private val lastElementTime = AtomicLong(0)

    private val kochOrder = "KMRSUAPTLOWI.NJEF0Y,VG5/Q9ZH38B?427C1D6XBT".toList()

    init {
        // Initial settings propagation
        keyer.updateSettings(_settings.value)
        
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
        _trainerMode.value = null
    }

    fun setTrainerMode(mode: TrainerMode) {
        _trainerMode.value = mode
        if (mode == TrainerMode.Koch) {
            startKochLesson(_progress.value.currentLessonIndex)
        } else {
            generateTrainerExercise(mode)
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

    // --- Receiving Trainer ---

    fun generateNewExercise(type: ExerciseType) {
        _currentExerciseType.value = type
        val text = generateExercise(type)
        _currentText.value = text
        _receiveFeedback.value = null
        playText(text)
    }

    fun checkReceiveAnswer(answer: String) {
        val expected = _currentText.value.trim().uppercase()
        val received = answer.trim().uppercase()
        
        val comparison = mutableListOf<Pair<Char, Boolean>>()
        val maxLen = maxOf(expected.length, received.length)
        
        for (i in 0 until maxLen) {
            val e = expected.getOrNull(i)
            val r = received.getOrNull(i)
            comparison.add((e ?: ' ') to (e == r))
        }
        
        val isCorrect = expected == received
        _receiveFeedback.value = ReceiveFeedback(isCorrect, expected, received, comparison)
        
        updateReceiveStats(isCorrect, expected)
    }

    private fun updateReceiveStats(isCorrect: Boolean, expected: String) {
        val stats = _receiveStats.value
        val newCorrect = if (isCorrect) stats.correctCount + 1 else stats.correctCount
        val newTotal = stats.totalCount + 1
        val newStreak = if (isCorrect) stats.currentStreak + 1 else 0
        val newLongest = maxOf(stats.longestStreak, newStreak)
        
        // Update character-level stats in progress
        val p = _progress.value
        val newCharStats = p.characterStats.toMutableMap()
        expected.forEach { char ->
            val current = newCharStats[char] ?: CharacterStat(char)
            newCharStats[char] = current.copy(
                correctCount = if (isCorrect) current.correctCount + 1 else current.correctCount,
                totalCount = current.totalCount + 1,
                lastPracticed = System.currentTimeMillis()
            )
        }

        _receiveStats.value = stats.copy(
            correctCount = newCorrect,
            totalCount = newTotal,
            currentStreak = newStreak,
            longestStreak = newLongest,
            charactersPracticed = stats.charactersPracticed + expected.length
        )

        _progress.value = p.copy(
            characterStats = newCharStats,
            totalAccuracy = (newCorrect.toFloat() / newTotal) * 100f,
            totalCharactersCopied = p.totalCharactersCopied + expected.length
        )
    }

    // --- Primary Practice Generator ---

    private fun generateExercise(type: ExerciseType): String {
        return when (type) {
            ExerciseType.Beginner -> listOf("E", "T", "A", "N", "I", "M", "S", "O").random()
            ExerciseType.Intermediate -> (1..3).map { (('A'..'Z') + ('0'..'9')).random() }.joinToString("")
            ExerciseType.Advanced -> listOf("THE", "AND", "CQ", "TEST", "BK", "AR", "SK", "KN", "73", "599").random()
            ExerciseType.Expert -> {
                val call = generateCallsign()
                if ((0..1).random() == 0) "CQ CQ DE $call K" else call
            }
            else -> "TEST"
        }
    }

    private fun generateCallsign(): String {
        val prefixes = listOf("VK", "W", "G", "F", "JA", "PY", "DL", "I", "VE", "ZL")
        val suffix = (1..(2..3).random()).map { ('A'..'Z').random() }.joinToString("")
        return "${prefixes.random()}${(1..9).random()}$suffix"
    }

    // --- Advanced Trainer (Koch / Specialized) ---

    fun startKochLesson(index: Int) {
        val learnedChars = kochOrder.take(index + 2)
        _trainerTarget.value = (1..5).map { learnedChars.random() }.joinToString("")
        _decodedText.value = ""
        _trainerFeedback.value = null
        playText(_trainerTarget.value)
    }

    private fun generateTrainerExercise(mode: TrainerMode) {
        val target = when (mode) {
            TrainerMode.Numbers -> (1..5).map { ('0'..'9').random() }.joinToString("")
            TrainerMode.Punctuation -> (1..5).map { ".,/?=".random() }.joinToString("")
            TrainerMode.Callsigns -> generateCallsign()
            TrainerMode.Words -> listOf("THE", "QUICK", "BROWN", "FOX", "JUMPS", "OVER").random()
            TrainerMode.AmateurRadio -> listOf("CQ CQ DE VK3ESE", "UR RST 599", "73 SK").random()
            TrainerMode.WeakReview -> generateWeakReviewExercise()
            else -> "TEST"
        }
        _trainerTarget.value = target
        _decodedText.value = ""
        _trainerFeedback.value = null
        playText(target)
    }

    private fun generateWeakReviewExercise(): String {
        val weakOnes = _progress.value.characterStats.values
            .filter { it.totalCount > 5 && it.accuracy < 80f }
            .sortedBy { it.accuracy }
            .take(5)
            .map { it.char }
        
        return if (weakOnes.isEmpty()) "PAR" else (1..5).map { weakOnes.random() }.joinToString("")
    }

    fun resetTrainerExercise() {
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        _trainerFeedback.value = null
        decodeJob?.cancel()
        playText(_trainerTarget.value)
    }

    fun clearSentText() {
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        decodeJob?.cancel()
    }

    fun submitSentText() {
        val text = _decodedText.value.trim()
        if (text.isNotEmpty()) {
            if (_currentSection.value == MorseSection.Simulator) {
                val currentMessages = _messages.value.toMutableList()
                currentMessages.add("You" to text)
                _messages.value = currentMessages

                viewModelScope.launch {
                    delay(1000)
                    val responses = listOf("R R", "73", "GM", "GA", "UR RST 599", "TU", "FB")
                    val responseMessages = _messages.value.toMutableList()
                    responseMessages.add("Simulator" to responses.random())
                    _messages.value = responseMessages
                }
            }
            _decodedText.value = ""
            _currentSymbolBuffer.value = ""
            decodeJob?.cancel()
        }
    }

    fun toggleDecoding() {
        _isDecoding.value = !_isDecoding.value
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

            if (isCorrect && _trainerMode.value == TrainerMode.Koch) {
                handleKochSuccess()
            }
        }
    }

    private fun handleKochSuccess() {
        val p = _progress.value
        val newCorrectCount = (p.characterStats['K']?.correctCount ?: 0) + 1 // Placeholder simplified logic
        if (p.totalAccuracy > 90f && p.totalCharactersCopied > 50) {
            _progress.value = p.copy(currentLessonIndex = p.currentLessonIndex + 1)
        }
        
        viewModelScope.launch {
            delay(1500)
            startKochLesson(_progress.value.currentLessonIndex)
        }
    }

    // --- Keyer & Playback ---

    fun playText(text: String) {
        keyer.playText(text)
    }

    fun stopPlayback() {
        keyer.stopPlayback()
    }

    fun onKeyDown(isRightPaddle: Boolean) {
        val config = _settings.value
        val isDash = if (config.paddleOrientation == PaddleOrientation.LeftDitRightDah) isRightPaddle else !isRightPaddle
        if (config.keyerMode == KeyerMode.Straight) keyer.onStraightKey(true) 
        else if (isDash) keyer.onDashPaddle(true) else keyer.onDotPaddle(true)
    }

    fun onKeyUp(isRightPaddle: Boolean) {
        val config = _settings.value
        val isDash = if (config.paddleOrientation == PaddleOrientation.LeftDitRightDah) isRightPaddle else !isRightPaddle
        if (config.keyerMode == KeyerMode.Straight) keyer.onStraightKey(false) 
        else if (isDash) keyer.onDashPaddle(false) else keyer.onDotPaddle(false)
    }

    private fun handleElementSent(element: String) {
        decodeJob?.cancel()
        _lastElementSent.value = element
        _currentSymbolBuffer.value += element
    }

    private fun startDecodingTimer() {
        decodeJob?.cancel()
        decodeJob = viewModelScope.launch {
            val settings = _settings.value
            val unitMs = 1200 / settings.farnsworthWpm
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
            
            delay((unitMs * 4.0).toLong())
            if (_decodedText.value.isNotEmpty() && !_decodedText.value.endsWith(" ")) {
                _decodedText.value += " "
            }
        }
    }

    override fun onCleared() {
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
