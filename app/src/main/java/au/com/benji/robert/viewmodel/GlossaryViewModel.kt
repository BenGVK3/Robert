package au.com.benji.robert.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.benji.robert.models.GlossaryCategory
import au.com.benji.robert.models.GlossaryItem
import au.com.benji.robert.repository.GlossaryRepository
import kotlinx.coroutines.flow.*

class GlossaryViewModel(
    private val repository: GlossaryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<GlossaryCategory?>(null)
    val selectedCategory: StateFlow<GlossaryCategory?> = _selectedCategory.asStateFlow()

    private val allItems = repository.getGlossaryItems()

    val commonItems = allItems.filter { it.isCommon }

    val filteredItems: StateFlow<List<GlossaryItem>> = combine(_searchQuery, _selectedCategory) { query, category ->
        allItems.filter { item ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                item.term.contains(query, ignoreCase = true) || 
                item.definition.contains(query, ignoreCase = true)
            }
            
            val matchesCategory = if (category == null) true else item.category == category
            
            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = allItems
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: GlossaryCategory?) {
        _selectedCategory.value = category
    }
}
