package com.example.timevault.Model

import com.google.firebase.Timestamp

data class Notification(
    var title : String? = null,
    var body : String? = null,
    var timestamp : Timestamp? = null,
    var seen : Boolean? = false
)
