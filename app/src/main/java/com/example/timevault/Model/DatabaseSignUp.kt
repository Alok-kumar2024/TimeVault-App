package com.example.timevault.Model

data class DatabaseSignUp(
    var userIdFireBase : String?= null,
    var name: String?= null,
    var email: String? = null,
    var imgUrl: String? = null,
    val signUpMethod: String? = null
)
