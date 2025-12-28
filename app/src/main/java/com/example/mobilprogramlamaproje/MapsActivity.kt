package com.example.mobilprogramlamaproje

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val TAG = "MapsActivity_DEBUG"
    private var isSelectionMode = false
    private var selectedLocationMarker: Marker? = null
    private var savedSelectedLatLng: LatLng? = null
    private var notificationType: String? = null
    private var notificationTypeSettings: List<String>? = null
    private var settingsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        if (savedInstanceState != null) {
            savedSelectedLatLng = savedInstanceState.getParcelable("selected_location")
        }

        isSelectionMode = intent.getBooleanExtra("from_bildirim_ekle", false)
        notificationType = intent.getStringExtra("notification_type")

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener { goToHome() }

        // Geri tuşu kontrolü
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToHome()
            }
        })

        attachSettingsListener()
    }

    private fun goToHome() {
        val intent = Intent(this, AnasayfaActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun attachSettingsListener() {
        val user = Firebase.auth.currentUser
        if (user != null) {
            val settingsRef = Firebase.firestore.collection("user_settings").document(user.uid)
            settingsListener = settingsRef.addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    notificationTypeSettings = snapshot.get("notification_types") as? List<String>
                } else {
                    notificationTypeSettings = null
                }
                if (::mMap.isInitialized) {
                    haritayaBildirimleriYukle()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        settingsListener?.remove()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedLocationMarker?.let {
            outState.putParcelable("selected_location", it.position)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.clear()
        val ataturkUniversitesi = LatLng(39.901253, 41.248184)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ataturkUniversitesi, 14f))

        haritayaBildirimleriYukle()

        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter())

        if (isSelectionMode) {
            Toast.makeText(this, "Haritadan bir konum seçin", Toast.LENGTH_LONG).show()
            val markerColor = getMarkerColor(notificationType ?: "") ?: BitmapDescriptorFactory.HUE_CYAN

            savedSelectedLatLng?.let { latLng ->
                selectedLocationMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Seçilen Konum")
                        .snippet("Onaylamak için buraya dokunun")
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                )
            }

            mMap.setOnMapClickListener { latLng ->
                selectedLocationMarker?.remove()
                selectedLocationMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Seçilen Konum")
                        .snippet("Onaylamak için buraya dokunun")
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                )
                selectedLocationMarker?.showInfoWindow()
            }

            mMap.setOnInfoWindowClickListener { marker ->
                if (marker.tag == null) {
                    val location = marker.position
                    val resultIntent = Intent()
                    resultIntent.putExtra("latitude", location.latitude)
                    resultIntent.putExtra("longitude", location.longitude)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
        } else {
            mMap.setOnInfoWindowClickListener { marker ->
                val notificationId = marker.tag as? String
                if (notificationId != null) {
                    val intent = Intent(this, NotificationDetailActivity::class.java)
                    intent.putExtra("NOTIFICATION_ID", notificationId)
                    startActivity(intent)
                }
            }
        }
    }

    private fun haritayaBildirimleriYukle() {
        if (!::mMap.isInitialized) return
        mMap.clear()
        
        val db = Firebase.firestore
        db.collection("notifications").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.toObject(Notification::class.java)?.let { notification ->
                        // Filtreleme kontrolü: Ayarlarda seçili değilse gösterme (Acil Durum her zaman gösterilir)
                        val isTypeAllowed = notificationTypeSettings == null || 
                                           notification.type == "Acil Durum" || 
                                           notificationTypeSettings?.contains(notification.type) == true

                        if (isTypeAllowed && notification.latitude != null && notification.longitude != null) {
                            getMarkerColor(notification.type ?: "")?.let { color ->
                                val position = LatLng(notification.latitude, notification.longitude)
                                val marker = mMap.addMarker(
                                    MarkerOptions()
                                        .position(position)
                                        .title(notification.title)
                                        .snippet(zamanFarkiniHesapla(notification.timestamp))
                                        .icon(BitmapDescriptorFactory.defaultMarker(color))
                                )
                                marker?.tag = document.id
                            }
                        }
                    }
                }
            }
    }

    private fun getMarkerColor(type: String): Float? {
        return when (type) {
            "Acil Durum" -> BitmapDescriptorFactory.HUE_RED
            "Sağlık" -> BitmapDescriptorFactory.HUE_BLUE
            "Güvenlik" -> BitmapDescriptorFactory.HUE_CYAN
            "Çevre" -> BitmapDescriptorFactory.HUE_GREEN
            "Kayıp-Buluntu" -> BitmapDescriptorFactory.HUE_YELLOW
            "Teknik Arıza" -> BitmapDescriptorFactory.HUE_VIOLET
            else -> null
        }
    }

    private fun zamanFarkiniHesapla(timestamp: Timestamp?): String {
        if (timestamp == null) return "Bilinmeyen zaman"
        val farkDakika = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - timestamp.toDate().time)
        if (farkDakika < 60) return "$farkDakika dk önce"
        val farkSaat = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - timestamp.toDate().time)
        if (farkSaat < 24) return "$farkSaat saat önce"
        val farkGun = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp.toDate().time)
        return "$farkGun gün önce"
    }
    
    inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {
        private val window: View = layoutInflater.inflate(R.layout.custom_info_window, null)
        override fun getInfoContents(marker: Marker): View? { render(marker, window); return window }
        override fun getInfoWindow(marker: Marker): View? { render(marker, window); return window }
        private fun render(marker: Marker, view: View) {
            view.findViewById<TextView>(R.id.tvTitle).text = marker.title
            view.findViewById<TextView>(R.id.tvSnippet).text = marker.snippet
        }
    }
}