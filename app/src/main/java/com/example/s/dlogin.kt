package com.example.s

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class DLogin : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var btnLogin: Button
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var loginCard: CardView
    private lateinit var ivTogglePassword: ImageView
    private lateinit var btnGoogleSignIn: Button

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "DLogin"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dlogin)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        loginCard = findViewById(R.id.loginCard)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)

        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // Check if already logged in via Firebase
        if (FirebaseManager.isLoggedIn) {
            goToDashboard()
            return
        }

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Register the Google Sign-In launcher
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    setLoading(true)
                    FirebaseManager.firebaseAuthWithGoogle(
                        idToken,
                        onSuccess = { user ->
                            Log.d(TAG, "Google auth success: ${user.email}")
                            // Save driver profile to Firestore
                            val profileData = hashMapOf<String, Any>(
                                "email" to (user.email ?: ""),
                                "displayName" to (user.displayName ?: "Driver"),
                                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                                "authProvider" to "google",
                                "lastLogin" to com.google.firebase.Timestamp.now()
                            )
                            FirebaseManager.saveDriverProfile(profileData)
                            saveLocalPrefs(user.displayName ?: "Driver", user.email ?: "")
                            Toast.makeText(this, "Welcome, ${user.displayName}! 👋", Toast.LENGTH_SHORT).show()
                            goToDashboard()
                        },
                        onFailure = { msg ->
                            setLoading(false)
                            Toast.makeText(this, "Google sign-in failed: $msg", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } catch (e: ApiException) {
                setLoading(false)
                Log.e(TAG, "Google sign-in failed: ${e.statusCode}", e)
                Toast.makeText(this, "Google sign-in cancelled", Toast.LENGTH_SHORT).show()
            }
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

        btnGoogleSignIn.setOnClickListener { startGoogleSignIn() }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, DSignup::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        tvForgotPassword?.setOnClickListener { showForgotPasswordDialog() }
    }

    private fun startGoogleSignIn() {
        setLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
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

        FirebaseManager.signInWithEmail(
            email, password,
            onSuccess = { user ->
                Log.d(TAG, "Email auth success: ${user.email}")
                // Fetch profile from Firestore
                FirebaseManager.getDriverProfile(
                    onSuccess = { data ->
                        val name = data?.get("displayName") as? String ?: user.displayName ?: "Driver"
                        saveLocalPrefs(name, user.email ?: email)
                        Toast.makeText(this, "Welcome back, $name! 👋", Toast.LENGTH_SHORT).show()
                        goToDashboard()
                    },
                    onFailure = {
                        saveLocalPrefs(user.displayName ?: "Driver", user.email ?: email)
                        goToDashboard()
                    }
                )
            },
            onFailure = { msg ->
                setLoading(false)
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showForgotPasswordDialog() {
        val emailInput = EditText(this).apply {
            hint = "Enter your registered email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("We'll send a password reset link to your email.")
            .setView(emailInput)
            .setPositiveButton("Send Reset Link") { _, _ ->
                val email = emailInput.text.toString().trim().lowercase()
                if (email.isNotEmpty()) {
                    FirebaseManager.sendPasswordResetEmail(
                        email,
                        onSuccess = {
                            Toast.makeText(this, "Password reset email sent! Check your inbox.", Toast.LENGTH_LONG).show()
                        },
                        onFailure = { msg ->
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Toast.makeText(this, "Please enter an email address", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveLocalPrefs(name: String, email: String) {
        getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE).edit().apply {
            putBoolean("isLoggedIn", true)
            putString("userEmail", email)
            putString("driverName", name)
        }.apply()
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
        btnGoogleSignIn.isEnabled = !loading
        btnLogin.text = if (loading) "Signing in..." else "LOGIN"
    }
}
