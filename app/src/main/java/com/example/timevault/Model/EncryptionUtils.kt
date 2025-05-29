package com.example.timevault.Model

import com.google.firebase.database.core.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {

//    fun encryptFile(inputFile: File, outputFile: File, password: String) : Boolean
//    {
//       return try {
//           val keybyte = password.padEnd(16, '0').toByteArray()
//           val secretKey = SecretKeySpec(keybyte,"ASE")
//           val cipher = Cipher.getInstance("ASE")
//           cipher.init(Cipher.ENCRYPT_MODE,secretKey)
//
//           val inputstream = FileInputStream(inputFile)
//           val cipherout = CipherOutputStream(FileOutputStream(outputFile),cipher)
//
//           inputstream.copyTo(cipherout)
//
//           inputstream.close()
//           cipherout.close()
//
//           true
//        }catch (e : Exception)
//        {
//            e.printStackTrace()
//            false
//        }
//    }

    fun encryption(inputfile :File , context : android.content.Context, password : String) : File
    {
        val secretKey = generateKey(password)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        java.security.SecureRandom().nextBytes(iv)
        val ivSpec = javax.crypto.spec.IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE,secretKey,ivSpec)

        val inputBytes = inputfile.readBytes()
        val encryptedBytes = cipher.doFinal(inputBytes)

        val ivPlusencrypted = iv + encryptedBytes

        val encryptedFile = File(context.cacheDir, inputfile.name)
        encryptedFile.writeBytes(ivPlusencrypted)

        return encryptedFile
    }

    private fun generateKey(password: String): SecretKeySpec {
        val keybyte =
            MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))

        return SecretKeySpec(keybyte,"AES")
    }
}