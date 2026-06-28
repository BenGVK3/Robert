package au.com.benji.robert.navigation

sealed class Screen(
    val route: String
) {
    object Dashboard : Screen("dashboard")
    object Propagation : Screen("propagation")
    object Shack : Screen("shack")
    object AddEquipment : Screen("add_equipment")
    object Tools : Screen("tools")
    object Settings : Screen("settings")

    object Sdr : Screen("sdr")
    object Aprs : Screen("aprs")
    object Satellites : Screen("satellites")
    object Logbook : Screen("logbook")
}