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
        
        // CHECK SESSION: Skip login if already logged in
        if (sessionManager.isLoggedIn()) {
            startActivity(android.content.Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_main)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // Setup Client
        val client = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()
                val cookies = CookieManager.getInstance().getCookie(url)
                
                val newRequest = request.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "application/json")
                    .header("Cookie", cookies ?: "")
                    .build()
                chain.proceed(newRequest)
            }.build()

        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://multi-tenant.ct.ws/") // FORCE HTTPS
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        // --- QUICK BYPASS UNLOCKER ---
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = RetrofitClient.USER_AGENT 
        
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        btnLogin.isEnabled = false
        btnLogin.text = "Unlocking..."

        // I-load lang natin ng isang beses para makuha niya yung unang __test session cookie
        webView.loadUrl("https://multi-tenant.ct.ws/api-mobile.php?action=login")

        // Regardless kung gaano kabilis yung internet, bubuksan niya agad ang button in 1.2 seconds!
        Handler(Looper.getMainLooper()).postDelayed({
            btnLogin.isEnabled = true
            btnLogin.text = "LOGIN TO MY ACCOUNT"
            CookieManager.getInstance().flush() 
        }, 1200)
        // --- END UNLOCKER ---

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
        Toast.makeText(this, "Welcome to $shopName! ($role)", Toast.LENGTH_LONG).show()
        val intent = android.content.Intent(this, HomeActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

