package com.example.timevault.Model

import java.io.File

//enum class fileType{IMAGE,VIDEO,PDF}

data class vaultFileItem(
    var file : File,
    var fileName : String,
//    var type : fileType
    )
