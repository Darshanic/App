package com.ksrtc.ticketprinter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ManagerSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager_setup)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manager Setup"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
