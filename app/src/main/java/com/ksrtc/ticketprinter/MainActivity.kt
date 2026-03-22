package com.ksrtc.ticketprinter

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dbHelper = RouteDatabaseHelper(this)

        tvAdultCount = findViewById(R.id.tvAdultCount)
        tvChildCount = findViewById(R.id.tvChildCount)

        findViewById<Button>(R.id.btnAdultPlus).setOnClickListener {
            adultCount++
            updateCounterUI()
        }

        findViewById<Button>(R.id.btnAdultMinus).setOnClickListener {
            if (adultCount > 1) {
                adultCount--
                updateCounterUI()
            }
        }

        findViewById<Button>(R.id.btnChildPlus).setOnClickListener {
            childCount++
            updateCounterUI()
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
        tvAdultCount.text = "Adult: $adultCount"
        tvChildCount.text = "Child: $childCount"
    }

    private fun printSampleTicket() {
        val pairedDevices = bluetoothManager.getPairedDevices()
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "No Bluetooth printers found! Please pair one in Settings.", Toast.LENGTH_LONG).show()
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
            
            Toast.makeText(this, "Ticket Printed!", Toast.LENGTH_SHORT).show()
            ticketCounter++
        } else {
            Toast.makeText(this, "Failed to connect to ${printer.name}", Toast.LENGTH_SHORT).show()
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
