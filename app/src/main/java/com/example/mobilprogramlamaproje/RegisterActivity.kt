package com.example.mobilprogramlamaproje

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobilprogramlamaproje.databinding.RegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: RegisterBinding
    private lateinit var auth: FirebaseAuth // Firebase Authentication nesnesi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth // Firebase Authentication nesnesini başlat

        binding.registerButton.setOnClickListener {
            binding.registerButton.isEnabled = false // Butonu devre dışı bırak

            val nameSurname = binding.nameSurnameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val role = binding.birimInput.text.toString().trim()
            val unit = "user" // Varsayılan birim değeri



            if (nameSurname.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && role.isNotEmpty()) {
                // Kayıt İşlemi
                auth.createUserWithEmailAndPassword(email, password) //sadece email ve password kaydı olabilir burada diğerleri sonra
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                val userMap = hashMapOf(
                                    "nameSurname" to nameSurname,
                                    "email" to email,
                                    "role" to role,
                                    "password" to password, //güvenlik açığı
                                    "unit" to unit
                                )
                                val db = Firebase.firestore
                                db.collection("users").document(user.uid)
                                    .set(userMap)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Kayıt Başarılı! Yönlendiriliyorsunuz...", Toast.LENGTH_LONG).show()

                                        Handler(Looper.getMainLooper()).postDelayed({
                                            val intent = Intent(this, MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        }, 2000)
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Firestore hata: ${e.message}", Toast.LENGTH_LONG).show()
                                        e.printStackTrace()
                                        binding.registerButton.isEnabled = true
                                    }

                            }
                        } else {
                            // Authentication hatası
                            Toast.makeText(this, "Kayıt Başarısız: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            binding.registerButton.isEnabled = true
                        }
                    }
            } else {
                Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                binding.registerButton.isEnabled = true
            }
        }
    }
}