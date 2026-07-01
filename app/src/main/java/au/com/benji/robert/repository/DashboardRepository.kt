package au.com.benji.robert.repository

import au.com.benji.robert.models.CardType
import au.com.benji.robert.models.InfoCardModel

class DashboardRepository {

    fun getDashboardCards(): List<InfoCardModel> {

        return listOf(

            InfoCardModel(
                type = CardType.BAND,
                icon = "📡",
                title = "Band Recommendation",
                value = "20m is open to Europe"
            ),

            InfoCardModel(
                type = CardType.SOLAR,
                icon = "☀️",
                title = "Solar Flux",
                value = "128"
            ),

            InfoCardModel(
                type = CardType.SOLAR,
                icon = "🌍",
                title = "K Index",
                value = "2"
            ),

            InfoCardModel(
                type = CardType.SOLAR,
                icon = "📶",
                title = "MUF",
                value = "31 MHz"
            ),

            InfoCardModel(
                type = CardType.WEATHER,
                icon = "🌤",
                title = "Weather",
                value = "14°C • Clear"
            )
        )
    }
}
