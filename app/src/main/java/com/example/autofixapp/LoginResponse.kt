package com.example.autofixapp

data class LoginResponse(
    val status: String,
    val message: String,
    val customer_id: String?,
    val name: String?,
    val email: String?,
    val tenant_id: String?, // Added this to identify the shop
    val shop_name: String?,
    val role: String? = "CUSTOMER",
    val shops: List<Shop>?
)

data class Shop(
    val tenant_id: String,
    val shop_name: String,
    val customer_id: String,
    val name: String
)
