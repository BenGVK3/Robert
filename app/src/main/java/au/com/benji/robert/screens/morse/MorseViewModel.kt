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

    private val _revealed = MutableStateFlow(false)
    val revealed = _revealed.asStateFlow()

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
    private var autoplayJob: Job? = null
    
    // Data-driven Koch Lessons
    val kochSequence = listOf(
        'K', 'M', 'R', 'S', 'U', 'A', 'P', 'T', 'L', 'O', 'W', 'I', 'N', 'J', 'E', 'F', 'V', 'G', 'Q', 'Z', 'H', 'B', 'C', 'Y', 'X', 'D',
        '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
        '.', ',', '?', '\'', '/', '(', ')', '&', ':', ';', '=', '+', '-', '_', '\"', '$', '@'
    )

    private val lessons: List<KochLesson> by lazy {
        List(kochSequence.size) { index ->
            if (index == 0) {
                KochLesson(1, listOf('K', 'M'), listOf('K', 'M'))
            } else {
                val char = kochSequence[index]
                KochLesson(index + 1, listOf(char), kochSequence.take(index + 1))
            }
        }
    }

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
                updateSessionFromProgress(it)
            }
        }
        
        viewModelScope.launch {
            while (isActive) {
                _isPlaying.value = keyer.isPlaybackActive()
                delay(100)
            }
        }
    }

    private fun updateSessionFromProgress(progress: MorseProgress) {
        val mode = _trainerMode.value
        val lessonIdx = if (mode == TrainerMode.Character) progress.receiveLessonIndex else progress.kochLessonIndex
        val safeIdx = lessonIdx.coerceIn(0, lessons.size - 1)
        val currentLesson = lessons[safeIdx]
        _trainerSession.value = _trainerSession.value.copy(
            lessonNumber = currentLesson.lessonNumber,
            targetCorrect = currentLesson.targetCorrect
        )
    }

    fun setSection(section: MorseSection) {
        _currentSection.value = section
        stopAll()
        _trainerMode.value = null
    }

    fun setTrainerMode(mode: TrainerMode) {
        _trainerMode.value = mode
        _trainerFeedback.value = null
        _receiveFeedback.value = null
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        _revealed.value = false
        keyer.stopPlayback()
        
        if (mode == TrainerMode.Koch || mode == TrainerMode.Character) {
            startLessonBasedMode(mode)
        } else {
            generateTrainerExercise(mode)
        }
    }

    private fun startLessonBasedMode(mode: TrainerMode) {
        val progress = _progress.value
        val lessonIdx = if (mode == TrainerMode.Character) progress.receiveLessonIndex else progress.kochLessonIndex
        val safeIdx = lessonIdx.coerceIn(0, lessons.size - 1)
        val currentLesson = lessons[safeIdx]
        
        _trainerSession.value = TrainerSessionProgress(
            lessonNumber = currentLesson.lessonNumber,
            startTime = System.currentTimeMillis(),
            currentCorrect = 0,
            currentTotal = 0,
            targetCorrect = currentLesson.targetCorrect,
            newCharAppearances = emptyMap(),
            introducedCharacters = currentLesson.introducedCharacters
        )
        if (mode == TrainerMode.Koch) {
            generateKochExercise(safeIdx)
        } else {
            generateReceiveExercise(safeIdx)
        }
    }

    private fun generateReceiveExercise(index: Int) {
        val lessonIdx = index.coerceIn(0, lessons.size - 1)
        val currentLesson = lessons[lessonIdx]
        
        val targetChar = if (Math.random() < 0.4) {
            currentLesson.introducedCharacters.random()
        } else {
            currentLesson.allCharacters.random()
        }
        
        _currentText.value = targetChar.toString()
        _trainerTarget.value = targetChar.toString() // Needed for playCurrentText() logic
        _receiveFeedback.value = null
        _revealed.value = false // Reset revealed state for new exercise
        keyer.stopPlayback()
        autoplayJob?.cancel()
        
        // Autoplay the target for receive practice
        autoplayJob = viewModelScope.launch {
            delay(500)
            playCurrentText()
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
        _revealed.value = false
    }

    fun updateSettings(newSettings: MorseSettings) {
        _settings.value = newSettings
        keyer.updateSettings(newSettings)
        viewModelScope.launch { repository.saveSettings(newSettings) }
    }

    fun resetKochCourse() {
        val current = _progress.value
        val newProgress = current.copy(kochLessonIndex = 0)
        _progress.value = newProgress
        viewModelScope.launch { repository.saveProgress(newProgress) }
        startLessonBasedMode(TrainerMode.Koch)
    }

    fun resetReceiveCourse() {
        val current = _progress.value
        val newProgress = current.copy(receiveLessonIndex = 0)
        _progress.value = newProgress
        viewModelScope.launch { repository.saveProgress(newProgress) }
        startLessonBasedMode(TrainerMode.Character)
    }

    // --- Training Logic ---

    fun generateNewExercise(type: ExerciseType) {
        _currentExerciseType.value = type
        _currentText.value = generateExercise(type)
        _receiveFeedback.value = null
        keyer.stopPlayback()
    }

    fun playCurrentText() {
        val textToPlay = if (_trainerMode.value == TrainerMode.Character) _currentText.value else _trainerTarget.value
        if (textToPlay.isNotEmpty()) {
            keyer.stopPlayback()
            keyer.playText(textToPlay)
        }
    }

    fun revealCurrentTarget() {
        _revealed.value = true
        _trainerSession.value = _trainerSession.value.copy(
            revealsUsed = _trainerSession.value.revealsUsed + 1
        )
    }

    fun resetTrainerExercise() {
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        _trainerFeedback.value = null
        _revealed.value = false
        _trainerSession.value = _trainerSession.value.copy(
            lastDecodedChar = null,
            lastDecodedStatus = DecodeStatus.None
        )
        decodeJob?.cancel()
        
        val progress = _progress.value
        val mode = _trainerMode.value
        val lessonIdx = if (mode == TrainerMode.Character) progress.receiveLessonIndex else progress.kochLessonIndex
        
        if (mode == TrainerMode.Koch) {
            generateKochExercise(lessonIdx)
        } else if (mode == TrainerMode.Character) {
            generateReceiveExercise(lessonIdx)
        } else {
            generateTrainerExercise(mode ?: TrainerMode.Koch)
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
        
        if (_trainerMode.value == TrainerMode.Character) {
            updateStructuredProgress(isCorrect)
        }
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


    private fun generateKochExercise(index: Int) {
        val lessonIdx = index.coerceIn(0, lessons.size - 1)
        val currentLesson = lessons[lessonIdx]
        
        val targetChar = if (Math.random() < 0.4) {
            currentLesson.introducedCharacters.random()
        } else {
            currentLesson.allCharacters.random()
        }
        
        if (currentLesson.introducedCharacters.contains(targetChar)) {
            val session = _trainerSession.value
            val currentCount = session.newCharAppearances[targetChar] ?: 0
            _trainerSession.value = session.copy(
                newCharAppearances = session.newCharAppearances + (targetChar to (currentCount + 1))
            )
        }
        
        _trainerTarget.value = targetChar.toString()
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        _trainerFeedback.value = null
        _revealed.value = false
        _trainerSession.value = _trainerSession.value.copy(
            lastDecodedChar = null,
            lastDecodedStatus = DecodeStatus.None
        )
        keyer.stopPlayback()
    }

    private fun generateTrainerExercise(mode: TrainerMode) {
        val target = when (mode) {
            TrainerMode.Numbers -> (1..5).map { ('0'..'9').random() }.joinToString("")
            TrainerMode.Punctuation -> (1..3).map { ".,/?=".random() }.joinToString("")
            TrainerMode.Callsigns -> generateCallsign()
            TrainerMode.Words -> listOf("THE", "AND", "BK", "CQ", "DX", "73").random()
            else -> (1..3).map { kochSequence.random() }.joinToString("")
        }
        _trainerTarget.value = target
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        _trainerFeedback.value = null
        _revealed.value = false
        _trainerSession.value = _trainerSession.value.copy(
            lastDecodedChar = null,
            lastDecodedStatus = DecodeStatus.None
        )
        keyer.stopPlayback()
    }

    fun submitTrainerAnswer() {
        val current = _decodedText.value.replace(" ", "").trim().uppercase()
        val target = _trainerTarget.value.replace(" ", "").trim().uppercase()
        val isCorrect = current == target
        
        _trainerFeedback.value = TrainerFeedback(
            isCorrect = isCorrect,
            expected = _trainerTarget.value,
            received = _decodedText.value,
            expectedCode = _trainerTarget.value.map { MorseCodeMap[it] ?: "" }.joinToString(" "),
            receivedCode = _decodedText.value.map { MorseCodeMap[it] ?: "" }.joinToString(" "),
            message = if (isCorrect) "EXCELLENT!" else "ERROR DETECTED"
        )

        if (_trainerMode.value == TrainerMode.Koch) {
            updateStructuredProgress(isCorrect)
        } else if (isCorrect) {
            // Automatic load next target for endless modes
            viewModelScope.launch {
                delay(1200)
                generateTrainerExercise(_trainerMode.value ?: TrainerMode.Koch)
            }
        }
        updateGlobalStats(isCorrect, target)
    }

    private fun updateStructuredProgress(isCorrect: Boolean) {
        val session = _trainerSession.value
        val mode = _trainerMode.value
        
        val newCorrect = (if (isCorrect) session.currentCorrect + 1 else session.currentCorrect).coerceAtMost(session.targetCorrect)
        val newTotal = session.currentTotal + 1
        
        val newRecent = (session.recentResults + isCorrect).takeLast(20)
        
        val newSession = session.copy(
            currentCorrect = newCorrect,
            currentTotal = newTotal,
            recentResults = newRecent
        )
        _trainerSession.value = newSession

        if (newSession.currentCorrect >= session.targetCorrect) {
            viewModelScope.launch {
                delay(1000)
                val currentProgress = _progress.value
                val currentIdx = if (mode == TrainerMode.Character) currentProgress.receiveLessonIndex else currentProgress.kochLessonIndex
                val nextIndex = currentIdx + 1
                
                if (nextIndex < lessons.size) {
                    val newProgress = if (mode == TrainerMode.Character) {
                        currentProgress.copy(receiveLessonIndex = nextIndex)
                    } else {
                        currentProgress.copy(kochLessonIndex = nextIndex)
                    }
                    _progress.value = newProgress
                    repository.saveProgress(newProgress)
                    startLessonBasedMode(mode ?: TrainerMode.Koch)
                }
            }
        } else if (isCorrect) {
            viewModelScope.launch {
                delay(1200)
                val currentProgress = _progress.value
                if (mode == TrainerMode.Koch) {
                    generateKochExercise(currentProgress.kochLessonIndex)
                } else if (mode == TrainerMode.Character) {
                    _currentText.value = "" // Clear current text to prevent replay
                    generateReceiveExercise(currentProgress.receiveLessonIndex)
                }
            }
        }
    }

    fun nextTrainerExercise() {
        val progress = _progress.value
        val mode = _trainerMode.value
        
        // Clear states to trigger UI resets
        _currentText.value = ""
        _receiveFeedback.value = null
        _revealed.value = false

        if (mode == TrainerMode.Koch) {
            generateKochExercise(progress.kochLessonIndex)
        } else if (mode == TrainerMode.Character) {
             generateReceiveExercise(progress.receiveLessonIndex)
        } else {
            generateTrainerExercise(mode ?: TrainerMode.Koch)
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
            val gapMultiplier = if (_currentSection.value == MorseSection.Trainer) 5.0 else 3.0
            delay((unitMs * gapMultiplier).toLong())
            
            val buffer = _currentSymbolBuffer.value
            if (buffer.isNotEmpty()) {
                val char = MorseCodeMap.entries.find { it.value == buffer }?.key
                
                if (char != null) {
                    _decodedText.value += char
                    
                    if (_currentSection.value == MorseSection.Trainer && _trainerMode.value != TrainerMode.Character) {
                        val target = _trainerTarget.value.replace(" ", "").trim().uppercase()
                        val currentDecoded = _decodedText.value.replace(" ", "").trim().uppercase()
                        
                        val status = if (target.startsWith(currentDecoded)) {
                            if (target == currentDecoded) DecodeStatus.Correct else DecodeStatus.None
                        } else {
                            DecodeStatus.Wrong
                        }
                        
                        _trainerSession.value = _trainerSession.value.copy(
                            lastDecodedChar = char,
                            lastDecodedStatus = status
                        )
                        
                        if (currentDecoded.length >= target.length) {
                            submitTrainerAnswer()
                        }
                    }
                } else {
                    _decodedText.value += "?"
                    if (_currentSection.value == MorseSection.Trainer) {
                        _trainerSession.value = _trainerSession.value.copy(
                            lastDecodedChar = '?',
                            lastDecodedStatus = DecodeStatus.Invalid
                        )
                    }
                }
                _currentSymbolBuffer.value = ""
            }
        }
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
