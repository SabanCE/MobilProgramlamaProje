package com.example.mobilprogramlamaproje

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
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
    private lateinit var mapView: MapView
    private val TAG = "MapsActivity_DEBUG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        mapView = findViewById(R.id.map_view_container)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Log.d(TAG, "Harita hazırlandı.")

        // YENİ: Bilgi penceremizi haritaya tanıtıyoruz.
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter())

        val ataturkUniversitesi = LatLng(39.901253, 41.248184)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ataturkUniversitesi, 14f))

        haritayaBildirimleriYukle()

        // YENİ: Bilgi penceresine tıklandığında ne olacağını belirliyoruz.
        mMap.setOnInfoWindowClickListener { marker ->
            val notificationId = marker.tag as? String
            if (notificationId != null) {
                Log.d(TAG, "Bilgi penceresine tıklandı. ID: $notificationId. Detaylar açılıyor...")
                val intent = Intent(this, NotificationDetailActivity::class.java)
                intent.putExtra("NOTIFICATION_ID", notificationId)
                startActivity(intent)
            } else {
                Log.w(TAG, "Tıklanan pin'in bir ID'si (tag) yok.")
            }
        }
    }

    private fun haritayaBildirimleriYukle() {
        val db = Firebase.firestore
        db.collection("notifications").get()
            .addOnSuccessListener { documents ->
                mMap.clear()
                for (document in documents) {
                    document.toObject(Notification::class.java)?.let { notification ->
                        if (notification.latitude != null && notification.longitude != null) {
                            val position = LatLng(notification.latitude, notification.longitude)
                            val zamanFarki = zamanFarkiniHesapla(notification.timestamp)
                            val ikonRengi = getMarkerColor(notification.type ?: "")

                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(position)
                                    .title(notification.title)
                                    .snippet(zamanFarki)
                                    .icon(BitmapDescriptorFactory.defaultMarker(ikonRengi))
                            )
                            marker?.tag = document.id
                        }
                    }
                }
            }.addOnFailureListener { Log.e(TAG, "Hata: Bildirimler çekilemedi!", it) }
    }

    private fun getMarkerColor(type: String): Float {
        return when (type) {
            "Sağlık" -> BitmapDescriptorFactory.HUE_GREEN
            "Güvenlik" -> BitmapDescriptorFactory.HUE_RED
            "Çevre" -> BitmapDescriptorFactory.HUE_BLUE
            "Kayıp" -> BitmapDescriptorFactory.HUE_YELLOW
            else -> BitmapDescriptorFactory.HUE_VIOLET
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
    
    // YENİ: Bu iç sınıf, haritaya özel bilgi penceremizi nasıl göstereceğini söyler.
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
            val title = marker.title
            val snippet = marker.snippet

            val titleTextView = view.findViewById<TextView>(R.id.tvTitle)
            titleTextView.text = title

            val snippetTextView = view.findViewById<TextView>(R.id.tvSnippet)
            snippetTextView.text = snippet
        }
    }

    // --- MAPVIEW YAŞAM DÖNGÜSÜ METOTLARI ---
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
}