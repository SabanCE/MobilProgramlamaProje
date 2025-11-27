package com.example.mobilprogramlamaproje

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Notification(
    @get:Exclude var id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "",
    val status: String = "",
    val authorId: String = "",
    val timestamp: Timestamp? = null
)
