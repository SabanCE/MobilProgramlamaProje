package com.example.mobilprogramlamaproje

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// import com.bumptech.glide.Glide // Fotoğraf yüklemek için Glide kütüphanesini eklersen bu satırı aç
import com.example.mobilprogramlamaproje.databinding.ActivityNotificationDetailBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit // HATA DÜZELTME: Bu import satırı eksikti.

class NotificationDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNotificationDetailBinding
    private lateinit var auth: FirebaseAuth
    private var notificationId: String? = null
    private var currentNotification: Notification? = null
    private lateinit var mMap: GoogleMap
    private var notificationLocation: LatLng? = null
    private var isFollowing = false

    private val TAG = "NotificationDetail_DEBUG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = Firebase.auth

        binding.detailMapView.onCreate(savedInstanceState)

        notificationId = intent.getStringExtra("NOTIFICATION_ID")

        if (notificationId == null) {
            Toast.makeText(this, "Hata: Bildirim ID'si bulunamadı!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadNotificationDetails()
        setupListeners()
    }

    private fun loadNotificationDetails() {
        val db = Firebase.firestore
        db.collection("notifications").document(notificationId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentNotification = document.toObject(Notification::class.java)?.copy(id = document.id)
                    if (currentNotification != null) {
                        checkUserRoleAndSetupUI()
                        updateUI()
                        initializeMap()
                    }
                } else {
                    Toast.makeText(this, "Bildirim bulunamadı.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { 
                Toast.makeText(this, "Veri yüklenemedi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUserRoleAndSetupUI(){
        val currentUser = auth.currentUser
        if (currentUser == null) {
            setupUserUI()
            return
        }

        // Kullanıcının takip durumunu kontrol et
        isFollowing = currentNotification?.followers?.contains(currentUser.uid) == true
        updateFollowButtonUI()

        val db = Firebase.firestore
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.getString("role") == "admin") {
                    setupAdminUI()
                } else {
                    setupUserUI()
                }
            }
            .addOnFailureListener { setupUserUI() }
    }

    private fun setupAdminUI() {
        binding.detailChipStatus.visibility = View.GONE
        binding.adminStatusLayout.visibility = View.VISIBLE
        setupAdminSpinner()
    }

    private fun setupUserUI() {
        binding.detailChipStatus.visibility = View.VISIBLE
        binding.adminStatusLayout.visibility = View.GONE
    }

    private fun setupAdminSpinner() {
        val statusOptions = arrayOf("Açık", "İnceleniyor", "Çözüldü")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.statusSpinner.adapter = adapter

        val currentStatus = currentNotification?.status
        val initialPosition = statusOptions.indexOf(currentStatus)
        if (initialPosition >= 0) {
            binding.statusSpinner.setSelection(initialPosition, false) // false animasyonu engeller
        }

        binding.statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = statusOptions[position]
                if (selectedStatus != currentNotification?.status) {
                    updateNotificationStatus(selectedStatus)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateNotificationStatus(newStatus: String) {
        val db = Firebase.firestore
        db.collection("notifications").document(notificationId!!)
            .update("status", newStatus)
            .addOnSuccessListener { 
                Toast.makeText(this, "Durum güncellendi: $newStatus", Toast.LENGTH_SHORT).show()
                currentNotification = currentNotification?.copy(status = newStatus)
            }
            .addOnFailureListener { 
                Toast.makeText(this, "Durum güncellenemedi.", Toast.LENGTH_SHORT).show()
                setupAdminSpinner()
            }
    }

    private fun updateUI() {
        currentNotification?.let { notif ->
            binding.detailTitle.text = notif.title
            binding.detailDescription.text = notif.description
            binding.detailChipType.text = notif.type
            binding.detailChipStatus.text = notif.status

            // İSTEĞE BAĞLI FOTOĞRAF GÖSTERME
            if (!notif.imageUrl.isNullOrEmpty()) {
                binding.detailImageView.visibility = View.VISIBLE
                // Glide.with(this).load(notif.imageUrl).into(binding.detailImageView)
            } else {
                binding.detailImageView.visibility = View.GONE
            }

            notif.userId?.let { userId ->
                val db = Firebase.firestore
                db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                    val userName = userDoc.getString("nameSurname") ?: "Bilinmeyen Kullanıcı"
                    val timeAgo = zamanFarkiniHesapla(notif.timestamp)
                    binding.detailInfoText.text = "$timeAgo $userName tarafından oluşturuldu"
                }
            }

            if (notif.latitude == null || notif.longitude == null) {
                binding.detailMapCard.visibility = View.GONE
            }
        }
    }

    private fun initializeMap() {
        currentNotification?.let { notif ->
            if (notif.latitude != null && notif.longitude != null) {
                notificationLocation = LatLng(notif.latitude, notif.longitude)
                binding.detailMapView.getMapAsync(this)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        notificationLocation?.let {
            mMap.addMarker(MarkerOptions().position(it).title(currentNotification?.title))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 16f))
        }
    }
    
    private fun setupListeners() {
        binding.buttonFollow.setOnClickListener { 
            toggleFollowStatus()
        }
    }

    private fun toggleFollowStatus() {
        val currentUser = auth.currentUser ?: return
        val db = Firebase.firestore
        val notificationRef = db.collection("notifications").document(notificationId!!)
        
        binding.buttonFollow.isEnabled = false

        if (isFollowing) {
            // Takipten Çık
            notificationRef.update("followers", FieldValue.arrayRemove(currentUser.uid))
                .addOnSuccessListener {
                    isFollowing = false
                    updateFollowButtonUI()
                    binding.buttonFollow.isEnabled = true
                    Toast.makeText(this, "Takipten çıkıldı.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { binding.buttonFollow.isEnabled = true }
        } else {
            // Takip Et
            notificationRef.update("followers", FieldValue.arrayUnion(currentUser.uid))
                .addOnSuccessListener {
                    isFollowing = true
                    updateFollowButtonUI()
                    binding.buttonFollow.isEnabled = true
                    Toast.makeText(this, "Takibe alındı.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { binding.buttonFollow.isEnabled = true }
        }
    }

    private fun updateFollowButtonUI() {
        if (isFollowing) {
            binding.buttonFollow.text = "Takipten Çık"
        } else {
            binding.buttonFollow.text = "Takip Et"
        }
    }

    private fun zamanFarkiniHesapla(timestamp: Timestamp?): String {
        if (timestamp == null) return "Bilinmeyen zaman"
        val gecenZamanMs = System.currentTimeMillis() - timestamp.toDate().time
        val dakika = TimeUnit.MILLISECONDS.toMinutes(gecenZamanMs)
        if (dakika < 60) return "$dakika dk önce"
        val saat = TimeUnit.MILLISECONDS.toHours(gecenZamanMs)
        if (saat < 24) return "$saat saat önce"
        val gun = TimeUnit.MILLISECONDS.toDays(gecenZamanMs)
        return "$gun gün önce"
    }

    override fun onResume() { super.onResume(); binding.detailMapView.onResume() }
    override fun onStart() { super.onStart(); binding.detailMapView.onStart() }
    override fun onStop() { super.onStop(); binding.detailMapView.onStop() }
    override fun onPause() { super.onPause(); binding.detailMapView.onPause() }
    override fun onLowMemory() { super.onLowMemory(); binding.detailMapView.onLowMemory() }
    override fun onDestroy() { super.onDestroy(); binding.detailMapView.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); binding.detailMapView.onSaveInstanceState(outState) }
}