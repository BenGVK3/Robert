package au.com.benji.robert.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = RobertColors.CardBackground
        )
    ) {

        Column(
            modifier = Modifier.padding(Spacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = icon,
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}