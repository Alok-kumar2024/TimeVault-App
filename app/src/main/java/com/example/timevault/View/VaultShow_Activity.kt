package com.example.timevault.View

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.Lottie
import com.airbnb.lottie.LottieDrawable
import com.example.timevault.Model.DownloadUtils
import com.example.timevault.Model.StoreUploadedFiles
import com.example.timevault.Model.ThemeHelper
import com.example.timevault.Model.VaultCretionFireStore
import com.example.timevault.Model.downloadandDecrypt
import com.example.timevault.Model.vaultFilesDecrypt
import com.example.timevault.R
import com.example.timevault.ViewModel.VaultShowAdapter
import com.example.timevault.ViewModel.vaultDownloadViewHolder
import com.example.timevault.ViewModel.vaultShowViewModel
import com.example.timevault.databinding.ActivityVaultShowBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class VaultShow_Activity : AppCompatActivity() {

    private lateinit var currentUserId: String
    private lateinit var password: String
    private lateinit var vaultId: String

    private lateinit var recyclerViewVaultShow: RecyclerView

    private lateinit var vaultshowadapter: VaultShowAdapter

    private lateinit var firestore: FirebaseFirestore

    private val vaultShowViewModelHolder: vaultDownloadViewHolder by viewModels()

    private lateinit var binding: ActivityVaultShowBinding

    private var fileToDownload: vaultFilesDecrypt? = null

//    private lateinit var permissionLauncher : ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharetheme = getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme =
            sharetheme.getString("themeOption", ThemeHelper.SYSTEM) ?: ThemeHelper.SYSTEM
        ThemeHelper.applyTheme(savedTheme)

        binding = ActivityVaultShowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
//        {result->
//            val allGranted = result.all { it.value }
//
//            if (allGranted)
//            {
//                fileToDownload?.let { file->
//                    DownloadUtils.downloadToDevice(this,file)
//                }
//            }else{
//                DownloadUtils.markPermissionDenied(this) // Optional tracking
//                Toast.makeText(this, "Permission Denied. Cannot save file.", Toast.LENGTH_SHORT).show()
//            }
//
//        }

//        vaultShowViewModelHolder = ViewModelProvider(this)[vaultDownloadViewHolder::class.java]

        binding.IvBackButtonVaultShow.setOnClickListener {
            finish()
        }

        recyclerViewVaultShow = binding.RvListOffileVaultShow

        recyclerViewVaultShow.layoutManager = LinearLayoutManager(this)

        firestore = FirebaseFirestore.getInstance()

        val getshare = getSharedPreferences("DATA", Context.MODE_PRIVATE)

        currentUserId = intent.getStringExtra("customuserID") ?: return

        Log.d("CustomUserIDInVaultShow", "The User ID gotten from sharedPreference $currentUserId")

        password = intent.getStringExtra("password") ?: return
        vaultId = intent.getStringExtra("vaultID") ?: return

        Log.d(
            "Password&VaultID", "The Vault Id -> $vaultId." +
                    "\nThePassword of this Vault is $password."
        )

        binding.ProgressbarAnimationShowVault.visibility = View.VISIBLE
        binding.ProgressbarAnimationShowVault.repeatCount = LottieDrawable.INFINITE
        binding.ProgressbarAnimationShowVault.playAnimation()

        firestore.collection("USERS").document(currentUserId).collection("Vaults").document(vaultId)
            .get().addOnSuccessListener { document ->

            if (document != null && document.exists()) {
                val dataList = document.toObject(VaultCretionFireStore::class.java)

                binding.TvVaultNameVaultShow.text = dataList?.vaultname.toString()

                binding.TvVaultIdVaultShow.text = "VaultID : $vaultId"
            }
        }.addOnFailureListener {
            binding.ProgressbarAnimationShowVault.cancelAnimation()
            binding.ProgressbarAnimationShowVault.visibility = View.GONE
            Toast.makeText(this, "Can't find vaults informantion", Toast.LENGTH_SHORT).show()
        }

        vaultShowViewModelHolder.showDownloadFileShow.observe(this)
        { triple ->

            decryptedandDisplay(triple)

        }

        vaultShowViewModelHolder.decryptedFiles.observe(this){file->
            showInRecycler(file)
        }

        Log.d(
            "LiveDataDebug",
            "LiveData value right after observing: ${vaultShowViewModelHolder.showDownloadFileShow.value}"
        )


        vaultShowViewModelHolder.isLoadingShow.observe(this)
        { loading ->
            if (loading && vaultShowViewModelHolder.decryptedFiles.value == null) {
                binding.ProgressbarAnimationShowVault.visibility = View.VISIBLE
                binding.ProgressbarAnimationShowVault.repeatCount = LottieDrawable.INFINITE
                binding.ProgressbarAnimationShowVault.playAnimation()

                binding.RvListOffileVaultShow.visibility = View.GONE
            } else {
                binding.RvListOffileVaultShow.visibility = View.VISIBLE

            }
        }

        vaultShowViewModelHolder.errorMessage.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        }

        vaultShowViewModelHolder.fetchDownloadFiles(currentUserId, vaultId)
//
//        firestore.collection("USERS").document(currentUserId).collection("Vaults").document(vaultId).collection("Files")
//            .get().addOnSuccessListener { document ->
//                Log.d("Check1","Inside on success")
//
//                val fileListT = document.documents.mapNotNull{ filemap->
//
//                    Log.d("Check2","Inside fileListT")
//                    val filename = filemap.getString("fileName")
//                    val fileURL = filemap.getString("fileURL")
//                    val uploadTime = filemap.getTimestamp("uploadedTime")
//
//
//                    if (filename != null && fileURL != null && uploadTime != null)
//                    {
//                        val formattedDate = SimpleDateFormat("dd MMM yyyy , HH:mm a", Locale.getDefault()).format(uploadTime.toDate())
//
//                        Triple(filename,fileURL,uploadTime)
//                    }else null
//                }
//                Log.d("DEBUG_FILE_LIST", "List size: ${fileListT.size}")
//
//                Log.d("Check3","near decryptedanddisplay")
//                decryptedandDisplay(fileListT)
//            }.addOnFailureListener {
//
//                binding.ProgressbarAnimationShowVault.cancelAnimation()
//                binding.ProgressbarAnimationShowVault.visibility = View.GONE
//                Toast.makeText(this, "Failed to load files", Toast.LENGTH_SHORT).show()
//            }


    }

    private fun decryptedandDisplay(fileList: List<Triple<String, String, Timestamp>>) {

        if (vaultShowViewModelHolder.decryptedFiles.value != null)
        {
            Log.d("Cache", "Already decrypted. Using cached files.")
            vaultShowViewModelHolder.decryptedFiles.value?.let { showInRecycler(it) }
            return
        }
        Log.d("Check4", "Inside decryptedandDisplay")
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("Check5", "Inside coroutinescope")
            val decrypedfile = fileList.mapNotNull { (name, url, time) ->
                downloadandDecrypt(url, name, password, time)
            }
            Log.d("DecryptedFileCount", "Decrypted files: ${decrypedfile.size}")

            withContext(Dispatchers.Main)
            {
                vaultShowViewModelHolder.setDecryptedFiles(decrypedfile)
            }

        }
    }

    private fun showInRecycler(decrypedfile : List<vaultFilesDecrypt>)
    {
        Log.d("Check6", "Inside withcontext Dispatchers")
        vaultshowadapter = VaultShowAdapter(decrypedfile, onDownloadClick = { file ->
            Log.d("Downloads", "clicked Download Button")

            val mimeType = when {
                file.file?.endsWith(".pdf") == true -> "application/pdf"
                file.file?.endsWith(".jpg") == true || file.file?.endsWith(".jpeg") == true -> "image/jpeg"
                file.file?.endsWith(".png") == true -> "image/png"
                file.file?.endsWith(".mp4") == true -> "video/mp4"
                else -> "*/*"
            }


            DownloadUtils.checkPermissionAndSave(this@VaultShow_Activity, file, mimeType)
        })

        recyclerViewVaultShow.adapter = vaultshowadapter
        vaultshowadapter.notifyDataSetChanged()

        binding.ProgressbarAnimationShowVault.cancelAnimation()
        binding.ProgressbarAnimationShowVault.visibility = View.GONE

    }

}