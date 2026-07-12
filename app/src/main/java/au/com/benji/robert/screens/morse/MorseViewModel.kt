package au.com.benji.robert.screens.morse

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class MorseViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MorseViewModel"
    private val engine = MorseEngine(application, viewModelScope)

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

    private val _currentSymbolBuffer = MutableStateFlow("")
    val currentSymbolBuffer = _currentSymbolBuffer.asStateFlow()

    private val _isKeyBusy = MutableStateFlow(false)
    val isKeyBusy = _isKeyBusy.asStateFlow()

    private val _lastElementSent = MutableStateFlow("")
    val lastElementSent = _lastElementSent.asStateFlow()

    private val _estimatedWpm = MutableStateFlow(0)
    val estimatedWpm = _estimatedWpm.asStateFlow()

    // Trainer specific state
    private val _trainerTarget = MutableStateFlow("")
    val trainerTarget = _trainerTarget.asStateFlow()

    private val _trainerFeedback = MutableStateFlow<TrainerFeedback?>(null)
    val trainerFeedback = _trainerFeedback.asStateFlow()

    private var decodeJob: Job? = null
    
    // Iambic State
    private var dotPressed = false
    private var dashPressed = false
    private var iambicJob: Job? = null
    private var modeBExtra = false
    private var lastSentElement = ""

    private val lessons = listOf(
        "K M", "K M R", "K M R S", "K M R S U", "K M R S U A", "K M R S U A P",
        "K M R S U A P T", "K M R S U A P T L", "K M R S U A P T L O", "K M R S U A P T L O W"
    )

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
        engine.stop()
        iambicJob?.cancel()
        _isPlaying.value = false
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
        _trainerFeedback.value = null
    }

    fun updateSettings(newSettings: MorseSettings) {
        _settings.value = newSettings
        engine.updateConfig(newSettings)
    }

    fun generateNewExercise(type: ExerciseType) {
        val text = engine.generateExercise(type)
        _currentText.value = text
        playText(text)
    }

    fun playText(text: String) {
        _isPlaying.value = true
        engine.playText(text) {
            _isPlaying.value = false
        }
    }

    fun stopPlayback() {
        engine.stop()
        _isPlaying.value = false
    }

    // --- Keying Logic ---

    private var keyStartTime = 0L

    fun onKeyDown(isRightPaddle: Boolean) {
        val isDash = if (_settings.value.paddleOrientation == PaddleOrientation.LeftDitRightDah) isRightPaddle else !isRightPaddle

        if (_settings.value.keyerMode == KeyerMode.Straight) {
            engine.startSidetone()
            keyStartTime = System.currentTimeMillis()
            _isKeyBusy.value = true
            decodeJob?.cancel()
        } else {
            if (isDash) dashPressed = true else dotPressed = true
            startIambicLogic()
        }
    }

    fun onKeyUp(isRightPaddle: Boolean) {
        if (_settings.value.keyerMode == KeyerMode.Straight) {
            engine.stopSidetone()
            _isKeyBusy.value = false
            val duration = System.currentTimeMillis() - keyStartTime
            val unit = (1200 / _settings.value.wpm).toLong()
            val element = if (duration < unit * 2) "." else "-"
            
            _currentSymbolBuffer.value += element
            _lastElementSent.value = element
            viewModelScope.launch {
                delay(200)
                if (_lastElementSent.value == element) _lastElementSent.value = ""
            }
            startDecodingTimer()
        } else {
            val isDash = if (_settings.value.paddleOrientation == PaddleOrientation.LeftDitRightDah) isRightPaddle else !isRightPaddle
            if (isDash) dashPressed = false else dotPressed = false
        }
    }

    private fun startIambicLogic() {
        if (iambicJob?.isActive == true) return
        
        iambicJob = viewModelScope.launch(Dispatchers.Default) {
            while (dotPressed || dashPressed || modeBExtra) {
                val unit = (1200 / _settings.value.wpm).toLong()
                
                val nextElement = when {
                    dotPressed && !dashPressed -> "."
                    !dotPressed && dashPressed -> "-"
                    dotPressed && dashPressed -> if (lastSentElement == ".") "-" else "."
                    modeBExtra -> {
                        modeBExtra = false
                        if (lastSentElement == ".") "-" else "."
                    }
                    else -> break
                }
                
                val duration = if (nextElement == ".") unit else (unit * 3 * _settings.value.weighting).toLong()
                lastSentElement = nextElement
                
                // Play
                engine.startSidetone()
                withContext(Dispatchers.Main) { 
                    _currentSymbolBuffer.value += nextElement 
                    _lastElementSent.value = nextElement
                    _isKeyBusy.value = true
                }
                
                // Track for Iambic B extra
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < duration) {
                    delay(5)
                    if (_settings.value.keyerMode == KeyerMode.IambicB) {
                        if (nextElement == "." && dashPressed) modeBExtra = true
                        if (nextElement == "-" && dotPressed) modeBExtra = true
                    }
                }
                engine.stopSidetone()
                withContext(Dispatchers.Main) { 
                    _isKeyBusy.value = false
                    val currentE = nextElement
                    viewModelScope.launch {
                        delay(200)
                        if (_lastElementSent.value == currentE) _lastElementSent.value = ""
                    }
                }
                
                // Inter-element space
                val spaceStartTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - spaceStartTime < unit) {
                    delay(5)
                    if (_settings.value.keyerMode == KeyerMode.IambicB) {
                        if (nextElement == "." && dashPressed) modeBExtra = true
                        if (nextElement == "-" && dotPressed) modeBExtra = true
                    }
                }
                
                startDecodingTimer()
            }
            lastSentElement = ""
        }
    }

    private fun startDecodingTimer() {
        decodeJob?.cancel()
        decodeJob = viewModelScope.launch {
            val unit = (1200 / _settings.value.wpm).toLong()
            delay(unit * 3) // Letter space
            
            val char = MorseCodeMap.entries.find { it.value == _currentSymbolBuffer.value }?.key
            if (char != null) {
                _decodedText.value += char
                if (_currentSection.value == MorseSection.Trainer) {
                    checkTrainerProgress()
                }
            }
            _currentSymbolBuffer.value = ""
            
            delay(unit * 4) // Word space
            if (_decodedText.value.isNotEmpty() && !_decodedText.value.endsWith(" ")) {
                _decodedText.value += " "
            }
        }
    }

    // --- Trainer Logic ---

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
    }

    private fun checkTrainerProgress() {
        val current = _decodedText.value.trim().uppercase()
        val target = _trainerTarget.value.uppercase()
        
        if (current.length >= target.length) {
            val isCorrect = current == target
            val expectedCode = target.map { MorseCodeMap[it] ?: "" }.joinToString(" ")
            val receivedCode = current.map { MorseCodeMap[it] ?: "" }.joinToString(" ")

            _trainerFeedback.value = TrainerFeedback(
                isCorrect = isCorrect,
                expected = target,
                received = current,
                expectedCode = expectedCode,
                receivedCode = receivedCode,
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
        if (success) {
            _trainerTarget.value.forEach { newMastered.add(it) }
        }
        
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

    // --- Simulator Logic ---
    private val _messages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val messages = _messages.asStateFlow()
    
    private var simulatorOp: SimulatorOperator? = null
    private var currentScenario: SimulatorScenario = SimulatorScenario.Ragchew
    private var simStage = 0

    fun clearMessages() {
        _messages.value = emptyList()
        simStage = 0
    }

    fun submitSentText() {
        if (_decodedText.value.isNotEmpty()) {
            val userText = _decodedText.value.trim().uppercase()
            _messages.value += ("You" to userText)
            _decodedText.value = ""
            processSimulatorResponse(userText)
        }
    }

    fun startSimulator() {
        clearMessages()
        currentScenario = SimulatorScenario.entries.random()
        simulatorOp = generateOperator()
        val op = simulatorOp!!
        
        val initialMsg = when (currentScenario) {
            SimulatorScenario.Contest -> "CQ TEST DE ${op.callsign} ${op.callsign} TEST"
            SimulatorScenario.POTA -> "CQ POTA DE ${op.callsign} ${op.callsign} K"
            SimulatorScenario.SOTA -> "CQ SOTA DE ${op.callsign} ${op.callsign} K"
            else -> "CQ CQ CQ DE ${op.callsign} ${op.callsign} K"
        }
        
        _messages.value += (op.callsign to initialMsg)
        playText(initialMsg)
        simStage = 1
    }

    private fun processSimulatorResponse(userText: String) {
        val op = simulatorOp ?: return
        viewModelScope.launch {
            delay(1500)
            val response = when (currentScenario) {
                SimulatorScenario.Contest -> {
                    if (simStage == 1) {
                        simStage = 2
                        "TU 5NN 001 BK"
                    } else "TU QRZ? ${op.callsign}"
                }
                else -> {
                    when (simStage) {
                        1 -> {
                            simStage = 2
                            "TU UR 599 OP ${op.name} QTH ${op.location} HW CPY? BK"
                        }
                        2 -> {
                            simStage = 3
                            "FB UR 599. RIG ${op.equipment}. WX ${op.weather}. TU 73 SK"
                        }
                        else -> "TU 73 SK"
                    }
                }
            }
            _messages.value += (op.callsign to response)
            playText(response)
        }
    }

    private fun generateOperator(): SimulatorOperator {
        val names = listOf("BEN", "JOHN", "SARAH", "MIKE", "LISA", "ALEX", "DAVID", "EMILY")
        val locations = listOf("MELBOURNE", "SYDNEY", "LONDON", "NEW YORK", "TOKYO", "BERLIN", "PARIS")
        val rigs = listOf("IC-7300", "FT-891", "KX3", "TS-590SG", "K4", "QCX-MINI")
        val weather = listOf("SUNNY", "CLOUDY", "RAINING", "COLD", "HOT", "WINDY")
        
        return SimulatorOperator(
            callsign = engine.generateExercise(ExerciseType.Callsigns),
            name = names.random(),
            location = locations.random(),
            equipment = rigs.random(),
            weather = weather.random(),
            personality = "Friendly",
            wpm = (15..30).random()
        )
    }

    fun clearSentText() {
        _decodedText.value = ""
        _currentSymbolBuffer.value = ""
    }

    private val _isDecoding = MutableStateFlow(false)
    val isDecoding = _isDecoding.asStateFlow()

    fun toggleDecoding() {
        _isDecoding.value = !_isDecoding.value
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
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
