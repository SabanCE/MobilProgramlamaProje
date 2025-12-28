package com.example.mobilprogramlamaproje

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobilprogramlamaproje.databinding.ActivityProfileSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileSettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var followedNotificationsAdapter: NotificationsAdapter
    private var isUpdatingCheckboxes = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        setupClickListeners()
        loadUserProfile()
        setupRecyclerView()
        setupBottomNavigation()
        checkUserRole()

        binding.backButton.setOnClickListener { goToHome() }

        binding.buttonCikisYap.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToHome()
            }
        })
    }

    private fun goToHome() {
        val intent = Intent(this, AnasayfaActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_profile_settings
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { goToHome(); true }
                R.id.nav_map -> { startActivity(Intent(this, MapsActivity::class.java)); true }
                R.id.nav_create -> { startActivity(Intent(this, BildirimEkleActivity::class.java)); true }
                R.id.nav_admin_panel -> { startActivity(Intent(this, AdminPanelActivity::class.java)); true }
                R.id.nav_profile_settings -> true
                else -> false
            }
        }
    }

    private fun checkUserRole() {
        val user = auth.currentUser
        if (user != null) {
            Firebase.firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role")
                        val menu = binding.bottomNavigation.menu
                        if (role == "Admin") {
                            menu.findItem(R.id.nav_admin_panel).isVisible = true
                            menu.findItem(R.id.nav_create).isVisible = false
                            
                            binding.headerTakipEdilenBildirimler.visibility = View.GONE
                            binding.contentTakipEdilenBildirimler.visibility = View.GONE
                            binding.lineBildirim.visibility = View.GONE // Gereksiz çizgiyi kaldır
                        } else {
                            menu.findItem(R.id.nav_admin_panel).isVisible = false
                            menu.findItem(R.id.nav_create).isVisible = true
                            
                            binding.headerTakipEdilenBildirimler.visibility = View.VISIBLE
                            binding.lineBildirim.visibility = View.VISIBLE
                        }
                    }
                }
        }
    }

    private fun setupClickListeners() {
        binding.headerProfilBilgileri.setOnClickListener { toggleVisibility(binding.contentProfilBilgileri) }
        binding.headerBildirimAyarlari.setOnClickListener {
            toggleVisibility(binding.contentBildirimAyarlari)
            if (binding.contentBildirimAyarlari.visibility == View.VISIBLE) loadNotificationSettings()
        }
        binding.headerTakipEdilenBildirimler.setOnClickListener {
            toggleVisibility(binding.contentTakipEdilenBildirimler)
            if (binding.contentTakipEdilenBildirimler.visibility == View.VISIBLE) loadFollowedNotifications()
        }
    }

    private fun toggleVisibility(view: View) {
        view.visibility = if (view.visibility == View.GONE) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerView() {
        followedNotificationsAdapter = NotificationsAdapter(ArrayList())
        binding.followedNotificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.followedNotificationsRecyclerView.adapter = followedNotificationsAdapter
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            Firebase.firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        binding.textViewAdSoyad.text = "Ad Soyad: ${document.getString("nameSurname") ?: ""}"
                        binding.textViewEmail.text = "E-posta: ${user.email}"
                        binding.textViewKullaniciRolu.text = "Kullanıcı Rolü: ${document.getString("role") ?: ""}"
                        binding.textViewBirim.text = "Birim: ${document.getString("unit") ?: ""}"
                    }
                }
        }
    }

    private fun loadFollowedNotifications() {
        val user = auth.currentUser
        if (user != null) {
            Firebase.firestore.collection("notifications")
                .whereArrayContains("followers", user.uid)
                .get()
                .addOnSuccessListener { documents ->
                    val followedList = ArrayList<Notification>()
                    for (document in documents) {
                        document.toObject(Notification::class.java)?.let {
                            it.id = document.id
                            followedList.add(it)
                        }
                    }
                    followedNotificationsAdapter.updateList(followedList)
                }
        }
    }

    private fun loadNotificationSettings() {
        val user = auth.currentUser
        if (user != null) {
            Firebase.firestore.collection("user_settings").document(user.uid).get()
                .addOnSuccessListener { document ->
                    isUpdatingCheckboxes = true
                    if (document != null && document.exists()) {
                        val settings = document.get("notification_types") as? List<String>
                        if (!settings.isNullOrEmpty()) {
                            binding.cbSaglik.isChecked = settings.contains("Sağlık")
                            binding.cbGuvenlik.isChecked = settings.contains("Güvenlik")
                            binding.cbCevre.isChecked = settings.contains("Çevre")
                            binding.cbKayipBuluntu.isChecked = settings.contains("Kayıp-Buluntu")
                            binding.cbTeknikAriza.isChecked = settings.contains("Teknik Arıza")
                        } else {
                            setAllCheckBoxes(true)
                        }
                    } else {
                        setAllCheckBoxes(true)
                    }
                    updateHepsiCheckboxState()
                    isUpdatingCheckboxes = false
                    setupCheckboxListeners()
                }
        }
    }

    private fun setupCheckboxListeners() {
        val boxes = listOf(binding.cbSaglik, binding.cbGuvenlik, binding.cbCevre, binding.cbKayipBuluntu, binding.cbTeknikAriza)
        binding.cbHepsi.setOnClickListener { 
            if (isUpdatingCheckboxes) return@setOnClickListener
            val check = binding.cbHepsi.isChecked
            isUpdatingCheckboxes = true
            boxes.forEach { it.isChecked = check }
            isUpdatingCheckboxes = false
            saveNotificationSettings()
        }
        boxes.forEach { box ->
            box.setOnClickListener { 
                if (isUpdatingCheckboxes) return@setOnClickListener
                updateHepsiCheckboxState()
                saveNotificationSettings()
            }
        }
    }
    
    private fun updateHepsiCheckboxState() {
        isUpdatingCheckboxes = true
        val boxes = listOf(binding.cbSaglik, binding.cbGuvenlik, binding.cbCevre, binding.cbKayipBuluntu, binding.cbTeknikAriza)
        binding.cbHepsi.isChecked = boxes.all { it.isChecked }
        isUpdatingCheckboxes = false
    }

    private fun setAllCheckBoxes(isChecked: Boolean) {
        binding.cbHepsi.isChecked = isChecked
        binding.cbSaglik.isChecked = isChecked
        binding.cbGuvenlik.isChecked = isChecked
        binding.cbCevre.isChecked = isChecked
        binding.cbKayipBuluntu.isChecked = isChecked
        binding.cbTeknikAriza.isChecked = isChecked
    }

    private fun saveNotificationSettings() {
        val user = auth.currentUser ?: return
        val selected = ArrayList<String>()
        if (binding.cbSaglik.isChecked) selected.add("Sağlık")
        if (binding.cbGuvenlik.isChecked) selected.add("Güvenlik")
        if (binding.cbCevre.isChecked) selected.add("Çevre")
        if (binding.cbKayipBuluntu.isChecked) selected.add("Kayıp-Buluntu")
        if (binding.cbTeknikAriza.isChecked) selected.add("Teknik Arıza")
        Firebase.firestore.collection("user_settings").document(user.uid).set(hashMapOf("notification_types" to selected))
    }
}