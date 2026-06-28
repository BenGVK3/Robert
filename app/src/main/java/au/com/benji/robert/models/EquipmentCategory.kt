package au.com.benji.robert.models

enum class EquipmentCategory(
    val displayName: String
) {
    RADIO("Radio"),
    ANTENNA("Antenna"),
    POWER_SUPPLY("Power Supply"),
    TUNER("Antenna Tuner"),
    AMPLIFIER("Amplifier"),
    MICROPHONE("Microphone"),
    KEY("CW Key"),
    COAX("Coax"),
    FILTER("Filter"),
    ANALYSER("Analyser"),
    SDR("SDR"),
    COMPUTER("Computer"),
    ACCESSORY("Accessory"),
    OTHER("Other")
}