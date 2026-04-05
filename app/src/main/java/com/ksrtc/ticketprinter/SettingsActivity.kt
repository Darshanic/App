package com.sktc.ticketprinter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_settings)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val themePreference = findPreference<ListPreference>(AppThemeManager.KEY_THEME_MODE)
            themePreference?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            themePreference?.setOnPreferenceChangeListener { _, _ ->
                AppThemeManager.applyTheme(requireContext())
                activity?.recreate()
                true
            }

            val childRatePreference = findPreference<EditTextPreference>("child_ticket_rate")
            childRatePreference?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
            childRatePreference?.setOnBindEditTextListener { editText ->
                editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            childRatePreference?.setOnPreferenceChangeListener { _, newValue ->
                val entered = newValue?.toString()?.trim().orEmpty()
                val rate = entered.toIntOrNull()
                if (rate == null || rate !in 0..100) {
                    false
                } else {
                    true
                }
            }
        }
    }
}
