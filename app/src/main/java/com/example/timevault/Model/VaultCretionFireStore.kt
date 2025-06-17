package com.example.timevault.Model

import com.google.firebase.Timestamp

data class VaultCretionFireStore(
    var uniqueID : String? = null,
    var vaultname: String?,
    var vaultpassword: String?,
    var description: String?,
    var emailrecipent: String?,
    var unlockTime: Timestamp? = null,
    var status : String? = null,
    var unlocked : Boolean? = null,
    val lastAccessedTime: Timestamp?= null,
    val pinned: Boolean = false
)
{
    constructor() : this(null,null,null,null,null,null,null)
}
