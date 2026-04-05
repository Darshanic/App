package com.sktc.ticketprinter

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyReportActivity : AppCompatActivity() {

    private val reportPrefsName = "daily_report"
    private val setupPrefsName = "manager_setup"
    private val bluetoothManager = BluetoothPrinterManager()

    private lateinit var tvReportDate: TextView
    private lateinit var tvDivision: TextView
    private lateinit var tvBusNumbers: TextView
    private lateinit var tvTickets: TextView
    private lateinit var tvAdults: TextView
    private lateinit var tvChildren: TextView
    private lateinit var tvPasses: TextView
    private lateinit var tvLuggage: TextView
    private lateinit var tvTotalPassengers: TextView
    private lateinit var tvRevenue: TextView
    private lateinit var btnPrintCurrentReport: Button
    private lateinit var tvClosedReportsTitle: TextView
    private lateinit var tvClosedReportsEmpty: TextView
    private lateinit var llClosedReports: LinearLayout
    private lateinit var tvHistoryTitle: TextView
    private lateinit var tvHistoryEmpty: TextView
    private lateinit var llTicketHistory: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_report)

        tvReportDate = findViewById(R.id.tvReportDate)
        tvDivision = findViewById(R.id.tvDivision)
        tvBusNumbers = findViewById(R.id.tvBusNumbers)
        tvTickets = findViewById(R.id.tvTickets)
        tvAdults = findViewById(R.id.tvAdults)
        tvChildren = findViewById(R.id.tvChildren)
        tvPasses = findViewById(R.id.tvPasses)
        tvLuggage = findViewById(R.id.tvLuggage)
        tvTotalPassengers = findViewById(R.id.tvTotalPassengers)
        tvRevenue = findViewById(R.id.tvRevenue)
        btnPrintCurrentReport = findViewById(R.id.btnPrintCurrentReport)
        tvClosedReportsTitle = findViewById(R.id.tvClosedReportsTitle)
        tvClosedReportsEmpty = findViewById(R.id.tvClosedReportsEmpty)
        llClosedReports = findViewById(R.id.llClosedReports)
        tvHistoryTitle = findViewById(R.id.tvHistoryTitle)
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty)
        llTicketHistory = findViewById(R.id.llTicketHistory)

        btnPrintCurrentReport.setOnClickListener {
            confirmAndPrintCurrentReport()
        }

        bindReport()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Daily Report"
    }

    override fun onResume() {
        super.onResume()
        bindReport()
    }

    private fun bindReport() {
        val today = getCurrentDate()
        val setupPrefs = getSharedPreferences(setupPrefsName, MODE_PRIVATE)
        val reportPrefs = getSharedPreferences(reportPrefsName, MODE_PRIVATE)

        val reportDate = reportPrefs.getString("date", "") ?: ""
        val hasTodayData = reportDate == today

        val tickets = if (hasTodayData) reportPrefs.getInt("tickets", 0) else 0
        val adults = if (hasTodayData) reportPrefs.getInt("adults", 0) else 0
        val children = if (hasTodayData) reportPrefs.getInt("children", 0) else 0
        val passes = if (hasTodayData) reportPrefs.getInt("passes", 0) else 0
        val luggage = if (hasTodayData) reportPrefs.getInt("luggage", 0) else 0
        val revenue = if (hasTodayData) reportPrefs.getFloat("revenue", 0f) else 0f

        tvReportDate.text = "Report Date: $today"
        tvDivision.text = "Division: ${setupPrefs.getString("division", "Not Configured") ?: "Not Configured"}"
        tvBusNumbers.text = "Bus Numbers: ${setupPrefs.getString("bus_numbers", "Not Configured") ?: "Not Configured"}"
        tvTickets.text = tickets.toString()
        tvAdults.text = adults.toString()
        tvChildren.text = children.toString()
        tvPasses.text = passes.toString()
        tvLuggage.text = luggage.toString()
        tvTotalPassengers.text = (adults + children + passes).toString()
        tvRevenue.text = "₹${String.format(Locale.getDefault(), "%.2f", revenue)}"

        bindClosedReports(reportPrefs)
        bindTicketHistory(reportPrefs)
    }

    private fun bindClosedReports(reportPrefs: android.content.SharedPreferences) {
        llClosedReports.removeAllViews()

        val rawTripReports = reportPrefs.getString("trip_reports", "[]") ?: "[]"
        val tripReports = JSONArray(rawTripReports)
        tvClosedReportsTitle.text = "Closed Day Reports: ${tripReports.length()}"

        if (tripReports.length() == 0) {
            tvClosedReportsEmpty.visibility = TextView.VISIBLE
            return
        }

        tvClosedReportsEmpty.visibility = TextView.GONE

        val snapshots = mutableListOf<Pair<Int, JSONObject>>()
        for (i in 0 until tripReports.length()) {
            snapshots.add(i to tripReports.getJSONObject(i))
        }

        snapshots.sortByDescending { (_, snapshot) -> parseTimestamp(snapshot.optString("closed_at", "")) }
        snapshots.forEach { (originalIndex, snapshot) ->
            llClosedReports.addView(makeClosedReportCard(snapshot, originalIndex))
        }
    }

    private fun makeClosedReportCard(snapshot: JSONObject, index: Int): LinearLayout {
        val date = snapshot.optString("date", "-")
        val route = snapshot.optString("route", "-")
        val routeNo = snapshot.optString("route_number", "-")
        val busType = snapshot.optString("bus_type", "-")
        val closedAt = snapshot.optString("closed_at", "-")
        val tickets = snapshot.optInt("tickets", 0)
        val adults = snapshot.optInt("adults", 0)
        val children = snapshot.optInt("children", 0)
        val passes = snapshot.optInt("passes", 0)
        val luggage = snapshot.optInt("luggage", 0)
        val revenue = snapshot.optDouble("revenue", 0.0)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { lp ->
                lp.bottomMargin = dp(10)
            }
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(ContextCompat.getColor(this@DailyReportActivity, R.color.section_background))

            val details = TextView(this@DailyReportActivity).apply {
                textSize = 14f
                text = "Date: $date  Closed: $closedAt\nRoute: $route  No: $routeNo  Type: $busType\nTickets: $tickets  A:$adults  C:$children  P:$passes  L:$luggage\nRevenue: ₹${String.format(Locale.getDefault(), "%.2f", revenue)}"
            }
            addView(details)

            val actions = LinearLayout(this@DailyReportActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { lp ->
                    lp.topMargin = dp(8)
                }
            }

            val printBtn = Button(this@DailyReportActivity).apply {
                text = "Print"
                textSize = 12f
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { confirmAndPrintSnapshotReport(snapshot) }
            }

            val deleteBtn = Button(this@DailyReportActivity).apply {
                text = "Delete"
                textSize = 12f
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { confirmDeleteClosedReport(index) }
            }

            actions.addView(printBtn)
            actions.addView(deleteBtn)
            addView(actions)
        }
    }

    private fun confirmDeleteClosedReport(index: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Closed Report")
            .setMessage("Delete this specific closed day report?")
            .setPositiveButton("Delete") { _, _ ->
                deleteClosedReport(index)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteClosedReport(index: Int) {
        val reportPrefs = getSharedPreferences(reportPrefsName, MODE_PRIVATE)
        val rawTripReports = reportPrefs.getString("trip_reports", "[]") ?: "[]"
        val tripReports = JSONArray(rawTripReports)
        if (index < 0 || index >= tripReports.length()) {
            Toast.makeText(this, "Invalid report selected", Toast.LENGTH_SHORT).show()
            return
        }

        val updated = JSONArray()
        for (i in 0 until tripReports.length()) {
            if (i != index) {
                updated.put(tripReports.getJSONObject(i))
            }
        }

        reportPrefs.edit().putString("trip_reports", updated.toString()).apply()
        bindReport()
        Toast.makeText(this, "Closed report deleted", Toast.LENGTH_SHORT).show()
    }

    private fun confirmAndPrintCurrentReport() {
        val setupPrefs = getSharedPreferences(setupPrefsName, MODE_PRIVATE)
        val reportPrefs = getSharedPreferences(reportPrefsName, MODE_PRIVATE)

        val reportJson = JSONObject().apply {
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
            put("closed_at", getCurrentDateTime())
        }

        val printText = buildReportPrintText(reportJson, "CURRENT REPORT")
        showReportPrintPreview(printText)
    }

    private fun confirmAndPrintSnapshotReport(snapshot: JSONObject) {
        val printText = buildReportPrintText(snapshot, "CLOSED REPORT")
        showReportPrintPreview(printText)
    }

    private fun showReportPrintPreview(printText: String) {
        AlertDialog.Builder(this)
            .setTitle("Print Report")
            .setMessage("Preview:\n\n$printText\nProceed with printing?")
            .setPositiveButton("Print") { _, _ ->
                printReportText(printText)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildReportPrintText(report: JSONObject, title: String): String {
        val revenue = report.optDouble("revenue", 0.0)
        return buildString {
            appendLine("$title")
            appendLine("Date: ${report.optString("date", "-")}")
            appendLine("Division: ${report.optString("division", "-")}")
            appendLine("Route: ${report.optString("route", "-")} (${report.optString("route_number", "-")})")
            appendLine("Bus Type: ${report.optString("bus_type", "-")}")
            appendLine("Bus No: ${report.optString("bus_numbers", "-")}")
            appendLine("Printed At: ${getCurrentDateTime()}")
            appendLine("------------------------------")
            appendLine("Tickets: ${report.optInt("tickets", 0)}")
            appendLine("Adults: ${report.optInt("adults", 0)}")
            appendLine("Children: ${report.optInt("children", 0)}")
            appendLine("Passes: ${report.optInt("passes", 0)}")
            appendLine("Luggage: ${report.optInt("luggage", 0)}")
            appendLine("Revenue: ₹${String.format(Locale.getDefault(), "%.2f", revenue)}")
            appendLine("------------------------------")
            appendLine()
            appendLine()
        }
    }

    private fun printReportText(text: String) {
        val ignoreBluetoothErrors = isIgnoreBluetoothErrorsEnabled()
        try {
            val pairedDevices = bluetoothManager.getPairedDevices()
            if (pairedDevices.isEmpty()) {
                if (ignoreBluetoothErrors) {
                    Toast.makeText(this, "Report processed (Bluetooth errors ignored)", Toast.LENGTH_LONG).show()
                    return
                }
                Toast.makeText(this, "No Bluetooth printer paired.", Toast.LENGTH_LONG).show()
                return
            }

            val printer = pairedDevices.first()
            if (!bluetoothManager.connectToPrinter(printer)) {
                if (ignoreBluetoothErrors) {
                    Toast.makeText(this, "Report processed (Bluetooth connect error ignored)", Toast.LENGTH_LONG).show()
                    return
                }
                Toast.makeText(this, "Failed to connect to printer.", Toast.LENGTH_LONG).show()
                return
            }

            val written = bluetoothManager.printData(text.toByteArray(Charsets.UTF_8))
            if (written) {
                Toast.makeText(this, "Report sent to printer", Toast.LENGTH_SHORT).show()
            } else if (ignoreBluetoothErrors) {
                Toast.makeText(this, "Report processed (Bluetooth write error ignored)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Report print failed while sending data.", Toast.LENGTH_LONG).show()
            }
        } catch (ex: Exception) {
            if (ignoreBluetoothErrors) {
                Toast.makeText(this, "Report processed (Bluetooth exception ignored)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Report print failed: ${ex.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
        } finally {
            bluetoothManager.disconnect()
        }
    }

    private fun isIgnoreBluetoothErrorsEnabled(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return prefs.getBoolean("ignore_bluetooth_error", false) || prefs.getBoolean("ignore_print_errors", false)
    }

    private fun parseTimestamp(value: String): Long {
        return try {
            SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).parse(value)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun bindTicketHistory(reportPrefs: android.content.SharedPreferences) {
        llTicketHistory.removeAllViews()

        val mergedHistory = JSONArray()

        // Current open trip history
        val rawCurrentHistory = reportPrefs.getString("ticket_history", "[]") ?: "[]"
        val currentHistory = JSONArray(rawCurrentHistory)
        for (i in 0 until currentHistory.length()) {
            mergedHistory.put(currentHistory.getJSONObject(i))
        }

        // Closed trip histories saved by Day-Close flow
        val rawTripReports = reportPrefs.getString("trip_reports", "[]") ?: "[]"
        val tripReports = JSONArray(rawTripReports)
        for (i in 0 until tripReports.length()) {
            val snapshot = tripReports.optJSONObject(i) ?: continue
            val snapshotHistory = snapshot.optJSONArray("ticket_history") ?: continue
            for (j in 0 until snapshotHistory.length()) {
                mergedHistory.put(snapshotHistory.getJSONObject(j))
            }
        }

        if (mergedHistory.length() == 0) {
            tvHistoryTitle.text = "Ticket History (Newest First): 0"
            tvHistoryEmpty.visibility = TextView.VISIBLE
            return
        }

        tvHistoryEmpty.visibility = TextView.GONE

        val entries = mutableListOf<org.json.JSONObject>()
        for (i in 0 until mergedHistory.length()) {
            entries.add(mergedHistory.getJSONObject(i))
        }

        entries.sortByDescending { it.optLong("timestamp", 0L) }
        tvHistoryTitle.text = "Ticket History (Newest First): ${entries.size}"

        var lastDateHeader = ""
        entries.forEach { entry ->
            val date = entry.optString("date", "")
            if (date.isNotBlank() && date != lastDateHeader) {
                llTicketHistory.addView(makeSectionHeader(date))
                lastDateHeader = date
            }
            llTicketHistory.addView(makeHistoryRow(entry))
        }
    }

    private fun makeSectionHeader(date: String): TextView {
        return TextView(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { lp ->
                lp.topMargin = dp(10)
                lp.bottomMargin = dp(4)
            }
            text = date
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    }

    private fun makeHistoryRow(entry: org.json.JSONObject): TextView {
        val dateTime = entry.optString("date_time", "")
        val ticketNo = entry.optString("ticket_no", "")
        val route = entry.optString("route", "")
        val from = entry.optString("from", "")
        val to = entry.optString("to", "")
        val adults = entry.optInt("adults", 0)
        val children = entry.optInt("children", 0)
        val passes = entry.optInt("passes", 0)
        val passType = entry.optString("pass_type", "None")
        val luggage = entry.optInt("luggage", 0)
        val fare = entry.optDouble("fare", 0.0)

        val rowText =
            "Ticket: $ticketNo  $dateTime\n$route | $from -> $to\nA:$adults  C:$children  P:$passes ($passType)  L:$luggage  Fare: ₹${String.format(Locale.getDefault(), "%.2f", fare)}"

        return TextView(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { lp ->
                lp.bottomMargin = dp(8)
            }
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(ContextCompat.getColor(this@DailyReportActivity, R.color.surface_background))
            text = rowText
            textSize = 13f
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
