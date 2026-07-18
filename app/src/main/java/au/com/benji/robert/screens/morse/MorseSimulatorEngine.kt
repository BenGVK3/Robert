package au.com.benji.robert.screens.morse

import android.util.Log
import java.util.Locale
import kotlin.random.Random

/**
 * Intelligent Morse Conversation Engine.
 * Handles intent recognition, context tracking, and natural response generation.
 */
class MorseSimulatorEngine {
    private val TAG = "SimulatorEngine"
    
    var currentOperator: SimulatorOperator? = null
    var context = QsoContext()
    var currentState = SimulatorState.Idle
    
    private val operators = listOf(
        SimulatorOperator("VK2ABC", "David", "Sydney", "IC-7300", "Dipole", 100, "Sunny", "Friendly", 20),
        SimulatorOperator("VK4XYZ", "Sarah", "Brisbane", "FT-710", "Vertical", 100, "Cloudy", "Professional", 22),
        SimulatorOperator("ZL1AAA", "John", "Auckland", "IC-705", "End Fed", 10, "Rainy", "Beginner Friendly", 15),
        SimulatorOperator("G3ZZZ", "Arthur", "London", "TS-590", "Yagi", 400, "Foggy", "Veteran", 25)
    )

    fun reset(userCall: String) {
        currentOperator = operators.random()
        context = QsoContext(userCallsign = userCall.uppercase())
        currentState = SimulatorState.Idle
    }

    /**
     * Analyzes raw decoded text to determine semantic meaning.
     */
    fun recognizeIntent(text: String): QsoIntent {
        val upper = text.uppercase().trim()
        
        return when {
            upper.contains("CQ CQ") -> QsoIntent.CQ
            upper.contains(" DE ") && upper.contains(currentOperator?.callsign ?: "") -> QsoIntent.CALLING_ME
            upper.contains("RST") || Regex("\\b[1-5][1-9][1-9]\\b").containsMatchIn(upper) -> QsoIntent.EXCHANGE_RST
            upper.contains("NAME") || upper.contains("OP ") -> QsoIntent.EXCHANGE_NAME
            upper.contains("QTH") || upper.contains("HR IN") -> QsoIntent.EXCHANGE_QTH
            upper.contains("RIG") || upper.contains("USING") -> QsoIntent.EXCHANGE_RIG
            upper.contains("73") || upper.contains("SK") -> QsoIntent.ENDING
            else -> QsoIntent.UNKNOWN
        }
    }

    /**
     * Generates a natural CW response based on context and intent.
     */
    fun generateResponse(intent: QsoIntent, userInput: String): String {
        val op = currentOperator ?: return ""
        val user = context.userCallsign ?: "DEAR OM"
        
        // Error Tolerance: Try to infer name if not found but text exists
        if (intent == QsoIntent.EXCHANGE_NAME && context.userName == null) {
             val inferred = inferMeaning(userInput)
             if (inferred != null) context.userName = inferred
        }

        return when (intent) {
            QsoIntent.CQ -> {
                context.exchangedInfo += QsoInfoType.CALLSIGN
                listOf(
                    "$user DE ${op.callsign} K",
                    "$user DE ${op.callsign} GA UR 599 HR IN ${op.location} BK",
                    "QRZ $user DE ${op.callsign} K"
                ).random()
            }
            
            QsoIntent.EXCHANGE_RST -> {
                context.exchangedInfo += QsoInfoType.RST
                val rst = extractRst(userInput) ?: "599"
                context.userRst = rst
                listOf(
                    "R UR RST $rst BK",
                    "FB R RST $rst. UR RST 579 IN ${op.location} BK",
                    "QSL UR $rst. MY NAME IS ${op.name} BK"
                ).random()
            }

            QsoIntent.EXCHANGE_NAME -> {
                context.exchangedInfo += QsoInfoType.NAME
                val name = extractName(userInput) ?: "OM"
                context.userName = name
                listOf(
                    "R $name. NAME HR ${op.name} BK",
                    "FB $name. MY NAME IS ${op.name} BK",
                    "QSL $name. NAME ${op.name} BK"
                ).random()
            }

            QsoIntent.EXCHANGE_QTH -> {
                context.exchangedInfo += QsoInfoType.QTH
                listOf(
                    "R QTH ${op.location} BK",
                    "QSL. MY QTH IS ${op.location} BK",
                    "FB. HR IN ${op.location} BK"
                ).random()
            }

            QsoIntent.ENDING -> {
                context.exchangedInfo += QsoInfoType.THANKS_73
                listOf(
                    "TNX FER QSO $user 73 SK",
                    "QSL 73 GL ES DX SK",
                    "73 $user CU AGN SK"
                ).random()
            }

            else -> "R BK"
        }
    }

    private fun extractRst(text: String): String? {
        val match = Regex("([1-5][1-9][1-9])").find(text)
        return match?.groupValues?.get(1)
    }

    private fun extractName(text: String): String? {
        val words = text.uppercase().split(" ")
        val index = words.indexOf("NAME")
        return if (index != -1 && index + 1 < words.size) words[index + 1] else null
    }

    private fun inferMeaning(input: String): String? {
        val clean = input.uppercase().trim()
        if (clean.isEmpty()) return null
        
        // Very basic fuzzy logic for names/words
        // In a real app, use Levenshtein distance
        return when {
            clean.startsWith("BE") -> "BEN"
            clean.startsWith("DA") -> "DAVID"
            else -> clean.split(" ").lastOrNull()
        }
    }
}
