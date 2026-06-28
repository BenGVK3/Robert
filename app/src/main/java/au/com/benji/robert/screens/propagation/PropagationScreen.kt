package au.com.benji.robert.screens.propagation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import au.com.benji.robert.theme.Spacing

@Composable
fun PropagationScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {

        Text(
            text = "Propagation",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Coming soon...")
    }
}