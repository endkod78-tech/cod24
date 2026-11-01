package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ErrorCodeItem
import com.example.data.model.KodyarOrder
import com.example.data.model.KodyarTechnician
import com.example.data.model.ProblemItem
import com.example.data.model.SparePartItem
import com.example.data.repository.KodyarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KodyarViewModel(application: Application) : AndroidViewModel(application) {

    val repository = KodyarRepository(application.applicationContext)

    val orders: StateFlow<List<KodyarOrder>> = repository.orders
    val problems: StateFlow<List<ProblemItem>> = repository.problems
    val spareParts: StateFlow<List<SparePartItem>> = repository.spareParts
    val technicians: StateFlow<List<KodyarTechnician>> = repository.technicians
    val isInitialized: StateFlow<Boolean> = repository.isInitialized

    // Error codes search and filter
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
        }.take(100)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Technicians filter and search
    val technicianSearchQuery = MutableStateFlow("")
    val technicianSelectedSpecialty = MutableStateFlow("همه")

    val filteredTechnicians: StateFlow<List<KodyarTechnician>> = combine(
        repository.technicians,
        technicianSearchQuery,
        technicianSelectedSpecialty
    ) { techList, query, spec ->
        techList.filter { tech ->
            val matchSpec = spec == "همه" || tech.hasSpecialty(spec)
            val matchQuery = query.isBlank() ||
                    tech.getDisplayName().contains(query, ignoreCase = true) ||
                    (tech.city ?: "").contains(query, ignoreCase = true) ||
                    (tech.activeLocation ?: "").contains(query, ignoreCase = true) ||
                    tech.getResolvedSpecialties().any { it.contains(query, ignoreCase = true) }
            matchSpec && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected technician for dispatch
    var selectedTechnicianForOrder by mutableStateOf<KodyarTechnician?>(null)
    var selectedCategoryForOrder by mutableStateOf("ماشین لباسشویی")

    fun selectTechnicianForOrder(tech: KodyarTechnician?) {
        selectedTechnicianForOrder = tech
        if (tech != null) {
            val specialties = tech.getResolvedSpecialties()
            val matchedCategory = when {
                tech.hasSpecialty("ماشین لباسشویی") -> "ماشین لباسشویی"
                tech.hasSpecialty("پکیج و آبگرمکن") -> "پکیج و آبگرمکن"
                tech.hasSpecialty("یخچال و فریزر") -> "یخچال و فریزر"
                tech.hasSpecialty("ماشین ظرفشویی") -> "ماشین ظرفشویی"
                tech.hasSpecialty("کولر گازی و اسپلیت") -> "کولر گازی و اسپلیت"
                tech.hasSpecialty("مایکروویو و فر") -> "مایکروویو و فر"
                specialties.isNotEmpty() -> specialties.first()
                else -> "ماشین لباسشویی"
            }
            selectedCategoryForOrder = matchedCategory
        }
    }

    fun getTechniciansForCategory(cat: String): List<KodyarTechnician> {
        return repository.technicians.value.filter { it.hasSpecialty(cat) }
    }

    // Order cancellation state
    var orderToCancel by mutableStateOf<KodyarOrder?>(null)
    var isCancelling by mutableStateOf(false)
    var snackbarMessage by mutableStateOf<String?>(null)

    // Order creation state
    var isSubmitting by mutableStateOf(false)
    var submitErrorMessage by mutableStateOf<String?>(null)
    var submitSuccessMessage by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            repository.loadInitialData()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            repository.reloadTechnicians()
            repository.reloadOrders()
        }
    }

    fun requestCancelOrder(order: KodyarOrder) {
        orderToCancel = order
    }

    fun dismissCancelDialog() {
        orderToCancel = null
    }

    fun confirmCancelOrder() {
        val target = orderToCancel ?: return
        orderToCancel = null
        isCancelling = true

        viewModelScope.launch {
            val (success, message) = repository.cancelOrder(target.id)
            isCancelling = false
            snackbarMessage = message
        }
    }

    fun createOrder(
        name: String,
        phone: String,
        category: String,
        brand: String,
        model: String,
        errorCode: String,
        description: String,
        city: String,
        address: String,
        technician: KodyarTechnician? = selectedTechnicianForOrder,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank() || phone.isBlank() || category.isBlank() || city.isBlank()) {
            submitErrorMessage = "لطفاً تمامی فیلدهای الزامی (نام، شماره تماس، دسته دستگاه و شهر) را پر کنید."
            return
        }

        if (!phone.startsWith("09") || phone.length < 11) {
            submitErrorMessage = "شماره تلفن باید با ۰۹ شروع شود و ۱۱ رقم باشد."
            return
        }

        isSubmitting = true
        submitErrorMessage = null
        submitSuccessMessage = null

        viewModelScope.launch {
            val (success, message) = repository.submitOrder(
                customerName = name,
                customerPhone = phone,
                category = category,
                brand = brand,
                model = model,
                errorCode = errorCode,
                problemDescription = description,
                city = city,
                address = address,
                technicianId = technician?.id,
                technicianName = technician?.getDisplayName(),
                technicianPhone = technician?.phone
            )
            isSubmitting = false
            if (success) {
                submitSuccessMessage = message
                selectedTechnicianForOrder = null
                onSuccess()
            } else {
                submitErrorMessage = message
            }
        }
    }

    fun clearFeedback() {
        snackbarMessage = null
        submitErrorMessage = null
        submitSuccessMessage = null
    }
}
