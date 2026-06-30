package au.com.benji.robert.navigation

sealed class Screen(
    val route: String
) {
    object Dashboard : Screen("dashboard")
    object Propagation : Screen("propagation")
    object Tools : Screen("tools")
    object Settings : Screen("settings")

    object Sdr : Screen("sdr")
    object Aprs : Screen("aprs")
    object Satellites : Screen("satellites")
    object BandPlan : Screen("bandplan")
    object RepeaterList : Screen("repeaterlist")
    object RepeaterMap : Screen("repeatermap")
    object RepeaterDetail : Screen("repeaterdetail/{callsign}/{frequency}") {
        fun createRoute(callsign: String, frequency: String) = "repeaterdetail/$callsign/$frequency"
    }
}