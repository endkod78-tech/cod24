package com.kodyar.diagnostic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kodyar.diagnostic.model.ErrorCodeItem
import com.kodyar.diagnostic.model.ProblemItem
import com.kodyar.diagnostic.model.SparePartItem
import com.kodyar.diagnostic.repository.DiagnosticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiagnosticViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DiagnosticRepository(application.applicationContext)

    val problems: StateFlow<List<ProblemItem>> = repository.problems
    val spareParts: StateFlow<List<SparePartItem>> = repository.spareParts
    val isLoaded: StateFlow<Boolean> = repository.isLoaded

    // Search and filter state
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("همه")

    val filteredErrorCodes: StateFlow<List<ErrorCodeItem>> = combine(
        repository.errorCodes,
        searchQuery,
        selectedCategory
    ) { codes, query, cat ->
        codes.filter { item ->
            val matchCat = cat == "همه" || item.category.contains(cat, ignoreCase = true)
            val matchQuery = query.isBlank() ||
                    item.code.contains(query, ignoreCase = true) ||
                    item.brand.contains(query, ignoreCase = true) ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true)
            matchCat && matchQuery
        }.take(100) // Top 100 for fast smooth Compose rendering
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.loadData()
        }
    }
}
