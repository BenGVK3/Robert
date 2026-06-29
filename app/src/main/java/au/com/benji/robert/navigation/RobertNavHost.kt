package au.com.benji.robert.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import au.com.benji.robert.screens.dashboard.DashboardScreen
import au.com.benji.robert.screens.propagation.PropagationScreen
import au.com.benji.robert.screens.settings.SettingsScreen
import au.com.benji.robert.screens.tools.ToolsScreen
import au.com.benji.robert.screens.sdr.SdrScreen
import au.com.benji.robert.screens.aprs.AprsScreen
import au.com.benji.robert.screens.satellites.SatellitesScreen
import au.com.benji.robert.screens.tools.BandPlanScreen

@Composable
fun RobertNavHost(
    navController: NavHostController
) {

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {

        composable(Screen.Dashboard.route) {
            DashboardScreen(navController)
        }

        composable(Screen.Propagation.route) {
            PropagationScreen()
        }

        composable(Screen.Tools.route) {
            ToolsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(Screen.Sdr.route) {
            SdrScreen()
        }

        composable(Screen.Aprs.route) {
            AprsScreen()
        }

        composable(Screen.Satellites.route) {
            SatellitesScreen()
        }

        composable(Screen.BandPlan.route) {
            BandPlanScreen()
        }
    }
}
