package com.example.autofixapp

data class LoginResponse(
    val status: String? = null,
    val message: String? = null,
    val customer_id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val tenant_id: String? = null,
    val shop_name: String? = null,
    val role: String? = "CUSTOMER",
    val shops: List<Shop>? = null
)

data class Shop(
    val tenant_id: String? = null,
    val shop_name: String? = null,
    val customer_id: String? = null,
    val name: String? = null
)
