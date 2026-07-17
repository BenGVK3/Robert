package au.com.benji.robert.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.screens.dashboard.DashboardScreen
import au.com.benji.robert.screens.propagation.PropagationScreen
import au.com.benji.robert.screens.moon.MoonScreen
import au.com.benji.robert.screens.logbook.LogbookScreen
import au.com.benji.robert.screens.logbook.LogbookEntryScreen
import au.com.benji.robert.screens.logbook.LogbookRadiosScreen
import au.com.benji.robert.screens.logbook.LogbookAntennasScreen
import au.com.benji.robert.screens.logbook.LogbookOperatorsScreen
import au.com.benji.robert.screens.logbook.LogbookStatsScreen
import au.com.benji.robert.screens.logbook.LogbookUserProfilesScreen
import au.com.benji.robert.screens.logbook.LogbookSyncScreen
import au.com.benji.robert.screens.logbook.LogbookSettingsScreen
import au.com.benji.robert.screens.logbook.LogbookActivationScreen
import au.com.benji.robert.screens.logbook.PileUpLoggingScreen
import au.com.benji.robert.screens.logbook.LogbookMapScreen
import au.com.benji.robert.screens.settings.SettingsScreen
import au.com.benji.robert.screens.tools.ToolsScreen
import au.com.benji.robert.screens.sdr.SdrScreen
import au.com.benji.robert.screens.dxlook.DxLookScreen
import au.com.benji.robert.screens.aprs.AprsScreen
import au.com.benji.robert.screens.satellites.SatellitesScreen
import au.com.benji.robert.screens.tools.BandPlanScreen
import au.com.benji.robert.screens.repeater.RepeaterMapScreen
import au.com.benji.robert.screens.repeater.RepeaterListScreen
import au.com.benji.robert.screens.repeater.RepeaterDetailScreen
import au.com.benji.robert.screens.propagation.BandDetailScreen
import au.com.benji.robert.screens.morse.MorseScreen

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
            PropagationScreen(paddingValues, dashboardViewModel = viewModel(), navController = navController)
        }

        composable(Screen.Moon.route) {
            MoonScreen(paddingValues)
        }

        composable(Screen.Logbook.route) {
            LogbookScreen(
                paddingValues = paddingValues,
                onNavigateToLogging = { navController.navigate(Screen.LogbookLogging.route) },
                onNavigateToActivation = { navController.navigate(Screen.LogbookActivation.route) },
                onNavigateToStats = { navController.navigate(Screen.LogbookStats.route) },
                onNavigateToSettings = { navController.navigate(Screen.LogbookSettings.route) },
                onNavigateToPileUp = { navController.navigate(Screen.LogbookPileUp.route) }
            )
        }

        composable(Screen.LogbookPileUp.route) {
            PileUpLoggingScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LogbookSettings.route) {
            LogbookSettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToOperators = { navController.navigate(Screen.LogbookOperators.route) },
                onNavigateToUserProfiles = { navController.navigate(Screen.LogbookUserProfiles.route) },
                onNavigateToSync = { navController.navigate(Screen.LogbookSync.route) }
            )
        }

        composable(Screen.LogbookSync.route) {
            LogbookSyncScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LogbookUserProfiles.route) {
            LogbookUserProfilesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LogbookActivation.route) {
            LogbookActivationScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LogbookLogging.route) {
            LogbookEntryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToMap = { grid: String -> navController.navigate(Screen.LogbookMap.createRoute(grid)) }
            )
        }

        composable(Screen.LogbookRadios.route) {
            LogbookRadiosScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LogbookAntennas.route) {
            LogbookAntennasScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LogbookOperators.route) {
            LogbookOperatorsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LogbookStats.route) {
            LogbookStatsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LogbookMap.route,
            arguments = listOf(navArgument("grid") { type = NavType.StringType })
        ) { backStackEntry ->
            val grid = backStackEntry.arguments?.getString("grid") ?: ""
            LogbookMapScreen(
                onBack = { navController.popBackStack() },
                grid = grid
            )
        }

        composable(
            route = "tools?initialTool={initialTool}",
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

        composable(Screen.DxLook.route) {
            DxLookScreen(paddingValues)
        }

        composable(Screen.Aprs.route) {
            AprsScreen(paddingValues)
        }

        composable(Screen.Satellites.route) {
            SatellitesScreen(paddingValues)
        }

        composable(Screen.Morse.route) {
            MorseScreen(
                paddingValues = paddingValues,
                onBack = { navController.popBackStack() }
            )
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

        composable(
            route = Screen.BandDetail.route,
            arguments = listOf(
                navArgument("band") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val band = backStackEntry.arguments?.getString("band") ?: ""
            BandDetailScreen(
                bandName = band,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
