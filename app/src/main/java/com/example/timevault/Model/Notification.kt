package com.example.timevault.Model

import android.provider.Settings
import com.google.firebase.Timestamp

data class Notification(
    var notificationId : String? = "",
    var title : String? = "",
    var body : String? = "",
    var timestamp : Long = System.currentTimeMillis(),
    var seen : Boolean = false
)
