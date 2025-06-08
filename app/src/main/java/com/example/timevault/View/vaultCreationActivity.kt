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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.timevault.Model.CloudinaryUploadWorker
import com.example.timevault.Model.ThemeHelper
import com.example.timevault.Model.VaultCretionFireStore
import com.example.timevault.Model.fileType
import com.example.timevault.Model.useGenerateID
import com.example.timevault.Model.vaultFileItem
import com.example.timevault.R
import com.example.timevault.ViewModel.VaultFileAdapter
import com.example.timevault.databinding.ActivityVaultCreationBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialStyledDatePickerDialog
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.File
import java.text.Format
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.uuid.Uuid

class vaultCreationActivity : AppCompatActivity() {

//    private lateinit var imageAdapter: VaultFileAdapter
//    private lateinit var videoAdapter: VaultFileAdapter
//    private lateinit var pdfAdapter: VaultFileAdapter

    private lateinit var selectedAdapter: VaultFileAdapter

    private lateinit var binding: ActivityVaultCreationBinding

    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: DatabaseReference

    private var selectedDate: Date? = null

    val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid


//    private val imageFiles = mutableListOf<vaultFileItem>()
//    private val videoFiles = mutableListOf<vaultFileItem>()
//    private val PDFFiles = mutableListOf<vaultFileItem>()

    private val selectedFiles = mutableListOf<vaultFileItem>()

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments())
        { uris ->
            uris?.forEach { uri ->

                uriToFile(uri)?.let { file ->

                    val name = getFileName(uri)
                    val type = contentResolver.getType(uri)

                    val fileType = when {
                        type?.startsWith("image/") == true -> fileType.IMAGE
                        type?.startsWith("video/") == true -> fileType.VIDEO
                        type == "application/pdf" -> fileType.PDF
                        else -> null
                    }

                    if ( fileType != null) {
                        if (isFileSizeAllowed(file)) {
                            selectedFiles.add(vaultFileItem(file, name, fileType))
                            Toast.makeText(
                                this,
                                "File $name added ",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {
                            Toast.makeText(
                                this,
                                "File size cant exceed a max limit of 10 MB.., file -> $name",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(this, "Error : Couldn't get the file.", Toast.LENGTH_SHORT)
                            .show()
                    }

                }
            }
            selectedAdapter.notifyDataSetChanged()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharetheme = getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme = sharetheme.getString("themeOption", ThemeHelper.SYSTEM) ?: ThemeHelper.SYSTEM
        ThemeHelper.applyTheme(savedTheme)

        binding = ActivityVaultCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.black)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance().getReference("USERS")

        val getdata = getSharedPreferences("DATA", MODE_PRIVATE)
        val userID = getdata.getString("customuserID", null) ?: "Not Received"

        Log.d("USERIDvault", "The Custom userid from sharedPreference is $userID")

        binding.IbVaultBackButton.setOnClickListener {
            finish()
        }

//        imageAdapter = VaultFileAdapter(
//            imageFiles,
//            R.drawable.vault_image_vector,
//            onDeleteClick = { item -> imageFiles.remove(item) })
//
//
//        videoAdapter = VaultFileAdapter(
//            videoFiles,
//            R.drawable.vault_videofile_vector,
//            onDeleteClick = { item -> videoFiles.remove(item) })
//
//        pdfAdapter = VaultFileAdapter(
//            PDFFiles,
//            R.drawable.vault_pdf_vector,
//            onDeleteClick = { item -> PDFFiles.remove(item) })
//
//        binding.RvShowImages.layoutManager = LinearLayoutManager(this)
//        binding.RvShowImages.adapter = imageAdapter
//
//        binding.RvShowVideos.layoutManager = LinearLayoutManager(this)
//        binding.RvShowVideos.adapter = videoAdapter
//
//        binding.RvShowPDFs.layoutManager = LinearLayoutManager(this)
//        binding.RvShowPDFs.adapter = pdfAdapter

        selectedAdapter = VaultFileAdapter(selectedFiles, onDeleteClick = { item ->
            selectedFiles.remove(item)
        })

        binding.RvShowFiles.layoutManager = LinearLayoutManager(this)
        binding.RvShowFiles.adapter = selectedAdapter

//
//
//        binding.btnVaultUploadImage.setOnClickListener {
//            imagePicker.launch(arrayOf("image/*"))
//        }
//
//        binding.btnVaultUploadVideo.setOnClickListener {
//            videoPicker.launch(arrayOf("video/*"))
//        }
//
//        binding.btnVaultUploadPDF.setOnClickListener {
//            pdfPicker.launch(arrayOf("application/pdf"))
//        }

        binding.btnVaultUploadFiles.setOnClickListener {
            filePicker.launch(arrayOf("image/*", "video/*", "application/pdf"))
        }

        binding.TvUnlockTime.setOnClickListener {
//            SingleDateAndTimePickerDialog.Builder(this).bottomSheet().curved().mustBeOnFuture()
//                .minutesStep(1).title("Choose Unlock Time")
//                .mainColor(ContextCompat.getColor(this, R.color.purple_dark)).listener { date ->
//                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
//                    val selectedTime = sdf.format(date)
//
//                    binding.TvUnlockTime.text = selectedTime
//                }.display()
            showDateTimePicker()
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
                        selectedDate?.let { it1 -> Timestamp(it1) },
                        "Locked",
                        false
                    )

                    val time = mapOf("createdAt : " to Timestamp.now())

                    firestore.collection("USERS").document(userID).set(time, SetOptions.merge()).addOnCompleteListener{
                        if (it.isSuccessful)
                        {
                            Log.d("FireStore Under USER","Added timestamp under USERS->$userID")
                        }else{
                            Log.e("FireStore Under USER","Couldn't add timestamp under USERS->$userID")
                        }
                    }

                    firestore.collection("USERS").document(userID).collection("Vaults")
                        .document(uniqueid).set(data)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    this,
                                    "SuccessFully Uploaded to FireStore",
                                    Toast.LENGTH_SHORT
                                ).show()
                                //will write
                                val allFiles = selectedFiles
                                Log.d("UploadFiles", "Total files to upload: ${allFiles.size}")
                                allFiles.forEach { (file, name, _) ->
                                    startUploadWorker(file, vaultPassword, uniqueid, userID,name)
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
        currentuserid: String,
        originalfile : String
    ) {
        val data = workDataOf(
            "filepath" to file.absolutePath,
            "password" to password,
            "USERID" to currentuserid,
            "VAULTID" to uniqueID,
            "ORIGINAL_NAME" to originalfile
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

    fun isFileSizeAllowed(file: File): Boolean {
        val maxSize = 10 * 1024 * 1024

        return file.length() <= maxSize

    }

    // Date Time Using IOS-Style (Not Using)

//    private fun showDateTimePicker() {
//        val currentTime = Date()
//        val datetoShow = selectedDate ?: currentTime
//
////        val calender = Calendar.getInstance()
////        calender.time = datetoShow
////        val year = calender.get(Calendar.YEAR)
//
//        SingleDateAndTimePickerDialog.Builder(this).bottomSheet().curved().defaultDate(datetoShow)
//            .mustBeOnFuture().minutesStep(1).displayMinutes(true).displayHours(true)
//            .displayDaysOfMonth(true).displayYears(true)
//            .title("Select Unlock Time").listener { date ->
//                val now = Date()
//                if (date.before(now)) {
//                    Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show()
//                } else {
//                    selectedDate = date
//                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
//                    binding.TvUnlockTime.text = sdf.format(date)
//                }
//            }.display()
//    }


    private fun showDateTimePicker() {
        val datePicker =
            MaterialDatePicker.Builder.datePicker().setTitleText("Select Unlock Time : ").build()

        datePicker.show(supportFragmentManager, "DATE_PICKER")

        datePicker.addOnPositiveButtonClickListener { selection ->

            val now = Calendar.getInstance()
            now.set(Calendar.HOUR_OF_DAY,0)
            now.set(Calendar.MINUTE,0)
            now.set(Calendar.SECOND,0)
            now.set(Calendar.MILLISECOND,0)

            val calender = Calendar.getInstance()
            calender.timeInMillis = selection

            if (calender.before(now))
            {
                Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show()
                return@addOnPositiveButtonClickListener
            }

            val timePicker =
                MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H).setHour(12)
                    .setMinute(0).setTitleText("Select Unlock Time : ").build()

            timePicker.show(supportFragmentManager,"TIME_PICKER")

            timePicker.addOnPositiveButtonClickListener {

                calender.set(Calendar.HOUR_OF_DAY,timePicker.hour)
                calender.set(Calendar.MINUTE,timePicker.minute)
                calender.set(Calendar.SECOND,0)
                calender.set(Calendar.MILLISECOND,0)

                selectedDate = calender.time

                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                binding.TvUnlockTime.text = selectedDate?.let { it1 -> sdf.format(it1) }
            }

        }
    }
}