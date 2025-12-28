package com.example.mobilprogramlamaproje

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MobilProgramlamaApp : Application() {
    private var followedStatusListener: ListenerRegistration? = null
    private lateinit var notificationHelper: NotificationHelper
    private var userRole: String? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        
        // Listen for auth state changes to start/stop the global listener
        Firebase.auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                // User is logged in, first get their role then start listening
                fetchRoleAndStartListener(user.uid)
            } else {
                // User logged out, stop listener
                stopGlobalListener()
            }
        }
    }

    private fun fetchRoleAndStartListener(uid: String) {
        Firebase.firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    userRole = document.getString("role")
                    startGlobalListener(uid)
                }
            }
    }

    private fun startGlobalListener(uid: String) {
        followedStatusListener?.remove()
        
        // Only non-admin users should get these notifications as per request
        followedStatusListener = Firebase.firestore.collection("notifications")
            .whereArrayContains("followers", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                if (userRole == "Admin") return@addSnapshotListener

                for (dc in snapshots.documentChanges) {
                    // Only show notification for modified documents (status change)
                    if (dc.type == DocumentChange.Type.MODIFIED) {
                        val notification = dc.document.toObject(Notification::class.java)
                        notificationHelper.showNotification(
                            notification.title ?: "Bilinmeyen",
                            notification.status ?: "Bilinmiyor",
                            dc.document.id
                        )
                    }
                }
            }
    }

    private fun stopGlobalListener() {
        followedStatusListener?.remove()
        followedStatusListener = null
        userRole = null
    }
}