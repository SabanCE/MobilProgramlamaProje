package com.example.mobilprogramlamaproje

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mobilprogramlamaproje.databinding.ActivityBildirimEkleBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.Date
import java.util.UUID

class BildirimEkleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBildirimEkleBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var secilenKonum: Location? = null
    private var secilenFotoUri: Uri? = null

    private val depolamaIzinKodu = 101

    private val resimSecmeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            secilenFotoUri = result.data?.data
            binding.ivOnizleme.setImageURI(secilenFotoUri)
            binding.flOnizlemeContainer.visibility = View.VISIBLE
        }
    }

    private val haritaKonumSecmeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val latitude = data?.getDoubleExtra("latitude", 0.0)
            val longitude = data?.getDoubleExtra("longitude", 0.0)
            if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
                val location = Location("")
                location.latitude = latitude
                location.longitude = longitude
                secilenKonum = location
                Toast.makeText(this, "Haritadan konum seçildi", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBildirimEkleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupSpinnerForUserRole()

        binding.backButton.setOnClickListener { finish() }
        binding.btnCihazKonumu.setOnClickListener { cihazKonumunuAl() }
        binding.btnHaritaKonumu.setOnClickListener { haritadanKonumSec() }
        binding.btnFotografEkle.setOnClickListener { fotografSec() }
        binding.btnGonder.setOnClickListener { bildirimiGonder() }
        binding.btnKaldirFoto.setOnClickListener { fotografiKaldir() }
    }

    private fun setupSpinnerForUserRole() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // Handle not logged in user
            finish()
            return
        }

        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val isAdmin = document != null && "admin".equals(document.getString("role"), ignoreCase = true)
                val statusOptions = if (isAdmin) {
                    resources.getStringArray(R.array.yeni_tur_array).toList() + "Acil Durum"
                } else {
                    resources.getStringArray(R.array.yeni_tur_array).toList()
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
                binding.spinnerTur.adapter = adapter
            }
    }

    private fun fotografiKaldir() {
        secilenFotoUri = null
        binding.flOnizlemeContainer.visibility = View.GONE
        binding.ivOnizleme.setImageURI(null)
    }

    private fun haritadanKonumSec() {
        val intent = Intent(this, MapsActivity::class.java)
        val secilenTur = binding.spinnerTur.selectedItem.toString()
        intent.putExtra("from_bildirim_ekle", true)
        intent.putExtra("notification_type", secilenTur)
        haritaKonumSecmeLauncher.launch(intent)
    }

    private fun cihazKonumunuAl() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    secilenKonum = location
                    Toast.makeText(this, "Cihaz konumu alındı", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Konum alınamadı. GPS açık mı?", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Konum alınırken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun fotografSec() {
        val gerekenIzin = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, gerekenIzin) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(gerekenIzin), depolamaIzinKodu)
        } else {
            galeriyiAc()
        }
    }

    private fun galeriyiAc() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resimSecmeLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == depolamaIzinKodu) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                galeriyiAc()
            } else {
                Toast.makeText(this, "Depolama izni reddedildi", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setUploadingState(isUploading: Boolean) {
        binding.progressBar.visibility = if (isUploading) View.VISIBLE else View.GONE
        binding.btnGonder.isEnabled = !isUploading
    }

    private fun bildirimiGonder() {
        val tur = binding.spinnerTur.selectedItem.toString()
        val baslik = binding.etBaslik.text.toString().trim()
        val aciklama = binding.etAciklama.text.toString().trim()

        if (baslik.isEmpty() || aciklama.isEmpty() || secilenKonum == null) {
            Toast.makeText(this, "Lütfen tüm zorunlu alanları doldurun ve konum seçin.", Toast.LENGTH_LONG).show()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Hata: Bu işlem için giriş yapmış olmalısınız.", Toast.LENGTH_LONG).show()
            return
        }

        setUploadingState(true)

        if (secilenFotoUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference
            val resimYolu = "images/${UUID.randomUUID()}"
            val resimRef = storageRef.child(resimYolu)
            
            resimRef.putFile(secilenFotoUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    resimRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        veritabaninaKaydet(user.uid, tur, baslik, aciklama, downloadUri.toString())
                    } else {
                        val hataMesaji = task.exception?.message ?: "Bilinmeyen bir hata oluştu."
                        Log.e("FotografYuklemeHatasi", "Hata: ", task.exception)
                        Toast.makeText(this, "Fotoğraf yüklenemedi: $hataMesaji", Toast.LENGTH_LONG).show()
                        setUploadingState(false)
                    }
                }
        } else {
            veritabaninaKaydet(user.uid, tur, baslik, aciklama, null)
        }
    }

    private fun veritabaninaKaydet(userId: String, tur: String, baslik: String, aciklama: String, fotoUrl: String?) {
        val bildirimStatus = "Açık" // Varsayılan durum her zaman "Açık" olsun

        val bildirim = hashMapOf(
            "type" to tur,
            "title" to baslik,
            "description" to aciklama,
            "latitude" to secilenKonum!!.latitude,
            "longitude" to secilenKonum!!.longitude,
            "imageUrl" to fotoUrl,
            "timestamp" to Date(),
            "status" to bildirimStatus,
            "userId" to userId
        )

        FirebaseFirestore.getInstance().collection("notifications")
            .add(bildirim)
            .addOnSuccessListener { 
                Toast.makeText(this, "Bildirim başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
                finish()
             }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hata: Veritabanına kaydedilemedi. ${e.message}", Toast.LENGTH_SHORT).show()
                setUploadingState(false)
            }
    }
}