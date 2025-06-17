package com.example.timevault.ViewModel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.timevault.Model.VaultCretionFireStore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class HomeVaultViewModel : ViewModel() {

    private val _showVaultHome = MutableLiveData<MutableList<VaultCretionFireStore>>()
    private val _isLoading = MutableLiveData<Boolean>(true)

    val showVaultHome: LiveData<MutableList<VaultCretionFireStore>> = _showVaultHome
    val isLoading: LiveData<Boolean> = _isLoading

    private val _showPinnedVault = MutableLiveData<MutableList<VaultCretionFireStore>>()
    val showPinnedVault: LiveData<MutableList<VaultCretionFireStore>> = _showPinnedVault

    private val _showRecentVault = MutableLiveData<MutableList<VaultCretionFireStore>>()
    val showRecentVault: LiveData<MutableList<VaultCretionFireStore>> = _showRecentVault

    private val _showOneWeekVault = MutableLiveData<MutableList<VaultCretionFireStore>>()
    val showOneWeekVault: LiveData<MutableList<VaultCretionFireStore>> = _showOneWeekVault

    fun fetchVault(currentId: String, context: Context) {
        if (_showVaultHome.value != null) return
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
                        vaultList.add(vaultlist)
                    }
                    _showVaultHome.value = vaultList

                    _showPinnedVault.value = vaultList.filter { it.pinned }.toMutableList()

                    _showRecentVault.value = vaultList.filter { it.lastAccessedTime != null }
                        .sortedByDescending { it.lastAccessedTime!!.toDate().time }.take(5)
                        .toMutableList()

                    val currentTime = Timestamp.now().toDate().time
                    val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L

                    val upcomingVault = vaultList.filter {
                        val unlockTime = it.unlockTime?.toDate()?.time ?: 0L
                        unlockTime in currentTime..(currentTime + oneWeekMillis)
                    }

                    _showOneWeekVault.value = upcomingVault.toMutableList()

                } else {
                    Toast.makeText(context, "No vaults yet..", Toast.LENGTH_SHORT).show()
                }

                _isLoading.value = false
            }
    }
}