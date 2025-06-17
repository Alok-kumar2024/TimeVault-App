package com.example.timevault.View

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieDrawable
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var isEditableClicked = false

    private var clickedRemove = false


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
                            .load(data?.imgUrl)
                            .placeholder(R.drawable.account_image_vector)
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

        binding.BtnRemoveProfileImage.setOnClickListener {

            clickedRemove = true
            Glide.with(this)
                .load("")
                .placeholder(R.drawable.profile_image_vector)
                .into(binding.IvProfileImageEditProfile)

            ImageURI = null
        }

        binding.TIEnameEditProfile.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
            {
                binding.ScrollViewEditProfile.post{
                    binding.ScrollViewEditProfile.smoothScrollBy(0, binding.TIEnameEditProfile.top)
                }
            }
        }

        binding.BtnSaveEditProfile.setOnClickListener {

            binding.ProgressbarAnimationEditProfile.visibility = View.VISIBLE
            binding.ProgressbarAnimationEditProfile.repeatCount = LottieDrawable.INFINITE
            binding.ProgressbarAnimationEditProfile.playAnimation()

            if (isEditableClicked) {
                Toast.makeText(this,"Currently In Progress, Please Wait Before Clicking Again",Toast.LENGTH_SHORT).show()
                binding.ProgressbarAnimationEditProfile.visibility = View.GONE
                binding.ProgressbarAnimationEditProfile.cancelAnimation()
                return@setOnClickListener
            }

            val name = binding.TIEnameEditProfile.text.toString().trim()

            val imageURi = ImageURI
            if (name.isNullOrBlank()) {
                Toast.makeText(this, "Name Field cannot be empty", Toast.LENGTH_SHORT).show()
                isEditableClicked = false
                binding.ProgressbarAnimationEditProfile.visibility = View.GONE
                binding.ProgressbarAnimationEditProfile.cancelAnimation()
                return@setOnClickListener
            }
            if (imageURi != null)
            {
                CoroutineScope(Dispatchers.IO).launch {
                    uploadToCloudinary(image = imageURi,this@EditProfile_Activity, onSuccess = {uploadedUrl->
                        val data = mapOf(
                            "name" to name,
                            "imgUrl" to uploadedUrl
                        )

                        database.child(currentID).updateChildren(data).addOnSuccessListener {
                            Log.d("EditProfile", "In Success Database")
                            Toast.makeText(this@EditProfile_Activity, "SuccessFully Update information", Toast.LENGTH_SHORT).show()

                            val share = getSharedPreferences(
                                "DATA",
                                MODE_PRIVATE
                            )

                            share.edit().putString("name", name).apply()
                            isEditableClicked = false
                            binding.ProgressbarAnimationEditProfile.visibility = View.GONE
                            binding.ProgressbarAnimationEditProfile.cancelAnimation()
                            finish()
                        }.addOnFailureListener {
                            Log.d("EditProfile", "In failure Database")
                            Toast.makeText(this@EditProfile_Activity, "Error : Couldn't Update Information", Toast.LENGTH_SHORT)
                                .show()
                            isEditableClicked = false
                            binding.ProgressbarAnimationEditProfile.visibility = View.GONE
                            binding.ProgressbarAnimationEditProfile.cancelAnimation()
                        }

                    }, onError = {
                        Toast.makeText(this@EditProfile_Activity,it,Toast.LENGTH_SHORT).show()
                        isEditableClicked= false
                        binding.ProgressbarAnimationEditProfile.visibility = View.GONE
                        binding.ProgressbarAnimationEditProfile.cancelAnimation()
                    })
                }
            }else{
                val data = mapOf(
                    "name" to name,
                )

                database.child(currentID).updateChildren(data).addOnSuccessListener {
                    Log.d("EditProfile", "In Success Database")
                    Toast.makeText(this, "SuccessFully Update information", Toast.LENGTH_SHORT).show()

                    val share = getSharedPreferences(
                        "DATA",
                        MODE_PRIVATE
                    )

                    share.edit().putString("name", name).apply()

                    binding.ProgressbarAnimationEditProfile.visibility = View.GONE
                    binding.ProgressbarAnimationEditProfile.cancelAnimation()


                    finish()
                }.addOnFailureListener {
                    Log.d("EditProfile", "In failure Database")
                    Toast.makeText(this, "Error : Couldn't Update Information", Toast.LENGTH_SHORT)
                        .show()
                    isEditableClicked = false
                    binding.ProgressbarAnimationEditProfile.visibility = View.GONE
                    binding.ProgressbarAnimationEditProfile.cancelAnimation()
                }

                if (clickedRemove)
                {
                    val mapimg = mapOf(
                        "imgUrl" to ""
                    )
                    database.child(currentID).updateChildren(mapimg)
                }

            }

            isEditableClicked = true

//            val data = mapOf(
//                "name" to name
//            )

        }

    }

    private suspend fun uploadToCloudinary(
        image: Uri,
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {

            val fileStream = context.contentResolver.openInputStream(image).use {
                it?.readBytes()
            }
            val folder = "USERS/$currentID"

            val requestBuilder =
                fileStream?.let { RequestBody.create("image/*".toMediaTypeOrNull(), it) }?.let {
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file","image.jpg",
                            it
                        ).addFormDataPart("upload_preset","Time_Vault_App")
                        .addFormDataPart("asset_folder", folder)
                        .build()

                }

            val request = requestBuilder?.let {
                Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/dxn2fhb7h/image/upload")
                    .post(it)
                    .build()
            }

            val client = OkHttpClient()

            val response = request?.let { client.newCall(it).execute() }

            if (!response?.isSuccessful!!)
            {
                Log.e("uploadToCloudinary_EditProfile", "Upload failed with code: ${response.code}")
                onError(response.message)
                return
            }

            val body = JSONObject(response.body?.string() ?: "{}")
            val imageurl = body.getString("secure_url")
            onSuccess(imageurl)

        }catch (e : Exception)
        {
            e.printStackTrace()
            Log.e("CloudinaryUploadWorker", "Upload failed", e)
            e.message?.let { onError(it) }
        }

    }
}