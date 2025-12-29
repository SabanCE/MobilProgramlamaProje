package com.example.mobilprogramlamaproje

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MobilProgramlamaApp : Application() {
    private var followedStatusListener: ListenerRegistration? = null
    private var emergencyListener: ListenerRegistration? = null
    private lateinit var notificationHelper: NotificationHelper
    private var cachedRole: String? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        
        // Auth durumunu dinle
        Firebase.auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                // Giriş yapıldıysa rolü kontrol et ve dinlemeye başla
                checkRoleAndStartListeners(user.uid)
            } else {
                // Çıkış yapıldıysa tüm dinleyicileri durdur
                stopGlobalListeners()
            }
        }
    }

    private fun checkRoleAndStartListeners(uid: String) {
        Firebase.firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    cachedRole = role
                    
                    // Rol kontrolü: Sadece rolü "admin" olmayanlar bildirim alabilir
                    if (role?.trim()?.lowercase() == "admin") {
                        Log.d("NotificationDebug", "Admin girişi: Bildirim dinleyicileri başlatılmadı.")
                        stopGlobalListeners()
                    } else {
                        Log.d("NotificationDebug", "User girişi: Bildirim dinleyicileri başlatılıyor.")
                        startGlobalListeners(uid)
                    }
                }
            }
            .addOnFailureListener {
                stopGlobalListeners()
            }
    }

    private fun startGlobalListeners(uid: String) {
        stopGlobalListeners() // Önce temizle

        // 1. Takip Edilen Normal Bildirimler (Sadece takip eden kullanıcıya)
        followedStatusListener = Firebase.firestore.collection("notifications")
            .whereArrayContains("followers", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                
                // Güvenlik: Admin ise asla gösterme
                if (cachedRole?.trim()?.lowercase() == "admin") return@addSnapshotListener

                for (dc in snapshots.documentChanges) {
                    if (dc.type == DocumentChange.Type.MODIFIED) {
                        val notification = dc.document.toObject(Notification::class.java)
                        // Acil durumlar için aşağıda ayrı dinleyici var, karışmasın
                        if (notification.type != "Acil Durum") {
                            notificationHelper.showNotification(
                                notification.title ?: "Bildirim",
                                notification.status ?: "Güncellendi",
                                dc.document.id
                            )
                        }
                    }
                }
            }

        // 2. Acil Durum Güncellemeleri (Tüm Normal Kullanıcılara)
        emergencyListener = Firebase.firestore.collection("notifications")
            .whereEqualTo("type", "Acil Durum")
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                
                // Güvenlik: Admin ise asla gösterme
                if (cachedRole?.trim()?.lowercase() == "admin") return@addSnapshotListener

                for (dc in snapshots.documentChanges) {
                    if (dc.type == DocumentChange.Type.MODIFIED) {
                        val notification = dc.document.toObject(Notification::class.java)
                        notificationHelper.showNotification(
                            notification.title ?: "Acil Durum",
                            notification.status ?: "Güncellendi",
                            dc.document.id
                        )
                    }
                }
            }
    }

    private fun stopGlobalListeners() {
        followedStatusListener?.remove()
        emergencyListener?.remove()
        followedStatusListener = null
        emergencyListener = null
        cachedRole = null
    }
}