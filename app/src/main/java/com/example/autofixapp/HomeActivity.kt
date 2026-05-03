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

        // 1. Fetch Loyalty Points
        val tvPoints = view.findViewById<TextView>(R.id.tvLoyaltyPoints)
        val tvTier = view.findViewById<TextView>(R.id.tvLoyaltyTier)
        
        api.getLoyaltyStatus(tid, cid).enqueue(object : Callback<LoyaltyResponse> {
            override fun onResponse(call: Call<LoyaltyResponse>, response: Response<LoyaltyResponse>) {
                if (isAdded && response.isSuccessful) {
                    val body = response.body()
                    tvPoints.text = (body?.points ?: 0).toString()
                    tvTier.text = body?.tier ?: "BRONZE MEMBER"
                }
            }
            override fun onFailure(call: Call<LoyaltyResponse>, t: Throwable) {}
        })

        // 2. Fetch Wait Time
        val tvWaitTime = view.findViewById<TextView>(R.id.tvHomeWaitTime)
        api.getAvailability(tid).enqueue(object : Callback<AvailabilityResponse> {
            override fun onResponse(call: Call<AvailabilityResponse>, response: Response<AvailabilityResponse>) {
                if (isAdded && response.isSuccessful) {
                    tvWaitTime.text = response.body()?.waiting_time ?: "Ready Now"
                }
            }
            override fun onFailure(call: Call<AvailabilityResponse>, t: Throwable) {
                if (isAdded) tvWaitTime.text = "Offline"
            }
        })

        // 3. Fetch Service Catalog
        val grid = view.findViewById<android.widget.GridLayout>(R.id.gridServices)
        val tvNoServices = view.findViewById<TextView>(R.id.tvNoServices)
        
        api.getServices(tid).enqueue(object : Callback<ServiceResponse> {
            override fun onResponse(call: Call<ServiceResponse>, response: Response<ServiceResponse>) {
                if (isAdded && response.isSuccessful) {
                    val services = response.body()?.data ?: emptyList()
                    if (services.isNotEmpty()) {
                        grid.visibility = View.VISIBLE
                        tvNoServices.visibility = View.GONE
                        grid.removeAllViews()
                        
                        services.take(4).forEach { service ->
                            val card: View = LayoutInflater.from(ctx).inflate(R.layout.item_service_home, grid, false)
                            val tvName = card.findViewById<TextView>(R.id.tvServiceNameHome)
                            val tvPrice = card.findViewById<TextView>(R.id.tvServicePriceHome)
                            
                            tvName.text = service.service_name
                            tvPrice.text = "Start at \u20b1${service.price}"
                            
                            val params = android.widget.GridLayout.LayoutParams()
                            params.width = 0
                            params.height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                            params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                            card.layoutParams = params
                            
                            grid.addView(card)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<ServiceResponse>, t: Throwable) {}
        })

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
        api.getAvailability(tid).enqueue(object : Callback<AvailabilityResponse> {
            override fun onResponse(call: Call<AvailabilityResponse>, response: Response<AvailabilityResponse>) {
                if (isAdded && response.isSuccessful) {
                    val b = response.body()
                    tvSlots.text = b?.available_bays?.toString() ?: "0"
                    tvMechs.text = b?.available_mechanics?.toString() ?: "0"
                    tvWait.text = b?.waiting_time ?: "-"
                }
            }
            override fun onFailure(call: Call<AvailabilityResponse>, t: Throwable) {
                if (isAdded) Toast.makeText(ctx, "Availability Info Offline", Toast.LENGTH_SHORT).show()
            }
        })

        // 2. Fetch Garage
        api.getGarage(tid, sm.getCustomerId() ?: "").enqueue(object : Callback<GarageResponse> {
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
        var selectedServiceIds = mutableListOf<String>()
        var selectedServiceNames = mutableListOf<String>()

        api.getServices(tid).enqueue(object : Callback<ServiceResponse> {
            override fun onResponse(call: Call<ServiceResponse>, response: Response<ServiceResponse>) {
                if (isAdded && response.isSuccessful) {
                    servicesList = response.body()?.data ?: emptyList()
                    tvSelectServices.setOnClickListener {
                        if (servicesList.isEmpty()) return@setOnClickListener
                        
                        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_services_select, null)
                        val rv = dialogView.findViewById<RecyclerView>(R.id.rvServicesSelect)
                        val btnApply = dialogView.findViewById<Button>(R.id.btnApplyServices)
                        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelServices)
                        
                        val tempIds = selectedServiceIds.toMutableList()
                        
                        rv.layoutManager = LinearLayoutManager(ctx)
                        rv.adapter = ServicesSelectAdapter(servicesList, tempIds) { id, isChecked ->
                            if (isChecked) if (!tempIds.contains(id)) tempIds.add(id) else {}
                            else tempIds.remove(id)
                        }
                        
                        val dialog = AlertDialog.Builder(ctx, R.style.CustomDialog)
                            .setView(dialogView)
                            .create()
                            
                        btnApply.setOnClickListener {
                            selectedServiceIds.clear()
                            selectedServiceIds.addAll(tempIds)
                            selectedServiceNames.clear()
                            var total = 0.0
                            for (s in servicesList) {
                                if (selectedServiceIds.contains(s.service_id ?: "")) {
                                    selectedServiceNames.add(s.service_name ?: "Unknown Service")
                                    total += s.price?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                                }
                            }
                            tvSelectServices.text = if (selectedServiceNames.isEmpty()) "Choose Services..." else selectedServiceNames.joinToString(", ")
                            tvEstimate.text = String.format("\u20b1%.2f", total)
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

        // 4. Fetch Mechanics & Bays
        api.getMechanicsAndBays(tid).enqueue(object : Callback<MechanicsBaysResponse> {
            override fun onResponse(call: Call<MechanicsBaysResponse>, response: Response<MechanicsBaysResponse>) {
                if (isAdded && response.isSuccessful && response.body()?.status == "success") {
                    val body = response.body()!!
                    val list = mutableListOf<Pair<Mechanic, Bay>>()
                    
                    body.mechanics?.let { mechanics ->
                        if (mechanics.isNotEmpty()) {
                            for (i in mechanics.indices) {
                                val m = mechanics[i]
                                val b = body.bays?.getOrNull(i % (body.bays?.size ?: 1)) ?: Bay("0", "Any Bay")
                                list.add(m to b)
                            }
                        } else {
                            list.add(Mechanic("0", "Auto Assign", "") to Bay("0", "Any Bay"))
                        }
                    } ?: run {
                        list.add(Mechanic("0", "Auto Assign", "") to Bay("0", "Any Bay"))
                    }

                    allMechBaysList = list
                    mechBaysList = list
                    val labels = list.map { it.first.full_name }
                    val adapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, labels)
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    spinnerAssignment.adapter = adapter
                }
            }
            override fun onFailure(call: Call<MechanicsBaysResponse>, t: Throwable) {}
        })

        // 5. Time Slots
        val allTimeSlots = listOf("8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")
        
        fun updateMechanicSpinner() {
            val selectedDate = etDate.text.toString()
            val selectedTime = spinnerTime.selectedItem?.toString()
            if (selectedDate.isNotEmpty() && selectedTime != null) {
                val sp = ctx.getSharedPreferences("LocalBookings", android.content.Context.MODE_PRIVATE)
                val bookedMechId = sp.getString("booked_mech_${selectedDate}_${selectedTime}", null)
                val bookedMechName = sp.getString("booked_mech_name_${selectedDate}_${selectedTime}", null)
                
                mechBaysList = allMechBaysList.filter { 
                     val isSameId = bookedMechId != null && it.first.mechanic_id == bookedMechId && it.first.mechanic_id != "0"
                     val isSameName = bookedMechName != null && it.first.full_name == bookedMechName
                     !(isSameId || isSameName)
                }
            } else {
                mechBaysList = allMechBaysList
            }
            val labels = mechBaysList.map { it.first.full_name }
            val adapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, labels)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            spinnerAssignment.adapter = adapter
        }

        val timeAdapter = ArrayAdapter(ctx, R.layout.spinner_item, android.R.id.text1, allTimeSlots)
        timeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerTime.adapter = timeAdapter

        // 6. Date Picker
        etDate.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            val year = c.get(java.util.Calendar.YEAR)
            val month = c.get(java.util.Calendar.MONTH)
            val day = c.get(java.util.Calendar.DAY_OF_MONTH)

            val dpd = android.app.DatePickerDialog(ctx, { _, y, m, d ->
                val chosen = java.util.Calendar.getInstance()
                chosen.set(y, m, d, 0, 0, 0)
                selectedCalendar = chosen
                val dateStr = String.format("%04d-%02d-%02d", y, m + 1, d)
                etDate.setText(dateStr)
                updateMechanicSpinner()
            }, year, month, day)

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
                Toast.makeText(ctxInner, "Please select a valid service first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val intent = Intent(ctxInner, PaymentActivity::class.java).apply {
                    putExtra("serviceId", serviceIdsString)
                    putExtra("serviceName", if (selectedServiceNames.size > 1) "Multiple Services" else selectedServiceNames.firstOrNull())
                    putExtra("vehicleId", selectedVehicleId)
                    putExtra("date", etDate.text.toString())
                    putExtra("time", spinnerTime.selectedItem?.toString())
                    putExtra("estimate", estimateVal)
                    putExtra("mechanicId", selectedAssgn?.first?.mechanic_id)
                    putExtra("mechanicName", selectedAssgn?.first?.full_name)
                    putExtra("bayId", null as String?)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(ctxInner, "Launch Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            api.getGarage(tid, sm.getCustomerId() ?: "").enqueue(object : Callback<GarageResponse> {
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

            api.deleteVehicle(tidQuery = tid, customerId = sm.getCustomerId() ?: "", vehicleId = vehicleId)
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

                api.addVehicle(tid, "add_vehicle", sm.getCustomerId() ?: "", plate, make, model, year)
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

        api.getHistory(sm.getTenantId() ?: "1", sm.getCustomerId() ?: "").enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (isAdded && response.isSuccessful) {
                    val body = response.body()
                    apptAdapter.updateData(body?.bookings ?: emptyList())
                    repairAdapter.updateData(body?.services ?: emptyList())
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

            api.trackRepair(sm.getTenantId() ?: "1", jobId, sm.getCustomerId() ?: "")
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
