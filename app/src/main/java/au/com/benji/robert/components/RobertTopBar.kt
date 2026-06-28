package au.com.benji.robert.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.benji.robert.core.Constants
import au.com.benji.robert.theme.Spacing

@Composable
fun RobertTopBar() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Constants.APP_NAME,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center,
            letterSpacing = 4.sp
        )
        Text(
            text = "Radio Operator's Band Exploration & Resource Tool",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = Spacing.Large)
        )
        
        Spacer(modifier = Modifier.height(Spacing.Medium))
        
        StatusIndicator(Constants.STATUS)
    }
}

@Composable
fun StatusIndicator(status: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.extraLarge
            )
            .padding(horizontal = Spacing.Small, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = Color(0xFF4CAF50), shape = CircleShape)
        )
        Text(
            text = "ALL SYSTEMS GO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}
