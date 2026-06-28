package au.com.benji.robert.models

data class InfoCardModel(
    val type: CardType = CardType.INFO,
    val title: String,
    val value: String,
    val icon: String = ""
)