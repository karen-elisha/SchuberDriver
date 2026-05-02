package com.example.s

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import org.json.JSONArray
import org.json.JSONObject

class DriverDashboard : AppCompatActivity(), OnMapReadyCallback {

    // Map
    private var googleMap: GoogleMap? = null
    private var driverMarker: Marker? = null
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // UI elements
    private lateinit var driverName: TextView
    private lateinit var driverVehicle: TextView
    private lateinit var driverStatus: TextView
    private lateinit var todayTrips: TextView
    private lateinit var totalEarnings: TextView
    private lateinit var driverRating: TextView
    private lateinit var btnLogout: ImageView
    private lateinit var btnSOS: Button
    private lateinit var switchAvailability: androidx.appcompat.widget.SwitchCompat
    private lateinit var tvLocationInfo: TextView
    private lateinit var tvSpeedInfo: TextView
    private lateinit var locationPulse: View
    private lateinit var statusBadge: LinearLayout

    private lateinit var btnStudents: LinearLayout
    private lateinit var btnSosHistory: LinearLayout
    private lateinit var btnProfile: LinearLayout
    private lateinit var btnAppInfo: LinearLayout

    // Location broadcast receiver
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "LOCATION_UPDATE") {
                val lat = intent.getDoubleExtra("latitude", 0.0)
                val lng = intent.getDoubleExtra("longitude", 0.0)
                val speed = intent.getFloatExtra("speed", 0f)
                updateMapLocation(lat, lng, speed)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "schuber_notifications"
        const val SOS_NOTIF_ID = 1001
        const val LOCATION_PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_dashboard)

        // Check Firebase auth
        if (!FirebaseManager.isLoggedIn) {
            redirectToLogin()
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        bindViews()
        loadDriverData()
        setupClickListeners()
        setupMap()
        checkLocationPermission()
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
        tvLocationInfo = findViewById(R.id.tvLocationInfo)
        tvSpeedInfo = findViewById(R.id.tvSpeedInfo)
        locationPulse = findViewById(R.id.locationPulse)
        statusBadge = findViewById(R.id.statusBadge)

        btnStudents = findViewById(R.id.btnStudents)
        btnSosHistory = findViewById(R.id.btnSosHistory)
        btnProfile = findViewById(R.id.btnProfile)
        btnAppInfo = findViewById(R.id.btnAppInfo)
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Dark map style for premium look
        try {
            map.setMapStyle(
                MapStyleOptions(
                    """
                    [
                      { "elementType": "geometry", "stylers": [{ "color": "#1d2c4d" }] },
                      { "elementType": "labels.text.fill", "stylers": [{ "color": "#8ec3b9" }] },
                      { "elementType": "labels.text.stroke", "stylers": [{ "color": "#1a3646" }] },
                      { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#304a7d" }] },
                      { "featureType": "road", "elementType": "geometry.stroke", "stylers": [{ "color": "#255763" }] },
                      { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#2c6675" }] },
                      { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#0e1626" }] },
                      { "featureType": "poi", "elementType": "geometry", "stylers": [{ "color": "#283d6a" }] },
                      { "featureType": "transit", "elementType": "geometry", "stylers": [{ "color": "#2f3948" }] }
                    ]
                    """.trimIndent()
                )
            )
        } catch (e: Exception) {
            // Style parsing failed — use default
        }

        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = true
            isMyLocationButtonEnabled = false
            isMapToolbarEnabled = false
        }

        // Get last known location immediately
        getLastKnownLocation()
    }

    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    updateMapLocation(it.latitude, it.longitude, it.speed)
                }
            }
        }
    }

    private fun updateMapLocation(lat: Double, lng: Double, speed: Float = 0f) {
        currentLatitude = lat
        currentLongitude = lng

        val latLng = LatLng(lat, lng)

        runOnUiThread {
            googleMap?.let { map ->
                // Update or create marker
                if (driverMarker == null) {
                    driverMarker = map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                } else {
                    driverMarker?.position = latLng
                    map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                }
            }

            // Update location info text
            tvLocationInfo.text = String.format("%.5f, %.5f", lat, lng)
            tvSpeedInfo.text = "${String.format("%.1f", speed * 3.6)} km/h"

            // Pulse animation
            startPulseAnimation()
        }
    }

    private fun startPulseAnimation() {
        val pulse = AlphaAnimation(1f, 0.3f).apply {
            duration = 1000
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        locationPulse.startAnimation(pulse)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_CODE
            )
        } else {
            startLocationService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService()
            } else {
                Toast.makeText(this, "Location permission is required for live tracking", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Enable my-location layer on map
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
        }
    }

    private fun stopLocationService() {
        stopService(Intent(this, LocationTrackingService::class.java))
    }

    private fun loadDriverData() {
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        val firebaseUser = FirebaseManager.currentUser

        val name = firebaseUser?.displayName
            ?: prefs.getString("driverName", "Driver")
            ?: "Driver"
        val vehicle = prefs.getString("vehicleNumber", "N/A") ?: "N/A"
        val isActive = prefs.getBoolean("driverIsActive", false)
        val trips = prefs.getInt("totalDeliveries", 0)
        val earnings = prefs.getInt("totalEarnings", 0)
        val rating = prefs.getFloat("driverRating", 5.0f)

        driverName.text = name
        driverVehicle.text = vehicle
        updateStatusUI(isActive)
        switchAvailability.isChecked = isActive

        todayTrips.text = trips.toString()
        totalEarnings.text = "₹$earnings"
        driverRating.text = String.format("%.1f", rating)

        // Also sync from Firestore
        FirebaseManager.getDriverProfile(
            onSuccess = { data ->
                if (data != null) {
                    runOnUiThread {
                        val firestoreName = data["displayName"] as? String ?: name
                        val firestoreVehicle = data["vehicleNumber"] as? String ?: vehicle
                        driverName.text = firestoreName
                        if (firestoreVehicle != "N/A" && firestoreVehicle.isNotEmpty()) {
                            driverVehicle.text = firestoreVehicle
                        }
                    }
                }
            }
        )
    }

    private fun updateStatusUI(isActive: Boolean) {
        driverStatus.text = if (isActive) "● LIVE" else "● OFFLINE"
        driverStatus.setTextColor(
            if (isActive) Color.parseColor("#00E676")
            else Color.parseColor("#78909C")
        )
        statusBadge.setBackgroundResource(
            if (isActive) R.drawable.bg_status_active
            else R.drawable.bg_status_offline
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

        // Update Firestore
        FirebaseManager.saveDriverProfile(
            hashMapOf("isActive" to isAvailable as Any)
        )

        val msg = if (isAvailable) "You are now Active ✅" else "You are now Offline"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        if (isAvailable) {
            startLocationService()
            sendNotification("Status Updated", "You are now Active and your location is being shared.")
        } else {
            stopLocationService()
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
                // Send to Firestore
                FirebaseManager.sendSosAlert(
                    driverName = driverNameStr,
                    vehicleNumber = vehicleNum,
                    latitude = currentLatitude,
                    longitude = currentLongitude,
                    onSuccess = { sosId ->
                        Toast.makeText(this, "🚨 SOS Alert Sent! Help is on the way.", Toast.LENGTH_LONG).show()
                        sendNotification(
                            "🚨 SOS Alert Sent",
                            "Emergency alert dispatched. Admin has been notified. Stay safe."
                        )
                    },
                    onFailure = { msg ->
                        Toast.makeText(this, "Failed to send SOS: $msg", Toast.LENGTH_LONG).show()
                    }
                )

                // Also save locally
                val sosList = getSosHistoryFromPrefs()
                val newSos = JSONObject().apply {
                    put("id", System.currentTimeMillis().toString())
                    put("driverId", FirebaseManager.currentUser?.uid ?: "")
                    put("driverName", driverNameStr)
                    put("vehicleNumber", vehicleNum)
                    put("timestamp", System.currentTimeMillis())
                    put("status", "ACTIVE")
                    put("resolved", false)
                    put("latitude", currentLatitude)
                    put("longitude", currentLongitude)
                    put("notes", "Emergency assistance requested")
                }
                sosList.put(newSos)
                saveSosHistoryToPrefs(sosList)
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
        stopLocationService()
        FirebaseManager.signOut()
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
        // Register location receiver
        val filter = IntentFilter("LOCATION_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(locationReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(locationReceiver)
        } catch (_: Exception) {}
    }
}
