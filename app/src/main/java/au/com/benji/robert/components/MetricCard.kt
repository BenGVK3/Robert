package au.com.benji.robert.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: String = ""
) {

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = RobertColors.CardBackground
        )
    ) {

        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
        ) {

            if (icon.isNotBlank()) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}