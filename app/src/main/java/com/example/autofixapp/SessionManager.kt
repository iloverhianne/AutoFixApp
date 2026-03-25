package com.example.autofixapp

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AutoFixPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_CUSTOMER_ID = "customerId"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_TENANT_ID = "tenantId"
        private const val KEY_SHOP_NAME = "shop_name"
        private const val KEY_ROLE = "user_role"
    }

    fun saveSession(customerId: String, name: String, email: String, tenantId: String, shopName: String, role: String = "CUSTOMER") {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_CUSTOMER_ID, customerId)
            putString(KEY_NAME, name)
            putString(KEY_EMAIL, email)
            putString(KEY_TENANT_ID, tenantId)
            putString(KEY_SHOP_NAME, shopName)
            putString(KEY_ROLE, role)
            apply()
        }
    }
    
    fun getRole(): String? = prefs.getString(KEY_ROLE, "CUSTOMER")

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getCustomerName(): String? = prefs.getString(KEY_NAME, "")
    fun getCustomerEmail(): String? = prefs.getString(KEY_EMAIL, "")
    fun getCustomerId(): String? = prefs.getString(KEY_CUSTOMER_ID, "")
    fun getTenantId(): String? = prefs.getString(KEY_TENANT_ID, "1") // Default to 1 if not found
    fun getShopName(): String? = prefs.getString(KEY_SHOP_NAME, "AutoFix Shop")


    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
