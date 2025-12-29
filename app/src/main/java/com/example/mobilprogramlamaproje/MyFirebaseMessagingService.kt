package com.example.mobilprogramlamaproje

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Firebase'den gelen bildirimleri uygulama kapalıyken de işlemek için
        remoteMessage.notification?.let {
            val title = it.title ?: "Bildirim"
            val body = it.body ?: ""
            // Bildirimi göster
            val helper = NotificationHelper(applicationContext)
            // FCM'den gelen veriye göre detay sayfasına yönlendirme yapılabilir
            // Şimdilik genel bir bildirim gösteriyoruz
            helper.showNotification(title, "Güncellendi", "") 
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token'ı sunucuya veya Firestore'a kaydedebilirsiniz
    }
}