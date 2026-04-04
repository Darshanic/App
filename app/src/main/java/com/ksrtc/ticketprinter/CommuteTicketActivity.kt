package com.sktc.ticketprinter

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommuteTicketActivity : AppCompatActivity() {

    private lateinit var tvDivision: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvRouteNumber: TextView
    private lateinit var tvRouteInfo: TextView
    private lateinit var spFromStop: Spinner
    private lateinit var spToStop: Spinner
    private lateinit var tvAdultCount: TextView
    private lateinit var tvChildCount: TextView
    private lateinit var tvPassCount: TextView
    private lateinit var tvLuggageCount: TextView
    private lateinit var tvTotalFare: TextView
    private lateinit var btnPrint: Button
    private lateinit var btnReset: Button
    private lateinit var btnReport: Button

    private var adultCount = 1
    private var childCount = 0
    private var passCount = 0
    private var luggageCount = 0
    private var totalFare = 0.0

    private val prefsName = "manager_setup"
    private var selectedFromStop = ""
    private var selectedToStop = ""
    private var allStops = listOf<String>()

    // Sample fare calculation (you can adjust this)
    private val baseFare = 10.0
    private val childDiscount = 0.5 // 50% of base fare
    private val passDiscount = 0.8 // 80% of base fare
    private val luggageFare = 5.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_commute_ticket)

        initializeViews()
        loadSetupData()
        setupPassengerCounters()
        setupStopSelectors()
        setupButtons()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Commute Ticket"
    }

    private fun initializeViews() {
        tvDivision = findViewById(R.id.tvDivision)
        tvDate = findViewById(R.id.tvDate)
        tvRouteNumber = findViewById(R.id.tvRouteNumber)
        tvRouteInfo = findViewById(R.id.tvRouteInfo)
        spFromStop = findViewById(R.id.spFromStop)
        spToStop = findViewById(R.id.spToStop)
        tvAdultCount = findViewById(R.id.tvAdultCount)
        tvChildCount = findViewById(R.id.tvChildCount)
        tvPassCount = findViewById(R.id.tvPassCount)
        tvLuggageCount = findViewById(R.id.tvLuggageCount)
        tvTotalFare = findViewById(R.id.tvTotalFare)
        btnPrint = findViewById(R.id.btnPrint)
        btnReset = findViewById(R.id.btnReset)
        btnReport = findViewById(R.id.btnReport)
    }

    private fun loadSetupData() {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val division = prefs.getString("division", "Not Configured") ?: "Not Configured"
        val route = prefs.getString("route", "Coming Soon") ?: "Coming Soon"

        tvDivision.text = "Division: $division"
        tvDate.text = "Date: ${getCurrentDate()}"
        tvRouteNumber.text = "Route: $route"
        tvRouteInfo.text = "From – To: (Select stops below)"

        // Load sample stops for the selected route
        // In a real app, these would come from a database or API
        allStops = getSampleStopsForRoute(route)
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return dateFormat.format(Date())
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
        // From Stop Spinner
        val fromAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Select From Stop") + allStops)
        spFromStop.adapter = fromAdapter
        spFromStop.setSelection(0)

        spFromStop.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    selectedFromStop = allStops[position - 1]
                    updateToStopSpinner()
                    calculateFare()
                } else {
                    selectedFromStop = ""
                    spToStop.setSelection(0)
                    tvRouteInfo.text = "From – To: (Select stops below)"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })

        // To Stop Spinner (will be updated based on From Stop)
        spToStop.isEnabled = false
    }

    private fun updateToStopSpinner() {
        if (selectedFromStop.isEmpty()) {
            spToStop.setSelection(0)
            spToStop.isEnabled = false
            return
        }

        // Get stops after the selected "From" stop
        val fromIndex = allStops.indexOf(selectedFromStop)
        val availableToStops = allStops.drop(fromIndex + 1)

        if (availableToStops.isEmpty()) {
            Toast.makeText(this, "No stops available after $selectedFromStop", Toast.LENGTH_SHORT).show()
            spToStop.isEnabled = false
            return
        }

        val toAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Select To Stop") + availableToStops)
        spToStop.adapter = toAdapter
        spToStop.setSelection(0)
        spToStop.isEnabled = true
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
            adultCount++
            updateCounterUI()
            calculateFare()
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
            childCount++
            updateCounterUI()
            calculateFare()
        }

        // Pass counters
        findViewById<Button>(R.id.btnPassMinus).setOnClickListener {
            if (passCount > 0) {
                passCount--
                updateCounterUI()
                calculateFare()
            }
        }

        findViewById<Button>(R.id.btnPassPlus).setOnClickListener {
            passCount++
            updateCounterUI()
            calculateFare()
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

        updateCounterUI()
    }

    private fun updateCounterUI() {
        tvAdultCount.text = "Adults: $adultCount"
        tvChildCount.text = "Children: $childCount"
        tvPassCount.text = "Passes: $passCount"
        tvLuggageCount.text = "Luggage: $luggageCount"
    }

    private fun calculateFare() {
        var fare = 0.0

        // Adult fare
        fare += adultCount * baseFare

        // Child fare (50% discount)
        fare += childCount * (baseFare * childDiscount)

        // Pass fare (80% of base fare)
        fare += passCount * (baseFare * passDiscount)

        // Luggage fare
        fare += luggageCount * luggageFare

        totalFare = if (selectedFromStop.isNotEmpty() && selectedToStop.isNotEmpty()) {
            fare
        } else {
            0.0
        }

        tvTotalFare.text = "TOT: ₹${String.format("%.2f", totalFare)}"
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
    }

    private fun resetForm() {
        adultCount = 1
        childCount = 0
        passCount = 0
        luggageCount = 0
        selectedFromStop = ""
        selectedToStop = ""

        spFromStop.setSelection(0)
        spToStop.setSelection(0)
        spToStop.isEnabled = false

        updateCounterUI()
        calculateFare()
        tvRouteInfo.text = "From – To: (Select stops below)"

        Toast.makeText(this, "Form reset", Toast.LENGTH_SHORT).show()
    }

    private fun printTicket() {
        // Validation
        if (selectedFromStop.isEmpty() || selectedToStop.isEmpty()) {
            Toast.makeText(this, "Please select both From and To stops", Toast.LENGTH_LONG).show()
            return
        }

        if (adultCount + childCount + passCount == 0) {
            Toast.makeText(this, "Please select at least one passenger type", Toast.LENGTH_LONG).show()
            return
        }

        // Check if confirm before print is enabled
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val confirmBeforePrint = prefs.getBoolean("confirm_before_print", true)

        if (confirmBeforePrint) {
            showConfirmDialog()
        } else {
            executeTicketPrint()
        }
    }

    private fun showConfirmDialog() {
        val message = """
            Confirm Ticket Details:
            
            From: $selectedFromStop
            To: $selectedToStop
            Adults: $adultCount
            Children: $childCount
            Passes: $passCount
            Luggage: $luggageCount
            
            Total Fare: ₹${String.format("%.2f", totalFare)}
            
            Proceed with printing?
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirm Ticket")
            .setMessage(message)
            .setPositiveButton("Print") { _, _ ->
                executeTicketPrint()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeTicketPrint() {
        // Generate ticket details
        val ticketDetails = mapOf(
            "division" to (getSharedPreferences(prefsName, MODE_PRIVATE).getString("division", "") ?: ""),
            "date" to getCurrentDate(),
            "route" to (getSharedPreferences(prefsName, MODE_PRIVATE).getString("route", "") ?: ""),
            "from" to selectedFromStop,
            "to" to selectedToStop,
            "adults" to adultCount.toString(),
            "children" to childCount.toString(),
            "passes" to passCount.toString(),
            "luggage" to luggageCount.toString(),
            "total_fare" to String.format("%.2f", totalFare),
            "date_time" to getCurrentDateTime()
        )

        // In a real implementation, you would:
        // 1. Send this to the printer via BluetoothPrinterManager
        // 2. Format the ticket using TicketFormatter
        // 3. Handle print success/failure

        Toast.makeText(
            this,
            "Ticket Printed!\nFare: ₹${String.format("%.2f", totalFare)}",
            Toast.LENGTH_LONG
        ).show()

        // Reset after successful print
        resetForm()
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun showReport() {
        val division = getSharedPreferences(prefsName, MODE_PRIVATE).getString("division", "") ?: ""
        val busNumbers = getSharedPreferences(prefsName, MODE_PRIVATE).getString("bus_numbers", "") ?: ""

        val reportMessage = """
            TRIP REPORT
            
            Division: $division
            Bus Numbers: $busNumbers
            Date: ${getCurrentDate()}
            
            This is a placeholder for detailed trip report.
            In production, this would show:
            - Total tickets issued
            - Total passengers
            - Total revenue
            - Trip duration
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Trip Report")
            .setMessage(reportMessage)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
