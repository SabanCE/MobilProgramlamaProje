package com.example.mobilprogramlamaproje

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mobilprogramlamaproje.databinding.ActivityNotificationDetailBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNotificationDetailBinding
    private var notificationId: String? = null
    private lateinit var mMap: GoogleMap
    private var notification: Notification? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        notificationId = intent.getStringExtra("NOTIFICATION_ID")

        if (notificationId == null) {
            finish()
            return
        }

        binding.mapViewDetail.onCreate(savedInstanceState)
        binding.mapViewDetail.getMapAsync(this)

        // Fix for MapView touch events in ScrollView
        binding.mapOverlay.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.detailScrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    binding.detailScrollView.requestDisallowInterceptTouchEvent(false)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    binding.detailScrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                else -> true
            }
        }

        loadNotificationDetails()
        checkUserRole()

        binding.btnToggleFollow.setOnClickListener { toggleFollow() }
    }

    private fun checkUserRole() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && "admin".equals(document.getString("role"), ignoreCase = true)) {
                    setupAdminUI()
                } else {
                    binding.btnToggleFollow.visibility = View.VISIBLE
                }
            }
    }

    private fun setupAdminUI() {
        binding.adminStatusLayout.visibility = View.VISIBLE
        binding.btnDeleteNotification.visibility = View.VISIBLE
        binding.publisherInfoLayout.visibility = View.VISIBLE
        binding.btnToggleFollow.visibility = View.GONE // Admin can't follow

        val statusOptions = arrayOf("Açık", "Çözüldü", "İnceleniyor")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        binding.spinnerStatus.adapter = adapter

        notification?.status?.let {
            val currentStatusPosition = statusOptions.indexOf(it)
            if (currentStatusPosition != -1) {
                binding.spinnerStatus.setSelection(currentStatusPosition)
            }
        }

        binding.btnUpdateStatus.setOnClickListener { updateStatus() }
        binding.btnDeleteNotification.setOnClickListener { confirmDelete() }

        loadPublisherInfo()
    }

    private fun loadPublisherInfo() {
        notification?.userId?.let { publisherId ->
            FirebaseFirestore.getInstance().collection("users").document(publisherId).get()
                .addOnSuccessListener { userDoc ->
                    if (userDoc != null && userDoc.exists()) {
                        val name = userDoc.getString("nameSurname") ?: "Bilinmiyor"
                        val email = userDoc.getString("email") ?: ""
                        binding.tvDetailPublisherName.text = "Oluşturan: $name"
                        binding.tvDetailPublisherEmail.text = "E-posta: $email"
                    }
                }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Bildirimi Sil")
            .setMessage("Bu bildirimi kalıcı olarak silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.")
            .setPositiveButton("Sil") { _, _ -> deleteNotification() }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun deleteNotification() {
        // Delete image from storage first
        notification?.imageUrl?.let {
            if (it.isNotEmpty()) {
                FirebaseStorage.getInstance().getReferenceFromUrl(it).delete()
            }
        }

        // Delete notification from firestore
        FirebaseFirestore.getInstance().collection("notifications").document(notificationId!!)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Bildirim başarıyla silindi", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun updateStatus() {
        val newStatus = binding.spinnerStatus.selectedItem.toString()
        FirebaseFirestore.getInstance().collection("notifications").document(notificationId!!)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Durum başarıyla güncellendi", Toast.LENGTH_SHORT).show()
                binding.tvDetailStatus.text = "Durum: $newStatus"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadNotificationDetails() {
        FirebaseFirestore.getInstance().collection("notifications").document(notificationId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    notification = document.toObject(Notification::class.java)
                    notification?.let {
                        it.id = document.id
                        updateUI(it)
                        if (::mMap.isInitialized) {
                            loadPinOnMap(it)
                        }
                        // Check role again after notification is loaded to ensure spinner is set correctly
                        checkUserRole()
                    }
                }
            }
    }

    private fun updateUI(notification: Notification) {
        binding.tvDetailTitle.text = notification.title
        binding.tvDetailDescription.text = notification.description
        binding.tvDetailType.text = "Tür: ${notification.type}"
        binding.tvDetailStatus.text = "Durum: ${notification.status}"

        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        binding.tvDetailTimestamp.text = sdf.format(notification.timestamp?.toDate())

        if (!notification.imageUrl.isNullOrEmpty()) {
            binding.ivDetailImage.visibility = View.VISIBLE
            Glide.with(this).load(notification.imageUrl).into(binding.ivDetailImage)
        } else {
            binding.ivDetailImage.visibility = View.GONE
        }

        updateFollowButton()
    }

    private fun updateFollowButton() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (notification?.followers?.contains(userId) == true) {
            binding.btnToggleFollow.text = "Takibi Bırak"
        } else {
            binding.btnToggleFollow.text = "Takip Et"
        }
    }

    private fun toggleFollow() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val docRef = FirebaseFirestore.getInstance().collection("notifications").document(notificationId!!)

        if (notification?.followers?.contains(userId) == true) {
            // Unfollow
            docRef.update("followers", FieldValue.arrayRemove(userId))
                .addOnSuccessListener { 
                    val mutableFollowers = notification?.followers?.toMutableList()
                    mutableFollowers?.remove(userId)
                    notification?.followers = mutableFollowers?.toList() ?: emptyList()
                    updateFollowButton()
                }
        } else {
            // Follow
            docRef.update("followers", FieldValue.arrayUnion(userId))
                .addOnSuccessListener { 
                    val mutableFollowers = notification?.followers?.toMutableList() ?: mutableListOf()
                    mutableFollowers.add(userId)
                    notification?.followers = mutableFollowers.toList()
                    updateFollowButton()
                }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        notification?.let { loadPinOnMap(it) }
    }

    private fun loadPinOnMap(notification: Notification) {
        notification.latitude?.let { lat ->
            notification.longitude?.let { lon ->
                val position = LatLng(lat, lon)
                mMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(notification.title)
                        .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(notification.type)))
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
            }
        }
    }

    private fun getMarkerColor(type: String?): Float {
        return when (type) {
            "Acil Durum" -> BitmapDescriptorFactory.HUE_RED
            "Sağlık" -> BitmapDescriptorFactory.HUE_BLUE
            "Güvenlik" -> BitmapDescriptorFactory.HUE_CYAN
            "Çevre" -> BitmapDescriptorFactory.HUE_GREEN
            "Kayıp-Buluntu" -> BitmapDescriptorFactory.HUE_YELLOW
            "Teknik Arıza" -> BitmapDescriptorFactory.HUE_VIOLET
            else -> BitmapDescriptorFactory.HUE_ORANGE
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapViewDetail.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapViewDetail.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapViewDetail.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapViewDetail.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapViewDetail.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapViewDetail.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapViewDetail.onSaveInstanceState(outState)
    }
}