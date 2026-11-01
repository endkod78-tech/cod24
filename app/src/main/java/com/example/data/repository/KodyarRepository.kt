package com.example.data.repository

import android.content.Context
import com.example.data.api.KodyarApi
import com.example.data.model.ApiResult
import com.example.data.model.ErrorCodeItem
import com.example.data.model.KodyarOrder
import com.example.data.model.ProblemItem
import com.example.data.model.SeedDatabase
import com.example.data.model.SparePartItem
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KodyarRepository(
    private val context: Context,
    private val api: KodyarApi = KodyarApi.create()
) {
    private val gson = Gson()
    private val sessionToken = "session_token_customer_user_96"

    private val _orders = MutableStateFlow<List<KodyarOrder>>(emptyList())
    val orders: StateFlow<List<KodyarOrder>> = _orders.asStateFlow()

    private val _errorCodes = MutableStateFlow<List<ErrorCodeItem>>(emptyList())
    val errorCodes: StateFlow<List<ErrorCodeItem>> = _errorCodes.asStateFlow()

    private val _problems = MutableStateFlow<List<ProblemItem>>(emptyList())
    val problems: StateFlow<List<ProblemItem>> = _problems.asStateFlow()

    private val _spareParts = MutableStateFlow<List<SparePartItem>>(emptyList())
    val spareParts: StateFlow<List<SparePartItem>> = _spareParts.asStateFlow()

    private val _technicians = MutableStateFlow<List<com.example.data.model.KodyarTechnician>>(emptyList())
    val technicians: StateFlow<List<com.example.data.model.KodyarTechnician>> = _technicians.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    suspend fun loadInitialData() = withContext(Dispatchers.IO) {
        if (_isInitialized.value) return@withContext
        try {
            context.assets.open("kodyar_database_seed.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val seed = gson.fromJson(reader, SeedDatabase::class.java)
                    if (seed != null) {
                        _errorCodes.value = seed.errorCodes
                        _problems.value = seed.problems
                        _spareParts.value = seed.spareParts
                        _technicians.value = seed.technicians
                        if (_orders.value.isEmpty()) {
                            _orders.value = seed.orders
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isInitialized.value = true
        }

        // Try syncing error codes and technicians
        reloadErrorCodes()
        reloadTechnicians()
    }

    suspend fun reloadErrorCodes() = withContext(Dispatchers.IO) {
        try {
            context.assets.open("kodyar_database_seed.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val seed = gson.fromJson(reader, SeedDatabase::class.java)
                    if (seed != null && seed.errorCodes.isNotEmpty()) {
                        if (_errorCodes.value.size != seed.errorCodes.size) {
                            _errorCodes.value = seed.errorCodes
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun reloadTechnicians() = withContext(Dispatchers.IO) {
        try {
            val res = api.getTechnicians(sessionToken)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                _technicians.value = res.body()!!
            }
        } catch (_: Exception) {}
    }

    suspend fun reloadOrders() = withContext(Dispatchers.IO) {
        try {
            val res = api.getOrders(sessionToken)
            if (res.isSuccessful && res.body() != null) {
                _orders.value = res.body()!!
            }
        } catch (_: Exception) {}
    }

    /**
     * Cancel an active order via POST /api/orders/{id}/status with {"status": "cancelled"}
     * Only completed after server confirms 2xx with status: "ok"
     */
    suspend fun cancelOrder(orderId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val res = api.setStatus(orderId, sessionToken, mapOf("status" to "cancelled"))
            val body = if (res.isSuccessful) {
                res.body()
            } else {
                val errorString = res.errorBody()?.string()
                runCatching { gson.fromJson(errorString, ApiResult::class.java) }.getOrNull()
            }

            if (res.isSuccessful && body?.status == "ok") {
                // Reload/update order status locally upon server success
                val currentList = _orders.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == orderId }
                if (index != -1) {
                    val existing = currentList[index]
                    currentList[index] = existing.copy(status = "cancelled")
                    _orders.value = currentList
                }
                return@withContext Pair(true, body.message ?: "سفارش با موفقیت لغو شد")
            } else {
                val msg = body?.message ?: "خطا در لغو سفارش"
                return@withContext Pair(false, msg)
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "خطا در ارتباط با سرور: ${e.localizedMessage ?: "نامشخص"}")
        }
    }

    /**
     * Check if customer phone number is blocked via POST /api/orders/check-blocked
     * If blocked == true, message is returned and order creation is rejected.
     */
    suspend fun checkPhoneBlocked(phone: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val cleanPhone = phone.trim()
        if (cleanPhone.length < 10) {
            return@withContext Pair(false, null)
        }

        try {
            val res = api.checkBlocked(sessionToken, mapOf("phone" to cleanPhone))
            val body = if (res.isSuccessful) {
                res.body()
            } else {
                val errStr = res.errorBody()?.string()
                runCatching { gson.fromJson(errStr, ApiResult::class.java) }.getOrNull()
            }

            if (body != null) {
                if (body.blocked == true || body.code == "order_blocked" || res.code == 403) {
                    return@withContext Pair(true, body.message ?: "شماره شما به دلیل ۳ بار لغو، مسدود شده است.")
                }
            }
            return@withContext Pair(false, null)
        } catch (e: Exception) {
            return@withContext Pair(false, null)
        }
    }

    /**
     * Submit customer repair order.
     * Checks check-blocked first. Shows error if blocked or if server rejects with 403 order_blocked.
     * No success message shown until server confirms 2xx and status: "ok".
     */
    suspend fun submitOrder(
        customerName: String,
        customerPhone: String,
        category: String,
        brand: String,
        model: String,
        errorCode: String,
        problemDescription: String,
        city: String,
        address: String,
        technicianId: String? = null,
        technicianName: String? = null,
        technicianPhone: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        // 1. Check blocked status before creating order
        val (isBlocked, blockedMessage) = checkPhoneBlocked(customerPhone)
        if (isBlocked) {
            return@withContext Pair(false, blockedMessage ?: "امکان ثبت سفارش برای شماره شما بسته شده است.")
        }

        val requestBody = mutableMapOf(
            "customer_name" to customerName,
            "customer_phone" to customerPhone,
            "category" to category,
            "brand" to brand,
            "model" to model,
            "error_code" to errorCode,
            "problem_description" to problemDescription,
            "city" to city,
            "address" to address
        )
        if (!technicianId.isNullOrBlank()) requestBody["technician_id"] = technicianId
        if (!technicianName.isNullOrBlank()) requestBody["technician_name"] = technicianName
        if (!technicianPhone.isNullOrBlank()) requestBody["technician_phone"] = technicianPhone

        try {
            val res = api.createOrder(sessionToken, requestBody)
            val body = if (res.isSuccessful) {
                res.body()
            } else {
                val errStr = res.errorBody()?.string()
                runCatching { gson.fromJson(errStr, ApiResult::class.java) }.getOrNull()
            }

            // Check if blocked in creation response
            if (res.code == 403 || body?.code == "order_blocked" || body?.blocked == true) {
                return@withContext Pair(false, body?.message ?: "امکان ثبت سفارش برای شماره شما بسته شده است.")
            }

            if (res.isSuccessful && body?.status == "ok") {
                val newOrderId = body.orderId ?: "order_${System.currentTimeMillis() % 100000}"
                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                val newOrder = KodyarOrder(
                    id = newOrderId,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    category = category,
                    brand = brand,
                    model = model,
                    errorCode = errorCode,
                    problemDescription = problemDescription,
                    city = city,
                    address = address,
                    status = "pending",
                    createdAt = nowStr,
                    technicianId = technicianId,
                    technicianName = technicianName,
                    technicianPhone = technicianPhone
                )
                _orders.value = listOf(newOrder) + _orders.value
                return@withContext Pair(true, body.message ?: "سفارش شما با موفقیت ثبت شد.")
            } else {
                val msg = body?.message ?: "خطا در ثبت سفارش (کد ${res.code})"
                return@withContext Pair(false, msg)
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "خطا در برقراری ارتباط با سرور کدیار: ${e.localizedMessage ?: "نامشخص"}")
        }
    }
}
