package com.example.s

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.util.regex.Pattern

class DSignup : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var btnSignup: Button
    private lateinit var signupCard: CardView

    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

    companion object {
        private const val TAG = "DSignup"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dsignup)

        val etName = findViewById<EditText>(R.id.etName)
        val etVehicle = findViewById<EditText>(R.id.etVehicle)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)

        btnSignup = findViewById(R.id.btnSignup)
        progressBar = findViewById(R.id.progressBar)
        signupCard = findViewById(R.id.signupCard)

        // Password visibility toggle
        var isPasswordVisible = false
        ivTogglePassword?.setOnClickListener {
            if (isPasswordVisible) {
                etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_eye)
            } else {
                etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            }
            etPassword.setSelection(etPassword.text.length)
            isPasswordVisible = !isPasswordVisible
        }

        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val vehicle = etVehicle.text.toString().trim().uppercase()
            val email = etEmail.text.toString().trim().lowercase()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                name.isEmpty() -> return@setOnClickListener showError(etName, "Enter your full name")
                name.length < 2 -> return@setOnClickListener showError(etName, "Name too short")
                email.isEmpty() -> return@setOnClickListener showError(etEmail, "Enter email address")
                !emailPattern.matcher(email).matches() -> return@setOnClickListener showError(etEmail, "Invalid email format")
                phone.isNotEmpty() && phone.length < 10 -> return@setOnClickListener showError(etPhone, "Enter valid phone number")
                password.length < 6 -> return@setOnClickListener showError(etPassword, "Min 6 characters required")
            }

            registerUser(name, vehicle, email, phone, password)
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, DLogin::class.java))
            finish()
        }
    }

    private fun registerUser(name: String, vehicle: String, email: String, phone: String, password: String) {
        setLoading(true)

        FirebaseManager.signUpWithEmail(
            email, password,
            onSuccess = { user ->
                Log.d(TAG, "Firebase signup success: ${user.email}")

                // Save driver profile to Firestore
                val profileData = hashMapOf<String, Any>(
                    "email" to email,
                    "displayName" to name,
                    "vehicleNumber" to vehicle,
                    "phone" to phone,
                    "authProvider" to "email",
                    "driverRating" to 5.0,
                    "totalTrips" to 0,
                    "totalEarnings" to 0,
                    "isActive" to false,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "lastLogin" to com.google.firebase.Timestamp.now()
                )

                FirebaseManager.saveDriverProfile(
                    profileData,
                    onSuccess = {
                        Log.d(TAG, "Firestore profile saved")
                    },
                    onFailure = { msg ->
                        Log.e(TAG, "Firestore save failed: $msg")
                    }
                )

                // Save to local prefs for quick access
                val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("userEmail", email)
                    putString("driverName", name)
                    putString("vehicleNumber", vehicle)
                    putString("driverPhone", phone)
                    putBoolean("isLoggedIn", true)
                    putFloat("driverRating", 5.0f)
                    putInt("totalDeliveries", 0)
                    putInt("totalEarnings", 0)
                    putBoolean("driverIsActive", false)
                    putLong("registrationDate", System.currentTimeMillis())
                }.apply()

                setLoading(false)
                Toast.makeText(this, "✅ Account created successfully!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, DriverDashboard::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            },
            onFailure = { msg ->
                setLoading(false)
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSignup.isEnabled = !loading
        btnSignup.text = if (loading) "Creating..." else "CREATE ACCOUNT"
    }

    private fun showError(et: EditText, msg: String) {
        et.error = msg
        et.requestFocus()
    }
}
