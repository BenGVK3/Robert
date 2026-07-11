package au.com.benji.robert.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import au.com.benji.robert.screens.dashboard.DashboardScreen
import au.com.benji.robert.screens.propagation.PropagationScreen
import au.com.benji.robert.screens.moon.MoonScreen
import au.com.benji.robert.screens.logbook.LogbookScreen
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
    navController: NavHostController,
    paddingValues: PaddingValues,
    onShowDxSpots: () -> Unit = {},
    onShowShack: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                navController = navController,
                paddingValues = paddingValues,
                onShowDxSpots = onShowDxSpots,
                onShowShack = onShowShack
            )
        }

        composable(Screen.Propagation.route) {
            PropagationScreen(paddingValues)
        }

        composable(Screen.Moon.route) {
            MoonScreen(paddingValues)
        }

        composable(Screen.Logbook.route) {
            LogbookScreen(paddingValues)
        }

        composable(
            route = Screen.Tools.route,
            arguments = listOf(
                navArgument("initialTool") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val initialTool = backStackEntry.arguments?.getString("initialTool")
            ToolsScreen(navController, paddingValues, initialTool = initialTool)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(paddingValues)
        }

        composable(Screen.Sdr.route) {
            SdrScreen(paddingValues)
        }

        composable(Screen.Aprs.route) {
            AprsScreen(paddingValues)
        }

        composable(Screen.Satellites.route) {
            SatellitesScreen(paddingValues)
        }

        composable(Screen.BandPlan.route) {
            BandPlanScreen(paddingValues)
        }

        composable(Screen.RepeaterList.route) {
            RepeaterListScreen(
                onNavigateToDetail = { callsign, freq ->
                    navController.navigate(Screen.RepeaterDetail.createRoute(callsign, freq))
                },
                paddingValues = paddingValues
            )
        }

        composable(Screen.RepeaterMap.route) {
            RepeaterMapScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { callsign, freq ->
                    navController.navigate(Screen.RepeaterDetail.createRoute(callsign, freq))
                },
                paddingValues = paddingValues
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
                onBack = { navController.popBackStack() },
                paddingValues = paddingValues
            )
        }
    }
}
