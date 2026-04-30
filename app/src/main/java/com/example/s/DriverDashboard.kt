package com.example.s

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject

class DriverDashboard : AppCompatActivity() {

    private lateinit var driverName: TextView
    private lateinit var driverVehicle: TextView
    private lateinit var driverStatus: TextView
    private lateinit var todayTrips: TextView
    private lateinit var totalEarnings: TextView
    private lateinit var driverRating: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnSOS: Button
    private lateinit var switchAvailability: androidx.appcompat.widget.SwitchCompat

    private lateinit var btnStudents: Button
    private lateinit var btnSosHistory: Button
    private lateinit var btnProfile: Button
    private lateinit var btnAppInfo: Button

    companion object {
        const val CHANNEL_ID = "schuber_notifications"
        const val SOS_NOTIF_ID = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_dashboard)

        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("isLoggedIn", false)) {
            redirectToLogin()
            return
        }

        createNotificationChannel()
        bindViews()
        loadDriverData()
        setupClickListeners()
    }

    private fun bindViews() {
        driverName = findViewById(R.id.driverName)
        driverVehicle = findViewById(R.id.driverVehicle)
        driverStatus = findViewById(R.id.driverStatus)
        todayTrips = findViewById(R.id.todayTrips)
        totalEarnings = findViewById(R.id.totalEarnings)
        driverRating = findViewById(R.id.driverRating)

        btnLogout = findViewById(R.id.btnLogout)
        btnSOS = findViewById(R.id.btnSOS)
        switchAvailability = findViewById(R.id.switchAvailability)

        btnStudents = findViewById(R.id.btnStudents)
        btnSosHistory = findViewById(R.id.btnSosHistory)
        btnProfile = findViewById(R.id.btnProfile)
        btnAppInfo = findViewById(R.id.btnAppInfo)
    }

    private fun loadDriverData() {
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        val name = prefs.getString("driverName", "Driver") ?: "Driver"
        val vehicle = prefs.getString("vehicleNumber", "N/A") ?: "N/A"
        val isActive = prefs.getBoolean("driverIsActive", false)
        val trips = prefs.getInt("totalDeliveries", 0)
        val earnings = prefs.getInt("totalEarnings", 0)
        val rating = prefs.getFloat("driverRating", 5.0f)

        driverName.text = "Hello, $name 👋"
        driverVehicle.text = "🚌 $vehicle"
        updateStatusUI(isActive)
        switchAvailability.isChecked = isActive

        todayTrips.text = trips.toString()
        totalEarnings.text = "₹$earnings"
        driverRating.text = String.format("%.1f ★", rating)
    }

    private fun updateStatusUI(isActive: Boolean) {
        driverStatus.text = if (isActive) "● Active" else "● Offline"
        driverStatus.setTextColor(
            if (isActive) resources.getColor(android.R.color.holo_green_dark, theme)
            else resources.getColor(android.R.color.darker_gray, theme)
        )
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ -> logoutUser() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnSOS.setOnClickListener { sendSOS() }

        switchAvailability.setOnCheckedChangeListener { _, isChecked ->
            updateAvailability(isChecked)
        }

        btnStudents.setOnClickListener {
            startActivity(Intent(this, StudentList::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        btnSosHistory.setOnClickListener {
            startActivity(Intent(this, SosHistory::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, DriverProfile::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        btnAppInfo.setOnClickListener {
            startActivity(Intent(this, AppInfo::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    private fun updateAvailability(isAvailable: Boolean) {
        getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE).edit()
            .putBoolean("driverIsActive", isAvailable).apply()
        updateStatusUI(isAvailable)
        val msg = if (isAvailable) "You are now Active ✅" else "You are now Offline"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        if (isAvailable) {
            sendNotification("Status Updated", "You are now marked as Active and ready for trips.")
        }
    }

    private fun sendSOS() {
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        val driverNameStr = prefs.getString("driverName", "Driver") ?: "Driver"
        val vehicleNum = prefs.getString("vehicleNumber", "") ?: ""

        AlertDialog.Builder(this)
            .setTitle("🚨 Send Emergency SOS")
            .setMessage("This will alert the school admin immediately.\n\nAre you in an emergency?")
            .setPositiveButton("Send SOS") { _, _ ->
                val sosList = getSosHistoryFromPrefs()
                val newSos = JSONObject().apply {
                    put("id", System.currentTimeMillis().toString())
                    put("driverId", prefs.getString("userEmail", "driver"))
                    put("driverName", driverNameStr)
                    put("vehicleNumber", vehicleNum)
                    put("timestamp", System.currentTimeMillis())
                    put("status", "ACTIVE")
                    put("resolved", false)
                    put("latitude", 0.0)
                    put("longitude", 0.0)
                    put("notes", "Emergency assistance requested")
                }
                sosList.put(newSos)
                saveSosHistoryToPrefs(sosList)

                Toast.makeText(this, "🚨 SOS Alert Sent! Help is on the way.", Toast.LENGTH_LONG).show()
                sendNotification(
                    "🚨 SOS Alert Sent",
                    "Emergency alert dispatched. Admin has been notified. Stay safe."
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Schuber Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Schuber Driver App Notifications"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(title: String, message: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            manager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            // Notification permission not granted — silently ignore
        }
    }

    private fun getSosHistoryFromPrefs(): JSONArray {
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        return try {
            JSONArray(prefs.getString("sos_history", "[]"))
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun saveSosHistoryToPrefs(array: JSONArray) {
        getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE).edit()
            .putString("sos_history", array.toString()).apply()
    }

    private fun logoutUser() {
        getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, DLogin::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        loadDriverData()
    }
}
