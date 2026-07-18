package au.com.benji.robert.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.benji.robert.theme.Spacing

@Composable
fun RobertHeader(
    title: String,
    description: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    isCentered: Boolean = true,
    isHorizontal: Boolean = false
) {
    if (isHorizontal) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCentered) Arrangement.Center else Arrangement.Start,
            modifier = modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(Modifier.width(Spacing.Small))
            
            Column(horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF03DAC6)
                )
            }
        }
    } else {
        Column(
            horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.Medium)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Column(horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = if (isCentered) TextAlign.Center else TextAlign.Start
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF03DAC6),
                    textAlign = if (isCentered) TextAlign.Center else TextAlign.Start
                )
            }
        }
    }
}
