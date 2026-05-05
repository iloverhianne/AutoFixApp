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
            
            // 2. Solve Security Challenge (InfinityFree/AES)
            val challengeWebView = WebView(this)
            challengeWebView.visibility = android.view.View.GONE
            challengeWebView.settings.javaScriptEnabled = true
            challengeWebView.settings.domStorageEnabled = true
            challengeWebView.settings.userAgentString = RetrofitClient.USER_AGENT
            challengeWebView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    android.util.Log.d("AutoFixChallenge", "Challenge Bypass Finished: $url")
                }
                override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                    android.util.Log.e("AutoFixChallenge", "Bypass Error: ${error?.description}")
                }
            }
            challengeWebView.loadUrl("http://multi-tenant.ct.ws/api-mobile.php")
            
            // Attach to root so it actually runs
            findViewById<android.view.ViewGroup>(android.R.id.content).addView(challengeWebView)

            // 3. Session Check
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
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Logging in..."

            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!btnLogin.isEnabled) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "LOGIN TO MY ACCOUNT"
                    Toast.makeText(this@MainActivity, "Login timed out. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
            handler.postDelayed(timeoutRunnable, 20000)

            val apiService = RetrofitClient.getApiService(this@MainActivity)
            apiService.login(action = "login", email = email, password = password).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    handler.removeCallbacks(timeoutRunnable)
                    btnLogin.isEnabled = true
                    btnLogin.text = "LOGIN TO MY ACCOUNT"

                    if (response.isSuccessful && response.body() != null) {
                        val loginRes = response.body()!!
                        if (loginRes.status == "success") {
                            if (loginRes.shops != null && loginRes.shops.size > 1) {
                                showShopSelector(loginRes)
                            } else {
                                proceedToDashboard(
                                    loginRes.customer_id ?: "",
                                    loginRes.name ?: "User",
                                    loginRes.email ?: email,
                                    loginRes.tenant_id ?: "1",
                                    loginRes.shop_name ?: "AutoFix Shop",
                                    loginRes.role ?: "CUSTOMER"
                                )
                            }
                        } else {
                            Toast.makeText(this@MainActivity, loginRes.message ?: "Invalid Credentials", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    handler.removeCallbacks(timeoutRunnable)
                    btnLogin.isEnabled = true
                    btnLogin.text = "LOGIN TO MY ACCOUNT"
                    
                    val errorMessage = if (t is com.google.gson.JsonSyntaxException || t is java.lang.IllegalStateException) {
                        "Connection Failed: Server returned invalid response (possibly Security Challenge). Please wait a moment and try again."
                    } else {
                        "Connection Failed: ${t.message}"
                    }
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun showShopSelector(response: LoginResponse) {
        val shops = response.shops ?: return
        val shopNames = shops.map { it.shop_name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Which workshop to view?")
            .setItems(shopNames) { _, which ->
                val selected = shops[which]
                proceedToDashboard(
                    selected.customer_id ?: "",
                    selected.name ?: "",
                    response.email ?: "",
                    selected.tenant_id ?: "1",
                    selected.shop_name ?: "AutoFix Shop",
                    response.role ?: "CUSTOMER"
                )
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

