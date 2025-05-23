package com.example.timevault.View

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.timevault.Model.CloudinaryUploadWorker
import com.example.timevault.Model.VaultCretionFireStore
import com.example.timevault.Model.useGenerateID
import com.example.timevault.Model.vaultFileItem
import com.example.timevault.R
import com.example.timevault.ViewModel.VaultFileAdapter
import com.example.timevault.databinding.ActivityVaultCreationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.util.UUID
import kotlin.uuid.Uuid

class vaultCreationActivity : AppCompatActivity() {

    private lateinit var imageAdapter: VaultFileAdapter
    private lateinit var videoAdapter: VaultFileAdapter
    private lateinit var pdfAdapter: VaultFileAdapter

    private lateinit var binding: ActivityVaultCreationBinding

    private lateinit var firestore: FirebaseFirestore
    private lateinit var database : DatabaseReference

    val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid


    private val imageFiles = mutableListOf<vaultFileItem>()
    private val videoFiles = mutableListOf<vaultFileItem>()
    private val PDFFiles = mutableListOf<vaultFileItem>()

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments())
        { uris ->

            uris?.forEach { uri ->
                uriToFile(uri)?.let { file ->
                    val name = getFileName(uri)
                    val item = vaultFileItem(file, name)

                    imageFiles.add(item)
                }

            }
            imageAdapter.notifyDataSetChanged()

//        val selectedImages = imageFiles
//        val names = selectedImages.joinToString("\n"){it.name}
//        findViewById<TextView>(R.id.TVShowImage).text = "Selected Images : \n$names"

        }

    private val videoPicker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments())
        { uris ->
            uris?.forEach { uri ->
                uriToFile(uri)?.let { file ->
                    val name = getFileName(uri)
                    val item = vaultFileItem(file, name)
                    videoFiles.add(item)
                }
            }
            videoAdapter.notifyDataSetChanged()

//        val selectedVideos = videoFiles
//        val names = selectedVideos.joinToString("\n"){it.name}
//        findViewById<TextView>(R.id.TVShowVideo).text = "Selected Videos : \n$names"

        }

    private val pdfPicker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments())
        { uris ->
            uris?.forEach { uri ->
                uriToFile(uri)?.let { file ->
                    val name = getFileName(uri)
                    val item = vaultFileItem(file, name)

                    PDFFiles.add(item)
                }
            }
            pdfAdapter.notifyDataSetChanged()

//        val selectedPDFs = PDFFiles
//        val names = selectedPDFs.joinToString("\n"){it.name}
//        findViewById<TextView>(R.id.TVShowPdf).text = "Selected PDF's : \n$names"

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVaultCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance().getReference("USERS")

        val getdata = getSharedPreferences("DATA", MODE_PRIVATE)
        val userID = getdata.getString("customuserID",null) ?: "Not Received"

        Log.d("USERIDvault","The Custom userid from sharedPreference is $userID")

        binding.IbVaultBackButton.setOnClickListener {
            finish()
        }

        imageAdapter = VaultFileAdapter(
            imageFiles,
            R.drawable.vault_image_vector,
            onDeleteClick = { item -> imageFiles.remove(item) })


        videoAdapter = VaultFileAdapter(
            videoFiles,
            R.drawable.vault_videofile_vector,
            onDeleteClick = { item -> videoFiles.remove(item) })

        pdfAdapter = VaultFileAdapter(
            PDFFiles,
            R.drawable.vault_pdf_vector,
            onDeleteClick = { item -> PDFFiles.remove(item) })

        binding.RvShowImages.layoutManager = LinearLayoutManager(this)
        binding.RvShowImages.adapter = imageAdapter

        binding.RvShowVideos.layoutManager = LinearLayoutManager(this)
        binding.RvShowVideos.adapter = videoAdapter

        binding.RvShowPDFs.layoutManager = LinearLayoutManager(this)
        binding.RvShowPDFs.adapter = pdfAdapter



        binding.btnVaultUploadImage.setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }

        binding.btnVaultUploadVideo.setOnClickListener {
            videoPicker.launch(arrayOf("video/*"))
        }

        binding.btnVaultUploadPDF.setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        binding.BtnCreateVault.setOnClickListener {

            val vaultName = binding.TIEvaultName.text.toString()
            val vaultPassword = binding.TIEvaultPassword.text.toString()
            val vaultConfirmPassword = binding.TIEvaultConfirmPassword.text.toString()
            val vaultDescription = binding.TIEvaultDescription.text.toString()
            val vaultEmailRecipent = binding.TIEvaultEmailRecipent.text.toString()
            val vaultUnlockTime = binding.TvUnlockTime.text.toString()

            if (vaultName.isEmpty() || vaultPassword.isEmpty() || vaultDescription.isEmpty()) {
                Toast.makeText(this, "Star Fields Cannto be Empty", Toast.LENGTH_SHORT).show()
            } else {
                if (vaultPassword == vaultConfirmPassword) {

                    val uniqueid = useGenerateID(vaultName)
                    val data = VaultCretionFireStore(
                        uniqueid,
                        vaultName,
                        vaultPassword,
                        vaultDescription,
                        vaultEmailRecipent,
                        vaultUnlockTime
                    )



                    firestore.collection("USERS").document(userID).set(data)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    this,
                                    "SuccessFully Uploaded to FireStore",
                                    Toast.LENGTH_SHORT
                                ).show()
                                //will write
                                val allFiles = imageFiles + videoFiles + PDFFiles
                                Log.d("UploadFiles", "Total files to upload: ${allFiles.size}")
                                allFiles.forEach { (file, _) ->
                                    startUploadWorker(file, vaultPassword, uniqueid, userID)
                                }

                                finish()

                            } else {
                                Toast.makeText(
                                    this,
                                    "Error : Couldn't Uploaded to FireStore",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // will Write
                            }
                        }


                } else {
                    Toast.makeText(
                        this,
                        "Password and Confirm password Not Matching",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    private fun getFileName(uri: Uri): String {
        var name = "unknow"

        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

//    private fun updateFileListUI() {
//        val imageNames = imageFiles.joinToString("\n\n") { "${it.second} " }
//        val videoNames = videoFiles.joinToString("\n\n") { "${it.second} " }
//        val pdfNames = PDFFiles.joinToString("\n\n") { "${it.second} " }
//
//        findViewById<TextView>(R.id.TVShowImage).text =
//            if (imageNames.isNotEmpty()) "Images : \n\n$imageNames" else "No Image Selected"
//
//        findViewById<TextView>(R.id.TVShowVideo).text =
//            if (videoNames.isNotEmpty()) "Videos : \n\n$videoNames" else "No Video Selected"
//
//        findViewById<TextView>(R.id.TVShowPdf).text =
//            if (pdfNames.isNotEmpty()) "PDF's : \n\n$pdfNames" else "No Pdf Selected"
//    }

    private fun startUploadWorker(
        file: File,
        password: String,
        uniqueID: String,
        currentuserid: String
    ) {
        val data = workDataOf(
            "filepath" to file.absolutePath,
            "password" to password,
            "USERID" to currentuserid,
            "VAULTID" to uniqueID
        )

        val uploadwork = OneTimeWorkRequestBuilder<CloudinaryUploadWorker>()
            .setInputData(data)
            .addTag("upload_${file.name}_${UUID.randomUUID()}")
            .build()

        WorkManager.getInstance(this).enqueue(uploadwork)

        Log.d("UploadWorker", "Starting upload worker for file: ${file.name}")

    }

    private fun uriToFile(uri: Uri): File? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("Temp", null, cacheDir)
        tempFile.outputStream().use { fileout ->
            inputStream.copyTo(fileout)
        }

        return tempFile
    }
}