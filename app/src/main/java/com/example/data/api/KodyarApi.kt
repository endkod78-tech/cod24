package com.example.data.api

import com.example.data.model.ApiResult
import com.example.data.model.KodyarOrder
import com.example.data.model.KodyarTechnician
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

interface KodyarApi {
    suspend fun setStatus(id: String, token: String, body: Map<String, String>): NetworkResponse<ApiResult>
    suspend fun checkBlocked(token: String, body: Map<String, String>): NetworkResponse<ApiResult>
    suspend fun createOrder(token: String, body: Map<String, String>): NetworkResponse<ApiResult>
    suspend fun getOrders(token: String): NetworkResponse<List<KodyarOrder>>
    suspend fun getTechnicians(token: String): NetworkResponse<List<KodyarTechnician>>

    companion object {
        private const val BASE_URL = "https://kodyar24.ir/"

        fun create(): KodyarApi = KodyarApiImpl()
    }
}

data class NetworkResponse<T>(
    val isSuccessful: Boolean,
    val code: Int,
    private val data: T?,
    private val errorText: String? = null
) {
    fun body(): T? = data
    fun errorBody(): ErrorBody? = if (errorText != null) ErrorBody(errorText) else null
}

data class ErrorBody(val text: String) {
    fun string(): String = text
}

class KodyarApiImpl(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson(),
    private val baseUrl: String = "https://kodyar24.ir/"
) : KodyarApi {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun setStatus(
        id: String,
        token: String,
        body: Map<String, String>
    ): NetworkResponse<ApiResult> = withContext(Dispatchers.IO) {
        val json = gson.toJson(body)
        val requestBody = json.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${baseUrl}api/orders/$id/status")
            .header("X-Session-Token", token)
            .post(requestBody)
            .build()

        executeRequest(request, ApiResult::class.java)
    }

    override suspend fun checkBlocked(
        token: String,
        body: Map<String, String>
    ): NetworkResponse<ApiResult> = withContext(Dispatchers.IO) {
        val json = gson.toJson(body)
        val requestBody = json.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${baseUrl}api/orders/check-blocked")
            .header("X-Session-Token", token)
            .post(requestBody)
            .build()

        executeRequest(request, ApiResult::class.java)
    }

    override suspend fun createOrder(
        token: String,
        body: Map<String, String>
    ): NetworkResponse<ApiResult> = withContext(Dispatchers.IO) {
        val json = gson.toJson(body)
        val requestBody = json.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${baseUrl}api/orders")
            .header("X-Session-Token", token)
            .post(requestBody)
            .build()

        executeRequest(request, ApiResult::class.java)
    }

    override suspend fun getOrders(token: String): NetworkResponse<List<KodyarOrder>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${baseUrl}api/orders")
            .header("X-Session-Token", token)
            .get()
            .build()

        val type = object : TypeToken<List<KodyarOrder>>() {}.type
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (response.isSuccessful && bodyStr != null) {
                    val parsed = gson.fromJson<List<KodyarOrder>>(bodyStr, type)
                    NetworkResponse(true, response.code, parsed)
                } else {
                    NetworkResponse(false, response.code, null, bodyStr)
                }
            }
        } catch (e: Exception) {
            NetworkResponse(false, 0, null, e.message)
        }
    }

    override suspend fun getTechnicians(token: String): NetworkResponse<List<KodyarTechnician>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${baseUrl}api/technicians")
            .header("X-Session-Token", token)
            .get()
            .build()

        val listType = object : TypeToken<List<KodyarTechnician>>() {}.type
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (response.isSuccessful && !bodyStr.isNullOrBlank()) {
                    try {
                        val rootElement = gson.fromJson(bodyStr, JsonElement::class.java)
                        val techniciansList = when {
                            rootElement.isJsonArray -> {
                                gson.fromJson<List<KodyarTechnician>>(rootElement, listType)
                            }
                            rootElement.isJsonObject -> {
                                val obj = rootElement.asJsonObject
                                when {
                                    obj.has("technicians") && obj.get("technicians").isJsonArray ->
                                        gson.fromJson<List<KodyarTechnician>>(obj.get("technicians"), listType)
                                    obj.has("data") && obj.get("data").isJsonArray ->
                                        gson.fromJson<List<KodyarTechnician>>(obj.get("data"), listType)
                                    obj.has("techs") && obj.get("techs").isJsonArray ->
                                        gson.fromJson<List<KodyarTechnician>>(obj.get("techs"), listType)
                                    else -> emptyList()
                                }
                            }
                            else -> emptyList()
                        }
                        NetworkResponse(true, response.code, techniciansList)
                    } catch (e: Exception) {
                        NetworkResponse(false, response.code, null, e.message)
                    }
                } else {
                    NetworkResponse(false, response.code, null, bodyStr)
                }
            }
        } catch (e: Exception) {
            NetworkResponse(false, 0, null, e.message)
        }
    }

    private fun <T> executeRequest(request: Request, clazz: Class<T>): NetworkResponse<T> {
        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (response.isSuccessful && bodyStr != null) {
                    val parsed = gson.fromJson(bodyStr, clazz)
                    NetworkResponse(true, response.code, parsed)
                } else {
                    NetworkResponse(false, response.code, null, bodyStr)
                }
            }
        } catch (e: Exception) {
            NetworkResponse(false, 0, null, e.message)
        }
    }
}
