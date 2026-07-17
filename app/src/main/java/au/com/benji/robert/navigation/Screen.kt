package au.com.benji.robert.navigation

sealed class Screen(
    val route: String
) {
    object Dashboard : Screen("dashboard")
    object Propagation : Screen("propagation")
    object Logbook : Screen("logbook")
    object LogbookLogging : Screen("logbook/logging")
    object LogbookRadios : Screen("logbook/radios")
    object LogbookAntennas : Screen("logbook/antennas")
    object LogbookOperators : Screen("logbook/operators")
    object LogbookStats : Screen("logbook/stats")
    object LogbookSettings : Screen("logbook/settings")
    object LogbookSync : Screen("logbook/sync")
    object LogbookUserProfiles : Screen("logbook/user_profiles")
    object LogbookActivation : Screen("logbook/activation")
    object LogbookPileUp : Screen("logbook/pileup")
    object LogbookMap : Screen("logbook/map/{grid}") {
        fun createRoute(grid: String) = "logbook/map/$grid"
    }
    object Tools : Screen("tools") {
        fun createRoute(initialTool: String? = null) = if (initialTool != null) "tools?initialTool=$initialTool" else "tools"
    }
    object Settings : Screen("settings")

    object Sdr : Screen("sdr")
    object Aprs : Screen("aprs")
    object Satellites : Screen("satellites")
    object Moon : Screen("moon")
    object DxLook : Screen("dxlook")
    object BandPlan : Screen("bandplan")
    object RepeaterList : Screen("repeaterlist")
    object RepeaterMap : Screen("repeatermap")
    object RepeaterDetail : Screen("repeaterdetail/{callsign}/{frequency}") {
        fun createRoute(callsign: String, frequency: String) = "repeaterdetail/$callsign/$frequency"
    }
    object BandDetail : Screen("banddetail/{band}") {
        fun createRoute(band: String) = "banddetail/$band"
    }
    object Morse : Screen("morse")
}