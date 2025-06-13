package com.example.timevault.View

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timevault.Model.VaultCretionFireStore
import com.example.timevault.R
import com.example.timevault.ViewModel.VaultItemShowHomeAdapter
import com.example.timevault.databinding.ActivityVaultLinkBinding
import com.google.firebase.firestore.FirebaseFirestore

class VaultLinkActivity : AppCompatActivity() {

    private lateinit var binding : ActivityVaultLinkBinding

    private lateinit var vaultShowOnlineAdapter: VaultItemShowHomeAdapter

    private lateinit var  builderShowPassword : AlertDialog

    private lateinit var firestore : FirebaseFirestore

    private var isDialogShowing = false

    private var VaultListsOnline = mutableListOf<VaultCretionFireStore>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVaultLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firestore = FirebaseFirestore.getInstance()
        Log.d("DeepLink", "Intent: ${intent?.toUri(Intent.URI_INTENT_SCHEME)}")
        Log.d("DeepLink", "Intent data: ${intent?.data}")

        val data = intent?.data
        val userId = data?.getQueryParameter("userId") ?: "Not Got"
        val vaultId = data?.getQueryParameter("vaultId") ?: "Not Got"

        Log.d("DeepLink", "Parsed userId: $userId")
        Log.d("DeepLink", "Parsed vaultId: $vaultId")

        val onlineShare = getSharedPreferences("OnlineData", MODE_PRIVATE)

        val onlineEdit = onlineShare.edit()

        onlineEdit.putString("onlineUserID",userId).apply()
        onlineEdit.putString("onlineVaultID",vaultId).apply()


        binding.RVShowingVaultListsOnlineVaultHomeFragment.layoutManager = LinearLayoutManager(this)

        vaultShowOnlineAdapter = VaultItemShowHomeAdapter(VaultListsOnline, onVaultClick = {
            VaultClick(it,userId)
        })

        binding.RVShowingVaultListsOnlineVaultHomeFragment.adapter = vaultShowOnlineAdapter

        if (userId.isNotEmpty() && vaultId.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("USERS")
                .document(userId)
                .collection("Vaults")
                .document(vaultId).get().addOnSuccessListener { value ->
                    if (value != null && value.exists()) {
                        val item = value.toObject(VaultCretionFireStore::class.java)
                        if (item != null) {
                            VaultListsOnline.add(item)
                        }
                        vaultShowOnlineAdapter.notifyDataSetChanged()
                    } else {
                        Log.d("onlineVault", "Vault No Longer Exists")
                    }

                }.addOnFailureListener {
                    Log.d("OnlineVault", "No Vault")
                }
        }

    }


    private fun VaultClick(itemlist: VaultCretionFireStore, currentUserId : String) {

        itemlist.uniqueID?.let {
            firestore.collection("USERS").document(currentUserId).collection("Vaults")
                .document(it).addSnapshotListener { query, error ->

                    if (error != null) {
                        Toast.makeText(
                            this,
                            "Error : Couldnt't Fetch Vaults Info..",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@addSnapshotListener
                    }

                    if (query != null) {
                        if (query.exists()) {
                            Log.d("VaultClick", "query exists")

                            val datalist = query.toObject(VaultCretionFireStore::class.java)

                            if (datalist?.unlockTime == null)
                            {
                                Toast.makeText(this,"Vault Locked Forever, Contact Admin For more Detail",
                                    Toast.LENGTH_SHORT).show()
                                return@addSnapshotListener
                            }

                            if (datalist.unlocked == false) {
                                Toast.makeText(
                                    this,
                                    "Unlock Time has yet to be Come..",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@addSnapshotListener
                            }

                            if (datalist.unlocked == true) {

                                showDialog(itemlist,currentUserId)

                            }

                        }
                    }


                }
        }


    }

    private fun showDialog(item: VaultCretionFireStore,currentUserId: String) {

        if (isDialogShowing) return

        val dialogView = layoutInflater.inflate(R.layout.alert_dialog_vaultpassword, null)
        val edittext = dialogView.findViewById<TextView>(R.id.EtPasswordVaultAlerBox)
        val vaultname = dialogView.findViewById<TextView>(R.id.VaultNameAlertDialogVaultPasword)
        val uploadBtn = dialogView.findViewById<Button>(R.id.BtnUnlockButtonAlertDialog)
//        val BackIb = dialogView.findViewById<ImageButton>(R.id.IBBackButtonOfAlertBoxVaultPassword)

        builderShowPassword = AlertDialog
            .Builder(this)
            .setView(dialogView)
            .create()


        vaultname.text = item.vaultname
        uploadBtn.setOnClickListener {
            val eneterdPassword = edittext.text.trim().toString()
            verifyPasswordVault(eneterdPassword, item,currentUserId)
        }

//        BackIb.setOnClickListener {
//            builder.dismiss()
//        }

        builderShowPassword.setOnDismissListener {
            isDialogShowing = false
        }
        isDialogShowing = true

        builderShowPassword.window?.setBackgroundDrawable(ContextCompat.getColor(this,R.color.transparent).toDrawable())

        builderShowPassword.show()

    }

    private fun verifyPasswordVault(enterpassword: String, item: VaultCretionFireStore ,currentUserId: String) {

        if (enterpassword.isEmpty()) {
            Toast.makeText(this, "Field Cannot be Empty..", Toast.LENGTH_SHORT).show()
            return
        }

        item.uniqueID?.let { ID ->
            firestore.collection("USERS").document(currentUserId).collection("Vaults").document(ID)
                .get().addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val storedPassword = document.getString("vaultpassword") ?: "Not Found"

                        if (enterpassword == storedPassword) {
                            Toast.makeText(
                                this,
                                "Password Matched , Entering The Vault",
                                Toast.LENGTH_SHORT
                            ).show()

                            runOnUiThread{
                                builderShowPassword.dismiss()
                            }

                            val intent = Intent(this, VaultShow_Activity::class.java)
                            intent.putExtra("customuserID",currentUserId)
                            intent.putExtra("password", enterpassword)
                            intent.putExtra("vaultID", item.uniqueID)
                            startActivity(intent)


                        } else {
                            Toast.makeText(
                                this,
                                "Failure : Password Not Matched..",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Vaults Data Not Found",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }.addOnFailureListener { error ->
                    Toast.makeText(
                        this,
                        "Error Fetching Data : ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

    }

}