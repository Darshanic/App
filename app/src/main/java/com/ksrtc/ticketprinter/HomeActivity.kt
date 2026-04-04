package com.sktc.ticketprinter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<Button>(R.id.btnManagerSetup).setOnClickListener {
            startActivity(Intent(this, ManagerSetupActivity::class.java))
        }

        findViewById<Button>(R.id.btnCommuteTicket).setOnClickListener {
            startActivity(Intent(this, CommuteTicketActivity::class.java))
        }

        findViewById<Button>(R.id.btnDailyReport).setOnClickListener {
            startActivity(Intent(this, DailyReportActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
