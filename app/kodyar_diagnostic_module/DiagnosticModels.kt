package com.kodyar.diagnostic.model

import com.google.gson.annotations.SerializedName

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

data class DiagnosticSeedDatabase(
    @SerializedName("error_codes") val errorCodes: List<ErrorCodeItem> = emptyList(),
    @SerializedName("problems") val problems: List<ProblemItem> = emptyList(),
    @SerializedName("spare_parts") val spareParts: List<SparePartItem> = emptyList()
)
