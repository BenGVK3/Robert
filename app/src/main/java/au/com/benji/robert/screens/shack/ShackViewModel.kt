package au.com.benji.robert.screens.shack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.models.ShackItemUi
import au.com.benji.robert.repository.ShackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ShackViewModel(
    private val repository: ShackRepository
) : ViewModel() {

    private val _equipment = MutableStateFlow<List<ShackItemUi>>(emptyList())
    val equipment: StateFlow<List<ShackItemUi>> = _equipment

    init {
        viewModelScope.launch {
            repository.equipment().collectLatest { items ->
                _equipment.value =
                    if (items.isEmpty()) {
                        listOf(
                            ShackItemUi(
                                id = 0,
                                title = "No equipment yet",
                                subtitle = "Tap + to add your first item"
                            )
                        )
                    } else {
                        items.map { it.toUi() }
                    }
            }
        }
    }
}

private fun ShackEntity.toUi() = ShackItemUi(
    id = id,
    title = listOf(manufacturer, model)
        .filter { it.isNotBlank() }
        .joinToString(" "),
    subtitle = nickname.ifBlank { category }
)