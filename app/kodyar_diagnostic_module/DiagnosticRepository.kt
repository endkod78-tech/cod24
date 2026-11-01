package com.kodyar.diagnostic.repository

import android.content.Context
import com.google.gson.Gson
import com.kodyar.diagnostic.model.DiagnosticSeedDatabase
import com.kodyar.diagnostic.model.ErrorCodeItem
import com.kodyar.diagnostic.model.ProblemItem
import com.kodyar.diagnostic.model.SparePartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class DiagnosticRepository(private val context: Context) {
    private val gson = Gson()

    private val _errorCodes = MutableStateFlow<List<ErrorCodeItem>>(emptyList())
    val errorCodes: StateFlow<List<ErrorCodeItem>> = _errorCodes.asStateFlow()

    private val _problems = MutableStateFlow<List<ProblemItem>>(emptyList())
    val problems: StateFlow<List<ProblemItem>> = _problems.asStateFlow()

    private val _spareParts = MutableStateFlow<List<SparePartItem>>(emptyList())
    val spareParts: StateFlow<List<SparePartItem>> = _spareParts.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    suspend fun loadData() = withContext(Dispatchers.IO) {
        if (_isLoaded.value) return@withContext
        try {
            context.assets.open("kodyar_database_seed.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val seed = gson.fromJson(reader, DiagnosticSeedDatabase::class.java)
                    if (seed != null) {
                        _errorCodes.value = seed.errorCodes
                        _problems.value = seed.problems
                        _spareParts.value = seed.spareParts
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoaded.value = true
        }
    }
}
