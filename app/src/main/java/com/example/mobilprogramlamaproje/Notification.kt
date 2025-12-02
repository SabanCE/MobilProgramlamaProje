package com.example.mobilprogramlamaproje

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude


/**
 * Firestore'daki 'notifications' koleksiyonundaki bir dokümanı temsil eden veri sınıfı.
 */
data class Notification(
    @get:Exclude
    var id: String = "",
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val type: String? = null,
    val timestamp: Timestamp? = null,
    val userId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    // YENİ: Bildirimi takip eden kullanıcıların ID listesi
    val followers: List<String> = emptyList()
)
