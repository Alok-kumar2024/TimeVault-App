package com.example.timevault.Model

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.timevault.R
import com.example.timevault.View.VaultShow_Activity
import com.google.firebase.firestore.FirebaseFirestore

class VaultDeepLink : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing || isDestroyed) return

        val uri = intent?.data
        val uid = uri?.getQueryParameter("userId")
        val vid = uri?.getQueryParameter("vaultId")


        if (uid != null && vid != null)
        {
            FirebaseFirestore.getInstance()
                .collection("USERS")
                .document(uid)
                .collection("Vaults")
                .document(vid).get().addOnSuccessListener { value->
                    if (value != null && value.exists()) {
                        val item = value.toObject(VaultCretionFireStore::class.java)
                        if (item != null) {
                            showDialog(uid,vid,item)
                        }
                    }else
                    {
                        Toast.makeText(this,"Vault No Longer Exists",Toast.LENGTH_SHORT).show()
                    }

                }
        }
    }
    private fun showDialog(uid : String?,vid:String?, item : VaultCretionFireStore) {

        val dialogView = layoutInflater.inflate(R.layout.alert_dialog_vaultpassword, null)
        val edittext = dialogView.findViewById<TextView>(R.id.EtPasswordVaultAlerBox)
        val vaultname = dialogView.findViewById<TextView>(R.id.VaultNameAlertDialogVaultPasword)
        val uploadBtn = dialogView.findViewById<Button>(R.id.BtnUnlockButtonAlertDialog)
//        val BackIb = dialogView.findViewById<ImageButton>(R.id.IBBackButtonOfAlertBoxVaultPassword)

        val builder = AlertDialog
            .Builder(this)
            .setView(dialogView)
            .create()


        vaultname.text = item.vaultname
        uploadBtn.setOnClickListener {
            val eneterdPassword = edittext.text.trim().toString()
            verifyPasswordVault(eneterdPassword, item,uid,builder)
        }

//        BackIb.setOnClickListener {
//            builder.dismiss()
//        }

        builder.show()

    }

    private fun verifyPasswordVault(enterpassword: String, item: VaultCretionFireStore, uid :String?,builder: AlertDialog) {

        val firestore = FirebaseFirestore.getInstance()

        if (enterpassword.isEmpty()) {
            Toast.makeText(this, "Field Cannot be Empty..", Toast.LENGTH_SHORT).show()
            return
        }

        item.uniqueID?.let { ID ->

            if (uid != null) {
                firestore.collection("USERS").document(uid).collection("Vaults").document(ID)
                    .get().addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val storedPassword = document.getString("vaultpassword") ?: "Not Found"

                            if (enterpassword == storedPassword) {
                                Toast.makeText(
                                    this,
                                    "Password Matched , Entering The Vault",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent = Intent(this, VaultShow_Activity::class.java)
                                intent.putExtra("customuserID",uid)
                                intent.putExtra("password", enterpassword)
                                intent.putExtra("vaultID", item.uniqueID)
                                startActivity(intent)
                                builder.dismiss()
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
                    }.addOnFailureListener { error->
                        Toast.makeText(
                            this,
                            "Error Fetching Data : ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

    }
}