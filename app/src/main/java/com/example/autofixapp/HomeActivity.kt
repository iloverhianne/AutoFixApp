package com.example.autofixapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import androidx.cardview.widget.CardView
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel()
            super.onBackPressed()
            return
        } else {
            backToast = Toast.makeText(baseContext, "Press back again to exit", Toast.LENGTH_SHORT)
            backToast.show()
        }
        backPressedTime = System.currentTimeMillis()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Default fragment logic
        val navigateTo = intent.getStringExtra("NAVIGATE_TO")
        when (navigateTo) {
            "track" -> {
                bottomNav.selectedItemId = R.id.nav_history
                loadFragment(HistoryFragment())
            }
            "garage" -> {
                bottomNav.selectedItemId = R.id.nav_garage
                loadFragment(GarageFragment())
            }
            else -> {
                loadFragment(HomeFragment())
            }
        }

        setupBottomNavListener(bottomNav)
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

    fun navigateToTracking(jobId: String) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener(null)
        bottomNav.selectedItemId = R.id.nav_history
        
        val fragment = TrackingFragment()
        val bundle = Bundle()
        bundle.putString("JOB_ID", jobId)
        fragment.arguments = bundle
        
        loadFragment(fragment)
        setupBottomNavListener(bottomNav)
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
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        val sessionManager = SessionManager(context)

        val tvWaitTime = view.findViewById<TextView>(R.id.tvHomeWaitTime)
        val gridServices = view.findViewById<GridLayout>(R.id.gridServices)
        val tvNoServices = view.findViewById<TextView>(R.id.tvNoServices)
        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcomeName)
        val tvPoints = view.findViewById<TextView>(R.id.tvLoyaltyPoints)
        val tvTier = view.findViewById<TextView>(R.id.tvLoyaltyTier)
        
        val name = sessionManager.getCustomerName().let { if (it.isNullOrBlank()) "Guest" else it }
        val cid = sessionManager.getCustomerId() ?: ""
        val tid = sessionManager.getTenantId() ?: "1"

        tvWelcome.text = "Mabuhay, $name!"

        val layoutPromos = view.findViewById<LinearLayout>(R.id.layoutPromos)
        // Make static promos clickable too
        val outValue = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        for (i in 0 until layoutPromos.childCount) {
            val child = layoutPromos.getChildAt(i)
            child.isClickable = true
            child.isFocusable = true
            child.foreground = context.getDrawable(outValue.resourceId)
            child.setOnClickListener {
                activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_book
                Toast.makeText(context, "Redirecting to Booking...", Toast.LENGTH_SHORT).show()
            }
        }

        fetchAvailability(tvWaitTime, tid)
        fetchServices(gridServices, tvNoServices, tid)
        fetchLoyalty(tvPoints, tvTier, layoutPromos, tid, cid)

        view.findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Logout")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Logout") { _, _ ->
                    sessionManager.clearSession()
                    startActivity(Intent(activity, MainActivity::class.java))
                    activity?.finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun fetchLoyalty(tvPoints: TextView, tvTier: TextView, layoutPromos: LinearLayout, tid: String, cid: String) {
        RetrofitClient.getApiService(requireContext()).getLoyaltyStatus(tid, cid)
            .enqueue(object : Callback<LoyaltyResponse> {
                override fun onResponse(call: Call<LoyaltyResponse>, response: Response<LoyaltyResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val data = response.body()
                        tvPoints.text = String.format("%,d", data?.points ?: 0)
                        tvTier.text = "${data?.tier?.uppercase()} MEMBER"
                        data?.available_promos?.let { populatePromos(layoutPromos, it) }
                    }
                }
                override fun onFailure(call: Call<LoyaltyResponse>, t: Throwable) {}
            })
    }

    private fun populatePromos(layout: LinearLayout, promos: List<Promo>) {
        layout.removeAllViews()
        val density = resources.displayMetrics.density
        for ((index, promo) in promos.withIndex()) {
            val card = CardView(layout.context).apply {
                layoutParams = LinearLayout.LayoutParams((260 * density).toInt(), (140 * density).toInt()).apply {
                    marginEnd = (16 * density).toInt()
                }
                radius = 20 * density
                cardElevation = 0f
                isClickable = true
                isFocusable = true
                val ripple = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)
                foreground = context.getDrawable(ripple.resourceId)
                setCardBackgroundColor(Color.parseColor(if (index % 2 == 0) "#1A10B981" else "#1A06B6D4"))
            }

            val container = LinearLayout(layout.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt())
                gravity = Gravity.CENTER_VERTICAL
            }

            container.addView(TextView(layout.context).apply {
                text = promo.title
                setTextColor(Color.parseColor(if (index % 2 == 0) "#10B981" else "#06B6D4"))
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
            })

            container.addView(TextView(layout.context).apply {
                text = promo.description
                setTextColor(Color.WHITE)
                textSize = 13f
                setPadding(0, (8 * density).toInt(), 0, 0)
            })

            card.addView(container)
            card.setOnClickListener {
                activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_book
                Toast.makeText(layout.context, "Promo Applied: ${promo.title}", Toast.LENGTH_SHORT).show()
            }
            layout.addView(card)
        }
    }

    private fun fetchAvailability(tvWaitTime: TextView, tid: String) {
        RetrofitClient.getApiService(requireContext()).getAvailability(tid)
            .enqueue(object : Callback<AvailabilityResponse> {
                override fun onResponse(call: Call<AvailabilityResponse>, response: Response<AvailabilityResponse>) {
                    if (response.isSuccessful) tvWaitTime.text = response.body()?.waiting_time ?: "N/A"
                }
                override fun onFailure(call: Call<AvailabilityResponse>, t: Throwable) { tvWaitTime.text = "Offline" }
            })
    }

    private fun fetchServices(grid: GridLayout, emptyMsg: TextView, tid: String) {
        RetrofitClient.getApiService(requireContext()).getServices(tid)
            .enqueue(object : Callback<ServiceResponse> {
                override fun onResponse(call: Call<ServiceResponse>, response: Response<ServiceResponse>) {
                    if (response.isSuccessful) {
                        val services = response.body()?.data ?: emptyList()
                        if (services.isNotEmpty()) {
                            populateGrid(grid, services)
                            grid.visibility = View.VISIBLE
                            emptyMsg.visibility = View.GONE
                        }
                    }
                }
                override fun onFailure(call: Call<ServiceResponse>, t: Throwable) {}
            })
    }

    private fun populateGrid(grid: GridLayout, services: List<Service>) {
        grid.removeAllViews()
        for (service in services) {
            val row = LayoutInflater.from(grid.context).inflate(R.layout.item_service_row, grid, false)
            row.findViewById<TextView>(R.id.tvServiceName).text = service.service_name
            row.findViewById<TextView>(R.id.tvServicePrice).text = "₱${service.price}"
            
            row.setOnClickListener {
                activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_book
                Toast.makeText(grid.context, "Booking ${service.service_name}...", Toast.LENGTH_SHORT).show()
            }
            
            grid.addView(row)
        }
    }
}

// --- 2. BOOKING FRAGMENT ---
class BookingFragment : Fragment(R.layout.fragment_booking) {
    private var servicesList: List<Service> = emptyList()
    private var vehicleList: List<Vehicle> = emptyList()
    private var mechBaysList: List<Pair<Mechanic, Bay>> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        val sm = SessionManager(context)
        val api = RetrofitClient.getApiService(context)
        val tid = sm.getTenantId() ?: "1"
        val cid = sm.getCustomerId() ?: ""

        val tvSlots = view.findViewById<TextView>(R.id.tvAvailableSlots)
        val tvMechs = view.findViewById<TextView>(R.id.tvAvailableMechanics)
        val tvWait = view.findViewById<TextView>(R.id.tvWaitingTime)
        val spinnerVehicle = view.findViewById<Spinner>(R.id.spinnerVehicle)
        val spinnerService = view.findViewById<Spinner>(R.id.spinnerService)
        val spinnerTime = view.findViewById<Spinner>(R.id.spinnerTime)
        val spinnerAssignment = view.findViewById<Spinner>(R.id.spinnerAssignment)
        val etDate = view.findViewById<EditText>(R.id.etDate)
        val tvEstimate = view.findViewById<TextView>(R.id.tvEstimate)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitBooking)

        // 1. Fetch Stats
        api.getAvailability(tid).enqueue(object : Callback<AvailabilityResponse> {
            override fun onResponse(call: Call<AvailabilityResponse>, response: Response<AvailabilityResponse>) {
                if (response.isSuccessful) {
                    val b = response.body()
                    tvSlots.text = b?.available_bays?.toString() ?: "-"
                    tvMechs.text = b?.available_mechanics?.toString() ?: "-"
                    tvWait.text = b?.waiting_time ?: "-"
                }
            }
            override fun onFailure(call: Call<AvailabilityResponse>, t: Throwable) {}
        })

        // 2. Fetch Garage (Vehicles)
        api.getGarage(tid, cid).enqueue(object : Callback<GarageResponse> {
            override fun onResponse(call: Call<GarageResponse>, response: Response<GarageResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    vehicleList = response.body()?.data ?: emptyList()
                    val labels = if (vehicleList.isEmpty()) listOf("No vehicles found! Add one in Garage first.") 
                                 else vehicleList.map { "${it.make} ${it.model} (${it.plate_no})" }
                    val adapter = ArrayAdapter(context, R.layout.spinner_item, labels)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerVehicle.adapter = adapter
                }
            }
            override fun onFailure(call: Call<GarageResponse>, t: Throwable) {}
        })

        // 3. Fetch Services
        api.getServices(tid).enqueue(object : Callback<ServiceResponse> {
            override fun onResponse(call: Call<ServiceResponse>, response: Response<ServiceResponse>) {
                if (response.isSuccessful) {
                    servicesList = response.body()?.data ?: emptyList()
                    val adapter = ArrayAdapter(context, R.layout.spinner_item, servicesList.map { it.service_name })
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerService.adapter = adapter
                }
            }
            override fun onFailure(call: Call<ServiceResponse>, t: Throwable) {}
        })

        // 4. Fetch Mechanics & Bays
        api.getMechanicsAndBays(tid).enqueue(object : Callback<MechanicsBaysResponse> {
            override fun onResponse(call: Call<MechanicsBaysResponse>, response: Response<MechanicsBaysResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val body = response.body()!!
                    val customNames = listOf("John Benedict", "Justine Dayao", "Keane Manaloto")
                    val list = mutableListOf<Pair<Mechanic, Bay>>()
                    
                    val maxSize = maxOf(body.mechanics.size, body.bays.size, 3) 
                    for (i in 0 until maxSize) {
                        val originalM = body.mechanics.getOrNull(i)
                        val name = customNames.getOrNull(i) ?: originalM?.full_name ?: "Auto Assign"
                        val m = Mechanic(originalM?.mechanic_id ?: "0", name, originalM?.specialization)
                        
                        val b = body.bays.getOrNull(i % body.bays.size) ?: Bay("0", "Any Bay")
                        list.add(m to b)
                    }
                    mechBaysList = list
                    val labels = list.map { "${it.second.bay_name} - ${it.first.full_name}" }
                    val adapter = ArrayAdapter(context, R.layout.spinner_item, labels)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerAssignment.adapter = adapter
                }
            }
            override fun onFailure(call: Call<MechanicsBaysResponse>, t: Throwable) {}
        })

        spinnerService.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (servicesList.isNotEmpty()) tvEstimate.text = "₱${servicesList[p2].price}"
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // 5. Time Slots (8 AM to 5 PM)
        val allTimeSlots = listOf("8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")
        
        fun updateTimeSlots(bookedSlots: List<String>) {
            val availableSlots = allTimeSlots.filter { slot -> !bookedSlots.contains(slot) }
            val timeAdapter = ArrayAdapter(context, R.layout.spinner_item, availableSlots)
            timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTime.adapter = timeAdapter
            
            if (availableSlots.isEmpty()) {
                Toast.makeText(context, "No slots available for this date. Please pick another day.", Toast.LENGTH_LONG).show()
            }
        }

        // Initial empty state or fetch for today? Let's just set default
        updateTimeSlots(emptyList())

        var selectedCalendar: java.util.Calendar? = null

        etDate.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            val datePickerDialog = android.app.DatePickerDialog(context, { _, y, m, d ->
                val chosen = java.util.Calendar.getInstance()
                chosen.set(y, m, d)
                selectedCalendar = chosen
                val dateStr = String.format("%04d-%02d-%02d", y, m + 1, d)
                etDate.setText(dateStr)
                
                // Fetch Booked Slots for this date
                api.getBookedSlots(tid, dateStr).enqueue(object : Callback<BookedSlotsResponse> {
                    override fun onResponse(call: Call<BookedSlotsResponse>, response: Response<BookedSlotsResponse>) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            updateTimeSlots(response.body()?.booked_slots ?: emptyList())
                        } else {
                            updateTimeSlots(emptyList())
                        }
                    }
                    override fun onFailure(call: Call<BookedSlotsResponse>, t: Throwable) {
                        updateTimeSlots(emptyList())
                    }
                })
            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH))
            
            datePickerDialog.show()
        }

        btnSubmit.setOnClickListener {
            if (etDate.text.isEmpty()) {
                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (vehicleList.isEmpty()) {
                Toast.makeText(context, "Please add a vehicle in your Garage first!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val selectedVehicleId = vehicleList.getOrNull(spinnerVehicle.selectedItemPosition)?.vehicle_id ?: "0"
            val selectedService = servicesList.getOrNull(spinnerService.selectedItemPosition)
            val estimateVal = tvEstimate.text.toString().replace("₱", "").trim()
            val selectedAssgn = mechBaysList.getOrNull(spinnerAssignment.selectedItemPosition)

            if (selectedService == null || estimateVal.isEmpty() || estimateVal == "0.00") {
                Toast.makeText(context, "Paki-select po muna ng valid service", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (spinnerTime.selectedItem == null) {
                Toast.makeText(context, "Please select an available time slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if date is in the past
            val today = java.util.Calendar.getInstance().apply { 
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            if (selectedCalendar != null && selectedCalendar!!.before(today)) {
                Toast.makeText(context, "Bawal po pumili ng nakalipas na araw.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Direct to Payment Activity
            val intent = Intent(activity, PaymentActivity::class.java).apply {
                putExtra("serviceId", selectedService.service_id)
                putExtra("serviceName", selectedService.service_name)
                putExtra("vehicleId", selectedVehicleId)
                putExtra("date", etDate.text.toString())
                putExtra("time", spinnerTime.selectedItem?.toString())
                putExtra("estimate", estimateVal)
                putExtra("mechanicId", selectedAssgn?.first?.mechanic_id)
                putExtra("bayId", selectedAssgn?.second?.bay_id)
            }
            startActivity(intent)
        }
    }
}

// --- 3. GARAGE FRAGMENT ---
class GarageFragment : Fragment(R.layout.fragment_garage) {
    private lateinit var adapter: VehicleAdapter
    private var vehicleList = mutableListOf<Vehicle>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvVehicles)
        val btnAdd = view.findViewById<ImageButton>(R.id.btnAddVehicle)
        val emptyState = view.findViewById<LinearLayout>(R.id.layoutEmptyGarage)
        
        adapter = VehicleAdapter(vehicleList)
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter
        
        btnAdd.setOnClickListener { showAddVehicleDialog(emptyState) }
        
        fetchGarage(emptyState)
    }

    private fun showAddVehicleDialog(emptyState: LinearLayout) {
        val context = requireContext()
<<<<<<< HEAD
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_vehicle, null)
        
        val etPlate = dialogView.findViewById<EditText>(R.id.etPlate)
        val etMake = dialogView.findViewById<EditText>(R.id.etMake)
        val etModel = dialogView.findViewById<EditText>(R.id.etModel)
        val etYear = dialogView.findViewById<EditText>(R.id.etYear)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAdd)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(context, R.style.CustomDialog)
            .setView(dialogView)
            .create()

        btnAdd.setOnClickListener {
            val p = etPlate.text.toString().trim()
            val mk = etMake.text.toString().trim()
            val md = etModel.text.toString().trim()
            val yr = etYear.text.toString().trim()
            
            if (p.isEmpty() || mk.isEmpty() || md.isEmpty() || yr.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerVehicle(p, mk, md, yr, emptyState)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
=======
        val density = resources.displayMetrics.density
        
        val dialogView = LayoutInflater.from(context).inflate(android.R.layout.select_dialog_item, null) // Not really used but needed for builder
        
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (24 * density).toInt(), (24 * density).toInt(), (24 * density).toInt())
            setBackgroundColor(Color.parseColor("#121212")) // Match app dark theme
        }

        val title = TextView(context).apply {
            text = "Register New Vehicle"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, (20 * density).toInt())
        }
        layout.addView(title)

        fun createStyledEditText(hintTxt: String, inputType: Int = android.text.InputType.TYPE_CLASS_TEXT): EditText {
            return EditText(context).apply {
                hint = hintTxt
                setHintTextColor(Color.parseColor("#66FFFFFF"))
                setTextColor(Color.WHITE)
                this.inputType = inputType
                background = context.getDrawable(R.drawable.edit_text_bg)
                setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, (12 * density).toInt())
                }
            }
        }

        val etPlate = createStyledEditText("Plate Number (e.g. ABC 1234)").apply {
            filters = arrayOf(android.text.InputFilter.AllCaps())
        }
        val etMake = createStyledEditText("Vehicle Make (e.g. Ford)")
        val etModel = createStyledEditText("Vehicle Model (e.g. Ranger)")
        val etYear = createStyledEditText("Year Model (e.g. 2023)", android.text.InputType.TYPE_CLASS_NUMBER)

        layout.addView(etPlate)
        layout.addView(etMake)
        layout.addView(etModel)
        layout.addView(etYear)

        val dialog = AlertDialog.Builder(context, R.style.CustomAlertDialog) // Ensure we use a dark style
            .setView(layout)
            .create()

        val btnAdd = Button(context).apply {
            text = "REGISTER VEHICLE"
            setTextColor(Color.WHITE)
            background = context.getDrawable(R.drawable.button_vibrant)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (56 * density).toInt()
            ).apply {
                setMargins(0, (8 * density).toInt(), 0, 0)
            }
            setOnClickListener {
                val p = etPlate.text.toString().trim()
                val mk = etMake.text.toString().trim()
                val md = etModel.text.toString().trim()
                val yr = etYear.text.toString().trim()

                if (p.isEmpty() || mk.isEmpty() || md.isEmpty() || yr.isEmpty()) {
                    Toast.makeText(context, "Paki-fill up po lahat ng columns", Toast.LENGTH_SHORT).show()
                } else if (p.length < 3) {
                    Toast.makeText(context, "Masyadong maikli ang Plate Number", Toast.LENGTH_SHORT).show()
                } else if (yr.length != 4) {
                    Toast.makeText(context, "Ilagay ang valid na 4-digit Year", Toast.LENGTH_SHORT).show()
                } else {
                    registerVehicle(p, mk, md, yr, emptyState)
                    dialog.dismiss()
                }
            }
        }
        layout.addView(btnAdd)

        val btnCancel = TextView(context).apply {
            text = "Cancel"
            setTextColor(Color.parseColor("#99FFFFFF"))
            gravity = Gravity.CENTER
            setPadding(0, (16 * density).toInt(), 0, 0)
            setOnClickListener { dialog.dismiss() }
        }
        layout.addView(btnCancel)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
>>>>>>> 8d1554beb2bd82544fe18be950cc8e71c2550568
        dialog.show()
    }


    private fun registerVehicle(p: String, mk: String, md: String, yr: String, emptyState: LinearLayout) {
        val sm = SessionManager(requireContext())
        val context = requireContext()
        
        Toast.makeText(context, "Registering vehicle...", Toast.LENGTH_SHORT).show()
        
        RetrofitClient.getApiService(context)
            .addVehicle(
                tenantIdQuery = sm.getTenantId() ?: "1",
                customerId = sm.getCustomerId() ?: "", 
                plateNo = p.uppercase(), 
                make = mk, 
                model = md, 
                year = yr
            )
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.status == "success") {
                            Toast.makeText(context, "🚗 Vehicle registered successfully!", Toast.LENGTH_LONG).show()
                            fetchGarage(emptyState)
                        } else {
                            Toast.makeText(context, "Registration failed: ${body?.message ?: "Unknown Error"}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                    Toast.makeText(context, "Connection Error: ${t.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("GARAGE_DEBUG", "Add vehicle error", t)
                }
            })
    }

    private fun fetchGarage(emptyState: LinearLayout) {
        val sm = SessionManager(requireContext())
        RetrofitClient.getApiService(requireContext()).getGarage(sm.getTenantId() ?: "1", sm.getCustomerId() ?: "")
            .enqueue(object : Callback<GarageResponse> {
                override fun onResponse(call: Call<GarageResponse>, response: Response<GarageResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        vehicleList.clear()
                        vehicleList.addAll(response.body()?.data ?: emptyList())
                        adapter.notifyDataSetChanged()
                        emptyState.visibility = if (vehicleList.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                override fun onFailure(call: Call<GarageResponse>, t: Throwable) { emptyState.visibility = View.VISIBLE }
            })
    }
}

// --- 4. HISTORY FRAGMENT ---
class HistoryFragment : Fragment(R.layout.fragment_history) {
    private lateinit var apptAdapter: AppointmentAdapter
    private lateinit var repairAdapter: HistoryAdapter
    private lateinit var payAdapter: PaymentHistoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        val sm = SessionManager(context)
        val api = RetrofitClient.getApiService(context)

        val rvAppts = view.findViewById<RecyclerView>(R.id.rvAppointments)
        val rvHistory = view.findViewById<RecyclerView>(R.id.rvHistory)
        val rvPayments = view.findViewById<RecyclerView>(R.id.rvPayments)
        val rgTabs = view.findViewById<RadioGroup>(R.id.rgTabs)

        apptAdapter = AppointmentAdapter(emptyList())
        rvAppts.layoutManager = LinearLayoutManager(context)
        rvAppts.adapter = apptAdapter

        repairAdapter = HistoryAdapter(emptyList()) { (activity as? HomeActivity)?.navigateToTracking(it) }
        rvHistory.layoutManager = LinearLayoutManager(context)
        rvHistory.adapter = repairAdapter

        payAdapter = PaymentHistoryAdapter(emptyList())
        rvPayments.layoutManager = LinearLayoutManager(context)
        rvPayments.adapter = payAdapter

        rgTabs.setOnCheckedChangeListener { _, checkedId ->
            rvAppts.visibility = if (checkedId == R.id.rbAppointments) View.VISIBLE else View.GONE
            rvHistory.visibility = if (checkedId == R.id.rbRepairs) View.VISIBLE else View.GONE
            rvPayments.visibility = if (checkedId == R.id.rbPayments) View.VISIBLE else View.GONE
        }

        api.getHistory(sm.getTenantId() ?: "1", customerId = sm.getCustomerId() ?: "").enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (response.isSuccessful) {
                    val reps = response.body()?.repairs ?: emptyList()
                    apptAdapter.updateData(reps.filter { it.status.lowercase() == "pending" || it.status.lowercase() == "confirmed" })
                    repairAdapter.updateData(reps.filter { it.status.lowercase() != "pending" && it.status.lowercase() != "confirmed" })
                    response.body()?.payments?.let { payAdapter.updateData(it) }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {}
        })
    }
}

// --- 5. OTHERS ---
class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private lateinit var adapter: TimelineAdapter
    private var timeline = mutableListOf<TimelineItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        val sm = SessionManager(context)
        val api = RetrofitClient.getApiService(context)

        val etJobId = view.findViewById<EditText>(R.id.etJobId)
        val btnTrack = view.findViewById<Button>(R.id.btnTrackRepair)
        val rv = view.findViewById<RecyclerView>(R.id.rvTimeline)
        val layoutJobDetails = view.findViewById<LinearLayout>(R.id.layoutTrackingDetails)
        val tvJobStatus = view.findViewById<TextView>(R.id.tvTrackStatus)
        val tvJobCar = view.findViewById<TextView>(R.id.tvTrackVehicle)

        adapter = TimelineAdapter(timeline)
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter

        btnTrack.setOnClickListener {
            val jobId = etJobId.text.toString().trim()
            if (jobId.isEmpty()) return@setOnClickListener

            api.trackRepair(sm.getTenantId() ?: "1", jobId, sm.getCustomerId() ?: "")
                .enqueue(object : Callback<TrackingResponse> {
                    override fun onResponse(call: Call<TrackingResponse>, response: Response<TrackingResponse>) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            val data = response.body()!!
                            layoutJobDetails.visibility = View.VISIBLE
                            tvJobStatus.text = data.jobInfo.status.uppercase()
                            tvJobCar.text = "${data.jobInfo.make} ${data.jobInfo.model}"
                            
                            timeline.clear()
                            timeline.addAll(data.timeline)
                            adapter.notifyDataSetChanged()
                        } else {
                            Toast.makeText(context, "Job ID not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<TrackingResponse>, t: Throwable) {
                        Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        arguments?.getString("JOB_ID")?.let { etJobId.setText(it); btnTrack.performClick() }
    }
}


// --- 6. PROFILE FRAGMENT ---
class ProfileFragment : Fragment(R.layout.fragment_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        val sessionManager = SessionManager(context)

        val tvName = view.findViewById<TextView>(R.id.tvProfileName)
        val layoutLogout = view.findViewById<LinearLayout>(R.id.layoutLogout)
        val layoutChat = view.findViewById<LinearLayout>(R.id.layoutChat)

        val name = sessionManager.getCustomerName().let { if (it.isNullOrBlank()) "Guest User" else it }
        tvName.text = name

        layoutChat.setOnClickListener {
            (activity as? HomeActivity)?.loadFragment(ChatFragment())
        }

        layoutLogout.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to end your session?")
                .setPositiveButton("Logout") { _, _ ->
                    sessionManager.clearSession()
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
        val btn = view.findViewById<Button>(R.id.btnBackFromChat)
        btn?.setOnClickListener {
            (activity as? HomeActivity)?.loadFragment(ProfileFragment())
        }
    }
}
