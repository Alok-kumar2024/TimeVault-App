package com.example.timevault.Model

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

object DownloadUtils {

//    fun hasAllPermission(context: Context): Boolean {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
//                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
//                    (Build.VERSION.SDK_INT > Build.VERSION_CODES.P || ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
//        } else {
//            true // Permissions are granted by default on Android Lollipop and below
//        }
//    }
//
//    fun getNeededPermission(): List<String> {
//        val list = mutableListOf<String>()
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            list.add(Manifest.permission.READ_MEDIA_IMAGES)
//            list.add(Manifest.permission.READ_MEDIA_VIDEO)
//        } else {
//            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
//
//            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
//                list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//            }
//        }
//        return list
//    }
//
//    fun markPermissionDenied(context: Context) {
//        getPrefs(context).edit().putBoolean("permission_denied", true).apply()
//    }
//
//    fun wasPermissionDenied(context: Context): Boolean {
//        return getPrefs(context).getBoolean("permission_denied", false)
//    }
//
//    private fun getPrefs(context: Context): SharedPreferences {
//        return context.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
//    }


    val STORAGE_PERMISSION_CODE = 101

    fun checkPermissionAndSave(context: Context, file: vaultFilesDecrypt, mimeType: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            } else {
                CoroutineScope(Dispatchers.IO).launch { downloadToDevice(context, file, mimeType) }
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch { downloadToDevice(context, file, mimeType) }
        }
    }


//    fun downloadToDevice(context: Context, file: vaultFilesDecrypt) {
//
//        try {
//            val downloaddir =
//                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//            val outfiles = File(downloaddir, file.file)
//
//            FileOutputStream(outfiles).use { it.write(file.decryptedByte) }
//            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
//        } catch (e: Exception) {
//            Toast.makeText(context, "Error saving file", Toast.LENGTH_SHORT).show()
//        }
//    }

    suspend fun downloadToDevice(context: Context, file: vaultFilesDecrypt, mimeType: String) {
        val fileName = file.file

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, contentValues)

            uri?.let {
                resolver.openOutputStream(uri)?.use { output ->
                    output.write(file.decryptedByte)
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                withContext(Dispatchers.Main)
                {
                    Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                withContext(Dispatchers.Main)
                {
                    Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                }
            }

        } else {
            // Android 9 and below — manual file write (needs permission)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outFile = File(downloadsDir, fileName)

            try {
                FileOutputStream(outFile).use { it.write(file.decryptedByte) }

                withContext(Dispatchers.Main)
                {
                    Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main)
                {
                    Toast.makeText(context, "Error saving file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}