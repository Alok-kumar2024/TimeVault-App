package com.example.timevault.Model

import com.google.firebase.Timestamp


data class StoreUploadedFiles(
    var fileName : String ,
    var fileURL : String,
    var uploadedTime : Timestamp,
)
