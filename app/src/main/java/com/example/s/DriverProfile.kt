package com.example.s

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.*

class DriverProfile : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var profileCard: CardView
    private lateinit var tvProfileInitial: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfilePhone: TextView
    private lateinit var tvProfileDOB: TextView
    private lateinit var tvLicenseNumber: TextView
    private lateinit var tvProfileVehicle: TextView
    private lateinit var tvProfileVehicleModel: TextView
    private lateinit var tvProfileVehicleColor: TextView
    private lateinit var tvVehicleType: TextView
    private lateinit var tvVehicleCapacity: TextView
    private lateinit var tvProfileRating: TextView
    private lateinit var tvStars: TextView
    private lateinit var tvTotalTrips: TextView
    private lateinit var tvMemberSince: TextView

    private lateinit var btnBack: ImageView
    private lateinit var btnEditPhoto: Button
    private lateinit var btnEditProfile: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnDocuments: Button
    private lateinit var btnPayment: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_profile)

        initializeViews()
        loadDriverProfile()
        setupClickListeners()
    }

    private fun initializeViews() {
        progressBar = findViewById(R.id.progressBar)
        profileCard = findViewById(R.id.profileCard)
        tvProfileInitial = findViewById(R.id.tvProfileInitial)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfilePhone = findViewById(R.id.tvProfilePhone)
        tvProfileDOB = findViewById(R.id.tvProfileDOB)
        tvLicenseNumber = findViewById(R.id.tvLicenseNumber)
        tvProfileVehicle = findViewById(R.id.tvProfileVehicle)
        tvProfileVehicleModel = findViewById(R.id.tvProfileVehicleModel)
        tvProfileVehicleColor = findViewById(R.id.tvProfileVehicleColor)
        tvVehicleType = findViewById(R.id.tvVehicleType)
        tvVehicleCapacity = findViewById(R.id.tvVehicleCapacity)
        tvProfileRating = findViewById(R.id.tvProfileRating)
        tvStars = findViewById(R.id.tvStars)
        tvTotalTrips = findViewById(R.id.tvTotalTrips)
        tvMemberSince = findViewById(R.id.tvMemberSince)

        btnBack = findViewById(R.id.btnBack)
        btnEditPhoto = findViewById(R.id.btnEditPhoto)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnDocuments = findViewById(R.id.btnDocuments)
        btnPayment = findViewById(R.id.btnPayment)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun loadDriverProfile() {
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        val name = prefs.getString("driverName", "Driver") ?: "Driver"
        val email = prefs.getString("userEmail", "driver@schuber.com") ?: ""
        val phone = prefs.getString("driverPhone", "Not specified") ?: "Not specified"
        val dob = prefs.getLong("driverDOB", 0L).let {
            if (it == 0L) "Not specified"
            else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
        }
        val license = prefs.getString("licenseNumber", "Not specified") ?: "Not specified"
        val vehicleNumber = prefs.getString("vehicleNumber", "Not specified") ?: "Not specified"
        val vehicleModel = prefs.getString("vehicleModel", "Not specified") ?: "Not specified"
        val vehicleColor = prefs.getString("vehicleColor", "Not specified") ?: "Not specified"
        val vehicleType = prefs.getString("vehicleType", "Not specified") ?: "Not specified"
        val vehicleCapacity = prefs.getInt("vehicleCapacity", 4)
        val rating = prefs.getFloat("driverRating", 5.0f)
        val totalTrips = prefs.getInt("totalDeliveries", 0)
        val memberSince = prefs.getLong("registrationDate", System.currentTimeMillis())

        tvProfileName.text = name
        tvProfileEmail.text = email.ifEmpty { "driver@schuber.com" }
        tvProfilePhone.text = phone
        tvProfileDOB.text = dob
        tvLicenseNumber.text = license
        tvProfileVehicle.text = vehicleNumber
        tvProfileVehicleModel.text = vehicleModel
        tvProfileVehicleColor.text = vehicleColor
        tvVehicleType.text = vehicleType
        tvVehicleCapacity.text = "$vehicleCapacity seats"
        tvProfileRating.text = String.format("%.1f", rating)
        tvTotalTrips.text = totalTrips.toString()
        tvMemberSince.text = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(memberSince))

        val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
        tvProfileInitial.text = initials.ifEmpty { "D" }

        val fullStars = rating.toInt().coerceIn(0, 5)
        tvStars.text = "★".repeat(fullStars) + "☆".repeat(5 - fullStars)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        btnEditPhoto.setOnClickListener { showEditPhotoDialog() }
        btnEditProfile.setOnClickListener { showEditProfileDialog() }
        btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        btnDocuments.setOnClickListener { showDocumentsDialog() }
        btnPayment.setOnClickListener { showPaymentDialog() }
        btnLogout.setOnClickListener { showLogoutConfirmation() }
    }

    private fun showEditPhotoDialog() {
        AlertDialog.Builder(this)
            .setTitle("Update Profile Photo")
            .setItems(arrayOf("📷 Take Photo", "🖼️ Choose from Gallery", "Cancel")) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Camera feature coming soon", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Gallery feature coming soon", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)

        val etName = dialogView.findViewById<EditText>(R.id.etEditName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etEditPhone)
        val etVehicleModel = dialogView.findViewById<EditText>(R.id.etEditVehicleModel)
        val etVehicleColor = dialogView.findViewById<EditText>(R.id.etEditVehicleColor)
        val etVehicleType = dialogView.findViewById<EditText>(R.id.etEditVehicleType)
        val etVehicleCapacity = dialogView.findViewById<EditText>(R.id.etEditVehicleCapacity)

        etName.setText(tvProfileName.text.toString())
        etPhone.setText(tvProfilePhone.text.toString().let { if (it == "Not specified") "" else it })
        etVehicleModel.setText(tvProfileVehicleModel.text.toString().let { if (it == "Not specified") "" else it })
        etVehicleColor.setText(tvProfileVehicleColor.text.toString().let { if (it == "Not specified") "" else it })
        etVehicleType.setText(tvVehicleType.text.toString().let { if (it == "Not specified") "" else it })
        etVehicleCapacity.setText(tvVehicleCapacity.text.toString().replace(" seats", ""))

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val capacity = etVehicleCapacity.text.toString().trim().toIntOrNull() ?: 4
                updateProfile(
                    name = name,
                    phone = etPhone.text.toString().trim(),
                    vehicleModel = etVehicleModel.text.toString().trim(),
                    vehicleColor = etVehicleColor.text.toString().trim(),
                    vehicleType = etVehicleType.text.toString().trim(),
                    vehicleCapacity = capacity
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateProfile(
        name: String, phone: String, vehicleModel: String,
        vehicleColor: String, vehicleType: String, vehicleCapacity: Int
    ) {
        showLoading(true)
        getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE).edit().apply {
            putString("driverName", name)
            putString("driverPhone", phone.ifEmpty { "Not specified" })
            putString("vehicleModel", vehicleModel.ifEmpty { "Not specified" })
            putString("vehicleColor", vehicleColor.ifEmpty { "Not specified" })
            putString("vehicleType", vehicleType.ifEmpty { "Not specified" })
            putInt("vehicleCapacity", vehicleCapacity)
        }.apply()
        showLoading(false)
        Toast.makeText(this, "Profile updated successfully! ✅", Toast.LENGTH_SHORT).show()
        loadDriverProfile()
        profileCard.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
    }

    @SuppressLint("MissingInflatedId")
    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)

        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val currentPwd = etCurrentPassword.text.toString()
                val newPwd = etNewPassword.text.toString()
                val confirmPwd = etConfirmPassword.text.toString()

                when {
                    currentPwd.isEmpty() ->
                        Toast.makeText(this, "Enter current password", Toast.LENGTH_SHORT).show()
                    newPwd.length < 6 ->
                        Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    newPwd != confirmPwd ->
                        Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    else -> changePassword(currentPwd, newPwd)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        showLoading(true)
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("userEmail", "") ?: ""
        val storedPassword = prefs.getString("${email}_password", null)
            ?: prefs.getString(email, null)

        if (storedPassword == currentPassword) {
            prefs.edit().apply {
                putString("${email}_password", newPassword)
                putString(email, newPassword)
            }.apply()
            showLoading(false)
            Toast.makeText(this, "Password changed successfully! ✅", Toast.LENGTH_LONG).show()
        } else {
            showLoading(false)
            Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDocumentsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Vehicle Documents")
            .setMessage("📄 RC Book\n📄 Insurance Certificate\n📄 Pollution Certificate\n📄 Driver's License\n\nUpload your documents for verification")
            .setPositiveButton("Upload") { _, _ ->
                Toast.makeText(this, "Document upload feature coming soon", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showPaymentDialog() {
        AlertDialog.Builder(this)
            .setTitle("Payment Settings")
            .setMessage("💳 Bank Account: Not linked\n\n💰 Total Trips: ${tvTotalTrips.text}\n\n⭐ Rating: ${tvProfileRating.text}")
            .setPositiveButton("Link Bank Account") { _, _ ->
                Toast.makeText(this, "Bank linking feature coming soon", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, DLogin::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        profileCard.alpha = if (show) 0.5f else 1f
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
