package au.com.benji.robert.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import au.com.benji.robert.theme.Spacing

@Composable
fun StatusChip(
    text: String
) {

    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(
                horizontal = Spacing.Small,
                vertical = Spacing.ExtraSmall
            ),
        style = MaterialTheme.typography.labelMedium
    )
}