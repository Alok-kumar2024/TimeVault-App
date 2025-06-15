package com.example.timevault.ViewModel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.timevault.Model.VaultCretionFireStore
import com.google.firebase.firestore.FirebaseFirestore

class HomeVaultViewModel : ViewModel() {

    private val _showVaultHome = MutableLiveData<MutableList<VaultCretionFireStore>>()
    private val _noResultHome = MutableLiveData<Boolean>(true)
    private val _isLoading = MutableLiveData<Boolean>(true)

    val showVaultHome : LiveData<MutableList<VaultCretionFireStore>> = _showVaultHome
    val noResultHome : LiveData<Boolean> = _noResultHome
    val isLoading : LiveData<Boolean> = _isLoading

    fun fetchVault(currentId : String , context : Context)
    {
        if (_showVaultHome.value !=  null) return
        _isLoading.value = true
        FirebaseFirestore.getInstance().collection("USERS").document(currentId).collection("Vaults")
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
                        val vaultlist = doc.toObject(VaultCretionFireStore::class.java)
                        if (vaultlist.unlocked == false) {
                            vaultList.add(vaultlist)
                        }
                    }
                    _showVaultHome.value = vaultList

                    _noResultHome.value = false


                } else {
                    _noResultHome.value = true
                    Toast.makeText(context, "No vaults yet..", Toast.LENGTH_SHORT).show()
                }

                _isLoading.value = false
            }
    }
}