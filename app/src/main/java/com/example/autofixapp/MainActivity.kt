package com.example.autofixapp
 
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.autofixapp.R
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request


class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_main)
        
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        try {
            // 1. Initialize essentials
            try { CookieManager.getInstance() } catch (e: Exception) {}
            sessionManager = SessionManager(this)

            if (etEmail == null || etPassword == null || btnLogin == null) {
                android.widget.Toast.makeText(this, "Critical UI Error: Views not found", android.widget.Toast.LENGTH_LONG).show()
                return
            }
            
            // 2. Session Check
            if (sessionManager.isLoggedIn()) {
                android.widget.Toast.makeText(this, "Logging you in...", android.widget.Toast.LENGTH_SHORT).show()
                val intent = android.content.Intent(this, HomeActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return
            }

            btnLogin.visibility = android.view.View.VISIBLE
            etEmail.visibility = android.view.View.VISIBLE
            etPassword.visibility = android.view.View.VISIBLE
            btnLogin.isEnabled = true
            
        } catch (e: Exception) {
            android.util.Log.e("AutoFixDebug", "Startup Error", e)
            android.widget.Toast.makeText(this, "Startup Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Paki-fill up lahat ng fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Logging in..."

            // Add a safety timeout for the login process
            val handler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!btnLogin.isEnabled) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "LOGIN TO MY ACCOUNT"
                    Toast.makeText(this, "Login timed out. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
            handler.postDelayed(timeoutRunnable, 15000) // 15 second timeout

            // Use WebView as a proxy to handle firewall challenges
            val loginWebView = WebView(this@MainActivity)
            loginWebView.settings.javaScriptEnabled = true
            loginWebView.settings.userAgentString = RetrofitClient.USER_AGENT
            loginWebView.settings.domStorageEnabled = true
            
            val postData = "email=${java.net.URLEncoder.encode(email, "UTF-8")}&password=${java.net.URLEncoder.encode(password, "UTF-8")}&action=login"
            
            loginWebView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript("(document.getElementsByTagName('body')[0].innerText)") { jsonString ->
                        handler.removeCallbacks(timeoutRunnable)
                        if (btnLogin.isEnabled) return@evaluateJavascript // Already timed out
                        
                        btnLogin.isEnabled = true
                        btnLogin.text = "LOGIN TO MY ACCOUNT"
                        
                        try {
                            val cleanJson = jsonString?.removeSurrounding("\"")
                                ?.replace("\\\"", "\"")
                                ?.replace("\\\\", "\\")
                                ?.trim()
                            
                            if (cleanJson.isNullOrBlank() || !cleanJson.startsWith("{")) {
                                if (cleanJson?.contains("403") == true || cleanJson?.contains("Forbidden") == true) {
                                    Toast.makeText(this@MainActivity, "Firewall active. Retrying...", Toast.LENGTH_SHORT).show()
                                    // Trigger a fresh unlock
                                    view.loadUrl("https://multi-tenant.ct.ws/api-mobile.php?action=login")
                                } else {
                                    Toast.makeText(this@MainActivity, "Connection Error. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                                return@evaluateJavascript
                            }

                            val response = com.google.gson.Gson().fromJson(cleanJson, LoginResponse::class.java)
                            if (response?.status == "success") {
                                if (response.shops != null && response.shops.size > 1) {
                                    showShopSelector(response)
                                } else {
                                    proceedToDashboard(response.customer_id ?: "", response.name ?: "User", response.email ?: "", response.tenant_id ?: "1", response.shop_name ?: "AutoFix Shop", response.role ?: "CUSTOMER")
                                }
                            } else {
                                Toast.makeText(this@MainActivity, response?.message ?: "Invalid credentials", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "System Busy. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                    handler.removeCallbacks(timeoutRunnable)
                    btnLogin.isEnabled = true
                    btnLogin.text = "LOGIN TO MY ACCOUNT"
                    Toast.makeText(this@MainActivity, "Network error. Check your connection.", Toast.LENGTH_SHORT).show()
                }
            }
            
            loginWebView.postUrl("https://multi-tenant.ct.ws/api-mobile.php?action=login", postData.toByteArray())
        }
    }

    private fun showShopSelector(response: LoginResponse) {
        val shops = response.shops ?: return
        val shopNames = shops.map { it.shop_name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Which workshop to view?")
            .setItems(shopNames) { _, which ->
                val selected = shops[which]
                proceedToDashboard(selected.customer_id, selected.name, response.email ?: "", selected.tenant_id, selected.shop_name, response.role ?: "CUSTOMER")
            }
            .setCancelable(false)
            .show()
    }

    private fun proceedToDashboard(customerId: String, name: String, email: String, tenantId: String, shopName: String, role: String) {
        sessionManager.saveSession(customerId, name, email, tenantId, shopName, role)
        Toast.makeText(this, "Welcome to $shopName!", Toast.LENGTH_LONG).show()
        
        // Finalize cookies before leaving
        CookieManager.getInstance().flush()
        
        val intent = android.content.Intent(this, HomeActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

