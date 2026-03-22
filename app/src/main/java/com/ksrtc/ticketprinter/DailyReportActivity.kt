package com.sktc.ticketprinter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class DailyReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_report)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Daily Report"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
