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
        sessionManager = SessionManager(this)
        
        // Use a simple splash/loading view while unlocking
        setContentView(R.layout.activity_main)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        
        // Hide UI during initial unlock
        btnLogin.visibility = android.view.View.INVISIBLE
        etEmail.visibility = android.view.View.INVISIBLE
        etPassword.visibility = android.view.View.INVISIBLE

        // --- ROBUST UNLOCKER (Always runs) ---
        val bypassWebView = WebView(this)
        bypassWebView.settings.javaScriptEnabled = true
        bypassWebView.settings.userAgentString = RetrofitClient.USER_AGENT 
        
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        
        fun onUnlockComplete() {
            cm.flush()
            if (sessionManager.isLoggedIn()) {
                startActivity(android.content.Intent(this@MainActivity, HomeActivity::class.java))
                finish()
            } else {
                btnLogin.visibility = android.view.View.VISIBLE
                etEmail.visibility = android.view.View.VISIBLE
                etPassword.visibility = android.view.View.VISIBLE
                btnLogin.isEnabled = true
                btnLogin.text = "LOGIN TO MY ACCOUNT"
            }
        }

        bypassWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                onUnlockComplete()
            }
        }

        // Trigger the challenge
        bypassWebView.loadUrl("https://multi-tenant.ct.ws/api-mobile.php?action=login")

        // Safety timeout
        Handler(Looper.getMainLooper()).postDelayed({
            if (btnLogin.visibility == android.view.View.INVISIBLE) {
                onUnlockComplete()
            }
        }, 4000)
        // --- END UNLOCKER ---

        // Unlocks happen on start now.

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Paki-fill up lahat ng fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Logging in..."

            // GAMIT TAYO NG WEBVIEW PARA MAG-LOGIN (PROXY METHOD)
            val loginWebView = WebView(this@MainActivity)
            loginWebView.settings.javaScriptEnabled = true
            loginWebView.settings.userAgentString = RetrofitClient.USER_AGENT // SET USER AGENT
            
            // I-inject ang POST data sa WebView
            val postData = "email=${java.net.URLEncoder.encode(email, "UTF-8")}&password=${java.net.URLEncoder.encode(password, "UTF-8")}&action=login"
            
            loginWebView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // Pagkatapos mag-load, basahin ang laman ng page (Dapat JSON ito)
                    view?.evaluateJavascript("(document.getElementsByTagName('body')[0].innerText)") { jsonString ->
                        btnLogin.isEnabled = true
                        btnLogin.text = "LOGIN TO MY ACCOUNT"
                        
                        try {
                            // Linisin ang string (minsan may extra quotes)
                            val cleanJson = jsonString?.removeSurrounding("\"")?.replace("\\\"", "\"")?.replace("\\\\", "\\")
                            
                            val response = com.google.gson.Gson().fromJson(cleanJson, LoginResponse::class.java)
                            
                            if (response.status == "success") {
                                if (response.shops != null && response.shops.size > 1) {
                                    showShopSelector(response)
                                } else {
                                    proceedToDashboard(response.customer_id ?: "", response.name ?: "User", response.email ?: "", response.tenant_id ?: "1", response.shop_name ?: "AutoFix Shop", response.role ?: "CUSTOMER")
                                }
                            } else {
                                Toast.makeText(this@MainActivity, response.message ?: "Invalid Login", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            // Kung HTML pa rin ang nakuha
                            Toast.makeText(this@MainActivity, "Firewall Blocked! Subukan ulit.", Toast.LENGTH_SHORT).show()
                            android.util.Log.e("LOGIN_DEBUG", "Raw: $jsonString")
                        }
                    }
                }
            }
            
            // I-execute ang login sa loob ng WebView (Simple URL)
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

