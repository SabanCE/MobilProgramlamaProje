package com.example.mobilprogramlamaproje

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
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

        binding.buttonCikisYap.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.headerProfilBilgileri.setOnClickListener {
            toggleVisibility(binding.contentProfilBilgileri)
        }

        binding.headerBildirimAyarlari.setOnClickListener {
            val content = binding.contentBildirimAyarlari
            toggleVisibility(content)
            if (content.visibility == View.VISIBLE) {
                loadNotificationSettings()
            }
        }

        binding.headerTakipEdilenBildirimler.setOnClickListener {
            val content = binding.contentTakipEdilenBildirimler
            toggleVisibility(content)
            if (content.visibility == View.VISIBLE) {
                loadFollowedNotifications()
            }
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
            val db = Firebase.firestore
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val adSoyad = document.getString("nameSurname") ?: ""
                        val email = user.email
                        val rol = document.getString("role") ?: ""
                        val birim = document.getString("unit") ?: ""

                        binding.textViewAdSoyad.text = "Ad Soyad: $adSoyad"
                        binding.textViewEmail.text = "E-posta: $email"
                        binding.textViewKullaniciRolu.text = "Kullanıcı Rolü: $rol"
                        binding.textViewBirim.text = "Birim: $birim"
                    }
                }
        }
    }

    private fun loadFollowedNotifications() {
        val user = auth.currentUser
        if (user != null) {
            val db = Firebase.firestore
            db.collection("notifications")
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
            val db = Firebase.firestore
            db.collection("user_settings").document(user.uid).get()
                .addOnSuccessListener { document ->
                    isUpdatingCheckboxes = true
                    if (document != null && document.exists() && (document.get("notification_types") as? List<*>)?.isNotEmpty() == true) {
                        val settings = document.get("notification_types") as List<String>
                        binding.cbSaglik.isChecked = settings.contains("Sağlık")
                        binding.cbGuvenlik.isChecked = settings.contains("Güvenlik")
                        binding.cbCevre.isChecked = settings.contains("Çevre")
                        binding.cbKayipBuluntu.isChecked = settings.contains("Kayıp-Buluntu")
                        binding.cbTeknikAriza.isChecked = settings.contains("Teknik Arıza")
                    } else {
                        // Varsayılan olarak hepsini seç
                        setAllCheckBoxes(true)
                    }
                    updateHepsiCheckboxState()
                    isUpdatingCheckboxes = false
                    setupCheckboxListeners()
                }
        }
    }

    private fun setupCheckboxListeners() {
        val otherCheckBoxes = listOf(binding.cbSaglik, binding.cbGuvenlik, binding.cbCevre, binding.cbKayipBuluntu, binding.cbTeknikAriza)

        binding.cbHepsi.setOnClickListener { 
            if (isUpdatingCheckboxes) return@setOnClickListener

            val wasChecked = !binding.cbHepsi.isChecked
            if (wasChecked) { 
                binding.cbHepsi.isChecked = true
                return@setOnClickListener
            }
            
            isUpdatingCheckboxes = true
            otherCheckBoxes.forEach { it.isChecked = true }
            isUpdatingCheckboxes = false
            saveNotificationSettings()
        }

        otherCheckBoxes.forEach { checkBox ->
            checkBox.setOnClickListener { 
                if (isUpdatingCheckboxes) return@setOnClickListener
                
                val totalChecked = otherCheckBoxes.count { it.isChecked }
                if (totalChecked == 0) { 
                    checkBox.isChecked = true
                    return@setOnClickListener
                }

                updateHepsiCheckboxState()
                saveNotificationSettings()
            }
        }
    }
    
    private fun updateHepsiCheckboxState() {
        isUpdatingCheckboxes = true
        val otherCheckBoxes = listOf(binding.cbSaglik, binding.cbGuvenlik, binding.cbCevre, binding.cbKayipBuluntu, binding.cbTeknikAriza)
        binding.cbHepsi.isChecked = otherCheckBoxes.all { it.isChecked }
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

        val selectedTypes = ArrayList<String>()
        if (binding.cbSaglik.isChecked) selectedTypes.add("Sağlık")
        if (binding.cbGuvenlik.isChecked) selectedTypes.add("Güvenlik")
        if (binding.cbCevre.isChecked) selectedTypes.add("Çevre")
        if (binding.cbKayipBuluntu.isChecked) selectedTypes.add("Kayıp-Buluntu")
        if (binding.cbTeknikAriza.isChecked) selectedTypes.add("Teknik Arıza")
        
        val settings = hashMapOf("notification_types" to selectedTypes)
        Firebase.firestore.collection("user_settings").document(user.uid).set(settings)
    }
}