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

        // Show Loading Overlay
        loadingOverlay.visibility = View.VISIBLE

        // Simulate PayMongo Payment Processing Delay
        Handler(Looper.getMainLooper()).postDelayed({

            // For simple interface simulation, assume payment is successful.
            val isSuccess = true

            loadingOverlay.visibility = View.GONE

            if (isSuccess) {
                showSuccessDialog()
            } else {
                Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_SHORT).show()
            }

        }, 2000)
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
