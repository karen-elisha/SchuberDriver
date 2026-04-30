package com.example.s

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SosHistory : AppCompatActivity() {

    private lateinit var rvSosLogs: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: LinearLayout
    private lateinit var spinnerFilter: Spinner
    private lateinit var btnExport: Button
    private lateinit var btnRefresh: Button
    private lateinit var btnBack: Button
    private lateinit var tvLogCount: TextView
    private lateinit var tvTotalSOS: TextView
    private lateinit var tvResolved: TextView
    private lateinit var tvPending: TextView

    private lateinit var sosAdapter: SosAdapter
    private val sosList = mutableListOf<SosAlert>()
    private val filteredList = mutableListOf<SosAlert>()
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_history)

        initializeViews()
        setupRecyclerView()
        setupFilterSpinner()
        loadSosHistory()
        setupClickListeners()
    }

    private fun initializeViews() {
        rvSosLogs = findViewById(R.id.rvSosLogs)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        spinnerFilter = findViewById(R.id.spinnerFilter)
        btnExport = findViewById(R.id.btnExport)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnBack = findViewById(R.id.btnBack)
        tvLogCount = findViewById(R.id.tvLogCount)
        tvTotalSOS = findViewById(R.id.tvTotalSOS)
        tvResolved = findViewById(R.id.tvResolved)
        tvPending = findViewById(R.id.tvPending)
    }

    private fun setupRecyclerView() {
        sosAdapter = SosAdapter(filteredList) { sos, action ->
            when (action) {
                "resolve" -> showResolveConfirmation(sos)
                "call" -> callEmergency()
                "map" -> openMapLocation(sos)
            }
        }
        rvSosLogs.layoutManager = LinearLayoutManager(this)
        rvSosLogs.adapter = sosAdapter
    }

    private fun setupFilterSpinner() {
        val filters = arrayOf("All", "Active", "Resolved", "Pending")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentFilter = filters[position]
                applyFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadSosHistory() {
        showLoading(true)
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("sos_history", "[]")
        try {
            val jsonArray = JSONArray(json)
            sosList.clear()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                sosList.add(SosAlert(
                    id = obj.getString("id"),
                    driverId = obj.optString("driverId", ""),
                    driverName = obj.optString("driverName", "Driver"),
                    vehicleNumber = obj.optString("vehicleNumber", ""),
                    timestamp = obj.getLong("timestamp"),
                    status = obj.optString("status", "ACTIVE"),
                    resolved = obj.optBoolean("resolved", false),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    notes = obj.optString("notes", "Emergency assistance requested"),
                    responseTime = if (obj.has("responseTime")) obj.getLong("responseTime") else null
                ))
            }
            sosList.sortByDescending { it.timestamp }
            updateStats()
            applyFilter()
            showLoading(false)
        } catch (e: Exception) {
            showEmptyState()
        }
    }

    private fun updateStats() {
        val total = sosList.size
        val resolved = sosList.count { it.resolved || it.status == "RESOLVED" }
        val pending = total - resolved

        tvTotalSOS.text = total.toString()
        tvResolved.text = resolved.toString()
        tvPending.text = pending.toString()
        tvLogCount.text = "$total Emergency Event${if (total != 1) "s" else ""}"
    }

    private fun applyFilter() {
        filteredList.clear()
        filteredList.addAll(when (currentFilter) {
            "Active"   -> sosList.filter { !it.resolved && it.status == "ACTIVE" }
            "Resolved" -> sosList.filter { it.resolved || it.status == "RESOLVED" }
            "Pending"  -> sosList.filter { !it.resolved && it.status == "PENDING" }
            else       -> sosList
        })
        sosAdapter.notifyDataSetChanged()
        emptyView.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showResolveConfirmation(sos: SosAlert) {
        AlertDialog.Builder(this)
            .setTitle("Resolve Emergency")
            .setMessage("Mark this emergency as resolved?\n\nIncident from: ${formatDate(sos.timestamp)}")
            .setPositiveButton("Mark Resolved") { _, _ -> resolveSosAlert(sos) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resolveSosAlert(sos: SosAlert) {
        showLoading(true)
        val currentTime = System.currentTimeMillis()
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("sos_history", "[]")
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getString("id") == sos.id) {
                    obj.put("status", "RESOLVED")
                    obj.put("resolved", true)
                    obj.put("resolvedAt", currentTime)
                    obj.put("responseTime", currentTime - sos.timestamp)
                    break
                }
            }
            prefs.edit().putString("sos_history", jsonArray.toString()).apply()
            showLoading(false)
            Toast.makeText(this, "✅ Emergency marked as resolved", Toast.LENGTH_SHORT).show()
            loadSosHistory()
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Failed to resolve", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMapLocation(sos: SosAlert) {
        if (sos.latitude != 0.0 && sos.longitude != 0.0) {
            val uri = Uri.parse("geo:${sos.latitude},${sos.longitude}?q=${sos.latitude},${sos.longitude}(SOS Location)")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            try { startActivity(intent) }
            catch (e: Exception) { Toast.makeText(this, "No map app found", Toast.LENGTH_SHORT).show() }
        } else {
            Toast.makeText(this, "Location not available for this incident", Toast.LENGTH_SHORT).show()
        }
    }

    private fun callEmergency() {
        AlertDialog.Builder(this)
            .setTitle("Emergency Call")
            .setItems(arrayOf("📞 Call 112 (Emergency)", "📞 Call 100 (Police)", "Cancel")) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
                    1 -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:100")))
                }
            }
            .show()
    }

    private fun exportLogs() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }
        val sb = StringBuilder()
        sb.append("SCHUBER SOS EMERGENCY LOGS\n${"=".repeat(50)}\n")
        sb.append("Export Date: ${SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("Filter: $currentFilter\n\n${"=".repeat(50)}\n\n")
        filteredList.forEachIndexed { i, sos ->
            sb.append("${i + 1}. ${formatDate(sos.timestamp)}\n")
            sb.append("   Status: ${if (sos.resolved) "RESOLVED" else sos.status}\n")
            sb.append("   Notes: ${sos.notes}\n\n")
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Schuber SOS Emergency Logs")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(shareIntent, "Export SOS Logs"))
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))

    private fun setupClickListeners() {
        btnExport.setOnClickListener { exportLogs() }
        btnRefresh.setOnClickListener {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
            loadSosHistory()
        }
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (!show && sosList.isEmpty()) emptyView.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        showLoading(false)
        tvTotalSOS.text = "0"; tvResolved.text = "0"; tvPending.text = "0"
        tvLogCount.text = "0 Emergency Events"
        emptyView.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    data class SosAlert(
        val id: String, val driverId: String, val driverName: String,
        val vehicleNumber: String, val timestamp: Long, var status: String,
        var resolved: Boolean, val latitude: Double, val longitude: Double,
        val notes: String, val responseTime: Long?
    )

    inner class SosAdapter(
        private val list: List<SosAlert>,
        private val onAction: (SosAlert, String) -> Unit
    ) : RecyclerView.Adapter<SosAdapter.SosViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SosViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sos, parent, false)
            return SosViewHolder(view)
        }

        override fun onBindViewHolder(holder: SosViewHolder, position: Int) = holder.bind(list[position])
        override fun getItemCount() = list.size

        inner class SosViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvStatusIcon: TextView = itemView.findViewById(R.id.tvStatusIcon)
            private val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
            private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
            private val tvResponseTime: TextView = itemView.findViewById(R.id.tvResponseTime)
            private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
            private val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)
            private val actionButtons: LinearLayout = itemView.findViewById(R.id.actionButtons)
            private val btnResolve: Button = itemView.findViewById(R.id.btnResolve)
            private val btnCall: Button = itemView.findViewById(R.id.btnCall)
            private val btnMap: Button = itemView.findViewById(R.id.btnMap)

            fun bind(sos: SosAlert) {
                tvDateTime.text = formatDate(sos.timestamp)
                tvLocation.text = if (sos.latitude != 0.0 && sos.longitude != 0.0)
                    "📍 ${String.format("%.4f", sos.latitude)}, ${String.format("%.4f", sos.longitude)}"
                else "📍 Location unavailable"
                tvNotes.text = sos.notes
                tvNotes.visibility = if (sos.notes.isNotEmpty()) View.VISIBLE else View.GONE

                when {
                    sos.resolved || sos.status == "RESOLVED" -> {
                        tvStatusIcon.text = "✅"; tvStatus.text = "Resolved"
                        tvStatus.setTextColor(itemView.context.resources.getColor(android.R.color.holo_green_light, null))
                        tvResponseTime.text = "Resp: ${sos.responseTime?.let { "${it / 1000}s" } ?: "N/A"}"
                        actionButtons.visibility = View.GONE
                    }
                    sos.status == "ACTIVE" -> {
                        tvStatusIcon.text = "🚨"; tvStatus.text = "Active Emergency"
                        tvStatus.setTextColor(itemView.context.resources.getColor(android.R.color.holo_red_light, null))
                        tvResponseTime.text = "URGENT"
                        actionButtons.visibility = View.VISIBLE
                    }
                    else -> {
                        tvStatusIcon.text = "⏳"; tvStatus.text = "Pending"
                        tvStatus.setTextColor(itemView.context.resources.getColor(android.R.color.holo_orange_light, null))
                        tvResponseTime.text = "Waiting"
                        actionButtons.visibility = View.VISIBLE
                    }
                }
                btnResolve.setOnClickListener { onAction(sos, "resolve") }
                btnCall.setOnClickListener { onAction(sos, "call") }
                btnMap.setOnClickListener { onAction(sos, "map") }
            }
        }
    }
}
