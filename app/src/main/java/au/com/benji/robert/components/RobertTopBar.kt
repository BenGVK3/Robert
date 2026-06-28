package au.com.benji.robert.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import au.com.benji.robert.core.Constants
import au.com.benji.robert.theme.Spacing

@Composable
fun RobertTopBar() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
    ) {

        Text(
            text = Constants.APP_NAME,
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Operating Companion",
            style = MaterialTheme.typography.titleMedium
        )

        StatusChip(Constants.STATUS)
    }
}