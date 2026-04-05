package com.sktc.ticketprinter

import android.app.Application

class TicketPrinterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppThemeManager.applyTheme(this)
    }
}
