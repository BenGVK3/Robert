package au.com.benji.robert.models

import kotlinx.serialization.Serializable

@Serializable
data class GlossaryItem(
    val term: String,
    val definition: String,
    val category: GlossaryCategory,
    val isCommon: Boolean = false
)

enum class GlossaryCategory {
    Q_CODE,
    NUMERIC_CODE, // e.g., 93 Codes, 73, etc.
    JARGON,
    PHONETIC,
    PROSIGN
}
