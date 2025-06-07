package com.example.timevault.View

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.timevault.Model.DatabaseSignUp
import com.example.timevault.Model.StoreUploadedFiles
import com.example.timevault.Model.ThemeHelper
import com.example.timevault.R
import com.example.timevault.databinding.ActivityEditProfileBinding
import com.google.firebase.Timestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.cloudinary.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class EditProfile_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    private lateinit var database: DatabaseReference

    private lateinit var currentID: String
    private var ImageURI: Uri? = null
//    private var Editable = false


    private val imagePciker = registerForActivityResult(ActivityResultContracts.GetContent())
    { uri ->

        if (uri != null)
        {
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.account_image_vector)
                .error(R.drawable.error_vector)
                .into(binding.IvProfileImageEditProfile)

            ImageURI = uri
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharetheme = getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme = sharetheme.getString("themeOption", ThemeHelper.SYSTEM) ?: ThemeHelper.SYSTEM
        ThemeHelper.applyTheme(savedTheme)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.IbBackButtonOfEditProfile.setOnClickListener {
            finish()
        }

        database = FirebaseDatabase.getInstance().getReference("USERS")

        val share = getSharedPreferences(
            "DATA",
            Context.MODE_PRIVATE
        )
        currentID = share.getString("customuserID", null) ?: "Not Found"

        Log.d("CurrentUSerIDEditProfile", "The CurrentID inEditProfile is $currentID")

        binding.FLEditProfile.setOnClickListener {
            imagePciker.launch("image/*")
        }

        database.child(currentID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val data = snapshot.getValue(DatabaseSignUp::class.java)

                    binding.TIEnameEditProfile.setText(data?.name)
                    binding.TIEUserIDEditProfile.setText(currentID)
                    binding.TIEEmailIDShowProfile.setText(data?.email)

                    if (!isDestroyed && !isFinishing) {
                        Glide.with(this@EditProfile_Activity)
                            .load(data?.ImgUrl)
                            .placeholder(R.drawable.account_image_vector)
                            .error(R.drawable.error_vector)
                            .into(binding.IvProfileImageEditProfile)
                    }
                } else {

                    binding.TIEnameEditProfile.setText("Not Found")
                    binding.TIEUserIDEditProfile.setText("Not Found")
                    binding.TIEEmailIDShowProfile.setText("Not Found")

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("DatabaseError", "The Error is ${error.message}")
            }

        })

        binding.BtnSaveEditProfile.setOnClickListener {

            val name = binding.TIEnameEditProfile.text.toString().trim()

//            if (Editable)
//            {
//                Editable= false
//                Toast.makeText(this,"InProgress",Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            Editable = true

            val imageURi = ImageURI
            if (name.isNullOrBlank()) {
                Toast.makeText(this, "Name Field cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (imageURi != null)
            {
                uploadToCloudinary(image = imageURi,this, onSuccess = {uploadedUrl->
                    val data = mapOf(
                        "name" to name,
                        "ImgUrl" to uploadedUrl
                    )

                    database.child(currentID).updateChildren(data).addOnSuccessListener {
                        Log.d("EditProfile", "In Success Database")
                        Toast.makeText(this, "SuccessFully Update information", Toast.LENGTH_SHORT).show()

                        val share = getSharedPreferences(
                            "DATA",
                            Context.MODE_PRIVATE
                        )
                        val editor = share.edit()
                        editor.putString("name", name.toString()).apply()

                        finish()
                    }.addOnFailureListener {
                        Log.d("EditProfile", "In failure Database")
                        Toast.makeText(this, "Error : Couldn't Update Information", Toast.LENGTH_SHORT)
                            .show()
                    }

                }, onError = {
                    Toast.makeText(this,it,Toast.LENGTH_SHORT).show()
                })
            }else{
                val data = mapOf(
                    "name" to name,
                )

                database.child(currentID).updateChildren(data).addOnSuccessListener {
                    Log.d("EditProfile", "In Success Database")
                    Toast.makeText(this, "SuccessFully Update information", Toast.LENGTH_SHORT).show()

                    val share = getSharedPreferences(
                        "DATA",
                        Context.MODE_PRIVATE
                    )
                    val editor = share.edit()
                    editor.putString("name", name.toString()).apply()

                    finish()
                }.addOnFailureListener {
                    Log.d("EditProfile", "In failure Database")
                    Toast.makeText(this, "Error : Couldn't Update Information", Toast.LENGTH_SHORT)
                        .show()
                }

            }


//            val data = mapOf(
//                "name" to name
//            )

        }

    }

    private fun uploadToCloudinary(
        image: Uri,
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cloudName = "dxn2fhb7h"
        val apiKey = "475188521866227"
        val apiSecret = "zplVYyRoP9Cn43Z6JnaOicg53G8"

        val fileStream = context.contentResolver.openInputStream(image)
        val imageByte = fileStream?.readBytes()
        fileStream?.close()

        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val toSign = "timestamp=$timestamp$apiSecret"
        val signature = MessageDigest.getInstance("SHA-1")
            .digest(toSign.toByteArray())
            .joinToString (""){ "%02x".format(it)  }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file","image.jpg",RequestBody.create("image/*".toMediaTypeOrNull(),imageByte!!))
            .addFormDataPart("api_key",apiKey)
            .addFormDataPart("timestamp",timestamp)
            .addFormDataPart("signature",signature)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue( object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                onError("Upload failed : ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful)
                {
                    val body = JSONObject(response.body?.string() ?: "")
                    val imageUrl = body.getString("secure_url")
                    onSuccess(imageUrl)
                }else{
                    onError("Error : ${response.code}")
                }
            }

        })

    }
}