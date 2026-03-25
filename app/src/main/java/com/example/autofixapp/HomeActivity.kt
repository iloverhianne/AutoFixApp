package com.example.autofixapp

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Default fragment
        val navigateTo = intent.getStringExtra("NAVIGATE_TO")
        if (navigateTo == "track") {
            bottomNav.selectedItemId = R.id.nav_track
            loadFragment(TrackingFragment())
        } else {
            loadFragment(HomeFragment())
        }

        setupBottomNavListener(bottomNav)
    }

    private fun setupBottomNavListener(bottomNav: BottomNavigationView) {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_book -> loadFragment(BookingFragment())
                R.id.nav_track -> loadFragment(TrackingFragment())
                R.id.nav_history -> loadFragment(HistoryFragment())
            }
            true
        }
    }

    fun navigateToTracking(jobId: String) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener(null)
        bottomNav.selectedItemId = R.id.nav_track
        
        val fragment = TrackingFragment()
        val bundle = Bundle()
        bundle.putString("JOB_ID", jobId)
        fragment.arguments = bundle
        
        loadFragment(fragment)
        setupBottomNavListener(bottomNav)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

// Dummy Fragments for now
class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        val sessionManager = SessionManager(context)
        
        val tvShopName = view.findViewById<TextView>(R.id.tvShopNameHeader)
        val tvNoServices = view.findViewById<TextView>(R.id.tvNoServices)
        val gridServices = view.findViewById<GridLayout>(R.id.gridServices)
        val btnLogout = view.findViewById<ImageView>(R.id.btnLogout)

        val tvProfileName = view.findViewById<TextView>(R.id.tvProfileCustomerName)
        val tvProfileEmail = view.findViewById<TextView>(R.id.tvProfileCustomerEmail)
        val tvHomeWaitTime = view.findViewById<TextView>(R.id.tvHomeWaitTime)

        val role = sessionManager.getRole() ?: "CUSTOMER"
        tvShopName.text = sessionManager.getShopName() ?: "AutoFix Shop"
        tvProfileName.text = (sessionManager.getCustomerName() ?: "Guest User") + " ($role)"
        tvProfileEmail.text = sessionManager.getCustomerEmail() ?: "guest@example.com"
        
        // Logout Functionality
        btnLogout.setOnClickListener {
            sessionManager.clearSession()
            val intent = android.content.Intent(context, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
        
        // Fetch Services (Table-style Inflation)
        val apiService = RetrofitClient.getApiService(context)
        val tid = sessionManager.getTenantId() ?: "1"

        // Fetch Availability for Home Screen
        apiService.getAvailability(tenantId = tid).enqueue(object : Callback<AvailabilityResponse> {
            override fun onResponse(call: Call<AvailabilityResponse>, response: Response<AvailabilityResponse>) {
                if (response.isSuccessful) {
                    tvHomeWaitTime.text = response.body()?.waiting_time ?: "0 mins"
                }
            }
            override fun onFailure(call: Call<AvailabilityResponse>, t: Throwable) {}
        })
        
        apiService.getServices(tenantId = tid).enqueue(object : Callback<ServiceResponse> {
            override fun onResponse(call: Call<ServiceResponse>, response: Response<ServiceResponse>) {
                if (response.isSuccessful) {
                    val services = response.body()?.data ?: emptyList()
                    if (services.isNotEmpty()) {
                        tvNoServices.visibility = View.GONE
                        gridServices.visibility = View.VISIBLE
                        gridServices.removeAllViews()
                        
                        for (service in services) {
                            val row = LinearLayout(context).apply {
                                layoutParams = GridLayout.LayoutParams().apply {
                                    width = GridLayout.LayoutParams.MATCH_PARENT
                                    height = GridLayout.LayoutParams.WRAP_CONTENT
                                    setMargins(0, 8, 0, 8)
                                }
                                orientation = LinearLayout.HORIZONTAL
                                gravity = android.view.Gravity.CENTER_VERTICAL
                                setPadding(0, 12, 0, 12)
                            }

                            val nameLayout = LinearLayout(context).apply {
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                orientation = LinearLayout.VERTICAL
                            }

                            val name = TextView(context).apply {
                                text = service.service_name
                                setTextColor(android.graphics.Color.WHITE)
                                textSize = 14f
                                setTypeface(null, android.graphics.Typeface.BOLD)
                            }
                            
                            val desc = TextView(context).apply {
                                text = if(service.description.isNullOrEmpty()) "Standard repair service" else service.description
                                setTextColor(android.graphics.Color.parseColor("#6B7280"))
                                textSize = 11f
                            }
                            
                            nameLayout.addView(name)
                            nameLayout.addView(desc)

                            val price = TextView(context).apply {
                                text = "₱${service.price}"
                                setTextColor(android.graphics.Color.parseColor("#10B981"))
                                textSize = 14f
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                gravity = android.view.Gravity.END
                            }

                            row.addView(nameLayout)
                            row.addView(price)
                            
                            gridServices.addView(row)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<ServiceResponse>, t: Throwable) {
                android.util.Log.e("API_DEBUG", "Failure: ${t.message}")
            }
        })
    }
}

class BookingFragment : Fragment(R.layout.fragment_booking) {
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private var selectedServiceId: String? = null
    private var selectedEstimate: String = "0.00"
    private var servicesList: List<Service> = emptyList()
    private var mechanicsList: List<Mechanic> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        sessionManager = SessionManager(context)
        apiService = RetrofitClient.getApiService(context)

        val tvAvailableSlots = view.findViewById<TextView>(R.id.tvAvailableSlots)
        val tvAvailableMechanics = view.findViewById<TextView>(R.id.tvAvailableMechanics)
        val tvWaitingTime = view.findViewById<TextView>(R.id.tvWaitingTime)
        val spinnerService = view.findViewById<Spinner>(R.id.spinnerService)
        val etDate = view.findViewById<EditText>(R.id.etDate)
        val spinnerTime = view.findViewById<Spinner>(R.id.spinnerTime)
        val spinnerAssignment = view.findViewById<Spinner>(R.id.spinnerAssignment)
        val tvEstimate = view.findViewById<TextView>(R.id.tvEstimate)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitBooking)

        view.findViewById<TextView>(R.id.tvShopNameHeader)?.text = sessionManager.getShopName() ?: "AutoFix Shop"

        val tid = sessionManager.getTenantId() ?: "1"

        // 1. Fetch Availability
        apiService.getAvailability(tenantId = tid).enqueue(object : Callback<AvailabilityResponse> {
            override fun onResponse(call: Call<AvailabilityResponse>, response: Response<AvailabilityResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    tvAvailableSlots.text = body?.available_bays?.toString() ?: "0"
                    tvAvailableMechanics.text = body?.available_mechanics?.toString() ?: "0"
                    tvWaitingTime.text = body?.waiting_time ?: "0 mins"
                }
            }
            override fun onFailure(call: Call<AvailabilityResponse>, t: Throwable) {}
        })

        // 2. Fetch Services
        apiService.getServices(tenantId = tid).enqueue(object : Callback<ServiceResponse> {
            override fun onResponse(call: Call<ServiceResponse>, response: Response<ServiceResponse>) {
                if (response.isSuccessful) {
                    servicesList = response.body()?.data ?: emptyList()
                    val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, servicesList.map { it.service_name })
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerService.adapter = adapter
                }
            }
            override fun onFailure(call: Call<ServiceResponse>, t: Throwable) {}
        })

        spinnerService.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val service = servicesList[position]
                selectedServiceId = service.service_id
                selectedEstimate = service.price
                tvEstimate.text = "₱$selectedEstimate"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 3. Date Selection
        etDate.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            val year = c.get(java.util.Calendar.YEAR)
            val month = c.get(java.util.Calendar.MONTH)
            val day = c.get(java.util.Calendar.DAY_OF_MONTH)

            android.app.DatePickerDialog(context, { _, y, m, d ->
                val dateStr = "$y-${String.format("%02d", m + 1)}-${String.format("%02d", d)}"
                etDate.setText(dateStr)
            }, year, month, day).show()
        }

        // 4. Time Spinner (Static slots for now)
        val timeSlots = listOf("08:00:00", "09:00:00", "10:00:00", "11:00:00", "13:00:00", "14:00:00", "15:00:00", "16:00:00")
        val timeAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, timeSlots)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTime.adapter = timeAdapter

        // 5. Fetch Mechanics & Bays
        apiService.getMechanicsAndBays(tenantId = tid).enqueue(object : Callback<MechanicsBaysResponse> {
            override fun onResponse(call: Call<MechanicsBaysResponse>, response: Response<MechanicsBaysResponse>) {
                if (response.isSuccessful) {
                    mechanicsList = response.body()?.mechanics ?: emptyList()
                    val mechNames = if (mechanicsList.isEmpty()) listOf("No specific mechanic") else mechanicsList.map { it.full_name }
                    val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, mechNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerAssignment.adapter = adapter
                }
            }
            override fun onFailure(call: Call<MechanicsBaysResponse>, t: Throwable) {}
        })

        // 6. Submit Booking
        btnSubmit.setOnClickListener {
            val date = etDate.text.toString()
            val time = spinnerTime.selectedItem?.toString() ?: "08:00:00"
            val customerId = sessionManager.getCustomerId() ?: ""
            val mechId = if (mechanicsList.isNotEmpty()) mechanicsList[spinnerAssignment.selectedItemPosition].mechanic_id else null
            val mechName = if (mechanicsList.isNotEmpty()) mechanicsList[spinnerAssignment.selectedItemPosition].full_name else null
            val serviceName = servicesList.find { it.service_id == selectedServiceId }?.service_name

            if (date.isEmpty()) {
                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = android.content.Intent(context, BookingConfirmationActivity::class.java).apply {
                putExtra("serviceId", selectedServiceId ?: "")
                putExtra("serviceName", serviceName ?: "Standard Service")
                putExtra("date", date)
                putExtra("time", time)
                putExtra("estimate", selectedEstimate)
                if (mechId != null) putExtra("mechanicId", mechId)
                if (mechName != null) putExtra("mechanicName", mechName)
            }
            startActivity(intent)
        }
    }
}

class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: TimelineAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        sessionManager = SessionManager(context)
        apiService = RetrofitClient.getApiService(context)
        
        view.findViewById<TextView>(R.id.tvShopNameHeader)?.text = sessionManager.getShopName() ?: "AutoFix Shop"

        val etJobId = view.findViewById<EditText>(R.id.etJobId)
        val btnTrackRepair = view.findViewById<Button>(R.id.btnTrackRepair)
        
        val cardEmptyState = view.findViewById<View>(R.id.cardEmptyState)
        val layoutTrackingDetails = view.findViewById<View>(R.id.layoutTrackingDetails)
        
        val tvTrackVehicle = view.findViewById<TextView>(R.id.tvTrackVehicle)
        val tvTrackStatus = view.findViewById<TextView>(R.id.tvTrackStatus)
        val tvTrackTotal = view.findViewById<TextView>(R.id.tvTrackTotal)
        val rvTimeline = view.findViewById<RecyclerView>(R.id.rvTimeline)

        rvTimeline.layoutManager = LinearLayoutManager(context)
        adapter = TimelineAdapter(emptyList())
        rvTimeline.adapter = adapter

        btnTrackRepair.setOnClickListener {
            val jobId = etJobId.text.toString().trim()
            if (jobId.isEmpty()) {
                Toast.makeText(context, "Please enter a Job ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tid = sessionManager.getTenantId() ?: "1"
            val customerId = sessionManager.getCustomerId() ?: ""

            apiService.trackRepair(tenantId = tid, jobId = jobId, customerId = customerId)
                .enqueue(object : Callback<TrackingResponse> {
                    override fun onResponse(call: Call<TrackingResponse>, response: Response<TrackingResponse>) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            val body = response.body()!!
                            
                            // Hide Empty state, show details
                            cardEmptyState.visibility = View.GONE
                            layoutTrackingDetails.visibility = View.VISIBLE

                            // Populate Job Info
                            val info = body.jobInfo
                            tvTrackVehicle.text = "Vehicle: ${info.plate_no} (${info.make} ${info.model})"
                            tvTrackStatus.text = info.status.uppercase()
                            tvTrackTotal.text = "₱${info.total_amount}"

                            // Populate Timeline
                            adapter.updateData(body.timeline)
                            
                        } else {
                            Toast.makeText(context, "Job completely not found or no permission", Toast.LENGTH_SHORT).show()
                            cardEmptyState.visibility = View.VISIBLE
                            layoutTrackingDetails.visibility = View.GONE
                        }
                    }

                    override fun onFailure(call: Call<TrackingResponse>, t: Throwable) {
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        val presetJobId = arguments?.getString("JOB_ID")
        if (!presetJobId.isNullOrEmpty()) {
            etJobId.setText(presetJobId)
            btnTrackRepair.performClick()
        }
    }
}

class HistoryFragment : Fragment(R.layout.fragment_history) {
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var repairAdapter: HistoryAdapter
    private lateinit var paymentAdapter: PaymentHistoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        
        val rvHistory = view.findViewById<RecyclerView>(R.id.rvHistory)
        rvHistory.layoutManager = LinearLayoutManager(context)
        repairAdapter = HistoryAdapter(emptyList()) { jobId ->
            (activity as? HomeActivity)?.navigateToTracking(jobId)
        }
        rvHistory.adapter = repairAdapter
        
        val rvPayments = view.findViewById<RecyclerView>(R.id.rvPayments)
        rvPayments.layoutManager = LinearLayoutManager(context)
        paymentAdapter = PaymentHistoryAdapter(emptyList())
        rvPayments.adapter = paymentAdapter
        
        val rgTabs = view.findViewById<RadioGroup>(R.id.rgTabs)
        rgTabs.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbRepairs) {
                rvHistory.visibility = View.VISIBLE
                rvPayments.visibility = View.GONE
            } else {
                rvHistory.visibility = View.GONE
                rvPayments.visibility = View.VISIBLE
            }
        }

        view.findViewById<TextView>(R.id.tvShopNameHeader)?.text = sessionManager.getShopName() ?: "AutoFix Shop"

        apiService = RetrofitClient.getApiService(requireContext())

        loadHistory()
    }

    private fun loadHistory() {
        val tid = sessionManager.getTenantId() ?: "1"
        apiService.getHistory(tenantId = tid, customerId = sessionManager.getCustomerId() ?: "")
            .enqueue(object : Callback<HistoryResponse> {
                override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.repairs?.let { repairAdapter.updateData(it) }
                        response.body()?.payments?.let { paymentAdapter.updateData(it) }
                    }
                }
                override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                    Toast.makeText(context, "Error loading history", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
