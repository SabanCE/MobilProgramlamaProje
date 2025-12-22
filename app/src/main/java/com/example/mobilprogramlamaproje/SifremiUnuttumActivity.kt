package com.example.mobilprogramlamaproje
//Eposta yenileme sayfası
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobilprogramlamaproje.databinding.SifremiUnuttumBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SifremiUnuttumActivity : AppCompatActivity() {

    private lateinit var binding: SifremiUnuttumBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SifremiUnuttumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        binding.backButton.setOnClickListener { finish() }

        binding.sendResetEmailButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()

            if (email.isNotEmpty()) {
                binding.sendResetEmailButton.isEnabled = false //buton devre dişi
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Sıfırlama e-postası gönderildi", Toast.LENGTH_SHORT).show()

                        }
                        else{
                            Toast.makeText(this, "Sıfırlama e-postası gönderme başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()

                        }
                        binding.sendResetEmailButton.isEnabled = true
                    }
            } else {
                Toast.makeText(this, "Lütfen e-posta adresinizi girin", Toast.LENGTH_SHORT).show()
            }
        }
    }
}