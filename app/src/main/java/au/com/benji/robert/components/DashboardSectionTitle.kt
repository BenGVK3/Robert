package au.com.benji.robert.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DashboardSectionTitle(
    title: String
) {

    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall
    )
}