package au.com.benji.robert.screens.morse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.repository.MorseRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicLong

class MorseViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = MorseRepository(application)
    
    private val keyer = MorseKeyer(
        context = application,
        onElementSent = { element ->
            viewModelScope.launch(Dispatchers.Main) {
                handleElementSent(element)
            }
        },
        onCharacterComplete = { },
        onToneStateChanged = { active ->
            viewModelScope.launch(Dispatchers.Main) {
                _isKeyBusy.value = active
                if (!active) {
                    _lastElementSent.value = ""
                    if (_currentSymbolBuffer.value.isNotEmpty()) {
                        startDecodingTimer()
                    }
                } else {
                    decodeJob?.cancel()
                }
            }
        }
    )

    private val _settings = MutableStateFlow(MorseSettings())
    val settings = _settings.asStateFlow()

    private val _currentSection = MutableStateFlow(MorseSection.Menu)
    val currentSection = _currentSection.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentText = MutableStateFlow("")
    val currentText = _currentText.asStateFlow()

    private val _decodedText = MutableStateFlow("")
    val decodedText = _decodedText.asStateFlow()

    private val _progress = MutableStateFlow(MorseProgress())
    val progress = _progress.asStateFlow()

    private val _trainerSession = MutableStateFlow(TrainerSessionProgress())
    val trainerSession = _trainerSession.asStateFlow()

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
    private val kochOrder = "KMRSUAPTLOWI.NJEF0Y,VG5/Q9ZH38B?427C1D6XBT".toList()

    init {
        viewModelScope.launch {
            repository.settings.collectLatest { 
                _settings.value = it
                keyer.updateSettings(it)
            }
        }
        viewModelScope.launch {
            repository.progress.collectLatest { 
                _progress.value = it
                _trainerSession.value = _trainerSession.value.copy(lessonNumber = it.currentLessonIndex + 1)
            }
        }
        
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
        _trainerFeedback.value = null
        _decodedText.value = ""
        keyer.stopPlayback() // Ensure NO autoplay when switching modes
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
        viewModelScope.launch { repository.saveSettings(newSettings) }
    }

    // --- Training Logic ---

    fun generateNewExercise(type: ExerciseType) {
        _currentExerciseType.value = type
        _currentText.value = generateExercise(type)
        _receiveFeedback.value = null
        keyer.stopPlayback() // Never autoplay
    }

    fun playCurrentText() {
        if (_currentSection.value == MorseSection.Receive) {
            playText(_currentText.value)
        } else {
            playText(_trainerTarget.value)
        }
    }

    fun checkReceiveAnswer(answer: String) {
        val expected = _currentText.value.trim().uppercase()
        val received = answer.trim().uppercase()
        val isCorrect = expected == received
        
        val comparison = mutableListOf<Pair<Char, Boolean>>()
        for (i in 0 until maxOf(expected.length, received.length)) {
            val e = expected.getOrNull(i)
            val r = received.getOrNull(i)
            comparison.add((e ?: ' ') to (e == r))
        }
        
        _receiveFeedback.value = ReceiveFeedback(isCorrect, expected, received, comparison)
        updateGlobalStats(isCorrect, expected)
    }

    private fun updateGlobalStats(isCorrect: Boolean, text: String) {
        val p = _progress.value
        val newCharStats = p.characterStats.toMutableMap()
        text.forEach { char ->
            val current = newCharStats[char] ?: CharacterStat(char)
            newCharStats[char] = current.copy(
                correctCount = if (isCorrect) current.correctCount + 1 else current.correctCount,
                totalCount = current.totalCount + 1,
                lastPracticed = System.currentTimeMillis()
            )
        }

        val newProgress = p.copy(
            totalCharactersCopied = p.totalCharactersCopied + text.length,
            characterStats = newCharStats,
            practiceStreak = if (isCorrect) p.practiceStreak + 1 else 0,
            longestStreak = maxOf(p.longestStreak, if (isCorrect) p.practiceStreak + 1 else 0),
            totalAccuracy = if (p.totalCharactersCopied == 0) (if (isCorrect) 100f else 0f) 
                           else (p.totalAccuracy * 0.95f) + (if (isCorrect) 5f else 0f)
        )
        _progress.value = newProgress
        viewModelScope.launch { repository.saveProgress(newProgress) }
    }

    // --- Koch Redesign ---

    fun startKochLesson(index: Int) {
        _trainerSession.value = TrainerSessionProgress(
            lessonNumber = index + 1,
            startTime = System.currentTimeMillis()
        )
        generateKochExercise(index)
    }

    private fun generateKochExercise(index: Int) {
        val learnedChars = kochOrder.take(index + 2)
        val newestChar = learnedChars.last()
        
        // Weighted selection: 40% chance for the newest character, others distributed
        val exercise = (1..(1..3).random()).map {
            if (Math.random() < 0.4) newestChar else learnedChars.random()
        }.joinToString("")
        
        _trainerTarget.value = exercise
        _decodedText.value = ""
        _trainerFeedback.value = null
        keyer.stopPlayback() // NO autoplay
    }

    private fun generateTrainerExercise(mode: TrainerMode) {
        val target = when (mode) {
            TrainerMode.Numbers -> (1..5).map { ('0'..'9').random() }.joinToString("")
            TrainerMode.Punctuation -> (1..3).map { ".,/?=".random() }.joinToString("")
            TrainerMode.Callsigns -> generateCallsign()
            TrainerMode.Words -> listOf("THE", "AND", "BK", "CQ", "DX", "73").random()
            else -> (1..3).map { kochOrder.random() }.joinToString("")
        }
        _trainerTarget.value = target
        _decodedText.value = ""
        _trainerFeedback.value = null
        keyer.stopPlayback() // NO autoplay
    }

    fun submitTrainerAnswer() {
        val current = _decodedText.value.trim().uppercase()
        val target = _trainerTarget.value.uppercase()
        val isCorrect = current == target
        
        _trainerFeedback.value = TrainerFeedback(
            isCorrect = isCorrect,
            expected = target,
            received = current,
            expectedCode = target.map { MorseCodeMap[it] ?: "" }.joinToString(" "),
            receivedCode = current.map { MorseCodeMap[it] ?: "" }.joinToString(" "),
            message = if (isCorrect) "✓ Correct!" else "✗ Incorrect"
        )

        if (_trainerMode.value == TrainerMode.Koch) {
            updateKochProgress(isCorrect)
        }
        updateGlobalStats(isCorrect, target)
    }

    private fun updateKochProgress(isCorrect: Boolean) {
        val session = _trainerSession.value
        val newRecent = (session.recentResults + isCorrect).takeLast(20)
        val currentAccuracy = if (newRecent.isNotEmpty()) (newRecent.count { it } * 100f / newRecent.size) else 0f
        
        val newSession = session.copy(
            currentCorrect = if (isCorrect) session.currentCorrect + 1 else session.currentCorrect,
            currentTotal = session.currentTotal + 1,
            recentResults = newRecent
        )
        _trainerSession.value = newSession

        if (newSession.currentCorrect >= session.targetCorrect && currentAccuracy >= session.requiredAccuracy) {
            // Unlock next lesson
            val nextIndex = _progress.value.currentLessonIndex + 1
            if (nextIndex < kochOrder.size - 1) {
                val newProgress = _progress.value.copy(currentLessonIndex = nextIndex)
                _progress.value = newProgress
                viewModelScope.launch { repository.saveProgress(newProgress) }
            }
        }
    }

    fun nextTrainerExercise() {
        if (_trainerMode.value == TrainerMode.Koch) {
            generateKochExercise(_progress.value.currentLessonIndex)
        } else {
            generateTrainerExercise(_trainerMode.value ?: TrainerMode.Koch)
        }
    }

    private fun generateExercise(type: ExerciseType): String {
        return when (type) {
            ExerciseType.Beginner -> (1..2).map { "ETAONIS".random() }.joinToString("")
            ExerciseType.Intermediate -> (1..3).map { (('A'..'Z') + ('0'..'9')).random() }.joinToString("")
            ExerciseType.Advanced -> listOf("CQ", "DE", "K", "R", "TNX", "73").random()
            ExerciseType.Expert -> generateCallsign()
            else -> "PAR"
        }
    }

    private fun generateCallsign(): String {
        val prefixes = listOf("VK", "W", "G", "F", "JA")
        return "${prefixes.random()}${(1..9).random()}${('A'..'Z').random()}${('A'..'Z').random()}"
    }

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
        _lastElementSent.value = element
        _currentSymbolBuffer.value += element
    }

    private fun startDecodingTimer() {
        decodeJob?.cancel()
        decodeJob = viewModelScope.launch {
            val settings = _settings.value
            val unitMs = 1200 / settings.farnsworthWpm
            
            // Standard ITU timing:
            // Inter-character space = 3 units
            // Inter-word space = 7 units
            
            // Wait for 3 units to complete a character
            delay((unitMs * 3.0).toLong())
            
            val buffer = _currentSymbolBuffer.value
            if (buffer.isNotEmpty()) {
                val char = MorseCodeMap.entries.find { it.value == buffer }?.key
                if (char != null) {
                    _decodedText.value += char
                } else {
                    _decodedText.value += "?"
                }
                _currentSymbolBuffer.value = ""
            }
            
            // Wait for 4 more units (total 7) to complete a word
            delay((unitMs * 4.0).toLong())
            if (_decodedText.value.isNotEmpty() && !_decodedText.value.endsWith(" ")) {
                _decodedText.value += " "
            }
        }
    }

    fun resetTrainerExercise() {
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        _trainerFeedback.value = null
        decodeJob?.cancel()
    }

    fun clearSentText() {
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        decodeJob?.cancel()
    }

    fun submitSentText() {
        val text = _decodedText.value.trim()
        if (text.isNotEmpty() && _currentSection.value == MorseSection.Simulator) {
            val currentMessages = _messages.value.toMutableList()
            currentMessages.add("You" to text)
            _messages.value = currentMessages
            _decodedText.value = ""
            _currentSymbolBuffer.value = ""
        }
    }

    fun toggleDecoding() {
        _isDecoding.value = !_isDecoding.value
    }

    override fun onCleared() {
        keyer.release()
    }
}
