package com.example.mobilprogramlamaproje

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
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

    private val TAG = "AnasayfaActivity_DEBUG"
    private var isUpdatingChipsProgrammatically = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnasayfaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        verileriYukle()
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun verileriYukle() {
        Firebase.firestore.collection("notifications").orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                tumBildirimlerListesi.clear()
                for (document in documents) {
                    document.toObject(Notification::class.java)?.let { 
                        it.id = document.id
                        tumBildirimlerListesi.add(it) 
                    }
                }
                listeyiGuncelle()
            }
    }

    private fun listeyiGuncelle() {
        val aramaMetni = binding.searchView.query.toString()
        val isAcikSecili = binding.chipAcik.isChecked
        val isTakipSecili = binding.chipTakipEttiklerim.isChecked
        val turFiltresi = binding.chipTurFilter.text.toString()
        val currentUserId = auth.currentUser?.uid

        var filtrelenmisListe = tumBildirimlerListesi.toList()

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
                R.id.nav_profile -> {
                    auth.signOut()
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                    true
                }
                R.id.nav_admin_paneli -> {
                    // TODO: Admin paneli aktivitesini başlat
                    true
                }
                else -> false
            }
        }
    }
}