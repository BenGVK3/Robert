package au.com.benji.robert.navigation

sealed class Screen(
    val route: String
) {
    object Dashboard : Screen("dashboard")
    object Propagation : Screen("propagation")
    object Logbook : Screen("logbook")
    object Tools : Screen("tools") {
        fun createRoute(initialTool: String? = null) = if (initialTool != null) "tools?initialTool=$initialTool" else "tools"
    }
    object Settings : Screen("settings")

    object Sdr : Screen("sdr")
    object Aprs : Screen("aprs")
    object Satellites : Screen("satellites")
    object Moon : Screen("moon")
    object BandPlan : Screen("bandplan")
    object RepeaterList : Screen("repeaterlist")
    object RepeaterMap : Screen("repeatermap")
    object RepeaterDetail : Screen("repeaterdetail/{callsign}/{frequency}") {
        fun createRoute(callsign: String, frequency: String) = "repeaterdetail/$callsign/$frequency"
    }
}