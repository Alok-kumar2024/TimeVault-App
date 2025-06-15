package com.example.timevault.ViewModel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.timevault.Model.VaultCretionFireStore
import com.google.firebase.firestore.FirebaseFirestore

class vaultShowViewModel() : ViewModel()
{
    private val _showVault = MutableLiveData<MutableList<VaultCretionFireStore>>()
    private val _showFullVault = MutableLiveData<MutableList<VaultCretionFireStore>>()
    private val _noResult = MutableLiveData<Boolean>(true)
    private val _isLoading = MutableLiveData<Boolean>(true)

    val showVault : LiveData<MutableList<VaultCretionFireStore>> = _showVault
    val showFullVault : LiveData<MutableList<VaultCretionFireStore>> = _showFullVault
    val noResult : LiveData<Boolean> = _noResult
    val isLoading : LiveData<Boolean> = _isLoading

    fun fetchData(currentuserID: String,context : Context)
    {
        if (_showFullVault.value != null && _showVault.value != null) return

        _isLoading.value = true
        FirebaseFirestore.getInstance().collection("USERS").document(currentuserID).collection("Vaults")
            .addSnapshotListener { querysnapShot, FirebaseFirestoreException ->
                if (FirebaseFirestoreException != null) {
                    Toast.makeText(
                        context,
                        "Error : Couldnt't Fetch Vaults Info..",
                        Toast.LENGTH_SHORT
                    ).show()
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                val vaultList = mutableListOf<VaultCretionFireStore>()

                if (querysnapShot != null && !querysnapShot.isEmpty) {
                    for (doc in querysnapShot) {
                        val vault = doc.toObject(VaultCretionFireStore::class.java)
                        vaultList.add(vault)

                    }
                    _showVault.value = vaultList
                    _showFullVault.value = vaultList
                    _noResult.value = false

                } else {
                    _noResult.value = true
                    Toast.makeText(context, "No vaults yet..", Toast.LENGTH_SHORT).show()
                }
                _isLoading.value = false
            }

    }

}