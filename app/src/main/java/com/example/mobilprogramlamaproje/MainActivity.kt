package com.example.mobilprogramlamaproje
//Login Ekranı
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobilprogramlamaproje.databinding.ActivityMainBinding
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth // Firebase Authentication nesnesi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth=Firebase.auth

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Eğer Kullanıcı girmiş ise sisteme
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Toast.makeText(this, "Giriş Yapıldı", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AnasayfaActivity::class.java)
            startActivity(intent)
            finish()
        }



        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim() //trim boşlukları siler
            val password = binding.passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()){
                // Giriş yap kontrolü
                binding.loginButton.isEnabled = false // Butona tekrar basılmasın
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener (this){ task ->
                        if (task.isSuccessful){ //Giriş Başarılı ise
                            Toast.makeText(this, "Giriş Başarılı", Toast.LENGTH_SHORT).show()
                           //anasayfaya yönlendiriyor
                            val intent = Intent(this, AnasayfaActivity::class.java)
                            startActivity(intent)
                            finish()
                        }else{

                            try {
                                throw task.exception!!
                            } catch (e: FirebaseAuthInvalidUserException) {
                                Toast.makeText(this, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
                            } catch (e: FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(this, "Geçersiz e-posta veya şifre", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(this, "Giriş başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            binding.loginButton.isEnabled = true // Butonu tekrar etkinleştir

                        }
}
            } else {
                Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            }
        }

        binding.registerTextButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.forgotPasswordText.setOnClickListener {
            val intent = Intent(this, SifremiUnuttumActivity::class.java)
            startActivity(intent)
        }
    }
}