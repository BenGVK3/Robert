package au.com.benji.robert.screens.shack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.repository.ShackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShackViewModel(
    private val repository: ShackRepository
) : ViewModel() {

    val equipment: StateFlow<List<ShackEntity>> = repository.equipment()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteEquipment(item: ShackEntity) {
        viewModelScope.launch {
            repository.deleteEquipment(item)
        }
    }
}
