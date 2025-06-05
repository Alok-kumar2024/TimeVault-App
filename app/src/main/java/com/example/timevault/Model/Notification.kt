package com.example.timevault.Model

import com.google.firebase.Timestamp

data class Notification(
    var notificationId : String? = "",
    var title : String? = "",
    var body : String? = "",
    var timestamp : Timestamp? = Timestamp.now(),
    var seen : Boolean? = false
)
