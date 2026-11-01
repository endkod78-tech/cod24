package com.example.data.model

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

data class ApiResult(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("blocked") val blocked: Boolean? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("order_id") val orderId: String? = null
)

data class KodyarOrder(
    @SerializedName("id") val id: String = "",
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("error_code") val errorCode: String? = null,
    @SerializedName("problem_description") val problemDescription: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("status") val status: String = "pending", // pending, accepted, in_progress, completed, cancelled
    @SerializedName("amount") val amount: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("technician_id") val technicianId: String? = null,
    @SerializedName("technician_name") val technicianName: String? = null,
    @SerializedName("technician_phone") val technicianPhone: String? = null
)

data class KodyarTechnician(
    @SerializedName("id") val id: String = "",
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("active_location") val activeLocation: String? = null,
    @SerializedName("specialties") val specialties: JsonElement? = null,
    @SerializedName("specialty") val specialty: JsonElement? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("status") val status: String? = "active",
    @SerializedName("rating") val rating: JsonElement? = null,
    @SerializedName("completed_orders") val completedOrders: JsonElement? = null,
    @SerializedName("is_verified") val isVerified: JsonElement? = null
) {
    fun getDisplayName(): String {
        return fullName?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() }
            ?: "تکنسین کدیار"
    }

    fun getRatingString(): String {
        return try {
            when {
                rating == null || rating.isJsonNull -> "5.0"
                rating.isJsonPrimitive -> rating.asString
                else -> "5.0"
            }
        } catch (_: Exception) {
            "5.0"
        }
    }

    fun getResolvedSpecialties(): List<String> {
        val list = mutableListOf<String>()

        fun extractFromElement(element: JsonElement?) {
            if (element == null || element.isJsonNull) return
            if (element.isJsonArray) {
                element.asJsonArray.forEach { item ->
                    if (item.isJsonPrimitive) {
                        val str = item.asString.trim()
                        if (str.isNotBlank() && !list.contains(str)) {
                            list.add(str)
                        }
                    }
                }
            } else if (element.isJsonPrimitive) {
                val raw = element.asString.trim()
                if (raw.isNotBlank()) {
                    if (raw.startsWith("[") && raw.endsWith("]")) {
                        try {
                            val parsedArray = JsonParser.parseString(raw).asJsonArray
                            parsedArray.forEach { item ->
                                val s = item.asString.trim()
                                if (s.isNotBlank() && !list.contains(s)) list.add(s)
                            }
                        } catch (_: Exception) {
                            if (!list.contains(raw)) list.add(raw)
                        }
                    } else if (raw.contains("،") || raw.contains(",")) {
                        val parts = raw.split("،", ",").map { it.trim() }.filter { it.isNotBlank() }
                        parts.forEach { if (!list.contains(it)) list.add(it) }
                    } else {
                        if (!list.contains(raw)) list.add(raw)
                    }
                }
            }
        }

        extractFromElement(specialties)
        extractFromElement(specialty)

        return if (list.isEmpty()) {
            listOf("خدمات عمومی و تاسیسات")
        } else {
            list
        }
    }

    /**
     * Checks if this technician specializes in the requested category.
     * Matches exact names or core keywords (e.g. لباسشویی, پکیج, یخچال, ظرفشویی, کولر, مایکروویو).
     */
    fun hasSpecialty(queryCategory: String): Boolean {
        if (queryCategory == "همه" || queryCategory.isBlank()) return true
        val resolved = getResolvedSpecialties()
        
        // Technicians offering all services
        if (resolved.any { it.contains("کلیه خدمات", ignoreCase = true) || it.contains("همه", ignoreCase = true) }) {
            return true
        }

        val cleanQuery = queryCategory.trim()
        val keyword = when {
            cleanQuery.contains("لباسشویی") -> "لباسشویی"
            cleanQuery.contains("ظرفشویی") -> "ظرفشویی"
            cleanQuery.contains("پکیج") || cleanQuery.contains("آبگرمکن") -> "پکیج"
            cleanQuery.contains("یخچال") || cleanQuery.contains("فریزر") -> "یخچال"
            cleanQuery.contains("کولر") || cleanQuery.contains("اسپلیت") -> "کولر"
            cleanQuery.contains("مایکروویو") || cleanQuery.contains("فر") -> "مایکروویو"
            else -> cleanQuery
        }

        return resolved.any { it.contains(keyword, ignoreCase = true) || it.contains(cleanQuery, ignoreCase = true) }
    }
}

data class ErrorCodeItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("code") val code: String = "",
    @SerializedName("brand") val brand: String = "",
    @SerializedName("model") val model: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("causes") val causes: List<String> = emptyList(),
    @SerializedName("steps") val steps: List<String> = emptyList()
)

data class ProblemItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("solutions") val solutions: List<String> = emptyList()
)

data class SparePartItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("price") val price: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("in_stock") val inStock: Boolean = true
)

data class SeedDatabase(
    @SerializedName("error_codes") val errorCodes: List<ErrorCodeItem> = emptyList(),
    @SerializedName("problems") val problems: List<ProblemItem> = emptyList(),
    @SerializedName("spare_parts") val spareParts: List<SparePartItem> = emptyList(),
    @SerializedName("orders") val orders: List<KodyarOrder> = emptyList(),
    @SerializedName("technicians") val technicians: List<KodyarTechnician> = emptyList()
)
