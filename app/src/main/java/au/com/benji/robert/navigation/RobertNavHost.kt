package au.com.benji.robert.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import au.com.benji.robert.screens.dashboard.DashboardScreen
import au.com.benji.robert.screens.propagation.PropagationScreen
import au.com.benji.robert.screens.settings.SettingsScreen
import au.com.benji.robert.screens.tools.ToolsScreen
import au.com.benji.robert.screens.sdr.SdrScreen
import au.com.benji.robert.screens.aprs.AprsScreen
import au.com.benji.robert.screens.satellites.SatellitesScreen
import au.com.benji.robert.screens.tools.BandPlanScreen
import au.com.benji.robert.screens.repeater.RepeaterMapScreen
import au.com.benji.robert.screens.repeater.RepeaterListScreen
import au.com.benji.robert.screens.repeater.RepeaterDetailScreen

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
            ToolsScreen(navController)
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

        composable(Screen.RepeaterList.route) {
            RepeaterListScreen(
                onNavigateToDetail = { callsign, freq ->
                    navController.navigate(Screen.RepeaterDetail.createRoute(callsign, freq))
                }
            )
        }

        composable(Screen.RepeaterMap.route) {
            RepeaterMapScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { callsign, freq ->
                    navController.navigate(Screen.RepeaterDetail.createRoute(callsign, freq))
                }
            )
        }

        composable(
            route = Screen.RepeaterDetail.route,
            arguments = listOf(
                navArgument("callsign") { type = NavType.StringType },
                navArgument("frequency") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val callsign = backStackEntry.arguments?.getString("callsign") ?: ""
            val frequency = backStackEntry.arguments?.getString("frequency") ?: ""
            RepeaterDetailScreen(
                callsign = callsign,
                frequency = frequency,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
