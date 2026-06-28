package au.com.benji.robert.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.benji.robert.core.Constants
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing

@Composable
fun RobertTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Constants.APP_NAME,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "OPERATING COMPANION",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified, // Adjusted in UI
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            StatusIndicator(Constants.STATUS)
        }
    }
}

@Composable
fun StatusIndicator(status: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraLarge
            )
            .padding(horizontal = Spacing.Small, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = RobertColors.StatusGreen, shape = CircleShape)
        )
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = RobertColors.StatusGreen
        )
    }
}
