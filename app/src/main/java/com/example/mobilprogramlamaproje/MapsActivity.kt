package com.example.mobilprogramlamaproje

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
        Log.d(TAG, "Harita hazırlandı.")
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
                selectedLocationMarker?.tag = null
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
                selectedLocationMarker?.tag = null
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
        val db = Firebase.firestore
        db.collection("notifications").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.toObject(Notification::class.java)?.let { notification ->
                        if (notification.latitude != null && notification.longitude != null) {
                            getMarkerColor(notification.type ?: "")?.let { color ->
                                val position = LatLng(notification.latitude, notification.longitude)
                                val zamanFarki = zamanFarkiniHesapla(notification.timestamp)

                                val marker = mMap.addMarker(
                                    MarkerOptions()
                                        .position(position)
                                        .title(notification.title)
                                        .snippet(zamanFarki)
                                        .icon(BitmapDescriptorFactory.defaultMarker(color))
                                )
                                marker?.tag = document.id
                            }
                        }
                    }
                }
            }.addOnFailureListener { Log.e(TAG, "Hata: Bildirimler çekilemedi!", it) }
    }

    private fun getMarkerColor(type: String): Float? {
        return when (type) {
            "Sağlık" -> BitmapDescriptorFactory.HUE_BLUE
            "Güvenlik" -> BitmapDescriptorFactory.HUE_RED
            "Çevre" -> BitmapDescriptorFactory.HUE_GREEN
            "Kayıp-Buluntu" -> BitmapDescriptorFactory.HUE_YELLOW
            "Teknik Arıza" -> BitmapDescriptorFactory.HUE_VIOLET
            else -> null // Diğer türler için renk atama, haritada gösterilmeyecek
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

        override fun getInfoContents(marker: Marker): View? {
            render(marker, window)
            return window
        }

        override fun getInfoWindow(marker: Marker): View? {
            render(marker, window)
            return window
        }

        private fun render(marker: Marker, view: View) {
            val titleTextView = view.findViewById<TextView>(R.id.tvTitle)
            titleTextView.text = marker.title

            val snippetTextView = view.findViewById<TextView>(R.id.tvSnippet)
            snippetTextView.text = marker.snippet
        }
    }
}