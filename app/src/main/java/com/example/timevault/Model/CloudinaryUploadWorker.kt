package com.example.timevault.Model

import android.annotation.SuppressLint
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.content.Context
import android.util.Log
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.cloudinary.json.JSONObject
import java.io.File


class CloudinaryUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val firestore = FirebaseFirestore.getInstance()


    override suspend fun doWork(): Result {
        val path = inputData.getString("filepath") ?: return Result.failure()
        Log.d("CloudinaryUploadWorker", "Uploading file at path: $path")

        val password = inputData.getString("password") ?: return Result.failure()
        val userId = inputData.getString("USERID") ?: return Result.failure()
        val vaultID = inputData.getString("VAULTID") ?: return Result.failure()

        val folderPath = "USERS/$userId/$vaultID"

        val originalfile = File(path)

//        val encrpytedFile = File(applicationContext.cacheDir, "enc${originalfile.name}")

        val originalName = inputData.getString("ORIGINAL_NAME") ?: originalfile.name


        return try {
            val encrypted = EncryptionUtils.encryption(originalfile, applicationContext, password)

//            val cloudinary = Cloudinary(
//                ObjectUtils.asMap(
//                    "cloud_name", "dxn2fhb7h",
//                    "api_key", "475188521866227",
//                    "api_secret", "zplVYyRoP9Cn43Z6JnaOicg53G8"
//                )
//            )
//
//            val uploadResult = cloudinary.uploader().upload(
//                encrypted, ObjectUtils.asMap(
//                    "folder", folderPath,
//                    "use_filename", true,
//                    "unique_filename", false,
//                    "overwrite", true,
//                    "resource_type", "raw",
//                    "public_id",File(originalName).nameWithoutExtension
//                )
//            )

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    encrypted.name,
                    encrypted.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
                .addFormDataPart("upload_preset","Time_Vault_App")
                .addFormDataPart("asset_folder", folderPath)
                .addFormDataPart("public_id",File(originalName).nameWithoutExtension)
                .addFormDataPart("resource_type","raw")
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/dxn2fhb7h/raw/upload")
                .post(requestBody)
                .build()

            val client = OkHttpClient()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful)
            {
                Log.e("CloudinaryUploadWorker", "Upload failed with code: ${response.code}")
                return Result.failure()
            }

            val responseBody = response.body?.string()
            val json = JSONObject(responseBody ?: "{}")
            val secureUrl = json.getString("secure_url") ?: "Not Got"


            Log.d("Cloudinary",
                    "Upload success: $secureUrl"
                )

            val data = StoreUploadedFiles(
                originalName,
                secureUrl, Timestamp.now()
            )

            firestore.collection("USERS").document(userId).collection("Vaults").document(vaultID)
                .collection("Files").add(data).addOnSuccessListener {
                    Log.d(
                        "FireStoreCloudinary",
                        "SuccessFully Uploaded Cloudinary URL in Firestore"
                    )
                }.addOnFailureListener {
                    Log.d(
                        "FireStoreCloudinary",
                        "Failure : Couldn't Upload Cloudinary URL in Firestore"
                    )
                }

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("CloudinaryUploadWorker", "Upload failed", e)
            Result.failure()
        }

    }


//    private fun uploadToCloudinary(file: File): Boolean {
//        return try {
//            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
//                .addFormDataPart("upload_preset", "your_upload_preset").build()
//
//            val request =
//                Request.Builder().url("https://api.cloudinary.com/v1_1/dxn2fhb7h/auto/upload")
//                    .post(body).build()
//
//            val client = OkHttpClient()
//            val response = client.newCall(request).execute()
//
//            response.isSuccessful
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//            false
//
//        }
//    }

}