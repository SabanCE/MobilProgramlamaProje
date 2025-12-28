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
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobilprogramlamaproje.databinding.ActivityAdminPanelBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.Date
import java.util.UUID

class AdminPanelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminPanelBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var adminAdapter: AdminNotificationsAdapter
    private var tumBildirimlerListesi = ArrayList<Notification>()
    private var notificationTypeSettings: List<String>? = null
    private var settingsListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private var isUpdatingChipsProgrammatically = false

    private var secilenAcilKonum: Location? = null
    private var secilenAcilFotoUri: Uri? = null

    private val resimSecmeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            secilenAcilFotoUri = result.data?.data
            binding.ivAcilOnizleme.setImageURI(secilenAcilFotoUri)
            binding.flAcilFotoContainer.visibility = View.VISIBLE
        }
    }

    private val haritaKonumSecmeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val latitude = result.data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val longitude = result.data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            if (latitude != 0.0 && longitude != 0.0) {
                secilenAcilKonum = Location("").apply {
                    this.latitude = latitude
                    this.longitude = longitude
                }
                Toast.makeText(this, "Konum seçildi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        
        setupRecyclerView()
        setupTabs()
        setupListeners()
        setupAcilYayinlaListeners()
        setupAdminBottomNav()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@AdminPanelActivity, AnasayfaActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
        })
    }

    private fun setupAdminBottomNav() {
        val menu = binding.bottomNavigation.menu
        menu.findItem(R.id.nav_admin_panel).isVisible = true
        menu.findItem(R.id.nav_create).isVisible = false
        binding.bottomNavigation.selectedItemId = R.id.nav_admin_panel
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
        binding.bottomNavigation.selectedItemId = R.id.nav_admin_panel
    }

    private fun attachSettingsListener() {
        settingsListener?.remove()
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
                    notificationTypeSettings = if (settings.isNullOrEmpty()) null else settings
                } else {
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
        notificationsListener = Firebase.firestore.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
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
        val isOlusturdugumSecili = binding.chipOlusturdugum.isChecked
        val turFiltresi = binding.chipTurFilter.text.toString()
        val currentUserId = auth.currentUser?.uid

        // 1. AYIRMA: Öncelikli Bildirimler (Acil Durum + Açık/İnceleniyor)
        val oncelikliAcilBildirimler = tumBildirimlerListesi.filter { 
            it.type == "Acil Durum" && (it.status.equals("Açık", ignoreCase = true) || it.status.equals("İnceleniyor", ignoreCase = true))
        }

        // 2. GERİYE KALANLAR: Filtrelenecek olanlar
        var filtrelenecekListe = tumBildirimlerListesi.filter { bildirim ->
            !(bildirim.type == "Acil Durum" && (bildirim.status.equals("Açık", ignoreCase = true) || bildirim.status.equals("İnceleniyor", ignoreCase = true)))
        }

        notificationTypeSettings?.let { settings ->
            filtrelenecekListe = filtrelenecekListe.filter { it.type == "Acil Durum" || settings.contains(it.type) }
        }

        if (!binding.chipTumU.isChecked) {
            if (isAcikSecili) filtrelenecekListe = filtrelenecekListe.filter { it.status.equals("Açık", ignoreCase = true) }
            if (isTakipSecili && currentUserId != null) filtrelenecekListe = filtrelenecekListe.filter { it.followers.contains(currentUserId) }
            if (isOlusturdugumSecili && currentUserId != null) filtrelenecekListe = filtrelenecekListe.filter { it.userId == currentUserId }
            if (turFiltresi != "Tüm Türler") filtrelenecekListe = filtrelenecekListe.filter { it.type.equals(turFiltresi, ignoreCase = true) }
        }

        if (aramaMetni.isNotEmpty()) {
            filtrelenecekListe = filtrelenecekListe.filter { bildirim ->
                val fullText = "${bildirim.title} ${bildirim.description}"
                fullText.contains(aramaMetni, ignoreCase = true)
            }
        }

        // 3. BİRLEŞTİRME
        val finalResult = ArrayList<Notification>()
        finalResult.addAll(oncelikliAcilBildirimler)
        finalResult.addAll(filtrelenecekListe)
        
        adminAdapter.updateList(finalResult)
    }

    private fun setupRecyclerView() {
        adminAdapter = AdminNotificationsAdapter(ArrayList())
        binding.adminNotificationsRecycler.layoutManager = LinearLayoutManager(this)
        binding.adminNotificationsRecycler.adapter = adminAdapter
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
            val active = binding.chipAcik.isChecked || binding.chipTakipEttiklerim.isChecked || binding.chipOlusturdugum.isChecked || binding.chipTurFilter.text.toString() != "Tüm Türler"
            isUpdatingChipsProgrammatically = true
            binding.chipTumU.isChecked = !active
            isUpdatingChipsProgrammatically = false
            listeyiGuncelle()
        }

        binding.chipTumU.setOnClickListener {
            isUpdatingChipsProgrammatically = true
            binding.chipAcik.isChecked = false
            binding.chipTakipEttiklerim.isChecked = false
            binding.chipOlusturdugum.isChecked = false
            binding.chipTurFilter.text = "Tüm Türler"
            binding.chipTumU.isChecked = true
            isUpdatingChipsProgrammatically = false
            listeyiGuncelle()
        }

        binding.chipAcik.setOnCheckedChangeListener { _, _ -> if (!isUpdatingChipsProgrammatically) syncFilters() }
        binding.chipTakipEttiklerim.setOnCheckedChangeListener { _, _ -> if (!isUpdatingChipsProgrammatically) syncFilters() }
        binding.chipOlusturdugum.setOnCheckedChangeListener { _, _ -> if (!isUpdatingChipsProgrammatically) syncFilters() }

        binding.chipTurFilter.setOnClickListener {
            val turler = resources.getStringArray(R.array.tur_array)
            AlertDialog.Builder(this).setTitle("Tür Seçin").setItems(turler) { _, which ->
                binding.chipTurFilter.text = turler[which]
                binding.chipTurFilter.isChecked = true // Her zaman aktif/renkli kalmasını sağla
                syncFilters()
            }.show()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, AnasayfaActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_map -> { startActivity(Intent(this, MapsActivity::class.java)); true }
                R.id.nav_profile_settings -> { startActivity(Intent(this, ProfileSettingsActivity::class.java)); true }
                R.id.nav_admin_panel -> true
                else -> false
            }
        }
    }

    private fun setupTabs() {
        binding.adminTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    binding.layoutYonetim.visibility = View.VISIBLE
                    binding.layoutAcilYayinla.visibility = View.GONE
                } else {
                    binding.layoutYonetim.visibility = View.GONE
                    binding.layoutAcilYayinla.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAcilYayinlaListeners() {
        binding.btnAcilKonumCihaz.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
                return@setOnClickListener
            }
            LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    secilenAcilKonum = location
                    Toast.makeText(this, "Konum alındı", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnAcilKonumHarita.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("from_bildirim_ekle", true)
            intent.putExtra("notification_type", "Acil Durum")
            haritaKonumSecmeLauncher.launch(intent)
        }

        binding.btnAcilFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            resimSecmeLauncher.launch(intent)
        }

        binding.btnAcilFotoKaldir.setOnClickListener {
            secilenAcilFotoUri = null
            binding.flAcilFotoContainer.visibility = View.GONE
        }

        binding.btnAcilYayinla.setOnClickListener { acilBildirimGonder() }
    }

    private fun acilBildirimGonder() {
        val baslik = "ACİL DURUM"
        val aciklama = binding.etAcilAciklama.text.toString().trim()

        if (aciklama.isEmpty() || secilenAcilKonum == null) {
            Toast.makeText(this, "Lütfen açıklama ve konum bilgilerini doldurun.", Toast.LENGTH_LONG).show()
            return
        }

        binding.acilProgress.visibility = View.VISIBLE
        binding.btnAcilYayinla.isEnabled = false

        if (secilenAcilFotoUri != null) {
            val ref = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}")
            ref.putFile(secilenAcilFotoUri!!).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    saveAcilToFirestore(baslik, aciklama, uri.toString())
                }
            }
        } else {
            saveAcilToFirestore(baslik, aciklama, null)
        }
    }

    private fun saveAcilToFirestore(baslik: String, aciklama: String, url: String?) {
        val data = hashMapOf(
            "type" to "Acil Durum",
            "title" to baslik,
            "description" to aciklama,
            "latitude" to secilenAcilKonum!!.latitude,
            "longitude" to secilenAcilKonum!!.longitude,
            "imageUrl" to url,
            "timestamp" to Date(),
            "status" to "Açık",
            "userId" to auth.currentUser?.uid
        )

        Firebase.firestore.collection("notifications").add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "ACİL DURUM YAYINLANDI!", Toast.LENGTH_LONG).show()
                binding.etAcilAciklama.text.clear()
                secilenAcilKonum = null
                secilenAcilFotoUri = null
                binding.flAcilFotoContainer.visibility = View.GONE
                binding.acilProgress.visibility = View.GONE
                binding.btnAcilYayinla.isEnabled = true
                
                val intent = Intent(this, AnasayfaActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
    }
}