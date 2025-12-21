package com.example.mobilprogramlamaproje

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobilprogramlamaproje.databinding.ActivityAnasayfaBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AnasayfaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnasayfaBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var notificationsAdapter: NotificationsAdapter
    private var tumBildirimlerListesi = ArrayList<Notification>()
    private var notificationTypeSettings: List<String>? = null
    private var settingsListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null

    private var isUpdatingChipsProgrammatically = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnasayfaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        setupRecyclerView()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        attachSettingsListener()
        attachNotificationsListener()
    }

    override fun onStop() {
        super.onStop()
        settingsListener?.remove()
        notificationsListener?.remove()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun attachSettingsListener() {
        settingsListener?.remove() // Önceki listener'ı kaldır
        val user = auth.currentUser
        if (user != null) {
            val settingsRef = Firebase.firestore.collection("user_settings").document(user.uid)
            settingsListener = settingsRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    notificationTypeSettings = null
                    listeyiGuncelle()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val settings = snapshot.get("notification_types") as? List<String>
                    // Ayarlar boşsa veya hiç yoksa null yap (hepsini göster)
                    notificationTypeSettings = if (settings.isNullOrEmpty()) null else settings
                } else {
                    // Ayar dökümanı yoksa null yap (hepsini göster)
                    notificationTypeSettings = null
                }
                listeyiGuncelle()
            }
        } else {
            notificationTypeSettings = null
            listeyiGuncelle()
        }
    }

    private fun attachNotificationsListener() {
        notificationsListener?.remove()
        notificationsListener = Firebase.firestore.collection("notifications").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    tumBildirimlerListesi.clear()
                    for (document in snapshot.documents) {
                        document.toObject(Notification::class.java)?.let {
                            it.id = document.id
                            tumBildirimlerListesi.add(it)
                        }
                    }
                    listeyiGuncelle()
                }
            }
    }

    private fun listeyiGuncelle() {
        val aramaMetni = binding.searchView.query.toString()
        val isAcikSecili = binding.chipAcik.isChecked
        val isTakipSecili = binding.chipTakipEttiklerim.isChecked
        val turFiltresi = binding.chipTurFilter.text.toString()
        val currentUserId = auth.currentUser?.uid

        var filtrelenmisListe = tumBildirimlerListesi.toList()

        // Kullanıcının bildirim ayarlarına göre filtrele
        notificationTypeSettings?.let { settings ->
             filtrelenmisListe = filtrelenmisListe.filter { notification -> settings.contains(notification.type) }
        }

        if (!binding.chipTumU.isChecked) {
            if (isAcikSecili) {
                filtrelenmisListe = filtrelenmisListe.filter { it.status.equals("Açık", ignoreCase = true) }
            }
            if (isTakipSecili) {
                if (currentUserId != null) {
                    filtrelenmisListe = filtrelenmisListe.filter { it.followers.contains(currentUserId) }
                } else {
                    filtrelenmisListe = emptyList()
                }
            }
            if (turFiltresi != "Tüm Türler") {
                filtrelenmisListe = filtrelenmisListe.filter { it.type.equals(turFiltresi, ignoreCase = true) }
            }
        }

        if (aramaMetni.isNotEmpty()) {
            val aramaKelimeleri = aramaMetni.split(' ').filter { it.isNotBlank() }
            filtrelenmisListe = filtrelenmisListe.filter { bildirim ->
                val birlesikMetin = "${bildirim.title.orEmpty()} ${bildirim.description.orEmpty()}"
                val metinKelimeleri = birlesikMetin.split(' ')
                
                aramaKelimeleri.all { aramaKelimesi ->
                    metinKelimeleri.any { metinKelimesi ->
                        metinKelimesi.startsWith(aramaKelimesi, ignoreCase = true)
                    }
                }
            }
        }

        notificationsAdapter.updateList(ArrayList(filtrelenmisListe))
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificationsAdapter(ArrayList())
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecyclerView.adapter = notificationsAdapter
    }

    private fun setupListeners() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                listeyiGuncelle()
                return true
            }
        })

        fun syncFilters() {
            if (isUpdatingChipsProgrammatically) return
            val isAnySpecialFilterActive = binding.chipAcik.isChecked || binding.chipTakipEttiklerim.isChecked || binding.chipTurFilter.text.toString() != "Tüm Türler"
            
            isUpdatingChipsProgrammatically = true
            binding.chipTumU.isChecked = !isAnySpecialFilterActive
            isUpdatingChipsProgrammatically = false
            
            listeyiGuncelle()
        }

        binding.chipTumU.setOnClickListener { chip ->
             if (isUpdatingChipsProgrammatically) return@setOnClickListener
            
            isUpdatingChipsProgrammatically = true
            binding.chipAcik.isChecked = false
            binding.chipTakipEttiklerim.isChecked = false
            binding.chipTurFilter.text = "Tüm Türler"
            (chip as Chip).isChecked = true
            isUpdatingChipsProgrammatically = false

            listeyiGuncelle()
        }

        binding.chipAcik.setOnCheckedChangeListener { _, _ ->  if (!isUpdatingChipsProgrammatically) syncFilters() }
        binding.chipTakipEttiklerim.setOnCheckedChangeListener { _, _ -> if (!isUpdatingChipsProgrammatically) syncFilters() }

        binding.chipTurFilter.setOnClickListener { chip ->
            (chip as Chip).isChecked = true // Rengi her zaman aktif tut
            val turler = resources.getStringArray(R.array.tur_array)
            AlertDialog.Builder(this)
                .setTitle("Tür Seçin")
                .setItems(turler) { _, which ->
                    binding.chipTurFilter.text = turler[which]
                    syncFilters()
                }
                .setOnDismissListener { 
                     (chip as Chip).isChecked = true 
                }
                .show()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_map -> {
                    startActivity(Intent(this, MapsActivity::class.java))
                    true
                }
                R.id.nav_create -> { 
                    startActivity(Intent(this, BildirimEkleActivity::class.java))
                    true 
                }
                R.id.nav_profile_settings -> {
                    startActivity(Intent(this, ProfileSettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}