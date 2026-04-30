package com.example.s

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class StudentList : AppCompatActivity() {

    private lateinit var rvStudents: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var btnRefresh: Button
    private lateinit var btnBack: Button
    private lateinit var tvStudentCount: TextView
    private lateinit var tvTotalStudents: TextView
    private lateinit var tvTodayPresent: TextView
    private lateinit var tvTodayAbsent: TextView
    private lateinit var tvUpdateTime: TextView
    private lateinit var tvLastSync: TextView

    private lateinit var studentAdapter: StudentAdapter
    private val studentList = mutableListOf<Student>()
    private val filteredList = mutableListOf<Student>()

    private val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_list)

        initializeViews()
        setupRecyclerView()
        loadStudents()
        setupSearch()
        setupClickListeners()
        updateLastSyncTime()
    }

    private fun initializeViews() {
        rvStudents = findViewById(R.id.rvStudents)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        etSearch = findViewById(R.id.etSearch)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnBack = findViewById(R.id.btnBack)
        tvStudentCount = findViewById(R.id.tvStudentCount)
        tvTotalStudents = findViewById(R.id.tvTotalStudents)
        tvTodayPresent = findViewById(R.id.tvTodayPresent)
        tvTodayAbsent = findViewById(R.id.tvTodayAbsent)
        tvUpdateTime = findViewById(R.id.tvUpdateTime)
        tvLastSync = findViewById(R.id.tvLastSync)
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentAdapter(filteredList,
            onMarkAttendance = { student -> showAttendanceDialog(student) },
            onCallParent = { student -> callParent(student) }
        )
        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.adapter = studentAdapter
    }

    private fun loadStudents() {
        showLoading(true)
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        val studentsJson = prefs.getString("assigned_students", getDefaultStudentsJson())
        try {
            val jsonArray = JSONArray(studentsJson)
            studentList.clear()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val student = Student(
                    studentId = obj.getString("studentId"),
                    name = obj.getString("name"),
                    className = obj.getString("className"),
                    address = obj.getString("address"),
                    parentPhone = obj.getString("parentPhone"),
                    emergencyContact = obj.optString("emergencyContact", "")
                )
                studentList.add(student)
            }
            loadAttendanceForToday()
            updateStats()
            showLoading(false)
        } catch (e: Exception) {
            showEmptyState()
        }
    }

    private fun getDefaultStudentsJson(): String {
        // Dummy assigned students (will be replaced by admin assign page later)
        return JSONArray().apply {
            put(JSONObject().apply {
                put("studentId", "S001"); put("name", "Aarav Sharma"); put("className", "5A")
                put("address", "123 MG Road, Bengaluru"); put("parentPhone", "9876543210")
                put("emergencyContact", "9988776655")
            })
            put(JSONObject().apply {
                put("studentId", "S002"); put("name", "Diya Patel"); put("className", "5A")
                put("address", "456 HSR Layout, Bengaluru"); put("parentPhone", "9876543211")
                put("emergencyContact", "9988776654")
            })
            put(JSONObject().apply {
                put("studentId", "S003"); put("name", "Rohan Verma"); put("className", "5B")
                put("address", "789 Koramangala, Bengaluru"); put("parentPhone", "9876543212")
                put("emergencyContact", "9988776653")
            })
            put(JSONObject().apply {
                put("studentId", "S004"); put("name", "Priya Nair"); put("className", "6A")
                put("address", "22 Indiranagar, Bengaluru"); put("parentPhone", "9876543213")
                put("emergencyContact", "9988776652")
            })
            put(JSONObject().apply {
                put("studentId", "S005"); put("name", "Karan Singh"); put("className", "6B")
                put("address", "55 Whitefield, Bengaluru"); put("parentPhone", "9876543214")
                put("emergencyContact", "9988776651")
            })
        }.toString()
    }

    private fun loadAttendanceForToday() {
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        val attendanceJson = prefs.getString("attendance_$todayDate", "{}")
        try {
            val attendanceObj = JSONObject(attendanceJson)
            for (student in studentList) {
                val status = attendanceObj.optString(student.studentId, "")
                student.isPresent = status == "PRESENT"
                student.attendanceStatus = status
            }
            updateFilteredList(etSearch.text.toString())
            updateStats()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun updateStats() {
        val total = studentList.size
        val present = studentList.count { it.isPresent }
        val absent = total - present

        tvTotalStudents.text = total.toString()
        tvTodayPresent.text = present.toString()
        tvTodayAbsent.text = absent.toString()
        tvStudentCount.text = "$total Student${if (total != 1) "s" else ""} Assigned"
    }

    private fun updateFilteredList(query: String) {
        filteredList.clear()
        filteredList.addAll(
            if (query.isEmpty()) studentList
            else studentList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.studentId.contains(query, ignoreCase = true) ||
                        it.className.contains(query, ignoreCase = true)
            }
        )
        studentAdapter.notifyDataSetChanged()
        emptyView.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAttendanceDialog(student: Student) {
        val options = arrayOf("✅ Present", "❌ Absent", "⏰ Late", "📋 Excused")
        AlertDialog.Builder(this)
            .setTitle("Mark Attendance — ${student.name}")
            .setItems(options) { _, which ->
                val status = when (which) {
                    0 -> "PRESENT"; 1 -> "ABSENT"; 2 -> "LATE"; else -> "EXCUSED"
                }
                markAttendance(student, status)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun markAttendance(student: Student, status: String) {
        showLoading(true)
        val prefs = getSharedPreferences("SchuberPrefs", Context.MODE_PRIVATE)
        val existingJson = prefs.getString("attendance_$todayDate", "{}")
        try {
            val attendanceObj = JSONObject(existingJson)
            attendanceObj.put(student.studentId, status)
            prefs.edit().putString("attendance_$todayDate", attendanceObj.toString()).apply()
            showLoading(false)
            Toast.makeText(this, "✅ ${student.name} marked as $status", Toast.LENGTH_SHORT).show()

            // Send notification for absent
            if (status == "ABSENT") {
                sendAttendanceNotification(student.name, status)
            }

            val idx = studentList.indexOfFirst { it.studentId == student.studentId }
            if (idx != -1) {
                studentList[idx].isPresent = status == "PRESENT"
                studentList[idx].attendanceStatus = status
                updateFilteredList(etSearch.text.toString())
                updateStats()
            }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Failed to mark attendance", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendAttendanceNotification(studentName: String, status: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(this, DriverDashboard.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Attendance Updated")
                .setContentText("$studentName marked as $status")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            manager.notify(studentName.hashCode(), notification)
        } catch (e: Exception) { /* ignore */ }
    }

    private fun callParent(student: Student) {
        if (student.parentPhone.isEmpty()) {
            Toast.makeText(this, "No parent phone number available", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Call Parent")
            .setMessage("Call ${student.name}'s parent at ${student.parentPhone}?")
            .setPositiveButton("Call") { _, _ ->
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.parentPhone}")))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateFilteredList(s.toString()) }
        })
    }

    private fun setupClickListeners() {
        btnRefresh.setOnClickListener {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
            loadStudents()
            updateLastSyncTime()
        }
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun updateLastSyncTime() {
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        tvUpdateTime.text = "Last sync: $time"
        tvLastSync.text = "Updated $time"
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (!show && studentList.isEmpty()) emptyView.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        showLoading(false)
        emptyView.visibility = View.VISIBLE
        tvTotalStudents.text = "0"; tvTodayPresent.text = "0"; tvTodayAbsent.text = "0"
        tvStudentCount.text = "0 Students Assigned"
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    data class Student(
        val studentId: String, val name: String, val className: String,
        val address: String, val parentPhone: String, val emergencyContact: String,
        var isPresent: Boolean = false, var attendanceStatus: String = ""
    )

    inner class StudentAdapter(
        private val students: List<Student>,
        private val onMarkAttendance: (Student) -> Unit,
        private val onCallParent: (Student) -> Unit
    ) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
            return StudentViewHolder(view)
        }

        override fun onBindViewHolder(holder: StudentViewHolder, position: Int) = holder.bind(students[position])
        override fun getItemCount() = students.size

        inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvInitial: TextView = itemView.findViewById(R.id.tvStudentInitial)
            private val tvName: TextView = itemView.findViewById(R.id.tvStudentName)
            private val tvId: TextView = itemView.findViewById(R.id.tvStudentId)
            private val tvClass: TextView = itemView.findViewById(R.id.tvStudentClass)
            private val tvAddress: TextView = itemView.findViewById(R.id.tvStudentAddress)
            private val tvAttendance: TextView = itemView.findViewById(R.id.tvAttendanceStatus)
            private val btnMark: Button = itemView.findViewById(R.id.btnMarkAttendance)
            private val btnCall: Button = itemView.findViewById(R.id.btnCallParent)

            fun bind(student: Student) {
                val initials = student.name.split(" ").take(2)
                    .mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                tvInitial.text = initials.ifEmpty { "S" }
                tvName.text = student.name
                tvId.text = "ID: ${student.studentId}"
                tvClass.text = "Class: ${student.className}"
                tvAddress.text = "📍 ${student.address}"

                when (student.attendanceStatus) {
                    "PRESENT" -> { tvAttendance.text = "✅ Present"; tvAttendance.setTextColor(itemView.context.resources.getColor(android.R.color.holo_green_light, null)) }
                    "ABSENT"  -> { tvAttendance.text = "❌ Absent"; tvAttendance.setTextColor(itemView.context.resources.getColor(android.R.color.holo_red_light, null)) }
                    "LATE"    -> { tvAttendance.text = "⏰ Late"; tvAttendance.setTextColor(itemView.context.resources.getColor(android.R.color.holo_orange_light, null)) }
                    "EXCUSED" -> { tvAttendance.text = "📋 Excused"; tvAttendance.setTextColor(itemView.context.resources.getColor(android.R.color.holo_blue_light, null)) }
                    else      -> { tvAttendance.text = "— Not Marked"; tvAttendance.setTextColor(itemView.context.resources.getColor(android.R.color.darker_gray, null)) }
                }

                btnMark.setOnClickListener { onMarkAttendance(student) }
                btnCall.setOnClickListener { onCallParent(student) }
                btnCall.visibility = if (student.parentPhone.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }
}
