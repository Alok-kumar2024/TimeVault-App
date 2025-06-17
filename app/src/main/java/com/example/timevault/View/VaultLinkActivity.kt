package com.example.timevault.View

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class VaultLinkActivity : AppCompatActivity() {

    private lateinit var binding : ActivityVaultLinkBinding

    private lateinit var vaultShowOnlineAdapter: VaultItemShowHomeAdapter

    private lateinit var  builderShowPassword : AlertDialog

    private lateinit var firestore : FirebaseFirestore

    private var popUp : PopupWindow? = null

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
        }, onMoreClick = { item,view->
            showPopUpVaultList(item,view,userId)
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

                            val intent = Intent(this, VaultShow_Activity::class.java)
                            intent.putExtra("customuserID",currentUserId)
                            intent.putExtra("password", enterpassword)
                            intent.putExtra("vaultID", item.uniqueID)

//                            val data = mapOf(
//                                "lastAccessedTime" to Timestamp.now()
//                            )
//
//
//                            item.uniqueID?.let {
//                                FirebaseFirestore.getInstance().collection("USERS")
//                                    .document(currentUserId).collection("Vaults").document(it)
//                                    .set(data, SetOptions.merge()).addOnSuccessListener {
//                                        Log.d("Added LastAccess","The Last Access Added is ${Timestamp.now()}")
//                                    }.addOnFailureListener {
//                                        Log.e("Added LastAccess","The Last Access Not Added error : ${it.message}")
//                                    }
//                            }


                            startActivity(intent)

                            runOnUiThread{
                                builderShowPassword.dismiss()
                            }

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

    fun showPopUpVaultList(item : VaultCretionFireStore,anchor : View,currentUserId : String)
    {
        if (!isDialogShowing && !isFinishing)
        {
            if (popUp == null)
            {
                val popUpView = layoutInflater.inflate(R.layout.popup_vaultlist,null)
                val shareBtn = popUpView.findViewById<LinearLayout>(R.id.LLSharePopUpVaultList)
//                val deleteBtn = popUpView.findViewById<LinearLayout>(R.id.LLDeletePopUpVaultList)
                val pinnedBtn = popUpView.findViewById<LinearLayout>(R.id.LLPinnedPopUpVaultList)
                val pinnedText = popUpView.findViewById<TextView>(R.id.TvPinPopUPVaultList)
                val divider = popUpView.findViewById<View>(R.id.PopUpDividerVaultList)



                popUp = PopupWindow(
                    popUpView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                ).apply {
                    elevation = 20f
                    isOutsideTouchable = true

                    setBackgroundDrawable(getColor(R.color.transparent)?.toDrawable())

                    setOnDismissListener {
                        popUp = null
                    }

                }

                shareBtn.setOnClickListener{

                    val shareBody = "Vault-> ${item.vaultname} \nLink -> https://alok-kumar2024.github.io/Vault-Web/vault.html?userId=$currentUserId&vaultId=${item.uniqueID}&hl=en"
                    val shareSub ="TimeVault"

                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"

                    shareIntent.putExtra(Intent.EXTRA_SUBJECT,shareSub)
                    shareIntent.putExtra(Intent.EXTRA_TEXT,shareBody)

                    popUp?.dismiss()

                    startActivity(shareIntent)

                }

                pinnedBtn.visibility = View.GONE
                divider.visibility = View.GONE

//                if (item.pinned)
//                {
//                    pinnedText.text = "UnPin"
//                }else{
//                    pinnedText.text = "Pin"
//                }
//
//                pinnedBtn.setOnClickListener {
//
//                    if (item.pinned)
//                    {
//                        val data = mapOf(
//                            "pinned" to false
//                        )
//
//                        item.uniqueID?.let { it1 ->
//                            FirebaseFirestore.getInstance().collection("USERS")
//                                .document(currentUserId)
//                                .collection("Vaults")
//                                .document(it1).set(data, SetOptions.merge()).addOnSuccessListener {
//                                    pinnedText.text = "UnPin"
//                                    popUp?.dismiss()
//                                    Log.d("PINNED_True","Pinned was true , upon clicking done it false")
//                                }.addOnFailureListener {
//                                    Log.d("PINNED_True","Pinned was true , upon clicking could not change it false")
//                                }
//                        }
//                    }else{
//
//                        val data = mapOf(
//                            "pinned" to true
//                        )
//
//                        item.uniqueID?.let { it1 ->
//                            FirebaseFirestore.getInstance().collection("USERS")
//                                .document(currentUserId)
//                                .collection("Vaults")
//                                .document(it1).set(data, SetOptions.merge()).addOnSuccessListener {
//                                    pinnedText.text = "Pin"
//                                    popUp?.dismiss()
//                                    Log.d("PINNED_False","Pinned was false , upon clicking done it true")
//                                }.addOnFailureListener {
//                                    Log.d("PINNED_False","Pinned was false , upon clicking could not change it true")
//                                }
//                        }
//
//                    }
//
//                }

            }

            if (popUp!!.isShowing) return

            popUp?.showAsDropDown(anchor)
        }
    }

}