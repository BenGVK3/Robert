package au.com.benji.robert.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import au.com.benji.robert.theme.Spacing

@Composable
fun DashboardSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        modifier = modifier.padding(vertical = Spacing.Small),
        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified // Adjusted for design
    )
}
