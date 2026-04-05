package com.sktc.ticketprinter

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MAX_ADULTS_PER_TICKET = 5
        private const val MAX_CHILDREN_PER_TICKET = 5
    }

    private val bluetoothManager = BluetoothPrinterManager()
    private val ticketFormatter = TicketFormatter()
    private lateinit var dbHelper: RouteDatabaseHelper
    
    // Hardcoded State for simulation
    private var adultCount = 1
    private var childCount = 0
    private var ticketCounter = 255228
    
    // UI Elements
    private lateinit var tvAdultCount: TextView
    private lateinit var tvChildCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dbHelper = RouteDatabaseHelper(this)

        tvAdultCount = findViewById(R.id.tvAdultCount)
        tvChildCount = findViewById(R.id.tvChildCount)

        findViewById<Button>(R.id.btnAdultPlus).setOnClickListener {
            if (adultCount < MAX_ADULTS_PER_TICKET) {
                adultCount++
                updateCounterUI()
            } else {
                Toast.makeText(this, getString(R.string.limit_adults_per_ticket, MAX_ADULTS_PER_TICKET), Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnAdultMinus).setOnClickListener {
            if (adultCount > 1) {
                adultCount--
                updateCounterUI()
            }
        }

        findViewById<Button>(R.id.btnChildPlus).setOnClickListener {
            if (childCount < MAX_CHILDREN_PER_TICKET) {
                childCount++
                updateCounterUI()
            } else {
                Toast.makeText(this, getString(R.string.limit_children_per_ticket, MAX_CHILDREN_PER_TICKET), Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnChildMinus).setOnClickListener {
            if (childCount > 0) {
                childCount--
                updateCounterUI()
            }
        }

        findViewById<Button>(R.id.btnPrint).setOnClickListener {
            printSampleTicket()
        }
        
        updateCounterUI()
    }

    private fun updateCounterUI() {
        tvAdultCount.text = getString(R.string.legacy_adult_count, adultCount)
        tvChildCount.text = getString(R.string.legacy_child_count, childCount)
    }

    private fun printSampleTicket() {
        if (adultCount > MAX_ADULTS_PER_TICKET || childCount > MAX_CHILDREN_PER_TICKET) {
            Toast.makeText(
                this,
                getString(R.string.legacy_limit_message, MAX_ADULTS_PER_TICKET, MAX_CHILDREN_PER_TICKET),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val pairedDevices = bluetoothManager.getPairedDevices()
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_bluetooth_printer), Toast.LENGTH_LONG).show()
            return
        }

        val printer = pairedDevices[0]
        
        if (bluetoothManager.connectToPrinter(printer)) {
            // Retrieve dynamic distance fare: Stop 1 (Mandya) to Stop 3 (Induvalu)
            // Example rate: Rs 1.50 per KM
            val baseAdultFare = dbHelper.calculateFare(1, 3, 1.50)
            val baseChildFare = baseAdultFare * 0.50
            
            val totalFare = (adultCount * baseAdultFare) + (childCount * baseChildFare)
            
            // Format Ticket Bitmap
            val ticketBitmap = ticketFormatter.createTicketBitmap(
                ticketNo = String.format("%07d", ticketCounter),
                fromStopEN = "MANDYA",
                fromStopKA = "ಮಂಡ್ಯ",
                toStopEN = "INDUVALU",
                toStopKA = "ಇಂದುವಾಳು",
                adults = adultCount,
                childs = childCount,
                totalFare = totalFare,
                rrn = "64126592159"
            )
            
            val printBytes = ticketFormatter.decodeBitmapToByteArray(ticketBitmap)
            bluetoothManager.printData(printBytes)
            bluetoothManager.disconnect()
            
            Toast.makeText(this, getString(R.string.ticket_printed), Toast.LENGTH_SHORT).show()
            ticketCounter++
        } else {
            Toast.makeText(this, getString(R.string.failed_to_connect_printer, printer.name), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
