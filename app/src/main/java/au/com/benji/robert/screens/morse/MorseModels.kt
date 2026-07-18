package au.com.benji.robert.screens.morse

import kotlinx.serialization.Serializable

@Serializable
data class MorseSettings(
    val wpm: Int = 20,
    val farnsworthWpm: Int = 20,
    val frequency: Int = 600,
    val volume: Float = 0.8f,
    val sidetoneVolume: Float = 0.8f,
    val sidetoneEnabled: Boolean = true,
    val autoAdvance: Boolean = false,
    val hideTextDuringPlayback: Boolean = false,
    val keyerMode: KeyerMode = KeyerMode.Straight,
    val paddleOrientation: PaddleOrientation = PaddleOrientation.LeftDitRightDah,
    val weighting: Float = 1.0f,
    val autoCharacterSpacing: Boolean = true,
    val autoWordSpacing: Boolean = true,
    
    // Decoder Advanced Settings
    val autoTone: Boolean = true,
    val autoWpm: Boolean = true,
    val noiseReductionLevel: Float = 0.5f,
    val agcEnabled: Boolean = true,
    val bandpassWidth: Int = 200,
    val autoCallsignDetection: Boolean = true,
    val morseAssistEnabled: Boolean = true,
    val debugMode: Boolean = false
)

enum class DecoderStatus {
    Idle, Searching, Locked, Decoding, SignalLost
}

@Serializable
data class SignalTelemetry(
    val signalStrengthDbfs: Float = -100f,
    val detectedFrequencyHz: Int = 0,
    val snrDb: Float = 0f,
    val estimatedWpm: Int = 20,
    val estimatedFarnsworthWpm: Int = 20,
    val confidence: Float = 0f,
    val sampleRate: Int = 44100,
    val status: DecoderStatus = DecoderStatus.Idle,
    
    // Diagnostic Debug Fields
    val noiseFloor: Float = 0f,
    val threshold: Float = 0f,
    val currentDitMs: Long = 0,
    val currentDahMs: Long = 0,
    val currentCharGapMs: Long = 0,
    val currentWordGapMs: Long = 0,
    val rawMorse: String = "",
    val isKeyDown: Boolean = false
)

@Serializable
data class ConfidenceCharacter(
    val char: Char,
    val confidence: Float, // 0.0 to 1.0
    val morse: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class MorseAssistPrediction(
    val callsign: String,
    val confidence: Int, // 0 to 100
    val isVerified: Boolean = false // Verified against database
)

@Serializable
data class DecoderHistoryEntry(
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val callsign: String? = null
)

enum class KeyerMode {
    Straight, IambicA, IambicB
}

enum class PaddleOrientation {
    LeftDitRightDah, LeftDahRightDit
}

@Serializable
data class SessionStat(
    val timestamp: Long,
    val wpm: Int,
    val accuracy: Float,
    val type: String
)

@Serializable
data class TrainerFeedback(
    val isCorrect: Boolean,
    val expected: String,
    val received: String,
    val expectedCode: String,
    val receivedCode: String,
    val message: String
)

@Serializable
data class KochLesson(
    val lessonNumber: Int,
    val introducedCharacters: List<Char>,
    val allCharacters: List<Char>,
    val targetCorrect: Int = 20
)

@Serializable
data class MorseProgress(
    val charactersMastered: Set<Char> = emptySet(),
    val lessonsCompleted: Int = 0,
    val kochLessonIndex: Int = 0,
    val receiveLessonIndex: Int = 0,
    val totalAccuracy: Float = 0f,
    val practiceStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalPracticeTimeSeconds: Long = 0,
    val totalCharactersCopied: Int = 0,
    val characterStats: Map<Char, CharacterStat> = emptyMap(),
    val dailyHistory: Map<String, Int> = emptyMap(),
    val sessionStats: List<SessionStat> = emptyList()
)

@Serializable
data class TrainerSessionProgress(
    val lessonNumber: Int = 1,
    val currentCorrect: Int = 0,
    val currentTotal: Int = 0,
    val targetCorrect: Int = 20,
    val requiredAccuracy: Float = 90f,
    val recentResults: List<Boolean> = emptyList(),
    val startTime: Long = System.currentTimeMillis(),
    val revealsUsed: Int = 0,
    val lastDecodedChar: Char? = null,
    val lastDecodedStatus: DecodeStatus = DecodeStatus.None,
    val newCharAppearances: Map<Char, Int> = emptyMap(),
    val introducedCharacters: List<Char> = emptyList()
)

enum class DecodeStatus {
    None, Correct, Wrong, Invalid
}

@Serializable
data class CharacterStat(
    val char: Char,
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val weight: Float = 1.0f,
    val lastPracticed: Long = 0
) {
    val accuracy: Float get() = if (totalCount > 0) (correctCount.toFloat() / totalCount) * 100f else 0f
}

enum class TrainerMode {
    Koch, Character, Numbers, Punctuation, Prosigns, Callsigns, Words, AmateurRadio, WeakReview, Statistics
}

sealed class SimulatorState {
    object Idle : SimulatorState()
    data class CallingCQ(val stationCall: String) : SimulatorState()
    data class WaitingForResponse(val stationCall: String) : SimulatorState()
    data class ExchangingInfo(val stationCall: String, val stage: Int) : SimulatorState()
    object Finished : SimulatorState()
}

enum class MorseSection {
    Menu, Send, Decoder, Trainer, Simulator, Practice
}

enum class ExerciseType {
    Characters, Numbers, Punctuation, Mixed, Callsigns, Words, Phrases, CQ,
    Beginner, Intermediate, Advanced, Expert
}

@Serializable
data class ReceiveStats(
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val charactersPracticed: Int = 0,
    val totalTimeSeconds: Long = 0
)

data class ReceiveFeedback(
    val isCorrect: Boolean,
    val expected: String,
    val received: String,
    val comparison: List<Pair<Char, Boolean>>
)

enum class SimulatorDifficulty {
    Beginner, Intermediate, Advanced
}

enum class SimulatorScenario {
    Ragchew, Contest, POTA, SOTA, DX, FieldDay
}

@Serializable
data class SimulatorOperator(
    val callsign: String,
    val name: String,
    val location: String,
    val rig: String = "IC-7300",
    val antenna: String = "Dipole",
    val power: Int = 100,
    val weather: String = "Sunny",
    val personality: String = "Friendly",
    val wpm: Int = 20,
    val difficulty: SimulatorDifficulty = SimulatorDifficulty.Intermediate
)

@Serializable
data class QsoContext(
    var userCallsign: String? = null,
    var userName: String? = null,
    var userQth: String? = null,
    var userRst: String? = null,
    var userRig: String? = null,
    var opName: String? = null,
    var opCallsign: String? = null,
    var exchangedInfo: Set<QsoInfoType> = emptySet()
)

enum class QsoInfoType {
    CALLSIGN, RST, NAME, QTH, RIG, WEATHER, THANKS_73
}

enum class QsoIntent {
    CQ, CALLING_ME, EXCHANGE_RST, EXCHANGE_NAME, EXCHANGE_QTH, EXCHANGE_RIG, EXCHANGE_WEATHER, ENDING, UNKNOWN
}

@Serializable
data class SimulatorSessionFeedback(
    val qsoCompleted: Boolean,
    val callsignCorrect: Boolean,
    val rstExchanged: Boolean,
    val nameExchanged: Boolean,
    val qthExchanged: Boolean,
    val proceduralScore: Int, // 0-100
    val totalScore: Int
)

@Serializable
data class KeyerDiagnosticEntry(
    val type: String,
    val durationMs: Long,
    val expectedMs: Long,
    val decodedAs: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class MorseCharacter(
    val char: Char,
    val code: String
)

val MorseCodeMap = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
    'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
    'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
    'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..",
    '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....",
    '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.", '0' to "-----",
    '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '/' to "-..-.", '=' to "-...-",
    '+' to ".-.-.", '-' to "-....-", '(' to "-.--.", ')' to "-.--.-", ':' to "---...",
    '\'' to ".----.", '\"' to ".-..-.", '@' to ".--.-.", '!' to "-.-.--"
)
