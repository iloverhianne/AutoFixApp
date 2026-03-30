package com.example.autofixapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PaymentActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService

    private var fullAmount: Double = 0.0
    private var amountToPay: Double = 0.0

    // Views
    private lateinit var tvPaymentAmount: TextView
    private lateinit var tvPaymentDesc: TextView
    private lateinit var rgPaymentType: RadioGroup
    private lateinit var rbDownpayment: RadioButton
    private lateinit var cvGcash: CardView
    private lateinit var cvCard: CardView
    private lateinit var rbGcash: RadioButton
    private lateinit var rbCard: RadioButton
    private lateinit var btnPayNow: Button
    private lateinit var loadingOverlay: FrameLayout

    // Intent Extras
    private var serviceId: String = ""
    private var serviceName: String = ""
    private var vehicleId: String = "0"
    private var date: String = ""
    private var time: String = ""
    private var mechanicId: String? = null
    private var bayId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        sessionManager = SessionManager(this)
        apiService = RetrofitClient.getApiService(this)

        serviceId = intent.getStringExtra("serviceId") ?: ""
        serviceName = intent.getStringExtra("serviceName") ?: "Unknown"
        vehicleId = intent.getStringExtra("vehicleId") ?: "0"
        date = intent.getStringExtra("date") ?: ""
        time = intent.getStringExtra("time") ?: ""
        mechanicId = intent.getStringExtra("mechanicId")
        bayId = intent.getStringExtra("bayId")

        val estimateStr = intent.getStringExtra("estimate") ?: "0.00"
        fullAmount = estimateStr.toDoubleOrNull() ?: 0.0

        initViews()
        setupListeners()
        updateAmountToPay()
    }

    private fun initViews() {
        tvPaymentAmount = findViewById(R.id.tvPaymentAmount)
        tvPaymentDesc = findViewById(R.id.tvPaymentDesc)
        rgPaymentType = findViewById(R.id.rgPaymentType)
        rbDownpayment = findViewById(R.id.rbDownpayment)

        cvGcash = findViewById(R.id.cvGcash)
        cvCard = findViewById(R.id.cvCard)
        rbGcash = findViewById(R.id.rbGcash)
        rbCard = findViewById(R.id.rbCard)

        btnPayNow = findViewById(R.id.btnPayNow)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        findViewById<ImageButton>(R.id.btnBackPayment).setOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        rgPaymentType.setOnCheckedChangeListener { _, _ -> updateAmountToPay() }

        cvGcash.setOnClickListener {
            rbGcash.isChecked = true
            rbCard.isChecked = false
        }

        cvCard.setOnClickListener {
            rbCard.isChecked = true
            rbGcash.isChecked = false
        }

        btnPayNow.setOnClickListener {
            processPayment()
        }
    }

    private fun updateAmountToPay() {
        amountToPay = if (rbDownpayment.isChecked) {
            fullAmount * 0.20 // 20% downpayment
        } else {
            fullAmount
        }

        val typeStr = if (rbDownpayment.isChecked) "Downpayment" else "Full Payment"
        tvPaymentDesc.text = "$typeStr for $serviceName"
        tvPaymentAmount.text = String.format("₱%.2f", amountToPay)
    }

    private fun processPayment() {
        val paymentMethod = if (rbGcash.isChecked) "GCash" else "Credit/Debit Card"
        
        // Detailed log to match the console
        android.util.Log.d("PAYMENT_SIM", "Initializing PayMongo Intent for $amountToPay ($paymentMethod)")

        // For simulation, let's create a WebView in a Dialog to "simulate" PayMongo Checkout
        val webView = android.webkit.WebView(this)
        webView.settings.javaScriptEnabled = true
        
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(webView)
            .setCancelable(false)
            .create()

        // Mock PayMongo HTML
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: sans-serif; background: #030712; color: white; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }
                    .card { background: #111827; padding: 30px; border-radius: 20px; width: 85%; border: 1px solid #1f2937; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                    .logo { width: 140px; margin-bottom: 30px; }
                    .amount { font-size: 32px; font-weight: bold; color: #10b981; margin: 10px 0; }
                    .btn { background: #005ce6; color: white; border: none; padding: 15px; width: 100%; border-radius: 12px; font-weight: bold; margin-top: 30px; font-size: 16px; cursor: pointer; }
                    .loading { display: none; margin-top: 20px; color: #94a3b8; }
                </style>
            </head>
            <body>
                <div class="card" id="card">
                    <img src="https://www.paymongo.com/static/images/paymongo-logo-horizontal.svg" class="logo">
                    <div style="color: #94a3b8; font-size: 14px;">Total Amount Due</div>
                    <div class="amount">₱${String.format("%.2f", amountToPay)}</div>
                    <div style="font-size: 12px; color: #4b5563; margin-top: 5px;">Ref: PAY-SIM-${System.currentTimeMillis() % 10000}</div>
                    
                    <button class="btn" onclick="payNow()">PAY WITH GCASH</button>
                    <div id="loader" class="loading">Processing payment...</div>
                    
                    <div style="margin-top: 20px; font-size: 11px; color: #374151;">Redirecting to GCash Gateway...</div>
                </div>

                <script>
                    function payNow() {
                        document.querySelector('.btn').style.display = 'none';
                        document.getElementById('loader').style.display = 'block';
                        
                        setTimeout(() => {
                            window.location.href = "https://success.paymongo.com/?status=success";
                        }, 2500);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, url: String?): Boolean {
                if (url?.contains("status=success") == true) {
                    dialog.dismiss()
                    showSuccessDialog()
                    return true
                }
                return false
            }
        }

        webView.loadDataWithBaseURL("https://pay.paymongo.com", html, "text/html", "UTF-8", null)
        dialog.show()
    }

    private fun showSuccessDialog() {
        val typeStr = if (rbDownpayment.isChecked) "Downpayment" else "Full Payment"
        val paymentMethod = if (rbGcash.isChecked) "GCash" else "Credit/Debit Card"

        AlertDialog.Builder(this)
            .setTitle("Payment Successful")
            .setMessage("Your $typeStr of ₱${String.format("%.2f", amountToPay)} via $paymentMethod via PayMongo was successfully processed.")
            .setCancelable(false)
            .setPositiveButton("Proceed") { _, _ ->
                bookAppointment()
            }
            .show()
    }

    private fun bookAppointment() {
        // Show Loading Overlay again for booking
        loadingOverlay.visibility = View.VISIBLE
        val tvLoadingStatus = findViewById<TextView>(R.id.tvLoadingStatus)
        tvLoadingStatus.text = "Confirming Appointment..."

        val tid = sessionManager.getTenantId() ?: "1"
        val customerId = sessionManager.getCustomerId() ?: ""

        val estimateStr = String.format("%.2f", fullAmount)

        apiService.bookAppointment(
            tenantIdQuery = tid,
            customerId = customerId,
            serviceId = serviceId,
            vehicleId = vehicleId,
            date = date,
            time = time,
            estimate = estimateStr,
            mechanicId = mechanicId,
            bayId = bayId
        ).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                loadingOverlay.visibility = View.GONE
                if (response.isSuccessful && response.body()?.status == "success") {
                    val appointmentId = response.body()?.appointment_id
                    val paymentMethod = if (rbGcash.isChecked) "GCash" else "Card"
                    val payType = if (rbDownpayment.isChecked) "DOWNPAYMENT" else "FULL_PAYMENT"

                    // Record payment in background to ensure it reflects on Web Dashboard
                    apiService.recordPayment(
                        tenantIdQuery = tid,
                        customerId = customerId,
                        amount = String.format("%.2f", amountToPay),
                        type = payType,
                        method = paymentMethod,
                        refId = appointmentId
                    ).enqueue(object : Callback<BaseResponse> {
                        override fun onResponse(call: Call<BaseResponse>, r: Response<BaseResponse>) {}
                        override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
                    })

                    Toast.makeText(this@PaymentActivity, "Booking successful!", Toast.LENGTH_LONG).show()

                    val intent = Intent(this@PaymentActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("NAVIGATE_TO", "track")
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@PaymentActivity, "Booking failed: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                loadingOverlay.visibility = View.GONE
                Toast.makeText(this@PaymentActivity, "Error connecting to server", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
