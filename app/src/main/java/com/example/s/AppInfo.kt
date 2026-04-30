package com.example.s

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class AppInfo : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var tvVersion: TextView
    private lateinit var tvBuild: TextView
    private lateinit var tvSdk: TextView
    private lateinit var tvCopyright: TextView
    private lateinit var tvDriverName: TextView
    private lateinit var tvDriverEmail: TextView

    private lateinit var emailLayout: View
    private lateinit var phoneLayout: View
    private lateinit var websiteLayout: View

    private lateinit var btnFacebook: TextView
    private lateinit var btnTwitter: TextView
    private lateinit var btnInstagram: TextView
    private lateinit var btnLinkedIn: TextView

    private lateinit var tvPrivacyPolicy: TextView
    private lateinit var tvTermsOfService: TextView
    private lateinit var tvEula: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_info)

        initializeViews()
        loadDriverInfo()
        setupVersionInfo()
        setupClickListeners()
        setupContactLinks()
        setupSocialMedia()
        setupLegalLinks()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        tvVersion = findViewById(R.id.tvVersion)
        tvBuild = findViewById(R.id.tvBuild)
        tvSdk = findViewById(R.id.tvSdk)
        tvCopyright = findViewById(R.id.tvCopyright)
        tvDriverName = findViewById(R.id.tvDriverName)
        tvDriverEmail = findViewById(R.id.tvDriverEmail)

        emailLayout = findViewById(R.id.emailLayout)
        phoneLayout = findViewById(R.id.phoneLayout)
        websiteLayout = findViewById(R.id.websiteLayout)

        btnFacebook = findViewById(R.id.btnFacebook)
        btnTwitter = findViewById(R.id.btnTwitter)
        btnInstagram = findViewById(R.id.btnInstagram)
        btnLinkedIn = findViewById(R.id.btnLinkedIn)

        tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy)
        tvTermsOfService = findViewById(R.id.tvTermsOfService)
        tvEula = findViewById(R.id.tvEula)
    }

    private fun loadDriverInfo() {
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        val name = prefs.getString("driverName", "Driver") ?: "Driver"
        val email = prefs.getString("userEmail", "") ?: ""

        tvDriverName.text = name
        tvDriverEmail.text = email.ifEmpty { "driver@schuber.com" }
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName ?: "1.0.0"
            tvVersion.text = versionName

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            tvBuild.text = versionCode.toString()
        } catch (e: Exception) {
            tvVersion.text = "1.0.0"
            tvBuild.text = "1"
        }

        tvSdk.text = "API ${Build.VERSION.SDK_INT} (Android ${Build.VERSION.RELEASE})"

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        tvCopyright.text = "© $currentYear Schuber Technologies. All rights reserved."
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setupContactLinks() {
        emailLayout.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@schuber.com")
                putExtra(Intent.EXTRA_SUBJECT, "Schuber App Support Request")
                putExtra(Intent.EXTRA_TEXT,
                    "\n\nApp Version: ${tvVersion.text}\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}")
            }
            try {
                startActivity(Intent.createChooser(emailIntent, "Send email"))
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        phoneLayout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Contact Support")
                .setItems(arrayOf("📞 Call Helpline (1800-123-4567)", "Cancel")) { _, which ->
                    if (which == 0) {
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:18001234567")))
                    }
                }
                .show()
        }

        websiteLayout.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.schuber.com")))
        }
    }

    private fun setupSocialMedia() {
        btnFacebook.setOnClickListener { openUrl("https://www.facebook.com/schuber") }
        btnTwitter.setOnClickListener { openUrl("https://www.twitter.com/schuber") }
        btnInstagram.setOnClickListener { openUrl("https://www.instagram.com/schuber") }
        btnLinkedIn.setOnClickListener { openUrl("https://www.linkedin.com/company/schuber") }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLegalLinks() {
        tvPrivacyPolicy.setOnClickListener { showLegalDialog("Privacy Policy", privacyPolicyText()) }
        tvTermsOfService.setOnClickListener { showLegalDialog("Terms of Service", termsOfServiceText()) }
        tvEula.setOnClickListener { showLegalDialog("End User License Agreement", eulaText()) }
    }

    private fun showLegalDialog(title: String, content: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton("Accept") { _, _ ->
                getSharedPreferences("SchuberPrefs", MODE_PRIVATE).edit().apply {
                    putBoolean("accepted_$title", true)
                    putLong("accepted_${title}_timestamp", System.currentTimeMillis())
                    apply()
                }
                Toast.makeText(this, "Thank you for accepting the $title", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun privacyPolicyText(): String = """
        PRIVACY POLICY
        Last updated: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}

        1. Information We Collect
        • Personal information (name, email, phone number)
        • Vehicle information (registration number, model)
        • Location data for trip tracking
        • Trip history and attendance records

        2. How We Use Your Information
        • To provide and maintain our service
        • To notify you about changes
        • To provide customer support
        • To monitor usage and improve service

        3. Data Security
        We implement appropriate security measures to protect your personal information.

        4. Data Sharing
        We do not sell your personal information. Data is shared only with:
        • School administration
        • Parents/guardians of assigned students
        • Emergency services (during SOS alerts)

        5. Your Rights
        You have the right to access, update, or delete your personal information.
        Contact us at: support@schuber.com
    """.trimIndent()

    private fun termsOfServiceText(): String = """
        TERMS OF SERVICE

        1. Acceptance of Terms
        By using the Schuber Driver App, you agree to these terms.

        2. Driver Responsibilities
        • Maintain valid driver's license and vehicle documents
        • Follow all traffic rules and regulations
        • Ensure student safety at all times
        • Report any incidents immediately

        3. Service Usage
        • Use real-time tracking responsibly
        • Respond to SOS alerts promptly
        • Maintain accurate attendance records
        • Keep app updated to latest version

        4. Termination
        We reserve the right to terminate accounts for:
        • Violation of terms
        • Safety violations
        • Fraudulent activities

        5. Limitation of Liability
        Schuber is not liable for indirect damages arising from service use.
    """.trimIndent()

    private fun eulaText(): String = """
        END USER LICENSE AGREEMENT (EULA)

        This EULA governs your use of the Schuber Driver App.

        1. License Grant
        We grant you a non-exclusive, non-transferable license to use the app.

        2. Restrictions
        You may not:
        • Modify or reverse engineer the app
        • Use the app for illegal purposes
        • Share your account credentials

        3. Updates
        We may automatically update the app to add features or fix issues.

        4. No Warranty
        The app is provided "as is" without warranties of any kind.

        5. Governing Law
        These terms are governed by the laws of India.
    """.trimIndent()

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
