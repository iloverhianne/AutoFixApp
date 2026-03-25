package com.example.autofixapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookingConfirmationActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_confirmation)

        sessionManager = SessionManager(this)
        apiService = RetrofitClient.getApiService(this)

        val serviceId = intent.getStringExtra("serviceId") ?: ""
        val serviceName = intent.getStringExtra("serviceName") ?: "Unknown"
        val date = intent.getStringExtra("date") ?: ""
        val time = intent.getStringExtra("time") ?: ""
        val estimate = intent.getStringExtra("estimate") ?: "0.00"
        val mechanicId = intent.getStringExtra("mechanicId")
        val mechanicName = intent.getStringExtra("mechanicName") ?: "Any Available"

        findViewById<TextView>(R.id.tvConfirmServiceName).text = serviceName
        findViewById<TextView>(R.id.tvConfirmDate).text = date
        findViewById<TextView>(R.id.tvConfirmTime).text = time
        findViewById<TextView>(R.id.tvConfirmAssignment).text = mechanicName
        findViewById<TextView>(R.id.tvConfirmEstimate).text = "₱$estimate"

        findViewById<Button>(R.id.btnCancelConfirm).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnFinalConfirm).setOnClickListener {
            val intent = Intent(this, PaymentActivity::class.java).apply {
                putExtra("serviceId", serviceId)
                putExtra("serviceName", serviceName)
                putExtra("date", date)
                putExtra("time", time)
                putExtra("estimate", estimate)
                putExtra("mechanicId", mechanicId)
                putExtra("mechanicName", mechanicName)
            }
            startActivity(intent)
        }
    }
}
