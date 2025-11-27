package com.example.mobilprogramlamaproje

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobilprogramlamaproje.databinding.ActivityAnasayfaBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AnasayfaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnasayfaBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var notificationsAdapter: NotificationsAdapter
    private var tumBildirimlerListesi = ArrayList<Notification>()

    // Filtreleme ve sıralama durumları
    private var mevcutSiralama = Query.Direction.DESCENDING
    private var mevcutAramaMetni = ""
    private var mevcutDurumFiltresi = "Tümü"
    private var mevcutTurFiltresi = "Hepsi"

    // Hata ayıklama için bir etiket (TAG)
    private val TAG = "AnasayfaActivity_DEBUG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnasayfaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate başladı.")

        auth = Firebase.auth
        setupRecyclerView()
        setupListeners()

        // ÖNCE rolü kontrol et, SONRA verileri yükle
        checkUserRole()
        verileriYukle()

        Log.d(TAG, "onCreate bitti.")
    }

    private fun setupListeners() {
        Log.d(TAG, "setupListeners çalıştı.")
        // Arama çubuğunu (SearchView) dinle
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                mevcutAramaMetni = newText.orEmpty()
                listeyiGuncelle()
                return true
            }
        })

        // Filtreleri dinle
        binding.statusFilterChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val tiklananChip: Chip? = group.findViewById(checkedId)
            mevcutDurumFiltresi = tiklananChip?.text?.toString() ?: "Tümü"
            listeyiGuncelle()
        }
        binding.typeFilterChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val tiklananChip: Chip? = group.findViewById(checkedId)
            mevcutTurFiltresi = tiklananChip?.text?.toString() ?: "Hepsi"
            listeyiGuncelle()
        }

        // Bottom Navigation Bar yönlendirmeleri
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // Zaten buradayız
                R.id.nav_map -> {
                    Toast.makeText(this, "Harita Sayfası açılıyor...", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_create -> {
                    bildirimEkle()
                    true
                }
                R.id.nav_profile -> {
                    auth.signOut()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                // ID'nin 'nav_admin_paneli' olduğundan emin olarak devam ediyoruz.
                R.id.nav_admin_paneli -> {
                    Toast.makeText(this, "Admin Paneli açılıyor...", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun checkUserRole() {
        Log.d(TAG, "checkUserRole başladı.")
        val currentUser = auth.currentUser ?: run {
            Log.e(TAG, "Kullanıcı null, checkUserRole'dan çıkılıyor.")
            return
        }

        try {
            val adminMenuItem = binding.bottomNavigation.menu.findItem(R.id.nav_admin_paneli)
            adminMenuItem?.isVisible = false // Başlangıçta gizle

            val db = Firebase.firestore
            val userDocRef = db.collection("users").document(currentUser.uid)

            userDocRef.get().addOnSuccessListener { document ->
                Log.d(TAG, "Kullanıcı rolü sorgusu başarılı.")
                if (document != null && document.exists()) {
                    if (document.getString("role") == "admin") {
                        Log.d(TAG, "Kullanıcı bir admin. Butonlar görünür yapılıyor.")
                        binding.chipAdminPanel.visibility = View.VISIBLE
                        adminMenuItem?.isVisible = true
                    }
                } else {
                    Log.w(TAG, "Kullanıcı dökümanı bulunamadı.")
                }
            }.addOnFailureListener {
                Log.e(TAG, "Kullanıcı rolü sorgusu başarısız.", it)
            }

        } catch (e: Exception) {
            // Eğer ID YANLIŞSA, uygulama burada çökmek yerine logcat'e hata yazacak.
            Log.e(TAG, "checkUserRole içinde menü ID'si bulunurken hata oluştu! ID yanlış olabilir.", e)
            Toast.makeText(this, "Kritik Hata: Admin menüsü bulunamadı.", Toast.LENGTH_LONG).show()
        }
    }

    private fun verileriYukle() {
        Log.d(TAG, "verileriYukle başladı.")
        val db = Firebase.firestore
        db.collection("notifications").orderBy("timestamp", mevcutSiralama)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Veritabanından ${documents.size()} adet döküman başarıyla çekildi.")
                tumBildirimlerListesi.clear()
                for (document in documents) {
                    try {
                        val notification = document.toObject(Notification::class.java)
                        notification.id = document.id
                        tumBildirimlerListesi.add(notification)
                    } catch (e: Exception) {
                        Log.e(TAG, "Döküman Notification nesnesine çevrilirken hata!", e)
                    }
                }
                listeyiGuncelle()
                Toast.makeText(this, "${tumBildirimlerListesi.size} bildirim listeye eklendi.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Veritabanı sorgusu başarısız oldu.", exception)
                Toast.makeText(this, "Veritabanı hatası: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun bildirimEkle() {
        Log.d(TAG, "bildirimEkle başladı.")
        // ... fonksiyon içeriği aynı ...
        val db = Firebase.firestore
        val currentUser = auth.currentUser ?: return

        val notificationData = hashMapOf(
            "title" to "Yeni Test Bildirimi",
            "description" to "Bu bildirim, 'Oluştur' butonuna basılarak eklendi.",
            "type" to "Genel",
            "status" to "Açık",
            "authorId" to currentUser.uid,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Toast.makeText(this, "Test bildirimi eklendi!", Toast.LENGTH_SHORT).show()
                verileriYukle() // Ekleme başarılı olunca listeyi yenile
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listeyiGuncelle() {
        Log.d(TAG, "listeyiGuncelle çalıştı. Adapter'a ${tumBildirimlerListesi.size} eleman gönderiliyor.")
        var filtrelenmisListe = ArrayList(tumBildirimlerListesi)
        // ... filtreleme kodları ...
        if (mevcutAramaMetni.isNotEmpty()) {
            filtrelenmisListe = filtrelenmisListe.filter {
                it.title.contains(mevcutAramaMetni, ignoreCase = true) ||
                        it.description.contains(mevcutAramaMetni, ignoreCase = true)
            } as ArrayList<Notification>
        }
        if (mevcutDurumFiltresi != "Tümü") {
            filtrelenmisListe = filtrelenmisListe.filter { it.status == mevcutDurumFiltresi } as ArrayList<Notification>
        }
        if (mevcutTurFiltresi != "Hepsi") {
            filtrelenmisListe = filtrelenmisListe.filter { it.type == mevcutTurFiltresi } as ArrayList<Notification>
        }
        notificationsAdapter.updateList(filtrelenmisListe)
        Log.d(TAG, "Adapter güncellendi.")
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView çalıştı.")
        notificationsAdapter = NotificationsAdapter(ArrayList())
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecyclerView.adapter = notificationsAdapter
    }
}
