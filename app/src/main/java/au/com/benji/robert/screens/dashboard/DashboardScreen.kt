package au.com.benji.robert.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.components.DashboardSectionTitle
import au.com.benji.robert.components.MetricCard
import au.com.benji.robert.components.QuickActionCard
import au.com.benji.robert.components.RobertTopBar
import au.com.benji.robert.theme.Spacing

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = viewModel(),
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {

        RobertTopBar()

        HorizontalDivider()

        DashboardSectionTitle("Conditions")

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {

            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Solar Flux",
                value = "128",
                icon = "☀️"
            )

            MetricCard(
                modifier = Modifier.weight(1f),
                title = "K Index",
                value = "2",
                icon = "🌍"
            )
        }

        DashboardSectionTitle("Quick Actions")

        repeat(4) { row ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {

                val first = viewModel.quickActions[row * 2]
                val second = viewModel.quickActions[row * 2 + 1]

                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = first.icon,
                    title = first.title,
                    onClick = {
                        navController.navigate(first.route)
                    }
                )

                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = second.icon,
                    title = second.title,
                    onClick = {
                        navController.navigate(second.route)
                    }
                )
            }
        }

        DashboardSectionTitle("Band Conditions")

        viewModel.cards.forEach { card ->

            MetricCard(
                title = card.title,
                value = card.value,
                icon = card.icon
            )
        }
    }
}