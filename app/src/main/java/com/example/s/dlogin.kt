package com.example.s

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DLogin : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var btnLogin: Button
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var loginCard: CardView
    private lateinit var ivTogglePassword: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dlogin)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        loginCard = findViewById(R.id.loginCard)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)

        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // Check if already logged in — go directly to dashboard
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("isLoggedIn", false)) {
            goToDashboard()
            return
        }

        // Password visibility toggle
        var isPasswordVisible = false
        ivTogglePassword.setOnClickListener {
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

        btnLogin.setOnClickListener { performLogin() }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, DSignup::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        tvForgotPassword?.setOnClickListener { showForgotPasswordDialog() }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim().lowercase()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = "Enter email address"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Enter password"
            etPassword.requestFocus()
            return
        }

        setLoading(true)

        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)

        // Try both key formats for compatibility
        val storedPassword = prefs.getString("${email}_password", null)
            ?: prefs.getString(email, null)

        if (storedPassword == null) {
            setLoading(false)
            Toast.makeText(this, "No account found with this email. Please sign up.", Toast.LENGTH_LONG).show()
            return
        }

        if (storedPassword != password) {
            setLoading(false)
            etPassword.error = "Incorrect password"
            return
        }

        // Save login state
        prefs.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("userEmail", email)
        }.apply()

        Toast.makeText(this, "Welcome back! 👋", Toast.LENGTH_SHORT).show()
        goToDashboard()
    }

    private fun showForgotPasswordDialog() {
        val emailInput = EditText(this).apply {
            hint = "Enter your registered email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter the email you registered with and we'll show you a reset option.")
            .setView(emailInput)
            .setPositiveButton("Check") { _, _ ->
                val email = emailInput.text.toString().trim().lowercase()
                val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
                val exists = prefs.getString("${email}_password", null) ?: prefs.getString(email, null)
                if (exists != null) {
                    Toast.makeText(this, "Account found. Contact admin to reset password.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "No account found with this email.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goToDashboard() {
        val intent = Intent(this, DriverDashboard::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        btnLogin.text = if (loading) "Signing in..." else "LOGIN"
    }
}
