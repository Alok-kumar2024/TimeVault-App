package com.example.timevault.ViewModel

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.timevault.Model.vaultFilesDecrypt
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class vaultDownloadViewHolder : ViewModel() {
    init {
        Log.d("ViewModelLifecycle", "vaultDownloadViewHolder created")
    }

    private val _isLoadingShow = MutableLiveData(true)
    private val _isWorkingShow = MutableLiveData(true)
    private val _showDownloadFileShow = MutableLiveData<List<Triple<String, String, Timestamp>>>()
    private val _errorMessage = MutableLiveData<String?>()

    val isLoadingShow: LiveData<Boolean> = _isLoadingShow
    val showDownloadFileShow: LiveData<List<Triple<String, String, Timestamp>>> = _showDownloadFileShow
    val isWorkingShow: LiveData<Boolean> = _isWorkingShow

    val errorMessage: LiveData<String?> = _errorMessage

    private val _decryptedFiles = MutableLiveData<List<vaultFilesDecrypt>>()
    val decryptedFiles: LiveData<List<vaultFilesDecrypt>> = _decryptedFiles


    fun fetchDownloadFiles(currentId: String, vaultId: String) {

        Log.d("LiveDataDebug", "Before fetch, LiveData = ${_showDownloadFileShow.value}")


        if (!_showDownloadFileShow.value.isNullOrEmpty())
        {
            Log.d("VM_FETCH", "Already fetched. Skipping.")
            return
        }
        Log.d("VM_FETCH", "fetchDownloadFiles() called")

        _isLoadingShow.value = true

        FirebaseFirestore.getInstance().collection("USERS").document(currentId).collection("Vaults")
            .document(vaultId).collection("Files")
            .get().addOnSuccessListener { document ->
                Log.d("Check1", "Inside on success")

                val fileListT = document.documents.mapNotNull { filemap ->

                    Log.d("Check2", "Inside fileListT")
                    val filename = filemap.getString("fileName")
                    val fileURL = filemap.getString("fileURL")
                    val uploadTime = filemap.getTimestamp("uploadedTime")


                    if (filename != null && fileURL != null && uploadTime != null) {
                        val formattedDate =
                            SimpleDateFormat("dd MMM yyyy , HH:mm a", Locale.getDefault()).format(
                                uploadTime.toDate()
                            )

                        Triple(filename, fileURL, uploadTime)
                    } else null
                }
                Log.d("DEBUG_FILE_LIST", "List size: ${fileListT.size}")

                Log.d("Check3", "near decryptedanddisplay")
                _showDownloadFileShow.value = fileListT

                _isLoadingShow.value = false
            }.addOnFailureListener {

                _isLoadingShow.value = false
                // Inside failure
                _errorMessage.value = "Failed to load files"
            }
    }


    fun setDecryptedFiles(file : List<vaultFilesDecrypt>)
    {
        _decryptedFiles.value = file
    }
}