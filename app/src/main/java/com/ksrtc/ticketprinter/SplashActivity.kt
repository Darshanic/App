package com.sktc.ticketprinter

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val splashDurationMs = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val versionText = findViewById<TextView>(R.id.tvAppVersion)
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "-"
        } catch (_: Exception) {
            "-"
        }
        versionText.text = getString(R.string.app_version_label, versionName)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }, splashDurationMs)
    }
}
