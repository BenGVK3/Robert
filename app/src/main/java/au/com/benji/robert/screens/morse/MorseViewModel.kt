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
                    wordGapJob?.cancel()
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

    private val _trainerTargetMeaning = MutableStateFlow<String?>(null)
    val trainerTargetMeaning = _trainerTargetMeaning.asStateFlow()

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

    private val _visualizerData = MutableStateFlow(FloatArray(512))
    val visualizerData = _visualizerData.asStateFlow()

    private val _telemetry = MutableStateFlow(SignalTelemetry())
    val telemetry = _telemetry.asStateFlow()

    private val _confidenceText = MutableStateFlow<List<ConfidenceCharacter>>(emptyList())
    val confidenceText = _confidenceText.asStateFlow()

    private val _predictions = MutableStateFlow<List<MorseAssistPrediction>>(emptyList())
    val predictions = _predictions.asStateFlow()

    private val _decoderHistory = MutableStateFlow<List<DecoderHistoryEntry>>(emptyList())
    val decoderHistory = _decoderHistory.asStateFlow()

    private val simulatorEngine = MorseSimulatorEngine()

    private val decoder = MorseAudioDecoder(
        onDecodedChar = { char ->
            viewModelScope.launch(Dispatchers.Main) {
                handleNewDecodedChar(char)
            }
        },
        onVisualizerData = { data ->
            _visualizerData.value = data
        },
        onTelemetryUpdate = { data ->
            _telemetry.value = data
        },
        initialSettings = _settings.value
    )

    private val _trainerMode = MutableStateFlow<TrainerMode?>(null)
    val trainerMode = _trainerMode.asStateFlow()

    private val _currentSimulatorOperator = MutableStateFlow<SimulatorOperator?>(null)
    val currentSimulatorOperator = _currentSimulatorOperator.asStateFlow()

    private val _simulatorExchangedInfo = MutableStateFlow<Set<QsoInfoType>>(emptySet())
    val simulatorExchangedInfo = _simulatorExchangedInfo.asStateFlow()

    private var decodeJob: Job? = null
    private var wordGapJob: Job? = null
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
                decoder.updateSettings(it)
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
        var meaning: String? = null
        val target = when (mode) {
            TrainerMode.Numbers -> (1..5).map { ('0'..'9').random() }.joinToString("")
            TrainerMode.Punctuation -> (1..3).map { ".,/?=".random() }.joinToString("")
            TrainerMode.Callsigns -> generateCallsign()
            TrainerMode.Words -> listOf(
                "THE", "AND", "FOR", "ARE", "BUT", "NOT", "YOU", "ALL", "ANY", "CAN", "HAD", "HER", "WAS", "ONE", "OUR", "OUT", "DAY", "GET", "HAS", "HIM",
                "HOW", "MAN", "NEW", "NOW", "OLD", "SEE", "TWO", "WAY", "WHO", "BOY", "DID", "ITS", "LET", "PUT", "SAY", "SHE", "TOO", "USE", "RADIO", "HAM",
                "MORSE", "CW", "SIGNAL", "POWER", "FREQ", "DX", "BEST", "NAME", "CITY", "STATION", "ANTENNA", "GROUND", "CABLE", "WIRE", "TUNER", "AMP",
                "VOLT", "WATTS", "OHMS", "KEYER", "SPEED", "FAST", "SLOW", "SOUND", "NOISE", "STATIC", "BAND", "METER", "WAVE", "SHORT", "LONG", "GOOD",
                "FINE", "NICE", "COPIED", "HEARD", "WORKED", "THANKS", "PLEASE", "SORRY", "AGAIN", "LATER", "TOMORROW", "NIGHT", "WEATHER", "SUNNY", "RAINY",
                "COLD", "HOT", "WINDY", "CLEAR", "CLOUDY", "TEMP", "DEGREE", "NORTH", "SOUTH", "EAST", "WEST", "HOME", "ROAD", "TRIP", "PORTABLE", "MOBILE"
            ).random()
            TrainerMode.Prosigns -> {
                val item = au.com.benji.robert.repository.GlossaryRepository().getGlossaryItems()
                    .filter { it.category == au.com.benji.robert.models.GlossaryCategory.Q_CODE || it.category == au.com.benji.robert.models.GlossaryCategory.JARGON || it.category == au.com.benji.robert.models.GlossaryCategory.NUMERIC_CODE }
                    .filter { !it.term.startsWith("10-") } // Exclude 10 codes
                    .random()
                meaning = item.definition
                item.term.uppercase()
            }
            else -> (1..3).map { kochSequence.random() }.joinToString("")
        }
        _trainerTarget.value = target
        _trainerTargetMeaning.value = meaning
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
                delay(1000)
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
                delay(1000)
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
        wordGapJob?.cancel()
        _lastElementSent.value = element
        _currentSymbolBuffer.value += element
    }

    private fun startDecodingTimer() {
        decodeJob?.cancel()
        wordGapJob?.cancel()
        decodeJob = viewModelScope.launch {
            val settings = _settings.value
            val unitMs = 1200.0 / settings.farnsworthWpm
            
            // Standard Morse: 1 unit intra, 3 units char, 7 units word.
            // We set the decoding thresholds between these values to be responsive but forgiving.
            // Char gap = 2.2 units (Wait for 2.2 units of silence to end a character)
            // Word gap = 6.0 units (Wait for 6.0 total units of silence to end a word)
            val charGapMultiplier = if (_currentSection.value == MorseSection.Trainer) 5.0 else 2.2
            val wordGapMultiplier = 6.0
            
            // Wait for character gap
            delay((unitMs * charGapMultiplier).toLong())
            
            val buffer = _currentSymbolBuffer.value
            if (buffer.isNotEmpty()) {
                val char = MorseCodeMap.entries.find { it.value == buffer }?.key
                
                if (char != null) {
                    _decodedText.value += char
                    
                    val isInTrainerOrPractice = _currentSection.value == MorseSection.Trainer || _currentSection.value == MorseSection.Practice
                    
                    if (isInTrainerOrPractice && _trainerMode.value != TrainerMode.Character) {
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

                // Start word gap timer ONLY after a character is finished and we've decoded it.
                if (settings.autoWordSpacing && (_currentSection.value == MorseSection.Send || _currentSection.value == MorseSection.Simulator)) {
                    wordGapJob = viewModelScope.launch {
                        delay((unitMs * (wordGapMultiplier - charGapMultiplier)).toLong())
                        if (_decodedText.value.isNotEmpty() && !_decodedText.value.endsWith(" ")) {
                            _decodedText.value += " "
                        }
                    }
                }
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
            
            // Process with Simulator Engine
            val intent = simulatorEngine.recognizeIntent(text)
            val response = simulatorEngine.generateResponse(intent, text)
            if (response.isNotEmpty()) {
                viewModelScope.launch {
                    delay(800) // Natural reaction delay
                    val sender = simulatorEngine.currentOperator?.callsign ?: "OP"
                    val updatedMessages = _messages.value.toMutableList()
                    updatedMessages.add(Pair(sender, response))
                    _messages.value = updatedMessages
                    _simulatorExchangedInfo.value = simulatorEngine.context.exchangedInfo
                    
                    // Actually "Send" the morse back so user can hear it
                    keyer.playText(response)
                }
            }
        }
    }

    fun resetSimulator() {
        viewModelScope.launch {
            repository.settings.first().let { 
                // We'd ideally use the user's callsign from settings
                simulatorEngine.reset("VK3XYZ") 
                _currentSimulatorOperator.value = simulatorEngine.currentOperator
                _messages.value = emptyList()
                _simulatorExchangedInfo.value = emptySet()
            }
        }
    }

    fun toggleDecoding() {
        val newState = !_isDecoding.value
        _isDecoding.value = newState
        if (newState) {
            _confidenceText.value = emptyList()
            _decodedText.value = ""
            decoder.start()
        } else {
            if (_decodedText.value.length > 5) {
                _decoderHistory.value = (_decoderHistory.value + DecoderHistoryEntry(_decodedText.value)).takeLast(20)
            }
            decoder.stop()
            _visualizerData.value = FloatArray(512)
            _telemetry.value = SignalTelemetry()
        }
    }

    private fun handleNewDecodedChar(c: ConfidenceCharacter) {
        val newList = (_confidenceText.value + c).takeLast(1000)
        _confidenceText.value = newList
        _decodedText.value = newList.joinToString("") { it.char.toString() }
        
        if (_settings.value.morseAssistEnabled) {
            updatePredictions(_decodedText.value)
        }
    }

    private fun updatePredictions(text: String) {
        val words = text.split(" ").filter { it.isNotEmpty() }
        val lastWord = words.lastOrNull()?.uppercase() ?: ""
        if (lastWord.length < 2) {
            _predictions.value = emptyList()
            return
        }
        
        // Amateur Radio Callsign Regex (Simplified)
        val callsignRegex = Regex("^[A-Z0-9]{1,3}[0-9][A-Z0-9]{1,3}$")
        
        val results = mutableListOf<MorseAssistPrediction>()
        
        // 1. Detect common phrases
        val phrases = listOf("CQ", "DE", "TEST", "QRZ", "73", "SK", "AR", "KN", "BK", "RST")
        if (phrases.contains(lastWord)) {
             // These are usually not callsigns but good to know
        }

        // 2. Predict callsigns
        if (lastWord.length >= 2) {
            // Simulated predictions based on prefixes and common suffixes
            if (lastWord.startsWith("VK")) {
                results.add(MorseAssistPrediction(if (lastWord.length == 2) "VK3ESE" else lastWord, 85))
                results.add(MorseAssistPrediction(if (lastWord.length == 2) "VK2ABC" else lastWord + "A", 70))
            } else if (lastWord.startsWith("K") || lastWord.startsWith("N") || lastWord.startsWith("W")) {
                 results.add(MorseAssistPrediction(lastWord + "JT", 90))
            }
        }
        
        _predictions.value = results.sortedByDescending { it.confidence }.take(5)
    }

    override fun onCleared() {
        keyer.release()
        decoder.stop()
    }
}
