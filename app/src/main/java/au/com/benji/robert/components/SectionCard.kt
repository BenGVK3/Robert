package au.com.benji.robert.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing

@Composable
fun SectionCard(
    content: @Composable () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = RobertColors.CardBackground
        )
    ) {

        Column(
            modifier = Modifier.padding(Spacing.Medium)
        ) {
            content()
        }
    }
}