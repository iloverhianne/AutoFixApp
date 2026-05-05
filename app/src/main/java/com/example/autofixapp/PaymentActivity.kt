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
    private var loyaltyDiscount: Double = 0.0
    private var userPoints: Int = 0
    private var isPointsApplied: Boolean = false

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
    private lateinit var tvCurrentPoints: TextView
    private lateinit var btnApplyPoints: Button

    // Intent Extras
    private var serviceId: String = ""
    private var serviceName: String = ""
    private var vehicleId: String = "0"
    private var date: String = ""
    private var time: String = ""
    private var mechanicId: String? = null
    private var mechanicName: String? = null
    private var bayId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        sessionManager = SessionManager(this)
        apiService = RetrofitClient.getApiService(this)

        serviceId = intent.getStringExtra("serviceId") ?: ""
        serviceName = intent.getStringExtra("serviceName") ?: "Repair Job"
        vehicleId = intent.getStringExtra("vehicleId") ?: "0"
        
        val amountStr = intent.getStringExtra("estimate") ?: intent.getStringExtra("AMOUNT") ?: "0.00"
        // Remove currency symbols and commas for safe parsing
        val cleanAmount = amountStr.replace("₱", "").replace(",", "").trim()
        fullAmount = cleanAmount.toDoubleOrNull() ?: 0.0
        
        date = intent.getStringExtra("date") ?: ""
        time = intent.getStringExtra("time") ?: ""
        mechanicId = intent.getStringExtra("mechanicId")
        mechanicName = intent.getStringExtra("mechanicName")
        bayId = intent.getStringExtra("bayId")
        
        // Check if this is a balance payment for an existing job
        val existingJobId = intent.getStringExtra("JOB_ID")
        if (existingJobId != null) {
            // Already a job, don't allow another downpayment calculation
            // We are paying the balance now.
            Handler(Looper.getMainLooper()).postDelayed({
              rbDownpayment.visibility = View.GONE
              findViewById<RadioButton>(R.id.rbFullPayment).isChecked = true
              findViewById<RadioButton>(R.id.rbFullPayment).text = "Balance Payment"
              updateAmountToPay()
            }, 100)
        }

        initViews()
        setupListeners()
        fetchLoyaltyPoints()
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
        tvCurrentPoints = findViewById(R.id.tvCurrentPoints)
        btnApplyPoints = findViewById(R.id.btnApplyPoints)

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

        btnApplyPoints.setOnClickListener {
            togglePoints()
        }
    }

    private fun fetchLoyaltyPoints() {
        val tid = sessionManager.getTenantId() ?: "1"
        val cid = sessionManager.getCustomerId() ?: ""
        
        apiService.getLoyaltyStatus(action = "loyalty_status", tenantId = tid, customerId = cid).enqueue(object : Callback<LoyaltyResponse> {
            override fun onResponse(call: Call<LoyaltyResponse>, response: Response<LoyaltyResponse>) {
                if (response.isSuccessful) {
                    userPoints = response.body()?.points ?: 0
                    tvCurrentPoints.text = "$userPoints pts"
                    if (userPoints <= 0) {
                        btnApplyPoints.isEnabled = false
                        btnApplyPoints.alpha = 0.5f
                    }
                }
            }
            override fun onFailure(call: Call<LoyaltyResponse>, t: Throwable) {}
        })
    }

    private fun togglePoints() {
        if (!isPointsApplied) {
            // Apply points
            loyaltyDiscount = userPoints.toDouble() // 1 Pt = 1 PHP
            if (loyaltyDiscount > fullAmount) loyaltyDiscount = fullAmount
            
            isPointsApplied = true
            btnApplyPoints.text = "REMOVE POINTS DISCOUNT"
            Toast.makeText(this, "₱$loyaltyDiscount discount applied!", Toast.LENGTH_SHORT).show()
        } else {
            // Remove points
            loyaltyDiscount = 0.0
            isPointsApplied = false
            btnApplyPoints.text = "APPLY ALL POINTS"
        }
        updateAmountToPay()
    }

    private fun updateAmountToPay() {
        var baseAmount = if (rbDownpayment.isChecked) {
            fullAmount * 0.20 // 20% downpayment
        } else {
            fullAmount
        }

        // Apply loyalty discount to the final amount
        amountToPay = baseAmount - loyaltyDiscount
        if (amountToPay < 0) amountToPay = 0.0

        val typeStr = if (rbDownpayment.isChecked) "Downpayment" else "Full Payment"
        tvPaymentDesc.text = "$typeStr for $serviceName"
        
        if (loyaltyDiscount > 0) {
            tvPaymentAmount.text = String.format("₱%.2f (Saved ₱%.2f)", amountToPay, loyaltyDiscount)
        } else {
            tvPaymentAmount.text = String.format("₱%.2f", amountToPay)
        }

        if (amountToPay <= 0) {
            btnPayNow.text = "CONFIRM FREE BOOKING"
        } else {
            btnPayNow.text = "PAY NOW"
        }
    }

    private fun processPayment() {
        val paymentMethod = if (rbGcash.isChecked) "GCash" else "Credit/Debit Card"
        android.util.Log.d("PAYMENT_SIM", "Initializing simulation for $amountToPay ($paymentMethod)")

        val overlay = findViewById<FrameLayout>(R.id.paymentSimulationOverlay)
        val webView = findViewById<android.webkit.WebView>(R.id.wvPayment)
        val btnClose = findViewById<ImageButton>(R.id.btnCloseSimulation)

        overlay.visibility = View.VISIBLE
        webView.settings.javaScriptEnabled = true
        
        btnClose.setOnClickListener {
            overlay.visibility = View.GONE
        }

        // Mock PayMongo HTML with stable design
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: -apple-system, sans-serif; background: #111827; color: white; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; overflow: hidden; }
                    .card { background: #1f2937; padding: 40px 20px; border-radius: 24px; width: 85%; box-shadow: 0 20px 40px rgba(0,0,0,0.4); border: 1px solid #374151; }
                    .logo { width: 150px; margin-bottom: 40px; filter: brightness(1.2); }
                    .label { color: #9ca3af; font-size: 14px; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 1px; }
                    .amount { font-size: 40px; font-weight: 800; color: #10b981; margin: 0 0 8px 0; }
                    .ref { font-size: 12px; color: #6b7280; font-family: monospace; }
                    .btn { background: #005ce6; color: white; border: none; padding: 18px; width: 100%; border-radius: 16px; font-weight: bold; margin-top: 40px; font-size: 16px; cursor: pointer; transition: all 0.2s; }
                    .btn:active { transform: scale(0.98); opacity: 0.9; }
                    .loader { display: none; margin-top: 30px; color: #10b981; font-weight: bold; }
                    .dots { display: inline-block; width: 8px; height: 8px; background: #10b981; border-radius: 50%; margin: 0 4px; animation: bounce 0.6s infinite alternate; }
                    @keyframes bounce { to { transform: translateY(-8px); } }
                </style>
            </head>
            <body>
                <div class="card">
                    <img src="https://www.paymongo.com/static/images/paymongo-logo-horizontal.svg" class="logo">
                    <div class="label">Amount to Pay</div>
                    <div class="amount">₱${String.format("%.2f", amountToPay)}</div>
                    <div class="ref">REF: PM-${System.currentTimeMillis() % 1000000}</div>
                    
                    <button class="btn" id="payBtn" onclick="payNow()">PAY WITH GCASH</button>
                    
                    <div id="loader" class="loader">
                        Authenticating
                        <div style="margin-top: 15px;">
                            <div class="dots"></div>
                            <div class="dots" style="animation-delay: 0.2s"></div>
                            <div class="dots" style="animation-delay: 0.4s"></div>
                        </div>
                    </div>
                </div>

                <script>
                    function payNow() {
                        document.getElementById('payBtn').style.display = 'none';
                        document.getElementById('loader').style.display = 'block';
                        
                        setTimeout(() => {
                            window.location.href = "https://success.paymongo.com/?status=success";
                        }, 3000);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, url: String?): Boolean {
                if (url?.contains("status=success") == true) {
                    overlay.visibility = View.GONE
                    showSuccessDialog()
                    return true
                }
                return false
            }
        }

        webView.loadDataWithBaseURL("https://pay.paymongo.com", html, "text/html", "UTF-8", null)
    }

    private fun showSuccessDialog() {
        val typeStr = if (rbDownpayment.isChecked) "Downpayment" else "Full Payment"
        val paymentMethod = if (rbGcash.isChecked) "GCash" else "Credit/Debit Card"

        AlertDialog.Builder(this)
            .setTitle("Payment Successful")
            .setMessage("Your $typeStr of ₱${String.format("%.2f", amountToPay)} via $paymentMethod via PayMongo was successfully processed.")
            .setCancelable(false)
            .setPositiveButton("Proceed") { _, _ ->
                val existingJobId = intent.getStringExtra("JOB_ID")
                if (existingJobId != null) {
                    recordBalancePayment(existingJobId)
                } else {
                    bookAppointment()
                }
            }
            .show()
    }

    private fun recordBalancePayment(jobId: String) {
        loadingOverlay.visibility = View.VISIBLE
        val tid = sessionManager.getTenantId() ?: "1"
        val cid = sessionManager.getCustomerId() ?: ""
        val method = if (rbGcash.isChecked) "GCash" else "Card"
        
        apiService.recordPayment(action = "record_payment", tenantId = tid, customerId = cid, amount = String.format("%.2f", amountToPay), type = "BALANCE", method = method, refId = jobId)
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this@PaymentActivity, "Balance payment recorded!", Toast.LENGTH_LONG).show()
                    finish()
                }
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this@PaymentActivity, "Payment recorded offline.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun bookAppointment() {
        // Show Loading Overlay again for booking
        loadingOverlay.visibility = View.VISIBLE
        val tvLoadingStatus = findViewById<TextView>(R.id.tvLoadingStatus)
        tvLoadingStatus.text = "Confirming Appointment..."

        val tid = sessionManager.getTenantId() ?: "1"
        val customerId = sessionManager.getCustomerId() ?: ""

        val estimateStr = String.format("%.2f", fullAmount)
        
        // Convert "2:00 PM" to "14:00:00" for server compatibility
        val time24 = try {
            val parts = time?.split(" ")
            if (parts?.size == 2) {
                val tParts = parts[0].split(":")
                var h = tParts[0].toInt()
                val m = tParts[1]
                val isPm = parts[1].equals("PM", ignoreCase = true)
                if (isPm && h < 12) h += 12
                if (!isPm && h == 12) h = 0
                String.format("%02d:%s:00", h, m)
            } else {
                time ?: "08:00:00"
            }
        } catch (e: Exception) {
            time ?: "08:00:00"
        }

        apiService.bookAppointment(
            action = "book_appointment",
            tenantId = tid,
            customerId = customerId,
            serviceId = serviceId,
            vehicleId = vehicleId,
            date = date,
            time = time24,
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

                    // Save booked mechanic locally to filter out in frontend
                    val sp = getSharedPreferences("LocalBookings", android.content.Context.MODE_PRIVATE)
                    val editor = sp.edit()
                    mechanicId?.let { editor.putString("booked_mech_${date}_${time}", it) }
                    mechanicName?.let { editor.putString("booked_mech_name_${date}_${time}", it) }
                    editor.apply()

                    // Determine exact amount to record based on payment type selected
                    val finalAmountToRecord = if (rbDownpayment.isChecked) {
                        fullAmount * 0.20
                    } else {
                        fullAmount
                    }

                    // Record payment in background to ensure it reflects on Web Dashboard
                    apiService.recordPayment(
                        action = "record_payment",
                        tenantId = tid,
                        customerId = customerId,
                        amount = String.format("%.2f", finalAmountToRecord),
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
                    intent.putExtra("NAVIGATE_TO", "history")
                    intent.putExtra("JOB_ID", appointmentId)
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
