package au.com.benji.robert.screens.dashboard

import androidx.lifecycle.ViewModel
import au.com.benji.robert.models.InfoCardModel
import au.com.benji.robert.models.QuickAction
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.repository.DashboardRepository

class DashboardViewModel : ViewModel() {

    private val repository = DashboardRepository()

    val cards = repository.getDashboardCards()

    val quickActions = listOf(
        QuickAction("📡", "Propagation", Screen.Propagation.route),
        QuickAction("📻", "SDR", Screen.Sdr.route),
        QuickAction("📍", "APRS", Screen.Aprs.route),
        QuickAction("🛰", "Satellites", Screen.Satellites.route),
        QuickAction("📖", "Logbook", Screen.Logbook.route),
        QuickAction("🔧", "Shack", Screen.Shack.route),
        QuickAction("🛠", "Tools", Screen.Tools.route),
        QuickAction("⚙", "Settings", Screen.Settings.route)
    )
}