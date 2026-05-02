package com.example.s

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Centralized manager for Firebase Authentication and Firestore operations.
 * Provides a clean API for all Firebase interactions across the app.
 */
object FirebaseManager {

    private const val TAG = "FirebaseManager"
    private const val COLLECTION_DRIVERS = "drivers"
    private const val COLLECTION_LOCATIONS = "driver_locations"
    private const val COLLECTION_SOS = "sos_alerts"
    private const val COLLECTION_STUDENTS = "assigned_students"

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    // ──────────────────────── AUTH ────────────────────────

    fun signInWithEmail(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.let { onSuccess(it) }
                        ?: onFailure("Authentication failed")
                } else {
                    val msg = task.exception?.message ?: "Sign in failed"
                    Log.e(TAG, "Email sign-in failed: $msg")
                    onFailure(msg)
                }
            }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.let { onSuccess(it) }
                        ?: onFailure("Account creation failed")
                } else {
                    val msg = task.exception?.message ?: "Sign up failed"
                    Log.e(TAG, "Email sign-up failed: $msg")
                    onFailure(msg)
                }
            }
    }

    fun firebaseAuthWithGoogle(
        idToken: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.let { user ->
                        Log.d(TAG, "Google sign-in success: ${user.email}")
                        onSuccess(user)
                    } ?: onFailure("Google authentication failed")
                } else {
                    val msg = task.exception?.message ?: "Google sign-in failed"
                    Log.e(TAG, "Google sign-in error: $msg")
                    onFailure(msg)
                }
            }
    }

    fun signOut() {
        auth.signOut()
    }

    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to send reset email")
            }
    }

    // ──────────────────────── DRIVER PROFILE ────────────────────────

    fun saveDriverProfile(
        driverData: Map<String, Any>,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val uid = currentUser?.uid ?: run {
            onFailure("User not authenticated")
            return
        }
        db.collection(COLLECTION_DRIVERS).document(uid)
            .set(driverData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Driver profile saved")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save driver profile: ${e.message}")
                onFailure(e.message ?: "Failed to save profile")
            }
    }

    fun getDriverProfile(
        onSuccess: (Map<String, Any>?) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        val uid = currentUser?.uid ?: run {
            onFailure("User not authenticated")
            return
        }
        db.collection(COLLECTION_DRIVERS).document(uid)
            .get()
            .addOnSuccessListener { doc ->
                onSuccess(doc.data)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get driver profile: ${e.message}")
                onFailure(e.message ?: "Failed to get profile")
            }
    }

    // ──────────────────────── LOCATION ────────────────────────

    fun updateDriverLocation(
        latitude: Double,
        longitude: Double,
        speed: Float = 0f,
        bearing: Float = 0f,
        isActive: Boolean = true
    ) {
        val uid = currentUser?.uid ?: return
        val locationData = hashMapOf(
            "uid" to uid,
            "email" to (currentUser?.email ?: ""),
            "latitude" to latitude,
            "longitude" to longitude,
            "speed" to speed,
            "bearing" to bearing,
            "isActive" to isActive,
            "lastUpdated" to com.google.firebase.Timestamp.now()
        )
        db.collection(COLLECTION_LOCATIONS).document(uid)
            .set(locationData, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update location: ${e.message}")
            }
    }

    // ──────────────────────── SOS ────────────────────────

    fun sendSosAlert(
        driverName: String,
        vehicleNumber: String,
        latitude: Double,
        longitude: Double,
        notes: String = "Emergency assistance requested",
        onSuccess: (String) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val uid = currentUser?.uid ?: return
        val sosData = hashMapOf(
            "driverId" to uid,
            "driverEmail" to (currentUser?.email ?: ""),
            "driverName" to driverName,
            "vehicleNumber" to vehicleNumber,
            "latitude" to latitude,
            "longitude" to longitude,
            "notes" to notes,
            "status" to "ACTIVE",
            "resolved" to false,
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        db.collection(COLLECTION_SOS)
            .add(sosData)
            .addOnSuccessListener { docRef ->
                Log.d(TAG, "SOS sent: ${docRef.id}")
                onSuccess(docRef.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "SOS failed: ${e.message}")
                onFailure(e.message ?: "Failed to send SOS")
            }
    }

    // ──────────────────────── STUDENTS ────────────────────────

    fun getAssignedStudents(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        val uid = currentUser?.uid ?: run {
            onFailure("User not authenticated")
            return
        }
        db.collection(COLLECTION_STUDENTS)
            .whereEqualTo("assignedDriverId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val students = snapshot.documents.mapNotNull { it.data }
                onSuccess(students)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to load students")
            }
    }
}
