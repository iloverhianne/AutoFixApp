package com.example.autofixapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    private var backPressedTime: Long = 0
    private lateinit var backToast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_home)

            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            if (bottomNav == null) {
                Toast.makeText(this, "Home UI Error: BottomNav not found", Toast.LENGTH_LONG).show()
                return
            }
            setupBottomNavListener(bottomNav)

            // Handle Back Press
            onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        if (::backToast.isInitialized) backToast.cancel()
                        finish()
                    } else {
                        backToast = Toast.makeText(baseContext, "Press back again to exit", Toast.LENGTH_SHORT)
                        backToast.show()
                    }
                    backPressedTime = System.currentTimeMillis()
                }
            })

            // Initial Fragment
            val navigateTo = intent.getStringExtra("NAVIGATE_TO")
            if (navigateTo == "track") {
                bottomNav.selectedItemId = R.id.nav_garage
                loadFragment(TrackingFragment().apply { 
                    arguments = Bundle().apply { putString("JOB_ID", intent.getStringExtra("JOB_ID")) }
                })
            } else {
                loadFragment(HomeFragment())
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Home Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupBottomNavListener(bottomNav: BottomNavigationView) {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_book -> loadFragment(BookingFragment())
                R.id.nav_garage -> loadFragment(GarageFragment())
                R.id.nav_history -> loadFragment(HistoryFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

// --- 1. HOME FRAGMENT ---
class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        val sm = SessionManager(ctx)
        val api = RetrofitClient.getApiService(ctx)
        val tid = sm.getTenantId() ?: "1"
        val cid = sm.getCustomerId() ?: ""

        // Update User Name
        view.findViewById<TextView>(R.id.tvWelcomeName).text = "Hi, ${sm.getCustomerName() ?: "Hershey"}!"

        // 1. Fetch Loyalty Points & Promos
        val tvPoints = view.findViewById<TextView>(R.id.tvLoyaltyPoints)
        val tvTier = view.findViewById<TextView>(R.id.tvLoyaltyTier)
        val layoutPromos = view.findViewById<LinearLayout>(R.id.layoutPromos)
        
        api.getLoyaltyStatus(action = "loyalty_status", tenantId = tid, customerId = cid).enqueue(object : Callback<LoyaltyResponse> {
            override fun onResponse(call: Call<LoyaltyResponse>, response: Response<LoyaltyResponse>) {
                if (isAdded) {
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        tvPoints.text = body.points.toString()
                        tvTier.text = (body.tier ?: "BRONZE MEMBER").uppercase()
                        
                        // Handle Dynamic Promos
                        val promos = body.available_promos
                        if (promos != null && promos.isNotEmpty()) {
                            layoutPromos.removeAllViews()
                            promos.forEach { promo ->
                                val promoCard = LayoutInflater.from(ctx).inflate(R.layout.fragment_home, null)
                                // Note: We need a dedicated layout for promo items if we want it perfect, 
                                // but for now we'll just use the hardcoded structure if no dynamic ones are found.
                                // Actually, let's keep the hardcoded ones if dynamic list is empty, 
                                // or create them dynamically if they exist.
                                addPromoCard(layoutPromos, promo.title ?: "PROMO", promo.description ?: "", promo.discount ?: "")
                            }
                        }
                    }
                }
            }
            override fun onFailure(call: Call<LoyaltyResponse>, t: Throwable) {
                if (isAdded) {
                    tvPoints.text = "0"
                    tvTier.text = "BRONZE MEMBER"
                }
            }
            
            private fun addPromoCard(container: LinearLayout, title: String, desc: String, discount: String) {
                val card = androidx.cardview.widget.CardView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        (260 * resources.displayMetrics.density).toInt(),
                        (140 * resources.displayMetrics.density).toInt()
                    ).apply { marginEnd = (16 * resources.displayMetrics.density).toInt() }
                    radius = 20 * resources.displayMetrics.density
                    cardElevation = 0f
                    setCardBackgroundColor(android.graphics.Color.parseColor("#1A10B981"))
                }
                
                val inner = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 20, 20, 20)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                
                val tvTitle = TextView(ctx).apply {
                    text = title
                    setTextColor(android.graphics.Color.parseColor("#10B981"))
                    textSize = 24f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                
                val tvDesc = TextView(ctx).apply {
                    text = desc
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 14f
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 4 }
                    layoutParams = params
                }
                
                inner.addView(tvTitle)
                inner.addView(tvDesc)
                card.addView(inner)
                container.addView(card)
            }
        })

        // 2. Fetch Wait Time
        val tvWaitTime = view.findViewById<TextView>(R.id.tvHomeWaitTime)
        api.getAvailability(action = "get_availability", tenantId = tid).enqueue(object : Callback<AvailabilityResponse> {
            override fun onResponse(call: Call<AvailabilityResponse>, response: Response<AvailabilityResponse>) {
                if (isAdded) {
                    if (response.isSuccessful) {
                        val wait = response.body()?.waiting_time
                        tvWaitTime.text = if (wait.isNullOrBlank()) "Ready Now" else wait
                        tvWaitTime.setTextColor(resources.getColor(R.color.warning_orange, null))
                    } else {
                        tvWaitTime.text = "Unavailable"
                    }
                }
            }
            override fun onFailure(call: Call<AvailabilityResponse>, t: Throwable) {
                if (isAdded) {
                    tvWaitTime.text = "Offline (${t.message?.take(15)})"
                    tvWaitTime.setTextColor(resources.getColor(R.color.error_red, null))
                    android.util.Log.e("AutoFixAPI", "Availability Failure", t)
                }
            }
        })

        // 3. Fetch Service Catalog
        val grid = view.findViewById<android.widget.GridLayout>(R.id.gridServices)
        val tvNoServices = view.findViewById<TextView>(R.id.tvNoServices)
        
        api.getServices(action = "get_services", tenantId = tid).enqueue(object : Callback<ServiceResponse> {
            override fun onResponse(call: Call<ServiceResponse>, response: Response<ServiceResponse>) {
                if (isAdded) {
                    if (response.isSuccessful) {
                        val services = response.body()?.data ?: emptyList()
                        if (services.isNotEmpty()) {
                            grid.visibility = View.VISIBLE
                            tvNoServices.visibility = View.GONE
                            grid.removeAllViews()
                            
                            // Show all services (not just 4)
                            services.forEach { service ->
                                val card: View = LayoutInflater.from(ctx).inflate(R.layout.item_service_home, grid, false)
                                val tvName = card.findViewById<TextView>(R.id.tvServiceNameHome)
                                val tvPrice = card.findViewById<TextView>(R.id.tvServicePriceHome)
                                
                                tvName.text = service.service_name
                                tvPrice.text = "Start at \u20b1${service.price}"
                                
                                val params = android.widget.GridLayout.LayoutParams()
                                params.width = android.widget.GridLayout.LayoutParams.MATCH_PARENT
                                params.height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                                params.setMargins(0, 0, 0, 16)
                                card.layoutParams = params
                                
                                grid.addView(card)
                            }
                        } else {
                            tvNoServices.text = "No services found in database"
                            tvNoServices.visibility = View.VISIBLE
                        }
                    } else {
                        tvNoServices.text = "Error connecting to service catalog"
                        tvNoServices.visibility = View.VISIBLE
                    }
                }
            }
            override fun onFailure(call: Call<ServiceResponse>, t: Throwable) {
                if (isAdded) {
                    tvNoServices.text = "Connection Error: Check internet"
                    tvNoServices.visibility = View.VISIBLE
                }
            }
        })

        // 4. Promo Click Listeners
        val navigateToBooking = {
            (activity as? HomeActivity)?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_book
            (activity as? HomeActivity)?.loadFragment(BookingFragment())
        }

        view.findViewById<View>(R.id.cardPromo1)?.setOnClickListener { navigateToBooking() }
        view.findViewById<View>(R.id.cardPromo2)?.setOnClickListener { navigateToBooking() }

        // Logout Shortcut
        view.findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            sm.clearSession()
            startActivity(Intent(ctx, MainActivity::class.java))
            activity?.finish()
        }
    }
}

// --- 2. BOOKING FRAGMENT ---
class BookingFragment : Fragment(R.layout.fragment_booking) {
    private var servicesList: List<Service> = emptyList()
    private var vehicleList: List<Vehicle> = emptyList()
    private var allMechBaysList: List<Pair<Mechanic, Bay>> = emptyList()
    private var mechBaysList: List<Pair<Mechanic, Bay>> = emptyList()
    private var selectedCalendar: java.util.Calendar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        val sm = SessionManager(ctx)
        val api = RetrofitClient.getApiService(ctx)
        val tid = sm.getTenantId() ?: "1"

        val tvSlots = view.findViewById<TextView>(R.id.tvAvailableSlots)
        val tvMechs = view.findViewById<TextView>(R.id.tvAvailableMechanics)
        val tvWait = view.findViewById<TextView>(R.id.tvWaitingTime)
        val spinnerVehicle = view.findViewById<Spinner>(R.id.spinnerVehicle)
        val tvSelectServices = view.findViewById<TextView>(R.id.tvSelectServices)
        val etDate = view.findViewById<EditText>(R.id.etDate)
        val spinnerTime = view.findViewById<Spinner>(R.id.spinnerTime)
        val spinnerAssignment = view.findViewById<Spinner>(R.id.spinnerAssignment)
        val tvEstimate = view.findViewById<TextView>(R.id.tvEstimate)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitBooking)

        // 1. Fetch Availability
        api.getAvailability(action = "get_availability", tenantId = tid).enqueue(object : Callback<AvailabilityResponse> {
            override fun onResponse(call: Call<AvailabilityResponse>, response: Response<AvailabilityResponse>) {
                if (isAdded && response.isSuccessful) {
                    val b = response.body()
                    tvSlots.text = b?.available_bays?.toString() ?: "0"
                    tvMechs.text = b?.available_mechanics?.toString() ?: "0"
                    tvWait.text = b?.waiting_time ?: "-"
                }
            }
            override fun onFailure(call: Call<AvailabilityResponse>, t: Throwable) {
                if (isAdded) Toast.makeText(ctx, "Availability Offline: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })

        // 2. Fetch Garage
        api.getGarage(action = "get_garage", tenantId = tid, customerId = sm.getCustomerId() ?: "").enqueue(object : Callback<GarageResponse> {
            override fun onResponse(call: Call<GarageResponse>, response: Response<GarageResponse>) {
                if (isAdded && response.isSuccessful) {
                    vehicleList = response.body()?.data ?: emptyList()
                    val labels = vehicleList.map { "${it.make} ${it.model} (${it.plate_no})" }
                    val adapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, labels)
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    spinnerVehicle.adapter = adapter
                }
            }
            override fun onFailure(call: Call<GarageResponse>, t: Throwable) {
                if (isAdded) Toast.makeText(ctx, "Garage Fetch Error", Toast.LENGTH_SHORT).show()
            }
        })

        // 3. Fetch Services
        val selectedServiceIds = mutableListOf<String>()
        val selectedServiceNames = mutableListOf<String>()

        fun updateMechanicSpinner() {
            val date = etDate.text.toString()
            val time = spinnerTime.selectedItem?.toString()

            if (date.isEmpty() || time.isNullOrEmpty()) {
                val adapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, listOf("Select date and time first"))
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                spinnerAssignment.adapter = adapter
                spinnerAssignment.isEnabled = false
                mechBaysList = emptyList()
                return
            }

            val loadingAdapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, listOf("Loading mechanics..."))
            loadingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            spinnerAssignment.adapter = loadingAdapter
            spinnerAssignment.isEnabled = false
            mechBaysList = emptyList()

            api.getAvailableMechanics(action = "get_available_mechanics", tenantId = tid, date = date, time = time).enqueue(object : Callback<MechanicsBaysResponse> {
                override fun onResponse(call: Call<MechanicsBaysResponse>, response: Response<MechanicsBaysResponse>) {
                    if (isAdded && response.isSuccessful) {
                        val body = response.body()
                        val list = mutableListOf<Pair<Mechanic, Bay>>()
                        body?.mechanics?.forEachIndexed { i, m ->
                            val b = body.bays?.getOrNull(i % (body.bays?.size ?: 1)) ?: Bay("0", "Any Bay")
                            list.add(m to b)
                        }
                        
                        if (list.isEmpty()) {
                            val adapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, listOf("No mechanics available for the selected schedule"))
                            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            spinnerAssignment.adapter = adapter
                            spinnerAssignment.isEnabled = false
                        } else {
                            mechBaysList = list
                            val labels = list.map { it.first.full_name ?: "Mechanic" }
                            val adapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, labels)
                            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            spinnerAssignment.adapter = adapter
                            spinnerAssignment.isEnabled = true
                        }
                    } else {
                        val adapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, listOf("Error loading mechanics"))
                        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                        spinnerAssignment.adapter = adapter
                        spinnerAssignment.isEnabled = false
                    }
                }
                override fun onFailure(call: Call<MechanicsBaysResponse>, t: Throwable) {
                    if (isAdded) {
                        val adapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, listOf("Network error"))
                        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                        spinnerAssignment.adapter = adapter
                        spinnerAssignment.isEnabled = false
                        Toast.makeText(ctx, "Failed to load mechanics", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        // Initialize with default state
        updateMechanicSpinner()

        api.getServices(action = "get_services", tenantId = tid).enqueue(object : Callback<ServiceResponse> {
            override fun onResponse(call: Call<ServiceResponse>, response: Response<ServiceResponse>) {
                if (isAdded && response.isSuccessful) {
                    servicesList = response.body()?.data ?: emptyList()
                    tvSelectServices.setOnClickListener {
                        if (servicesList.isEmpty()) {
                            Toast.makeText(ctx, "Services not loaded. Check server.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        
                        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_services_select, null)
                        val rv = dialogView.findViewById<RecyclerView>(R.id.rvServicesSelect)
                        val btnApply = dialogView.findViewById<Button>(R.id.btnApplyServices)
                        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelServices)
                        
                        val tempIds = selectedServiceIds.toMutableList()
                        rv.layoutManager = LinearLayoutManager(ctx)
                        rv.adapter = ServicesSelectAdapter(servicesList, tempIds) { id, isChecked ->
                            if (isChecked) tempIds.add(id) else tempIds.remove(id)
                        }
                        
                        val dialog = AlertDialog.Builder(ctx, R.style.CustomDialog).setView(dialogView).create()
                        btnApply.setOnClickListener {
                            selectedServiceIds.clear()
                            selectedServiceIds.addAll(tempIds)
                            selectedServiceNames.clear()
                            var total = 0.0
                            for (s in servicesList) {
                                if (selectedServiceIds.contains(s.service_id ?: "")) {
                                    selectedServiceNames.add(s.service_name ?: "")
                                    total += s.price?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                                }
                            }
                            tvSelectServices.text = if (selectedServiceNames.isEmpty()) "Choose Services..." else selectedServiceNames.joinToString(", ")
                            tvEstimate.text = String.format("\u20b1%.2f", total)
                            updateMechanicSpinner()
                            dialog.dismiss()
                        }
                        btnCancel.setOnClickListener { dialog.dismiss() }
                        dialog.show()
                        
                        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
                        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                    }
                }
            }
            override fun onFailure(call: Call<ServiceResponse>, t: Throwable) {
                if (isAdded) Toast.makeText(ctx, "Services Fetch Error", Toast.LENGTH_SHORT).show()
            }
        })

        // 4. Mechanic logic handled inside updateMechanicSpinner() which is triggered by date/time selection

        // 5. Time Slots
        val allTimeSlots = listOf("8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")
        
        fun updateTimeSlots() {
            val date = etDate.text.toString()
            if (date.isEmpty()) return
            api.getBookedSlots(action = "get_booked_slots", tenantId = tid, date = date).enqueue(object : Callback<BookedSlotsResponse> {
                override fun onResponse(call: Call<BookedSlotsResponse>, response: Response<BookedSlotsResponse>) {
                    if (isAdded && response.isSuccessful) {
                        val booked = response.body()?.booked_slots ?: emptyList()
                        val available = allTimeSlots.filter { !booked.contains(it) }
                        val adapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, available)
                        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                        spinnerTime.adapter = adapter
                    }
                }
                override fun onFailure(call: Call<BookedSlotsResponse>, t: Throwable) {
                    if (isAdded) Toast.makeText(ctx, "Slots Offline: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        val timeAdapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, allTimeSlots)
        timeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerTime.adapter = timeAdapter

        spinnerTime.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateMechanicSpinner()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 6. Date Picker
        etDate.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            val dpd = android.app.DatePickerDialog(ctx, { _, y, m, d ->
                val dateStr = String.format("%04d-%02d-%02d", y, m + 1, d)
                etDate.setText(dateStr)
                updateTimeSlots()
                updateMechanicSpinner()
            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH))
            dpd.datePicker.minDate = c.timeInMillis
            dpd.show()
        }

        // 7. Submit
        btnSubmit.setOnClickListener {
            val ctxInner = context ?: return@setOnClickListener
            
            if (etDate.text.isEmpty()) {
                Toast.makeText(ctxInner, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val slotsText = tvSlots.text.toString()
            val availableBays = if (slotsText == "-" || slotsText == "...") 1 else slotsText.toIntOrNull() ?: 0
            
            if (availableBays <= 0) {
                Toast.makeText(ctxInner, "Sorry, all service bays are currently occupied.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val today = java.util.Calendar.getInstance()
            today.set(java.util.Calendar.HOUR_OF_DAY, 0)
            today.set(java.util.Calendar.MINUTE, 0)
            today.set(java.util.Calendar.SECOND, 0)
            today.set(java.util.Calendar.MILLISECOND, 0)

            if (selectedCalendar != null && selectedCalendar!!.before(today)) {
                Toast.makeText(ctxInner, "You cannot select a past date.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val selectedVehicleId = vehicleList.getOrNull(spinnerVehicle.selectedItemPosition)?.vehicle_id ?: "0"
            val serviceIdsString = selectedServiceIds.joinToString(",")
            val estimateVal = tvEstimate.text.toString().replace("\u20b1", "").replace(",", "").trim()
            val selectedAssgn = mechBaysList.getOrNull(spinnerAssignment.selectedItemPosition)

            if (selectedServiceIds.isEmpty() || estimateVal.isEmpty() || estimateVal == "0.00") {
                Toast.makeText(ctxInner, "Please select at least one service.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (mechBaysList.isEmpty()) {
                Toast.makeText(ctxInner, "Please select an available mechanic for the chosen schedule.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                // Determine Mechanic ID safely
                val mechanicId = selectedAssgn?.first?.mechanic_id ?: "0"
                val mechanicName = selectedAssgn?.first?.full_name ?: "Auto Assign"

                val intent = Intent(ctxInner, PaymentActivity::class.java).apply {
                    putExtra("serviceId", serviceIdsString)
                    putExtra("serviceName", if (selectedServiceNames.size > 1) "Multiple Services" else selectedServiceNames.firstOrNull() ?: "General Service")
                    putExtra("vehicleId", selectedVehicleId)
                    putExtra("date", etDate.text.toString())
                    putExtra("time", spinnerTime.selectedItem?.toString() ?: "8:00 AM")
                    putExtra("estimate", estimateVal)
                    putExtra("mechanicId", mechanicId)
                    putExtra("mechanicName", mechanicName)
                    putExtra("bayId", "0") 
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(ctxInner, "Could not open Payment screen. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// --- 3. GARAGE FRAGMENT ---
class GarageFragment : Fragment(R.layout.fragment_garage) {
    private lateinit var adapter: VehicleAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        val sm = SessionManager(ctx)
        val api = RetrofitClient.getApiService(ctx)
        val tid = sm.getTenantId() ?: "1"

        val rv = view.findViewById<RecyclerView>(R.id.rvVehicles)
        val emptyLayout = view.findViewById<View>(R.id.layoutEmptyGarage)
        val btnAdd = view.findViewById<View>(R.id.btnAddVehicle)

        fun refreshGarage() {
            api.getGarage(action = "get_garage", tenantId = tid, customerId = sm.getCustomerId() ?: "").enqueue(object : Callback<GarageResponse> {
                override fun onResponse(call: Call<GarageResponse>, response: Response<GarageResponse>) {
                    if (isAdded && response.isSuccessful) {
                        val list = response.body()?.data ?: emptyList()
                        adapter.updateData(list)
                        emptyLayout.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                override fun onFailure(call: Call<GarageResponse>, t: Throwable) {}
            })
        }

        adapter = VehicleAdapter(emptyList()) { vehicle ->
            val vehicleId = vehicle.vehicle_id ?: return@VehicleAdapter
            
            // Immediate UI update (Optimistic)
            val currentList = adapter.getVehicles().toMutableList()
            val removedItem = currentList.find { it.vehicle_id == vehicleId }
            if (removedItem != null) {
                currentList.remove(removedItem)
                adapter.updateData(currentList)
                emptyLayout.visibility = if (currentList.isEmpty()) View.VISIBLE else View.GONE
            }

            api.deleteVehicle(action = "remove_vehicle", tid = tid, customerId = sm.getCustomerId() ?: "", vehicleId = vehicleId)
                .enqueue(object : Callback<BaseResponse> {
                    override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            // Success! No need to do anything as it's already removed from UI
                        } else {
                            // If it failed on server, tell the user why
                            val msg = response.body()?.message ?: "Server sync failed"
                            if (isAdded) Toast.makeText(ctx, "Database Error: $msg", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                        if (isAdded) Toast.makeText(ctx, "Network Error: Could not reach database", Toast.LENGTH_SHORT).show()
                    }
                })
        }
        rv.layoutManager = LinearLayoutManager(ctx)
        rv.adapter = adapter


        btnAdd.setOnClickListener {
            val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_vehicle, null)
            val etPlate = dialogView.findViewById<EditText>(R.id.etPlate)
            val etMake = dialogView.findViewById<EditText>(R.id.etMake)
            val etModel = dialogView.findViewById<EditText>(R.id.etModel)
            val etYear = dialogView.findViewById<EditText>(R.id.etYear)
            val btnSubmit = dialogView.findViewById<Button>(R.id.btnAdd)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

            val dialog = AlertDialog.Builder(ctx, R.style.CustomDialog)
                .setView(dialogView)
                .create()

            btnSubmit.setOnClickListener {
                val plate = etPlate.text.toString().trim()
                val make = etMake.text.toString().trim()
                val model = etModel.text.toString().trim()
                val year = etYear.text.toString().trim()

                if (plate.isEmpty() || make.isEmpty() || model.isEmpty()) {
                    Toast.makeText(ctx, "Please fill in all details", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (make.any { it.isDigit() } || model.any { it.isDigit() }) {
                    Toast.makeText(ctx, "Numbers are not allowed in the Make or Model.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                api.addVehicle(action = "add_vehicle", tid = tid, customerId = sm.getCustomerId() ?: "", plateNo = plate, make = make, model = model, year = year)
                    .enqueue(object : Callback<BaseResponse> {
                        override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                            if (isAdded && response.isSuccessful && response.body()?.status == "success") {
                                Toast.makeText(ctx, "Vehicle Added!", Toast.LENGTH_SHORT).show()
                                refreshGarage()
                                dialog.dismiss()
                            } else {
                                val msg = response.body()?.message ?: "Failed to add vehicle"
                                if (isAdded) Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
                    })
            }

            btnCancel.setOnClickListener { dialog.dismiss() }
            dialog.show()

            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        refreshGarage()
    }
}

// --- 4. HISTORY FRAGMENT ---
class HistoryFragment : Fragment(R.layout.fragment_history) {
    private lateinit var apptAdapter: AppointmentAdapter
    private lateinit var repairAdapter: HistoryAdapter
    private lateinit var payAdapter: PaymentHistoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        val sm = SessionManager(ctx)
        val api = RetrofitClient.getApiService(ctx)

        val rvAppt = view.findViewById<RecyclerView>(R.id.rvAppointments)
        val rvRepair = view.findViewById<RecyclerView>(R.id.rvHistory)
        val rvPay = view.findViewById<RecyclerView>(R.id.rvPayments)
        val rgTabs = view.findViewById<android.widget.RadioGroup>(R.id.rgTabs)

        apptAdapter = AppointmentAdapter(emptyList())
        repairAdapter = HistoryAdapter(emptyList())
        payAdapter = PaymentHistoryAdapter(emptyList())

        rvAppt.layoutManager = LinearLayoutManager(ctx)
        rvAppt.adapter = apptAdapter
        rvRepair.layoutManager = LinearLayoutManager(ctx)
        rvRepair.adapter = repairAdapter
        rvPay.layoutManager = LinearLayoutManager(ctx)
        rvPay.adapter = payAdapter

        // Tab Switching Logic
        rgTabs.setOnCheckedChangeListener { _, checkedId ->
            rvAppt.visibility = if (checkedId == R.id.rbAppointments) View.VISIBLE else View.GONE
            rvRepair.visibility = if (checkedId == R.id.rbRepairs) View.VISIBLE else View.GONE
            rvPay.visibility = if (checkedId == R.id.rbPayments) View.VISIBLE else View.GONE
        }

        api.getHistory(action = "get_history", tenantId = sm.getTenantId() ?: "1", customerId = sm.getCustomerId() ?: "").enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (isAdded && response.isSuccessful) {
                    val body = response.body()
                    val repairList = body?.repairs ?: emptyList()
                    apptAdapter.updateData(repairList)
                    repairAdapter.updateData(repairList)
                    body?.payments?.let { payAdapter.updateData(it) }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                if (isAdded) Toast.makeText(ctx, "History Sync Failed", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

// --- 5. TRACKING FRAGMENT ---
class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private lateinit var adapter: TimelineAdapter
    private var timeline = mutableListOf<TimelineItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        val sm = SessionManager(ctx)
        val api = RetrofitClient.getApiService(ctx)

        val etJobId = view.findViewById<EditText>(R.id.etJobId)
        val btnTrack = view.findViewById<View>(R.id.btnTrackRepair)
        val rv = view.findViewById<RecyclerView>(R.id.rvTimeline)
        val layoutJobDetails = view.findViewById<LinearLayout>(R.id.layoutTrackingDetails)
        val tvJobStatus = view.findViewById<TextView>(R.id.tvTrackStatus)
        val tvJobCar = view.findViewById<TextView>(R.id.tvTrackVehicle)

        adapter = TimelineAdapter(timeline)
        rv.layoutManager = LinearLayoutManager(ctx)
        rv.adapter = adapter

        btnTrack.setOnClickListener {
            val jobId = etJobId.text.toString().trim()
            if (jobId.isEmpty()) return@setOnClickListener

            api.trackRepair(action = "track_repair", tenantId = sm.getTenantId() ?: "1", jobId = jobId, customerId = sm.getCustomerId() ?: "")
                .enqueue(object : Callback<TrackingResponse> {
                    override fun onResponse(call: Call<TrackingResponse>, response: Response<TrackingResponse>) {
                        if (isAdded && response.isSuccessful && response.body()?.status == "success") {
                            val data = response.body()!!
                            layoutJobDetails.visibility = View.VISIBLE
                            tvJobStatus.text = data.jobInfo.status?.uppercase() ?: "UNKNOWN"
                            tvJobCar.text = "${data.jobInfo.make ?: ""} ${data.jobInfo.model ?: ""}"
                            
                            timeline.clear()
                            timeline.addAll(data.timeline)
                            adapter.notifyDataSetChanged()
                        } else {
                            Toast.makeText(ctx, "Job ID not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<TrackingResponse>, t: Throwable) {
                        if (isAdded) Toast.makeText(ctx, "Network Error", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        arguments?.getString("JOB_ID")?.let { etJobId.setText(it); btnTrack.performClick() }
    }
}

// --- 5. PROFILE FRAGMENT ---
class ProfileFragment : Fragment(R.layout.fragment_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        val sm = SessionManager(ctx)

        val tvName = view.findViewById<TextView>(R.id.tvProfileName)
        val layoutLogout = view.findViewById<androidx.cardview.widget.CardView>(R.id.layoutLogout)
        val layoutChat = view.findViewById<androidx.cardview.widget.CardView>(R.id.layoutChat)

        tvName.text = sm.getCustomerName().let { if (it.isNullOrBlank()) "Guest User" else it }

        layoutChat.setOnClickListener {
            (activity as? HomeActivity)?.loadFragment(ChatFragment())
        }

        layoutLogout.setOnClickListener {
            AlertDialog.Builder(ctx)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to end your session?")
                .setPositiveButton("Logout") { _, _ ->
                    sm.clearSession()
                    startActivity(Intent(activity, MainActivity::class.java))
                    activity?.finish()
                }
                .setNegativeButton("Wait, stay", null)
                .show()
        }
    }
}

class ChatFragment : Fragment(R.layout.fragment_chat) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.btnBackFromChat)?.setOnClickListener {
            (activity as? HomeActivity)?.loadFragment(ProfileFragment())
        }
    }
}
