package com.example.mobilprogramlamaproje

import android.os.Bundle
import android.view.View
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
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNotificationDetailBinding
    private var notificationId: String? = null
    private lateinit var mMap: GoogleMap
    private var notification: Notification? = null

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

        loadNotificationDetails()

        binding.btnToggleFollow.setOnClickListener { toggleFollow() }
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
                        // Harita hazırsa, pini yükle
                        if (::mMap.isInitialized) {
                            loadPinOnMap(it)
                        }
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
        // Harita hazır olduğunda, eğer bildirim bilgisi daha önce yüklendiyse pini haritaya ekle
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
            "Sağlık" -> BitmapDescriptorFactory.HUE_BLUE
            "Güvenlik" -> BitmapDescriptorFactory.HUE_RED
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
