package com.sktc.ticketprinter

import android.os.Bundle
import android.widget.AdapterView
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import java.util.concurrent.Executor
import androidx.core.content.ContextCompat

class ManagerSetupActivity : AppCompatActivity() {

    private lateinit var etPassKey: EditText
    private lateinit var etBusNumbers: EditText
    private lateinit var spRoute: Spinner
    private lateinit var spZone: Spinner
    private lateinit var spDivision: Spinner
    private lateinit var tvUpRoute: TextView
    private lateinit var tvDownRoute: TextView
    private lateinit var btnAuthorize: Button
    private lateinit var btnBiometric: Button
    private lateinit var btnConfirmAdminScope: Button
    private lateinit var switchConfirmBeforePrint: SwitchCompat
    private lateinit var adminStatusText: TextView
    private lateinit var rgAuthMethod: RadioGroup
    private lateinit var rbPassKey: RadioButton
    private lateinit var rbBiometric: RadioButton

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var biometricAvailable = false

    private val prefsName = "manager_setup"
    private val managerPassKey = "1415"

    private val routes = listOf(
        "Select Route",
        "Coming Soon"
    )

    private val zoneToDivisions = mapOf(
        "Bengaluru Central" to listOf(
            "Attibele",
            "Bannerughatta",
            "Bengaluru Central (Majestic-Market)",
            "K.R.Pura",
            "Koramangala",
            "Tavarekere (Magadi)",
            "Yelahanka",
            "Yeshwanthpura"
        ),
        "Bengaluru Rural" to listOf(
            "Anekal",
            "Chennapattana",
            "Kanakapura",
            "Magadi",
            "Ramanagara"
        ),
        "Bengaluru Urban" to listOf(
            "Banashankari",
            "Banasawadi",
            "Bidadi",
            "Chandra Layout",
            "Deepanjalinagara",
            "Devanahalli",
            "Doddaballapura",
            "Electronic City",
            "Hebbala",
            "Hennuru",
            "Hosakote",
            "HSR Layout",
            "ITPL",
            "Jayanagara",
            "Jigani",
            "Kalyananagara",
            "Kannalli",
            "Kengeri",
            "Kothanuru",
            "K R Pura",
            "Koramangala",
            "Peenya",
            "Poornapragna Layout",
            "R R Nagar",
            "R T Nagar",
            "Rajanukunte (Sadenahalli)",
            "Shanthinagara",
            "Subhashnagara",
            "Summanahalli",
            "Whitefield",
            "Yelahanka",
            "Yeshwanthpura"
        ),
        "Chamarajanagara" to listOf(
            "Arakalagudu",
            "Chamarajanagara",
            "Gundlupete",
            "Hanuru",
            "Kollegala",
            "Nanjanagudu",
            "T.Narsipura",
            "Yalanduru"
        ),
        "Chikkaballapura" to listOf(
            "Bagepalli",
            "Chikkaballapura",
            "Chintamani",
            "Gauribidanuru",
            "Gudibande",
            "Shidlaghatta"
        ),
        "Chikkamagaluru" to listOf(
            "Chikkamagaluru",
            "Kaduru",
            "Mudigere",
            "N.R.Pura",
            "Tarikere"
        ),
        "Hassan" to listOf(
            "Arakalagudu",
            "Chennarayapattana",
            "Hassan City",
            "Hassan Rural",
            "Holenarasipura"
        ),
        "Kolar" to listOf(
            "Bangarapete",
            "K.G.F.",
            "Kolara",
            "Maluru",
            "Mulubagilu",
            "Srinivasapura"
        ),
        "Madhugiri" to listOf(
            "Koratagere",
            "Madhugiri",
            "Pavagada",
            "Shira"
        ),
        "Mandya City" to listOf(
            "Arakere",
            "Basaralu",
            "Dudda",
            "Hanakere",
            "Induvalu",
            "Koppa",
            "Kothathi",
            "Madduru",
            "Mandya Central",
            "Melukote",
            "Pandavapura",
            "Srirangapattana"
        ),
        "Mandya Rural" to listOf(
            "K.R.Pete",
            "Madduru",
            "Malavalli",
            "Mandya",
            "Nagamangala",
            "Pandavapura"
        ),
        "Mangaluru" to listOf(
            "Bantwala (B.C.Road)",
            "Mangaluru City",
            "Mangaluru Rural",
            "Surathkal"
        ),
        "Mysuru City" to listOf(
            "Bannimantap",
            "Ilawala",
            "Jayapura",
            "Kuvempu Nagar",
            "Sathagalli",
            "Varuna",
            "Vijayanagar"
        ),
        "Mysuru Rural" to listOf(
            "H.D.Kote",
            "Hunasuru",
            "K.R.Nagar",
            "Mysuru",
            "Piriyapattana",
            "Saraguru",
            "Virajapete"
        ),
        "Putturu" to listOf(
            "Beltangadi",
            "Dharmasthala",
            "Kadaba",
            "Madikeri",
            "Putturu",
            "Subrahmanya",
            "Sulya"
        ),
        "Sakaleshapura" to listOf(
            "Aluru",
            "Beluru",
            "Sakaleshapura",
            "Somavarapete"
        ),
        "Tumkur" to listOf(
            "Gubbi",
            "Kunigal",
            "Tiptur - C N Halli",
            "Tumkur City",
            "Tumkur Rural",
            "Turuvekere"
        ),
        "Udupi" to listOf(
            "Kalasa",
            "Karkala",
            "Koppa",
            "Kundapura",
            "Mudubidire",
            "Shringeri",
            "Udupi"
        )
    )

    private var adminUnlocked = false
    private var adminScopeLocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager_setup)

        etPassKey = findViewById(R.id.etPassKeyInput)
        etBusNumbers = findViewById(R.id.etBusNumbers)
        spRoute = findViewById(R.id.spRoute)
        spZone = findViewById(R.id.spZone)
        spDivision = findViewById(R.id.spDivision)
        tvUpRoute = findViewById(R.id.tvUpRoute)
        tvDownRoute = findViewById(R.id.tvDownRoute)
        btnAuthorize = findViewById(R.id.btnAuthorizePassKey)
        btnBiometric = findViewById(R.id.btnBiometric)
        btnConfirmAdminScope = findViewById(R.id.btnConfirmAdminScope)
        switchConfirmBeforePrint = findViewById(R.id.switchConfirmBeforePrint)
        adminStatusText = findViewById(R.id.tvAdminStatus)
        rgAuthMethod = findViewById(R.id.rgAuthMethod)
        rbPassKey = findViewById(R.id.rbPassKey)
        rbBiometric = findViewById(R.id.rbBiometric)

        setupBiometric()
        setupRouteSpinner()
        setupZoneSpinner()
        setupDivisionSpinner()
        setAdminControlsEnabled(false)

        rbPassKey.isChecked = true
        updateAuthMethodUI()

        rgAuthMethod.setOnCheckedChangeListener { _, checkedId ->
            updateAuthMethodUI()
        }

        btnAuthorize.setOnClickListener {
            authorizePassKey()
        }

        btnBiometric.setOnClickListener {
            authorizeBiometric()
        }

        btnConfirmAdminScope.setOnClickListener {
            confirmAdminScope()
        }

        findViewById<Button>(R.id.btnSaveSetup).setOnClickListener {
            saveSetup()
        }

        findViewById<Button>(R.id.btnResetSetup).setOnClickListener {
            resetDefaults()
        }

        loadSetup()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manager Setup"
    }

    private fun setupBiometric() {
        val biometricManager = BiometricManager.from(this)
        biometricAvailable = when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }

        if (!biometricAvailable) {
            rbBiometric.isEnabled = false
            Toast.makeText(this, "Biometric not available on this device", Toast.LENGTH_SHORT).show()
        }

        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                adminUnlocked = true
                setAdminControlsEnabled(true)
                bindZones("")
                setDivisionItems("", "")
                updateRouteComingSoon(false)
                adminStatusText.text = "Biometric verified. Select Zone first, then Division. Route will show Coming Soon for now."
                Toast.makeText(this@ManagerSetupActivity, "Biometric verified.", Toast.LENGTH_SHORT).show()
                getSharedPreferences(prefsName, MODE_PRIVATE)
                    .edit()
                    .putString("auth_method", "biometric")
                    .apply()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@ManagerSetupActivity, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@ManagerSetupActivity, "Biometric authentication failed. Try again.", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Manager Authentication")
            .setSubtitle("Authenticate using your biometric data")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun updateAuthMethodUI() {
        val usePassKey = rbPassKey.isChecked
        etPassKey.visibility = if (usePassKey) View.VISIBLE else View.GONE
        btnAuthorize.visibility = if (usePassKey) View.VISIBLE else View.GONE
        btnBiometric.visibility = if (!usePassKey && biometricAvailable) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tvPassKeyLabel).visibility = if (usePassKey) View.VISIBLE else View.GONE

        if (!usePassKey) {
            etPassKey.text.clear()
        }
    }

    private fun authorizeBiometric() {
        if (!biometricAvailable) {
            Toast.makeText(this, "Biometric not available on this device.", Toast.LENGTH_LONG).show()
            return
        }
        biometricPrompt.authenticate(promptInfo)
    }

    private fun loadSetup() {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        etBusNumbers.setText(prefs.getString("bus_numbers", ""))
        switchConfirmBeforePrint.isChecked = prefs.getBoolean("confirm_before_print", true)

        val authMethod = prefs.getString("auth_method", "passkey") ?: "passkey"
        if (authMethod == "biometric") {
            rbBiometric.isChecked = true
        } else {
            rbPassKey.isChecked = true
        }

        updateRouteComingSoon(false)

        adminScopeLocked = prefs.getBoolean("admin_scope_locked", false)
        if (adminScopeLocked) {
            adminUnlocked = true
            val zone = prefs.getString("zone", "") ?: ""
            val division = prefs.getString("division", "") ?: ""
            bindZones(zone)
            setDivisionItems(zone, division)
            updateRouteComingSoon(true)
            lockAdminScopeUi()
            adminStatusText.text = "Admin scope locked for this setup. Route remains Coming Soon until routes are wired."
        } else {
            adminStatusText.text = "Admin controls are locked. Choose authentication method above and authorize to unlock Zones/Division."
        }
    }

    private fun saveSetup() {
        val selectedZone = spZone.selectedItem?.toString().orEmpty()
        val selectedDivision = spDivision.selectedItem?.toString().orEmpty()
        val busNumbers = etBusNumbers.text.toString().trim()

        if (!adminScopeLocked || selectedZone.isBlank() || selectedDivision.isBlank()) {
            Toast.makeText(this, "Authorize pass key and confirm zone/division scope first.", Toast.LENGTH_LONG).show()
            return
        }

        if (busNumbers.isBlank()) {
            Toast.makeText(this, "Enter at least one bus number.", Toast.LENGTH_LONG).show()
            return
        }

        getSharedPreferences(prefsName, MODE_PRIVATE)
            .edit()
            .putString("route", "Coming Soon")
            .putString("zone", selectedZone)
            .putString("division", selectedDivision)
            .putString("bus_numbers", busNumbers)
            .putBoolean("confirm_before_print", switchConfirmBeforePrint.isChecked)
            .putBoolean("admin_scope_locked", adminScopeLocked)
            .apply()

        hideKeyboard(etBusNumbers)
        Toast.makeText(this, "Manager setup saved. Commute pass config is active.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun resetDefaults() {
        etPassKey.setText("")
        etBusNumbers.setText("")
        bindZones("")
        setDivisionItems("", "")
        updateRouteComingSoon(false)
        adminUnlocked = false
        adminScopeLocked = false
        setAdminControlsEnabled(false)
        etPassKey.isEnabled = true
        btnAuthorize.visibility = View.VISIBLE
        btnBiometric.visibility = View.GONE
        btnConfirmAdminScope.isEnabled = true
        rbPassKey.isChecked = true
        updateAuthMethodUI()
        adminStatusText.text = "Admin controls are locked. Choose authentication method above and authorize to configure zones/division."
        switchConfirmBeforePrint.isChecked = true
        Toast.makeText(this, "Setup reset. Re-authorize to configure zones/division.", Toast.LENGTH_SHORT).show()
    }

    private fun setupRouteSpinner() {
        spRoute.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, routes)
        spRoute.setSelection(0)
        spRoute.isEnabled = false
        spRoute.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                tvUpRoute.text = routes[position]
                tvDownRoute.text = routes[position]
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // No-op
            }
        })
    }

    private fun setupZoneSpinner() {
        bindZones("")

        spZone.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!adminUnlocked) {
                    return
                }
                val selectedZone = spZone.selectedItem?.toString().orEmpty()
                if (selectedZone.startsWith("Select")) {
                    setDivisionItems("", "")
                    updateRouteComingSoon(false)
                } else {
                    setDivisionItems(selectedZone, "")
                    updateRouteComingSoon(false)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // No-op
            }
        })
    }

    private fun bindZones(selectedZone: String) {
        val zones = if (adminUnlocked) {
            listOf("Select Zone") + zoneToDivisions.keys.sorted()
        } else {
            listOf("Pass key required to access zones")
        }
        spZone.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, zones)
        val index = zones.indexOf(selectedZone).coerceAtLeast(0)
        spZone.setSelection(index)
        spZone.isEnabled = adminUnlocked && !adminScopeLocked
    }

    private fun setDivisionItems(zone: String, selectedDivision: String) {
        val divisions = when {
            !adminUnlocked -> listOf("Pass key required to access divisions")
            zone.isBlank() || zone.startsWith("Select") -> listOf("Select division after zone")
            else -> listOf("Select Division") + (zoneToDivisions[zone]?.sortedWith(String.CASE_INSENSITIVE_ORDER) ?: emptyList())
        }
        spDivision.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, divisions)
        val index = divisions.indexOf(selectedDivision).coerceAtLeast(0)
        spDivision.setSelection(index)
        spDivision.isEnabled = adminUnlocked && !adminScopeLocked && zone.isNotBlank() && !zone.startsWith("Select")
    }

    private fun setupDivisionSpinner() {
        spDivision.isEnabled = false
        spDivision.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!adminUnlocked || adminScopeLocked) {
                    return
                }
                val selectedDivision = spDivision.selectedItem?.toString().orEmpty()
                if (selectedDivision.isBlank() || selectedDivision.startsWith("Select")) {
                    updateRouteComingSoon(false)
                } else {
                    updateRouteComingSoon(true)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // No-op
            }
        })
    }

    private fun authorizePassKey() {
        val entered = etPassKey.text.toString().trim()
        if (entered != managerPassKey) {
            Toast.makeText(this, "Invalid pass key. Zones and divisions remain locked.", Toast.LENGTH_LONG).show()
            return
        }
        hideKeyboard(etPassKey)
        adminUnlocked = true
        setAdminControlsEnabled(true)
        bindZones("")
        setDivisionItems("", "")
        updateRouteComingSoon(false)
        adminStatusText.text = "Pass key accepted. Select Zone first, then Division. Route will show Coming Soon for now."
        Toast.makeText(this, "Pass key verified.", Toast.LENGTH_SHORT).show()
        getSharedPreferences(prefsName, MODE_PRIVATE)
            .edit()
            .putString("auth_method", "passkey")
            .apply()
    }

    private fun confirmAdminScope() {
        if (!adminUnlocked) {
            Toast.makeText(this, "Enter pass key first to access admin controls.", Toast.LENGTH_LONG).show()
            return
        }

        val zone = spZone.selectedItem?.toString().orEmpty()
        val division = spDivision.selectedItem?.toString().orEmpty()
        if (zone.startsWith("Select") || zone.isBlank() || division.startsWith("Select") || division.isBlank()) {
            Toast.makeText(this, "Select valid Zone and Division before confirming.", Toast.LENGTH_LONG).show()
            return
        }

        adminScopeLocked = true
        lockAdminScopeUi()
        Toast.makeText(this, "Admin scope locked. You can now modify remaining fields only.", Toast.LENGTH_LONG).show()
    }

    private fun lockAdminScopeUi() {
        spZone.isEnabled = false
        spDivision.isEnabled = false
        etPassKey.isEnabled = false
        btnAuthorize.visibility = View.GONE
        btnBiometric.visibility = View.GONE
        rgAuthMethod.visibility = View.GONE
        findViewById<TextView>(R.id.tvPassKeyLabel).visibility = View.GONE
        btnConfirmAdminScope.isEnabled = false
        adminStatusText.text = "Admin scope locked for this setup. Route will remain Coming Soon until route data is added."
    }

    private fun setAdminControlsEnabled(enabled: Boolean) {
        spZone.isEnabled = enabled
        spDivision.isEnabled = enabled
        btnConfirmAdminScope.isEnabled = enabled
    }

    private fun updateRouteComingSoon(enabled: Boolean) {
        spRoute.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Coming Soon"))
        spRoute.setSelection(0)
        spRoute.isEnabled = enabled
        tvUpRoute.text = "Coming Soon"
        tvDownRoute.text = "Coming Soon"
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
