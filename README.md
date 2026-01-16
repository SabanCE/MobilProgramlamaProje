🛡️ Kampüs İçi Bildirim ve Güvenlik Sistemi
Kotlin Firebase Android SDK
Üniversite kampüsleri için geliştirilmiş, sorunların ve acil durumların konum tabanlı olarak raporlanmasını ve yönetilmesini sağlayan modern bir Android uygulamasıdır.
🚀 Özellikler
👤 Kullanıcı Özellikleri
•
Giriş/Kayıt: Firebase Auth ile güvenli kimlik doğrulama.
•
Konum Tabanlı Bildirim: Sorunları (Teknik, Temizlik, Güvenlik vb.) harita üzerinden konum seçerek veya cihaz konumunu kullanarak raporlama.
•
Fotoğraf Kanıtı: Bildirimlere Firebase Storage üzerinden fotoğraf ekleyebilme.
•
Gerçek Zamanlı Takip: Bildirimlerin durumunu (Açık, İnceleniyor, Çözüldü) anlık olarak izleme.
•
Akıllı Filtreleme: Arama çubuğu ve kategorik filtreler (Chip) ile bildirimlere hızlı erişim.
•
Kişiselleştirilmiş Bildirim Ayarları: Sadece ilgi duyulan kategorilerdeki bildirimleri ana ekranda görme tercihi.
🔑 Admin Özellikleri (Rol Tabanlı Yetkilendirme)
•
Durum Yönetimi: Bildirimlerin çözüm sürecini anlık olarak güncelleme.
•
Acil Durum Yayınlama: Tüm kullanıcılara anlık giden ve ana ekranda sabitlenen kırmızı öncelikli bildirimler oluşturma.
•
İçerik Denetimi: Gereksiz veya asılsız bildirimleri sistemden tamamen silebilme.
•
Kullanıcı Takibi: Bildirimi oluşturan kullanıcıların detaylarını görüntüleme yetkisi.
🛠️ Kullanılan Teknolojiler
•
Dil: Kotlin
•
Veritabanı: Firebase Firestore (NoSQL, Real-time)
•
Kimlik Doğrulama: Firebase Auth
•
Dosya Saklama: Firebase Storage (Fotoğraflar için)
•
Harita Servisleri: Google Maps SDK & Google Play Services Location
•
Görsel İşleme: Glide (Resimlerin verimli yüklenmesi ve önbelleğe alınması)
•
UI Bileşenleri: Material Design Components, RecyclerView, ViewBinding, ConstraintLayout, Lottie (Opsiyonel)
📸 Ekran Görüntüleri
| Ana Ekran (Harita + Liste) | Yeni Bildirim Oluştur | Bildirim Detayı (Admin) | | :---: | :---: | :---: | | Ana Ekran | Ekleme | Detay |
🏗️ Proje Yapısı
app/src/main/java/com/example/mobilprogramlamaproje/
├── AnasayfaActivity.kt         # Ana ekran mantığı, filtreleme ve harita
├── BildirimEkleActivity.kt     # Yeni bildirim oluşturma, konum ve fotoğraf seçimi
├── NotificationDetailActivity.kt # Bildirim detayları ve admin yönetim araçları
├── NotificationsAdapter.kt     # Dinamik renklendirme (when bloğu) ve liste yönetimi
└── Register/LoginActivities.kt # Kimlik doğrulama işlemleri
⚙️ Kurulum
1.
Bu projeyi bilgisayarınıza indirin veya clone'layın.
2.
Android Studio ile projeyi açın.
3.
Firebase Console üzerinden bir proje oluşturun.
4.
google-services.json dosyanızı indirin ve app/ dizinine kopyalayın.
5.
Google Cloud Console'dan Maps SDK anahtarınızı alın ve AndroidManifest.xml içine ekleyin:
Manifest
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="SİZİN_API_ANAHTARINIZ" />
Merge Into Manifest
6.
Projeyi derleyin ve çalıştırın.
