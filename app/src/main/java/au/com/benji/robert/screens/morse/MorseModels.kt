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
data class MorseProgress(
    val charactersMastered: Set<Char> = emptySet(),
    val lessonsCompleted: Int = 0,
    val currentLessonIndex: Int = 0,
    val totalAccuracy: Float = 0f,
    val practiceStreak: Int = 0,
    val totalPracticeTimeMinutes: Long = 0,
    val averageWpm: Int = 20,
    val lastPracticeTimestamp: Long = 0,
    val attempts: Int = 0,
    val bestWpm: Int = 0,
    val timingAccuracy: Float = 0f,
    val sessionStats: List<SessionStat> = emptyList()
)

@Serializable
data class SessionStat(
    val timestamp: Long,
    val wpm: Int,
    val accuracy: Float,
    val type: String
)

enum class TrainerMode {
    Koch, Farnsworth, Callsigns, Words, Contest, QSO
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
    Characters, Numbers, Punctuation, Mixed, Callsigns, Words, Phrases, CQ
}

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
