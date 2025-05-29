package com.example.timevault.Model

import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class vaultFilesDecrypt(
    var file : String? =null,
    var decryptedByte : ByteArray? = null,
    var uploadTime : Timestamp? = null
)

suspend fun downloadandDecrypt(
    url : String,
    filename : String,
    password : String,
    uploadTime : Timestamp
) : vaultFilesDecrypt? = withContext(Dispatchers.IO)
{
    try {

        Log.d("DEBUG_DECRYPT", "Decrypted file: $filename , password : $password")

        val request = Request.Builder().url(url).build()
        val response = OkHttpClient().newCall(request).execute()

        if (!response.isSuccessful)
        {
            Log.e("DEBUG_DECRYPT", "Failed to download file: $filename. HTTP code: ${response.code}")
            return@withContext null
        }
        val encryptbyte = response.body?.bytes() ?: run {
            Log.e("DEBUG_DECRYPT", "Empty body for file: $filename")
            return@withContext null
        }
        val decryptBye = decryptFile(encryptbyte,password)
        Log.d("DEBUG_DECRYPT", "Decryption successful for file: $filename")

        vaultFilesDecrypt(filename,decryptBye,uploadTime)

    }catch (e : Exception)
    {
        Log.e("DEBUG_DECRYPT", "Error decrypting $filename: ${e.message}", e)
        null
    }
}

fun decryptFile(encrypArray : ByteArray , password : String) : ByteArray
{
    return try {
        val keyByte = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyByte,"AES")

        val iv = encrypArray.sliceArray(0 until 16)
        val encryptedData = encrypArray.sliceArray(16 until encrypArray.size)


        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivSpec = javax.crypto.spec.IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE,secretKey,ivSpec)

        cipher.doFinal(encryptedData)
    }catch (e : Exception)
    {
        Log.e("DECRYPT_ERROR", "Decryption failed: ${e.message}", e)
        throw e
    }
}

