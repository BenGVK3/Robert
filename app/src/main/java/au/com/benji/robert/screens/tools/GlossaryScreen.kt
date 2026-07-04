package au.com.benji.robert.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.GlossaryCategory
import au.com.benji.robert.models.GlossaryItem
import au.com.benji.robert.repository.GlossaryRepository
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.viewmodel.GlossaryViewModel
import au.com.benji.robert.viewmodel.RobertViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryScreen(
    paddingValues: PaddingValues = PaddingValues()
) {
    val repository = remember { GlossaryRepository() }
    val viewModel: GlossaryViewModel = viewModel(
        factory = RobertViewModelFactory { GlossaryViewModel(repository) }
    )

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val items by viewModel.filteredItems.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val commonItems = viewModel.commonItems

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = Spacing.Medium)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.Small),
            placeholder = { Text("Search Terms...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(Spacing.Medium),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Category Buttons Grid
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            CategoryButton(
                title = "Common",
                isSelected = selectedCategory == null && searchQuery.isEmpty(),
                onClick = { viewModel.selectCategory(null); viewModel.updateSearchQuery("") },
                modifier = Modifier.weight(1f)
            )
            CategoryButton(
                title = "Q Codes",
                isSelected = selectedCategory == GlossaryCategory.Q_CODE,
                onClick = { viewModel.selectCategory(GlossaryCategory.Q_CODE) },
                modifier = Modifier.weight(1f)
            )
            CategoryButton(
                title = "Numeric",
                isSelected = selectedCategory == GlossaryCategory.NUMERIC_CODE,
                onClick = { viewModel.selectCategory(GlossaryCategory.NUMERIC_CODE) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            CategoryButton(
                title = "Jargon",
                isSelected = selectedCategory == GlossaryCategory.JARGON,
                onClick = { viewModel.selectCategory(GlossaryCategory.JARGON) },
                modifier = Modifier.weight(1f)
            )
            CategoryButton(
                title = "Phonetic",
                isSelected = selectedCategory == GlossaryCategory.PHONETIC,
                onClick = { viewModel.selectCategory(GlossaryCategory.PHONETIC) },
                modifier = Modifier.weight(1f)
            )
            CategoryButton(
                title = "All",
                isSelected = selectedCategory == null && searchQuery.isNotEmpty(),
                onClick = { viewModel.selectCategory(null) },
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = Spacing.Large)
        ) {
            if (selectedCategory == null && searchQuery.isEmpty()) {
                item {
                    Text(
                        text = "Most Used",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(commonItems) { item ->
                    CompactGlossaryCard(item)
                }
            } else {
                item {
                    Text(
                        text = when {
                            selectedCategory != null -> selectedCategory.toString().replace("_", " ")
                            searchQuery.isNotEmpty() -> "Search Results"
                            else -> "All Terms"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(items) { item ->
                    CompactGlossaryCard(item)
                }
            }
        }
    }
}

@Composable
fun CategoryButton(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CompactGlossaryCard(item: GlossaryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.term,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(65.dp)
            )
            
            VerticalDivider(
                modifier = Modifier.height(20.dp).padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            Text(
                text = item.definition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
