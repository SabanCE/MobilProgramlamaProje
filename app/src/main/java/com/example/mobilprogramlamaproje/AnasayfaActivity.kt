package com.example.mobilprogramlamaproje

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobilprogramlamaproje.databinding.ActivityAnasayfaBinding
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import com.google.firebase.auth.ktx.auth // Bu import satırı önemli!
import com.google.firebase.ktx.Firebase

class AnasayfaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnasayfaBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnasayfaBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = Firebase.auth
        binding.cikisyapButon.setOnClickListener {

            val intent = Intent(this, MainActivity::class.java)
            auth.signOut()
            startActivity(intent)
            finish()
        }

    }
}