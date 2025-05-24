package com.example.timevault.Model

data class VaultCretionFireStore(
    var uniqueID : String? = null,
    var vaultname: String?,
    var vaultpassword: String?,
    var description: String?,
    var emailrecipent: String?,
    var unlockTime: String?,
    var status : String? = null
)
{
    constructor() : this(null,null,null,null,null,null,null)
}
