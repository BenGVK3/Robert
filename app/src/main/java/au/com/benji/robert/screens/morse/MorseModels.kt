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
    val autoWordSpacing: Boolean = true
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
data class MorseProgress(
    val charactersMastered: Set<Char> = emptySet(),
    val lessonsCompleted: Int = 0,
    val currentLessonIndex: Int = 0,
    val totalAccuracy: Float = 0f,
    val practiceStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalPracticeTimeSeconds: Long = 0,
    val totalCharactersCopied: Int = 0,
    val characterStats: Map<Char, CharacterStat> = emptyMap(),
    val dailyHistory: Map<String, Int> = emptyMap(), // Date string to chars copied
    val sessionStats: List<SessionStat> = emptyList()
)

@Serializable
data class TrainerSessionProgress(
    val lessonNumber: Int = 1,
    val currentCorrect: Int = 0,
    val currentTotal: Int = 0,
    val targetCorrect: Int = 20, // Number of correct answers to finish lesson
    val requiredAccuracy: Float = 90f,
    val recentResults: List<Boolean> = emptyList(), // Last N results for accuracy check
    val startTime: Long = System.currentTimeMillis()
)

@Serializable
data class CharacterStat(
    val char: Char,
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val weight: Float = 1.0f, // For focusing on difficult characters
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
    Menu, Receive, Send, Decoder, Trainer, Simulator
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
    val comparison: List<Pair<Char, Boolean>> // Char and whether it matched
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
    val equipment: String,
    val weather: String,
    val personality: String,
    val wpm: Int
)

@Serializable
data class KeyerDiagnosticEntry(
    val type: String, // "DIT", "DAH", "GAP", "CHAR_GAP", "WORD_GAP"
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
