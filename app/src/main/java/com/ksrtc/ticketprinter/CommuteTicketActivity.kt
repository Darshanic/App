package com.sktc.ticketprinter

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Bundle
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class CommuteTicketActivity : AppCompatActivity() {

    companion object {
        private const val MAX_ADULTS_PER_TICKET = 5
        private const val MAX_CHILDREN_PER_TICKET = 5
    }

    private val bluetoothManager = BluetoothPrinterManager()
    private val ticketFormatter = TicketFormatter()
    private lateinit var routeDslParser: RouteDslParser

    private lateinit var tvDivision: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvRouteNumber: TextView
    private lateinit var tvRouteInfo: TextView
    private lateinit var spFromStop: Spinner
    private lateinit var spToStop: Spinner
    private lateinit var spPassType: Spinner
    private lateinit var tvAdultCount: TextView
    private lateinit var tvChildCount: TextView
    private lateinit var tvPassCount: TextView
    private lateinit var tvLuggageCount: TextView
    private lateinit var tvTotalFare: TextView
    private lateinit var btnPrint: Button
    private lateinit var btnReset: Button
    private lateinit var btnReport: Button
    private lateinit var btnDayClose: Button

    private var adultCount = 1
    private var childCount = 0
    private var passCount = 0
    private var luggageCount = 0
    private var totalFare = 0.0

    private val prefsName = "manager_setup"
    private val reportPrefsName = "daily_report"
    private val pendingRouteSelectionKey = "pending_route_selection_after_day_close"
    private var selectedFromStop = ""
    private var selectedToStop = ""
    private var selectedPassType = "None"
    private var selectedPassIdType = ""
    private var selectedPassIdLast4 = ""
    private var allStops = listOf<String>()
    private var selectedParsedRoute: RouteDslParser.ParsedRoute? = null
    private val passTypes = listOf(
        "None",
        "Student Pass",
        "Senior Discount Pass",
        "Day Pass"
    )

    // Sample fare calculation (you can adjust this)
    private val baseFare = 10.0
    private val passDiscount = 0.8 // 80% of base fare
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockTicker = object : Runnable {
        override fun run() {
            updateDateTimeDisplay()
            clockHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_commute_ticket)
        routeDslParser = RouteDslParser(this)

        initializeViews()
        loadSetupData()
        setupPassengerCounters()
        setupStopSelectors()
        setupButtons()
        maybeForceRouteSelectionAfterDayClose()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_commute_ticket)
    }

    private fun initializeViews() {
        tvDivision = findViewById(R.id.tvDivision)
        tvDate = findViewById(R.id.tvDate)
        tvRouteNumber = findViewById(R.id.tvRouteNumber)
        tvRouteInfo = findViewById(R.id.tvRouteInfo)
        spFromStop = findViewById(R.id.spFromStop)
        spToStop = findViewById(R.id.spToStop)
        spPassType = findViewById(R.id.spPassType)
        tvAdultCount = findViewById(R.id.tvAdultCount)
        tvChildCount = findViewById(R.id.tvChildCount)
        tvLuggageCount = findViewById(R.id.tvLuggageCount)
        tvTotalFare = findViewById(R.id.tvTotalFare)
        btnPrint = findViewById(R.id.btnPrint)
        btnReset = findViewById(R.id.btnReset)
        btnReport = findViewById(R.id.btnReport)
        btnDayClose = findViewById(R.id.btnDayClose)

        setupPassTypeSpinner()
    }

    private fun setupPassTypeSpinner() {
        spPassType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, passTypes)
        spPassType.setSelection(0)
        spPassType.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val chosen = passTypes[position]
                if (chosen.startsWith("Day Pass") && !isCityBusService()) {
                    val dayPassValidityMessage = if (isCityAcBusService()) {
                        getString(R.string.day_pass_valid_city_ac)
                    } else {
                        getString(R.string.day_pass_valid_city_bus)
                    }
                    Toast.makeText(
                        this@CommuteTicketActivity,
                        dayPassValidityMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    spPassType.setSelection(0)
                    selectedPassType = "None"
                    selectedPassIdType = ""
                    selectedPassIdLast4 = ""
                    calculateFare()
                    return
                }
                selectedPassType = chosen
                if (selectedPassType == "None") {
                    selectedPassIdType = ""
                    selectedPassIdLast4 = ""
                }
                passCount = if (selectedPassType == "None") 0 else 1
                calculateFare()

                if (selectedPassType != "None") {
                    verifyPassIdentityThenProceed(
                        onVerified = {
                            Toast.makeText(this@CommuteTicketActivity, getString(R.string.id_verification_saved, selectedPassType), Toast.LENGTH_SHORT).show()
                        },
                        onCancelled = {
                            spPassType.setSelection(0)
                            selectedPassType = "None"
                            selectedPassIdType = ""
                            selectedPassIdLast4 = ""
                            passCount = 0
                            calculateFare()
                            Toast.makeText(this@CommuteTicketActivity, getString(R.string.pass_selection_cancelled), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No-op
            }
        })
    }

    private fun loadSetupData() {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val division = prefs.getString("division", "Not Configured") ?: "Not Configured"
        val route = prefs.getString("route", "Coming Soon") ?: "Coming Soon"
        val busType = prefs.getString("bus_type", "") ?: ""
        val routeNumber = prefs.getString("route_number", "") ?: ""

        tvDivision.text = getString(R.string.division_format, division)
        updateDateTimeDisplay()
        tvRouteNumber.text = getString(R.string.route_number_format, route, if (routeNumber.isNotBlank()) getString(R.string.route_number_suffix, routeNumber) else "", busType)

        selectedParsedRoute = routeDslParser.findRouteByLabelAnyBusType(route)
        allStops = selectedParsedRoute?.stops ?: getSampleStopsForRoute(route)
        
        // Auto-select first and second stops
        if (allStops.isNotEmpty()) {
            selectedFromStop = allStops[0]
            if (allStops.size > 1) {
                selectedToStop = allStops[1]
            }
            tvRouteInfo.text = getString(R.string.route_info_selected, selectedFromStop, selectedToStop)
        } else {
            tvRouteInfo.text = getString(R.string.route_info_no_stops)
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getCurrentTime(): String {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return timeFormat.format(Date())
    }

    private fun updateDateTimeDisplay() {
        tvDate.text = getString(R.string.date_time_format, getCurrentDate(), getCurrentTime())
    }

    private fun getSampleStopsForRoute(route: String): List<String> {
        // Sample stops - in a real app, this would come from database/API based on route
        return listOf(
            "Mandya Central",
            "Mandya Bus Stand",
            "Induvalu",
            "Madduru",
            "Koppa",
            "Srirangapattana",
            "Pandavapura",
            "Melukote",
            "K.R.Pete"
        )
    }

    private fun setupStopSelectors() {
        // From Stop Spinner - Pre-select first stop
        val fromAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, allStops)
        spFromStop.adapter = fromAdapter
        spFromStop.setSelection(0)  // Select first stop by default
        if (allStops.isNotEmpty()) {
            selectedFromStop = allStops[0]
        }

        spFromStop.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFromStop = allStops.getOrNull(position).orEmpty()
                updateToStopSpinner()
                calculateFare()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })

        // To Stop Spinner - Initially disabled, will be updated based on From Stop
        updateToStopSpinner()
        
        spToStop.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0) {
                    selectedToStop = parent?.getItemAtPosition(position)?.toString().orEmpty()
                    if (selectedToStop.isNotEmpty()) {
                        tvRouteInfo.text = getString(R.string.route_info_selected, selectedFromStop, selectedToStop)
                    } else {
                        tvRouteInfo.text = getString(R.string.route_info_selected, selectedFromStop, getString(R.string.commute_to_stop))
                    }
                    calculateFare()
                } else {
                    selectedToStop = ""
                    tvRouteInfo.text = getString(R.string.route_info_selected, selectedFromStop, getString(R.string.commute_to_stop))
                    calculateFare()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No-op
            }
        })
    }

    private fun updateToStopSpinner() {
        if (selectedFromStop.isEmpty()) {
            spToStop.isEnabled = false
            return
        }

        // Get index of selected From stop
        val fromStopIndex = allStops.indexOf(selectedFromStop)
        
        // Show only stops AFTER the selected From stop
        val availableToStops = if (fromStopIndex >= 0 && fromStopIndex < allStops.size - 1) {
            allStops.drop(fromStopIndex + 1)
        } else {
            emptyList()
        }

        if (availableToStops.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_stops_after, selectedFromStop), Toast.LENGTH_SHORT).show()
            spToStop.isEnabled = false
            selectedToStop = ""
            return
        }

        val toAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, availableToStops)
        spToStop.adapter = toAdapter
        spToStop.setSelection(0)  // Auto-select first available stop
        spToStop.isEnabled = true
        selectedToStop = availableToStops.firstOrNull().orEmpty()
        if (selectedToStop.isNotEmpty()) {
            tvRouteInfo.text = getString(R.string.route_info_selected, selectedFromStop, selectedToStop)
        } else {
            tvRouteInfo.text = getString(R.string.route_info_selected, selectedFromStop, getString(R.string.commute_to_stop))
        }
    }

    private fun setupPassengerCounters() {
        // Adult counters
        findViewById<Button>(R.id.btnAdultMinus).setOnClickListener {
            if (adultCount > 0) {
                adultCount--
                updateCounterUI()
                calculateFare()
            }
        }

        findViewById<Button>(R.id.btnAdultPlus).setOnClickListener {
            if (adultCount < MAX_ADULTS_PER_TICKET) {
                adultCount++
                updateCounterUI()
                calculateFare()
            } else {
                Toast.makeText(this, getString(R.string.max_adults_message, MAX_ADULTS_PER_TICKET), Toast.LENGTH_SHORT).show()
            }
        }

        // Child counters
        findViewById<Button>(R.id.btnChildMinus).setOnClickListener {
            if (childCount > 0) {
                childCount--
                updateCounterUI()
                calculateFare()
            }
        }

        findViewById<Button>(R.id.btnChildPlus).setOnClickListener {
            if (childCount < MAX_CHILDREN_PER_TICKET) {
                childCount++
                updateCounterUI()
                calculateFare()
            } else {
                Toast.makeText(this, getString(R.string.max_children_message, MAX_CHILDREN_PER_TICKET), Toast.LENGTH_SHORT).show()
            }
        }

        // Luggage counters
        findViewById<Button>(R.id.btnLuggageMinus).setOnClickListener {
            if (luggageCount > 0) {
                luggageCount--
                updateCounterUI()
                calculateFare()
            }
        }

        findViewById<Button>(R.id.btnLuggagePlus).setOnClickListener {
            luggageCount++
            updateCounterUI()
            calculateFare()
        }

        tvLuggageCount.setOnClickListener {
            showLuggageWeightInputDialog()
        }

        updateCounterUI()
    }

    private fun showLuggageWeightInputDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(3))
            hint = "Weight in kg"
            setText(luggageCount.toString())
            setSelection(text.length)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(input)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.enter_luggage_weight_title)
            .setMessage(R.string.enter_luggage_weight_message)
            .setView(container)
            .setPositiveButton("Apply", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val valueText = input.text?.toString()?.trim().orEmpty()
                val newWeight = valueText.toIntOrNull()

                if (newWeight == null || newWeight < 0) {
                    input.error = "Enter valid non-negative kg"
                    return@setOnClickListener
                }

                luggageCount = newWeight
                updateCounterUI()
                calculateFare()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateCounterUI() {
        tvAdultCount.text = getString(R.string.counter_adults, adultCount)
        tvChildCount.text = getString(R.string.counter_children, childCount)
        tvLuggageCount.text = getString(R.string.counter_luggage, luggageCount)
    }

    private fun calculateFare() {
        val childRate = getChildRateFactor()
        val segmentFare = getSegmentBaseFare()
        val effectiveBaseFare = segmentFare * getBusTypeFareMultiplier(segmentFare)
        val dayPassAmount = getDayPassAmount()
        var fare = 0.0

        fare += when (selectedPassType) {
            "Student Pass" -> 0.0
            "Senior Discount Pass" -> adultCount * (effectiveBaseFare * 0.70)
            "Day Pass" -> adultCount * dayPassAmount
            else -> {
                // Normal ticket fare
                adultCount * effectiveBaseFare + (childCount * (effectiveBaseFare * childRate))
            }
        }

        // Day Pass and Student Pass do not add luggage to ticket fare.
        if (selectedPassType != "Day Pass" && selectedPassType != "Student Pass") {
            val luggageFarePerKg = getLuggageFarePerKg(effectiveBaseFare)
            fare += luggageCount * luggageFarePerKg
        }

        totalFare = if (selectedPassType == "Day Pass" || (selectedFromStop.isNotEmpty() && selectedToStop.isNotEmpty())) {
            ceilFare(fare)
        } else {
            0.0
        }

        tvTotalFare.text = getString(R.string.commute_total_default).replace("0.00", String.format("%.2f", totalFare))
    }

    private fun setupButtons() {
        btnReset.setOnClickListener {
            resetForm()
        }

        btnPrint.setOnClickListener {
            printTicket()
        }

        btnReport.setOnClickListener {
            showReport()
        }

        btnDayClose.setOnClickListener {
            confirmDayClose()
        }
    }

    private fun confirmDayClose() {
        AlertDialog.Builder(this)
            .setTitle(R.string.day_close_title)
            .setMessage(R.string.day_close_message)
            .setPositiveButton("Yes") { _, _ ->
                closeTripAndSwitchRoute()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun closeTripAndSwitchRoute() {
        getSharedPreferences(prefsName, MODE_PRIVATE)
            .edit()
            .putBoolean(pendingRouteSelectionKey, true)
            .apply()
        saveTripSnapshot()
        resetTripCountersForNextRoute()
        showRouteSwitchDialog()
    }

    private fun maybeForceRouteSelectionAfterDayClose() {
        val setupPrefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val pendingRouteSelection = setupPrefs.getBoolean(pendingRouteSelectionKey, false)
        if (!pendingRouteSelection) {
            return
        }

        // Show route picker after initial UI bind so the user cannot continue on stale route.
        btnPrint.post {
            showRouteSwitchDialog(includeCurrentRoute = true, forceSelection = true)
        }
    }

    private fun saveTripSnapshot() {
        val setupPrefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val reportPrefs = getSharedPreferences(reportPrefsName, MODE_PRIVATE)

        val snapshot = JSONObject().apply {
            put("closed_at", getCurrentDateTime())
            put("date", reportPrefs.getString("date", getCurrentDate()) ?: getCurrentDate())
            put("division", setupPrefs.getString("division", "") ?: "")
            put("route", setupPrefs.getString("route", "") ?: "")
            put("route_number", setupPrefs.getString("route_number", "") ?: "")
            put("bus_type", setupPrefs.getString("bus_type", "") ?: "")
            put("bus_numbers", setupPrefs.getString("bus_numbers", "") ?: "")
            put("tickets", reportPrefs.getInt("tickets", 0))
            put("adults", reportPrefs.getInt("adults", 0))
            put("children", reportPrefs.getInt("children", 0))
            put("passes", reportPrefs.getInt("passes", 0))
            put("luggage", reportPrefs.getInt("luggage", 0))
            put("revenue", reportPrefs.getFloat("revenue", 0f).toDouble())
            put("ticket_history", JSONArray(reportPrefs.getString("ticket_history", "[]") ?: "[]"))
        }

        val snapshotsRaw = reportPrefs.getString("trip_reports", "[]") ?: "[]"
        val snapshots = JSONArray(snapshotsRaw)
        snapshots.put(snapshot)

        reportPrefs.edit().putString("trip_reports", snapshots.toString()).apply()
    }

    private fun resetTripCountersForNextRoute() {
        val reportPrefs = getSharedPreferences(reportPrefsName, MODE_PRIVATE)
        reportPrefs.edit()
            .putString("date", getCurrentDate())
            .putInt("tickets", 0)
            .putInt("adults", 0)
            .putInt("children", 0)
            .putInt("passes", 0)
            .putInt("luggage", 0)
            .putFloat("revenue", 0f)
            .putString("ticket_history", "[]")
            .apply()
    }

    private fun showRouteSwitchDialog(includeCurrentRoute: Boolean = false, forceSelection: Boolean = false) {
        val setupPrefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val division = setupPrefs.getString("division", "") ?: ""
        val currentRoute = setupPrefs.getString("route", "") ?: ""
        val routes = if (includeCurrentRoute) {
            routeDslParser.getRoutesByDivision(division)
        } else {
            routeDslParser.getRoutesByDivision(division).filter { it.displayLabel != currentRoute }
        }

        if (routes.isEmpty()) {
            setupPrefs.edit().putBoolean(pendingRouteSelectionKey, false).apply()
            Toast.makeText(this, getString(R.string.no_alternate_routes), Toast.LENGTH_LONG).show()
            return
        }

        val routeLabels = routes.map { it.displayLabel }.toTypedArray()
        var selectedIndex = 0

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.select_new_route)
            .setSingleChoiceItems(routeLabels, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Switch") { _, _ ->
                val selectedRoute = routes[selectedIndex]
                setupPrefs.edit()
                    .putString("route", selectedRoute.displayLabel)
                    .putString("route_number", selectedRoute.routeNumber)
                    .putString("route_source", selectedRoute.source)
                    .putString("route_destination", selectedRoute.destination)
                    .putString("bus_type", selectedRoute.busTypeCode)
                    .putBoolean(pendingRouteSelectionKey, false)
                    .apply()

                loadSetupData()
                setupStopSelectors()
                resetForm()
                Toast.makeText(this, getString(R.string.trip_closed_route_switched, selectedRoute.displayLabel), Toast.LENGTH_LONG).show()
            }

        if (!forceSelection) {
            dialog.setNegativeButton("Cancel", null)
        }

        dialog.setCancelable(!forceSelection).show()
    }

    private fun resetForm(preserveStops: Boolean = false) {
        adultCount = 1
        childCount = 0
        passCount = 0
        luggageCount = 0
        selectedPassType = "None"
        selectedPassIdType = ""
        selectedPassIdLast4 = ""

        val canPreserveStops = preserveStops &&
            selectedFromStop.isNotEmpty() &&
            selectedToStop.isNotEmpty() &&
            allStops.contains(selectedFromStop) &&
            allStops.contains(selectedToStop)

        if (canPreserveStops) {
            val preservedFromStop = selectedFromStop
            val preservedToStop = selectedToStop
            val fromIndex = allStops.indexOf(preservedFromStop)
            if (fromIndex >= 0) {
                spFromStop.setSelection(fromIndex)
                updateToStopSpinner()
                val availableToStops = allStops.drop(fromIndex + 1)
                val toIndex = availableToStops.indexOf(preservedToStop)
                if (toIndex >= 0) {
                    spToStop.setSelection(toIndex)
                    selectedFromStop = preservedFromStop
                    selectedToStop = preservedToStop
                }
            }
        } else if (allStops.isNotEmpty()) {
            // Reset to first and second stops by default
            selectedFromStop = allStops[0]
            spFromStop.setSelection(0)
            
            updateToStopSpinner()  // Will auto-select second stop
            
            if (allStops.size > 1) {
                selectedToStop = allStops[1]
                spToStop.setSelection(0)  // First item in filtered list is second stop
            }
        } else {
            selectedFromStop = ""
            selectedToStop = ""
            spFromStop.setSelection(0)
            spToStop.setSelection(0)
        }

        spPassType.setSelection(0)

        updateCounterUI()
        calculateFare()
        if (selectedFromStop.isNotEmpty() && selectedToStop.isNotEmpty()) {
            tvRouteInfo.text = getString(R.string.route_info_selected, selectedFromStop, selectedToStop)
        } else {
            tvRouteInfo.text = getString(R.string.commute_from_to_select)
        }

        val resetMessage = if (canPreserveStops) {
            "Ticket printed. From/To retained for next ticket."
        } else {
            "Form reset"
        }
        Toast.makeText(this, resetMessage, Toast.LENGTH_SHORT).show()
    }

    private fun printTicket() {
        // Validation
        if (selectedPassType != "Day Pass" && (selectedFromStop.isEmpty() || selectedToStop.isEmpty())) {
            Toast.makeText(this, getString(R.string.select_both_stops), Toast.LENGTH_LONG).show()
            return
        }

        val hasPassengers = (adultCount + childCount) > 0
        val hasLuggage = luggageCount > 0

        // Luggage and passenger journeys must be issued as separate tickets.
        if (hasPassengers && hasLuggage) {
            Toast.makeText(
                this,
                "Passengers and luggage must be printed as separate tickets.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!hasPassengers && !hasLuggage) {
            Toast.makeText(this, getString(R.string.select_passengers_or_luggage), Toast.LENGTH_LONG).show()
            return
        }

        if (!hasPassengers && selectedPassType != "None") {
            Toast.makeText(this, getString(R.string.pass_not_applicable_luggage), Toast.LENGTH_LONG).show()
            return
        }

        if (adultCount > MAX_ADULTS_PER_TICKET || childCount > MAX_CHILDREN_PER_TICKET) {
            Toast.makeText(
                this,
                "Limit is $MAX_ADULTS_PER_TICKET adults and $MAX_CHILDREN_PER_TICKET children per ticket",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (selectedPassType == "Day Pass" && !isCityBusService()) {
            val dayPassValidityMessage = if (isCityAcBusService()) {
                "Day Pass is valid in City AC Bus services only."
            } else {
                "Day Pass can be used only for city bus class services."
            }
            Toast.makeText(this, dayPassValidityMessage, Toast.LENGTH_LONG).show()
            return
        }

        if (selectedPassType == "Day Pass" && childCount > 0) {
            Toast.makeText(this, getString(R.string.day_pass_adults_only), Toast.LENGTH_LONG).show()
            return
        }

        if (selectedPassType == "Senior Discount Pass" && childCount > 0) {
            Toast.makeText(this, getString(R.string.senior_ticket_child_zero), Toast.LENGTH_LONG).show()
            return
        }

        if (selectedPassType == "Student Pass" && childCount > 0) {
            Toast.makeText(this, getString(R.string.student_pass_adult_count), Toast.LENGTH_LONG).show()
            return
        }

        if (selectedPassType == "Student Pass" && luggageCount > 0) {
            Toast.makeText(this, getString(R.string.student_pass_no_luggage), Toast.LENGTH_LONG).show()
            return
        }

        if (!isPassIdentityCaptured()) {
            Toast.makeText(this, getString(R.string.complete_id_verification), Toast.LENGTH_LONG).show()
            return
        }

        // Keep manager-level confirmation and global settings confirmation in sync.
        val managerPrefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val managerConfirmBeforePrint = managerPrefs.getBoolean("confirm_before_print", true)
        val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val settingsConfirmBeforePrint = settingsPrefs.getBoolean("confirm_printing", true)
        val confirmBeforePrint = managerConfirmBeforePrint || settingsConfirmBeforePrint

        if (confirmBeforePrint) {
            showConfirmDialog()
        } else {
            executeTicketPrint()
        }
    }

    private fun verifyPassIdentityThenProceed(onVerified: () -> Unit, onCancelled: () -> Unit = {}) {
        when (selectedPassType) {
            "Student Pass" -> showLast4DigitsDialog(
                title = "Student Pass Verification",
                message = "Enter last 4 digits of student pass number",
                idType = "Student Pass",
                onVerified = onVerified,
                onCancelled = onCancelled
            )

            "Senior Discount Pass" -> showLast4DigitsDialog(
                title = "Senior Pass Verification",
                message = "Enter last 4 digits of senior pass number",
                idType = "Senior Pass",
                onVerified = onVerified,
                onCancelled = onCancelled
            )

            "Day Pass" -> showDayPassIdDialog(onVerified = onVerified, onCancelled = onCancelled)

            else -> {
                selectedPassIdType = ""
                selectedPassIdLast4 = ""
                onVerified()
            }
        }
    }

    private fun showLast4DigitsDialog(
        title: String,
        message: String,
        idType: String,
        onVerified: () -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(4))
            hint = "Last 4 digits"
            setText(selectedPassIdLast4)
            setSelection(text.length)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(input)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setView(container)
            .setPositiveButton("Continue", null)
            .setNegativeButton("Cancel") { _, _ -> onCancelled() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val last4 = input.text?.toString()?.trim().orEmpty()
                if (!last4.matches(Regex("\\d{4}"))) {
                    input.error = "Enter exactly 4 digits"
                    return@setOnClickListener
                }
                selectedPassIdType = idType
                selectedPassIdLast4 = last4
                dialog.dismiss()
                onVerified()
            }
        }

        dialog.show()
    }

    private fun showDayPassIdDialog(onVerified: () -> Unit, onCancelled: () -> Unit = {}) {
        val idTypes = listOf("Aadhaar", "Voter ID", "PAN", "Driving License", "Other")
        val idTypeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@CommuteTicketActivity,
                android.R.layout.simple_spinner_dropdown_item,
                idTypes
            )
            val previousIndex = idTypes.indexOf(selectedPassIdType)
            if (previousIndex >= 0) setSelection(previousIndex)
        }

        val last4Input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(4))
            hint = "Last 4 digits of selected ID"
            setText(selectedPassIdLast4)
            setSelection(text.length)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(idTypeSpinner)
            addView(last4Input)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.day_pass_id_verification)
            .setMessage(R.string.select_id_type_message)
            .setView(container)
            .setPositiveButton("Continue", null)
            .setNegativeButton("Cancel") { _, _ -> onCancelled() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val selectedType = idTypeSpinner.selectedItem?.toString().orEmpty()
                val last4 = last4Input.text?.toString()?.trim().orEmpty()
                if (selectedType.isBlank()) {
                    Toast.makeText(this, getString(R.string.select_id_type), Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (!last4.matches(Regex("\\d{4}"))) {
                    last4Input.error = "Enter exactly 4 digits"
                    return@setOnClickListener
                }
                selectedPassIdType = selectedType
                selectedPassIdLast4 = last4
                dialog.dismiss()
                onVerified()
            }
        }

        dialog.show()
    }

    private fun showConfirmDialog() {
        val fromLabel = if (selectedPassType == "Day Pass") "DIVISION-WIDE" else selectedFromStop
        val toLabel = if (selectedPassType == "Day Pass") "UNLIMITED TRAVEL (1 DAY)" else selectedToStop
        val passengerLabel = if (selectedPassType == "Student Pass") "Students" else "Adults"
        val idLine = if (selectedPassType != "None" && selectedPassIdLast4.isNotBlank()) {
            "ID: ${if (selectedPassIdType.isNotBlank()) selectedPassIdType else "Pass"} ****$selectedPassIdLast4"
        } else {
            "ID: Not provided"
        }
        val message = """
            Confirm Ticket Details:
            
            From: $fromLabel
            To: $toLabel
            $passengerLabel: $adultCount
            Children: $childCount
            Pass Type: ${if (selectedPassType == "None") "Normal Ticket" else selectedPassType}
            $idLine
            Luggage: $luggageCount
            
            Total Fare: ₹${String.format("%.2f", totalFare)}
            
            Proceed with printing?
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.confirm_ticket)
            .setMessage(message)
            .setPositiveButton("Print") { _, _ ->
                executeTicketPrint()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeTicketPrint() {
        if (!isPassIdentityCaptured()) {
            Toast.makeText(this, getString(R.string.enter_pass_id_details), Toast.LENGTH_LONG).show()
            return
        }

        if (selectedPassType == "Student Pass") {
            executeStudentPassEntry()
            return
        }

        try {
            val ticketNo = getNextTicketNumber()
            val rrn = generateRrn()
            val setupPrefs = getSharedPreferences(prefsName, MODE_PRIVATE)
            val route = setupPrefs.getString("route", "") ?: ""
            val routeNumber = setupPrefs.getString("route_number", "") ?: ""
            val division = setupPrefs.getString("division", "") ?: ""
            val busNumber = setupPrefs.getString("bus_numbers", "")?.split(",")?.firstOrNull()?.trim().orEmpty()
            val busType = getConfiguredBusType()
            val dateTime = getCurrentDateTime()
            val segmentFare = getSegmentBaseFare()
            val effectiveBaseFare = segmentFare * getBusTypeFareMultiplier(segmentFare)
            val dayPassAmount = getDayPassAmount()

            val childRate = getChildRateFactor()
            val adultFareTotal = ceilFare(
                when (selectedPassType) {
                    "Senior Discount Pass" -> adultCount * (effectiveBaseFare * 0.70)
                    "Day Pass" -> adultCount * dayPassAmount
                    "Student Pass" -> 0.0
                    else -> adultCount * effectiveBaseFare
                }
            )
            val childFareTotal = if (selectedPassType == "None") ceilFare(childCount * (effectiveBaseFare * childRate)) else 0.0
            val adultFarePerPassenger = ceilFare(effectiveBaseFare)
            val childFarePerPassenger = ceilFare(effectiveBaseFare * childRate)
            val seniorFareTotal = if (selectedPassType == "Senior Discount Pass") adultFareTotal else 0.0
            val passFareTotal = if (selectedPassType == "Senior Discount Pass") seniorFareTotal else 0.0
            val luggageFarePerKg = getLuggageFarePerKg(effectiveBaseFare)
            val luggageFareTotal = ceilFare(luggageCount * luggageFarePerKg)
            val dayPassValidityText = if (isCityAcBusService()) {
                "Valid in City AC Bus services only"
            } else {
                "Valid in City Bus services only"
            }

            val routeDisplayValue = when {
                routeNumber.isNotBlank() -> routeNumber
                route.isNotBlank() -> route
                else -> "-"
            }

            // Generate ticket details
            val ticketDetails = mapOf(
                "division" to division,
                "date" to getCurrentDate(),
                "route" to routeDisplayValue,
                "ticket_no" to ticketNo,
                "from" to selectedFromStop,
                "to" to selectedToStop,
                "adults" to adultCount.toString(),
                "children" to childCount.toString(),
                "passes" to passCount.toString(),
                "pass_type" to selectedPassType,
                "luggage" to luggageCount.toString(),
                "total_fare" to String.format("%.2f", totalFare),
                "date_time" to dateTime
            )

            val ticketBitmap = if (selectedPassType == "Senior Discount Pass") {
                ticketFormatter.createSeniorTicketBitmap(
                    ticketNo = ticketNo,
                    seniorCount = adultCount,
                    fromStopEN = selectedFromStop,
                    fromStopKA = selectedFromStop,
                    toStopEN = selectedToStop,
                    toStopKA = selectedToStop,
                    seniorFareTotal = seniorFareTotal,
                    totalFare = totalFare,
                    rrn = rrn,
                    division = division,
                    routeNumber = routeDisplayValue,
                    busType = busType,
                    busNumber = busNumber,
                    printDateTime = dateTime
                )
            } else if (selectedPassType == "Day Pass") {
                ticketFormatter.createDayPassTicketBitmap(
                    ticketNo = ticketNo,
                    dayPassCount = adultCount,
                    dayPassAmount = dayPassAmount,
                    totalFare = totalFare,
                    rrn = rrn,
                    division = division,
                    routeNumber = routeDisplayValue,
                    busType = busType,
                    busNumber = busNumber,
                    validityText = dayPassValidityText,
                    printDateTime = dateTime
                )
            } else {
                ticketFormatter.createTicketBitmap(
                    ticketNo = ticketNo,
                    fromStopEN = selectedFromStop,
                    fromStopKA = selectedFromStop,
                    toStopEN = selectedToStop,
                    toStopKA = selectedToStop,
                    adults = adultCount,
                    childs = childCount,
                    totalFare = totalFare,
                    rrn = rrn,
                    division = division,
                    routeNumber = routeDisplayValue,
                    busType = busType,
                    busNumber = busNumber,
                    adultFareTotal = adultFareTotal,
                    childFareTotal = childFareTotal,
                    adultFarePerPassenger = adultFarePerPassenger,
                    childFarePerPassenger = childFarePerPassenger,
                    passType = selectedPassType,
                    passFareTotal = passFareTotal,
                    luggageFareTotal = luggageFareTotal,
                    printDateTime = dateTime
                )
            }

            val printSent = printTicketBitmap(ticketBitmap)
            if (!printSent) {
                return
            }

            updateDailyReportTotals()
            appendTicketHistory()

            val message = if (isIgnoreBluetoothErrorsEnabled()) {
                "Ticket processed (Bluetooth errors ignored). Fare: ₹${String.format("%.2f", totalFare)}"
            } else {
                "Ticket printed. Fare: ₹${String.format("%.2f", totalFare)}"
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()

            resetForm(preserveStops = true)
        } catch (ex: Exception) {
            if (isIgnoreBluetoothErrorsEnabled()) {
                updateDailyReportTotals()
                appendTicketHistory()
                Toast.makeText(
                    this,
                    "Ticket processed (Bluetooth error ignored: ${ex.message ?: "Unknown error"})",
                    Toast.LENGTH_LONG
                ).show()
                resetForm(preserveStops = true)
            } else {
                Toast.makeText(this, "Ticket print failed: ${ex.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun printTicketBitmap(bitmap: android.graphics.Bitmap): Boolean {
        val ignoreBluetoothErrors = isIgnoreBluetoothErrorsEnabled()
        val pairedDevices = bluetoothManager.getPairedDevices()
        if (pairedDevices.isEmpty()) {
            if (ignoreBluetoothErrors) {
                return true
            }
            Toast.makeText(this, getString(R.string.no_bluetooth_printer_ticket), Toast.LENGTH_LONG).show()
            return false
        }

        val printer = pairedDevices.first()
        val connected = bluetoothManager.connectToPrinter(printer)
        if (!connected) {
            if (ignoreBluetoothErrors) {
                return true
            }
            Toast.makeText(this, getString(R.string.connect_bluetooth_printer_failed), Toast.LENGTH_LONG).show()
            return false
        }

        return try {
            val printBytes = ticketFormatter.decodeBitmapToByteArray(bitmap)
            val written = bluetoothManager.printData(printBytes)
            if (!written && !ignoreBluetoothErrors) {
                Toast.makeText(this, getString(R.string.send_ticket_data_failed), Toast.LENGTH_LONG).show()
            }
            written || ignoreBluetoothErrors
        } finally {
            bluetoothManager.disconnect()
        }
    }

    private fun isIgnoreBluetoothErrorsEnabled(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return prefs.getBoolean("ignore_bluetooth_error", false) || prefs.getBoolean("ignore_print_errors", false)
    }

    private fun executeStudentPassEntry() {
        try {
            if (!isPassIdentityCaptured()) {
                Toast.makeText(this, getString(R.string.enter_student_pass_last4), Toast.LENGTH_LONG).show()
                return
            }
            // Sequence is still advanced for audit trail even when no paper ticket is printed.
            getNextTicketNumber()
            updateDailyReportTotals()
            appendTicketHistory()
            Toast.makeText(this, getString(R.string.student_pass_recorded, selectedPassIdLast4), Toast.LENGTH_LONG).show()
            resetForm(preserveStops = true)
        } catch (ex: Exception) {
            Toast.makeText(this, "Failed to record student pass entry: ${ex.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getChildRateFactor(): Double {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val childRateText = prefs.getString("child_ticket_rate", "60") ?: "60"
        val childRatePercent = childRateText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 60.0
        return childRatePercent / 100.0
    }

    private fun ceilFare(value: Double): Double {
        return if (value <= 0.0) 0.0 else ceil(value)
    }

    private fun isPassIdentityCaptured(): Boolean {
        return when (selectedPassType) {
            "Student Pass", "Senior Discount Pass" -> selectedPassIdLast4.matches(Regex("\\d{4}"))
            "Day Pass" -> selectedPassIdType.isNotBlank() && selectedPassIdLast4.matches(Regex("\\d{4}"))
            else -> true
        }
    }

    private fun isCityBusService(): Boolean {
        val busType = getConfiguredBusType()
        val normalizedBusType = busType.uppercase().replace("_", "").replace("-", "").replace(" ", "")
        return normalizedBusType.contains("CITY")
    }

    private fun isCityAcBusService(): Boolean {
        val busType = getConfiguredBusType()
        val normalizedBusType = busType.uppercase().replace("_", "").replace("-", "").replace(" ", "")
        return normalizedBusType.contains("CITY") && normalizedBusType.contains("AC")
    }

    private fun getSegmentBaseFare(): Double {
        if (selectedFromStop.isBlank() || selectedToStop.isBlank()) {
            return baseFare
        }
        return selectedParsedRoute?.fareBetween(selectedFromStop, selectedToStop) ?: baseFare
    }

    private fun getConfiguredBusType(): String {
        val setupPrefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        return setupPrefs.getString("bus_type", "Ordinary Bus") ?: "Ordinary Bus"
    }

    private fun getBusTypeFareMultiplier(segmentFare: Double): Double {
        val normalizedBusType = getConfiguredBusType()
            .uppercase()
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "")

        return when {
            normalizedBusType.contains("CITY") && normalizedBusType.contains("AC") -> 2.5
            normalizedBusType.contains("NONSTOP") && normalizedBusType.contains("AC") -> 2.5
            normalizedBusType.contains("SLEEPER") && normalizedBusType.contains("AC") -> 2.5
            else -> 1.0
        }
    }

    private fun getDayPassAmount(): Double {
        val baseDayPassAmount = 70.0
        return if (isCityAcBusService()) {
            ceilFare(baseDayPassAmount * 2.5)
        } else {
            baseDayPassAmount
        }
    }

    private fun getLuggageFarePerKg(segmentFareWithBusType: Double): Double {
        val calculatedPerKg = segmentFareWithBusType * 0.10
        return maxOf(5.0, calculatedPerKg)
    }

    private fun appendTicketHistory() {
        val setupPrefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val reportPrefs = getSharedPreferences(reportPrefsName, MODE_PRIVATE)
        val rawHistory = reportPrefs.getString("ticket_history", "[]") ?: "[]"
        val history = JSONArray(rawHistory)

        val historyAdults = if (selectedPassType == "Student Pass") 0 else adultCount
        val historyPasses = if (selectedPassType == "Student Pass") adultCount else passCount

        val entry = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("date", getCurrentDate())
            put("date_time", getCurrentDateTime())
            put("ticket_no", getNextTicketNumber(peekOnly = true))
            put("division", setupPrefs.getString("division", "") ?: "")
            put("route", setupPrefs.getString("route", "") ?: "")
            put("from", selectedFromStop)
            put("to", selectedToStop)
            put("adults", historyAdults)
            put("children", childCount)
            put("passes", historyPasses)
            put("pass_type", selectedPassType)
            put("id_type", selectedPassIdType)
            put("id_last4", selectedPassIdLast4)
            put("luggage", luggageCount)
            put("fare", totalFare)
        }

        history.put(entry)

        // Keep storage bounded while preserving most recent records.
        val maxEntries = 2000
        val boundedHistory = if (history.length() > maxEntries) {
            val trimmed = JSONArray()
            val start = history.length() - maxEntries
            for (i in start until history.length()) {
                trimmed.put(history.getJSONObject(i))
            }
            trimmed
        } else {
            history
        }

        reportPrefs.edit()
            .putString("ticket_history", boundedHistory.toString())
            .apply()
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getNextTicketNumber(peekOnly: Boolean = false): String {
        val reportPrefs = getSharedPreferences(reportPrefsName, MODE_PRIVATE)
        val current = reportPrefs.getLong("ticket_sequence", 255228L)
        val nextValue = if (peekOnly) current else current + 1
        if (!peekOnly) {
            reportPrefs.edit().putLong("ticket_sequence", nextValue).apply()
        }
        return String.format(Locale.getDefault(), "%07d", nextValue)
    }

    private fun generateRrn(): String {
        val base = System.currentTimeMillis().toString()
        return base.takeLast(11).padStart(11, '0')
    }

    private fun updateDailyReportTotals() {
        val reportPrefs = getSharedPreferences(reportPrefsName, MODE_PRIVATE)
        val reportDate = getCurrentDate()
        val savedDate = reportPrefs.getString("date", "") ?: ""

        // Reset accumulated counters when day changes.
        if (savedDate != reportDate) {
            reportPrefs.edit()
                .putString("date", reportDate)
                .putInt("tickets", 0)
                .putInt("adults", 0)
                .putInt("children", 0)
                .putInt("passes", 0)
                .putInt("luggage", 0)
                .putFloat("revenue", 0f)
                .apply()
        }

        val currentTickets = reportPrefs.getInt("tickets", 0)
        val currentAdults = reportPrefs.getInt("adults", 0)
        val currentChildren = reportPrefs.getInt("children", 0)
        val currentPasses = reportPrefs.getInt("passes", 0)
        val currentLuggage = reportPrefs.getInt("luggage", 0)
        val currentRevenue = reportPrefs.getFloat("revenue", 0f)

        val adultsToAdd = if (selectedPassType == "Student Pass") 0 else adultCount
        val passesToAdd = if (selectedPassType == "Student Pass") adultCount else passCount

        reportPrefs.edit()
            .putString("date", reportDate)
            .putInt("tickets", currentTickets + 1)
            .putInt("adults", currentAdults + adultsToAdd)
            .putInt("children", currentChildren + childCount)
            .putInt("passes", currentPasses + passesToAdd)
            .putInt("luggage", currentLuggage + luggageCount)
            .putFloat("revenue", currentRevenue + totalFare.toFloat())
            .apply()
    }

    private fun showReport() {
        startActivity(Intent(this, DailyReportActivity::class.java))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onStart() {
        super.onStart()
        clockTicker.run()
    }

    override fun onStop() {
        super.onStop()
        clockHandler.removeCallbacks(clockTicker)
    }
}
