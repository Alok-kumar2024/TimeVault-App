package com.example.timevault.View

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieDrawable
import com.example.timevault.Model.SearchFragments
import com.example.timevault.Model.ThemeHelper
import com.example.timevault.Model.VaultCretionFireStore
import com.example.timevault.R
import com.example.timevault.ViewModel.VaultItemShowHomeAdapter
import com.example.timevault.ViewModel.vaultShowViewModel
import com.example.timevault.databinding.FragmentVaultBinding
import com.google.firebase.Timestamp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


class VaultFragment : Fragment(), SearchFragments {
    private var _binding: FragmentVaultBinding? = null
    private val binding get() = _binding!!

    private lateinit var vaultShowAdapter: VaultItemShowHomeAdapter

    private lateinit var builderShowPassword: AlertDialog
    private var isDialogShowing = false

    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var vaultViewHolder: vaultShowViewModel

    private var popUp: PopupWindow? = null

    private var fullVaultLists = mutableListOf<VaultCretionFireStore>()
    private var VaultLists = mutableListOf<VaultCretionFireStore>()

    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val sharetheme = requireActivity().getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme =
            sharetheme.getString("themeOption", ThemeHelper.SYSTEM) ?: ThemeHelper.SYSTEM
        ThemeHelper.applyTheme(savedTheme)

        // Inflate the layout for this fragment
        _binding = FragmentVaultBinding.inflate(inflater, container, false)


        val getshare = requireActivity().getSharedPreferences("DATA", Context.MODE_PRIVATE)

        vaultViewHolder = ViewModelProvider(this)[vaultShowViewModel::class.java]

        currentUserId = getshare.getString("customuserID", null) ?: "Not Found"

        Log.d("CustomUserID", "The User ID gotten from sharedPreference $currentUserId")

        firestore = FirebaseFirestore.getInstance()

        vaultShowAdapter = VaultItemShowHomeAdapter(VaultLists, onVaultClick = { item ->
            VaultClick(item)
        }, onMoreClick = { item, view ->
            showPopUpVaultList(item, view)
        })
        binding.RVShowingVaultListsVaultFragment.layoutManager =
            LinearLayoutManager(requireContext())
        binding.RVShowingVaultListsVaultFragment.adapter = vaultShowAdapter


        vaultViewHolder.showVault.observe(viewLifecycleOwner) { vaultList ->
            VaultLists.clear()
            VaultLists.addAll(vaultList)
            vaultShowAdapter.notifyDataSetChanged()


        }

        vaultViewHolder.showFullVault.observe(viewLifecycleOwner) { vaultList ->
            fullVaultLists.clear()
            fullVaultLists.addAll(vaultList)


        }

        vaultViewHolder.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading) {
                binding.RVShowingVaultListsVaultFragment.visibility = View.GONE
                binding.TvNoResultFoundVaultFragment.visibility = View.GONE

                binding.ProgressbarAnimationVaultFragment.visibility = View.VISIBLE
                binding.ProgressbarAnimationVaultFragment.repeatCount = LottieDrawable.INFINITE
                binding.ProgressbarAnimationVaultFragment.playAnimation()
            } else {
                binding.RVShowingVaultListsVaultFragment.visibility = View.VISIBLE

                binding.ProgressbarAnimationVaultFragment.cancelAnimation()
                binding.ProgressbarAnimationVaultFragment.visibility = View.GONE

                if (VaultLists.isEmpty()) {
                    binding.TvNoResultFoundVaultFragment.visibility = View.VISIBLE
                } else {
                    binding.TvNoResultFoundVaultFragment.visibility = View.GONE
                }
            }
        }

        vaultViewHolder.fetchData(currentUserId, requireContext())

        return binding.root
    }


    private fun VaultClick(itemlist: VaultCretionFireStore) {

        itemlist.uniqueID?.let {
            firestore.collection("USERS").document(currentUserId).collection("Vaults")
                .document(it).addSnapshotListener { query, error ->

                    if (error != null) {
                        Toast.makeText(
                            requireContext(),
                            "Error : Couldnt't Fetch Vaults Info..",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@addSnapshotListener
                    }

                    if (query != null) {
                        if (query.exists()) {
                            Log.d("VaultClick", "query exists")

                            val datalist = query.toObject(VaultCretionFireStore::class.java)

                            if (datalist?.unlockTime == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Vault Locked Forever, Contact Admin For more Detail",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@addSnapshotListener
                            }

                            if (datalist?.unlocked == false) {
                                Toast.makeText(
                                    requireContext(),
                                    "Unlock Time has yet to be Come..",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@addSnapshotListener
                            }

                            if (datalist.unlocked == true) {

                                showDialog(itemlist)

                            }

                        }
                    }


                }
        }


    }

    private fun showDialog(item: VaultCretionFireStore) {
        if (!isAdded || isRemoving || requireActivity().isFinishing) {
            return
        }

        if (isDialogShowing) return

        val dialogView = layoutInflater.inflate(R.layout.alert_dialog_vaultpassword, null)
        val edittext = dialogView.findViewById<TextView>(R.id.EtPasswordVaultAlerBox)
        val vaultname = dialogView.findViewById<TextView>(R.id.VaultNameAlertDialogVaultPasword)
        val uploadBtn = dialogView.findViewById<Button>(R.id.BtnUnlockButtonAlertDialog)
//        val BackIb = dialogView.findViewById<ImageButton>(R.id.IBBackButtonOfAlertBoxVaultPassword)

        builderShowPassword = AlertDialog
            .Builder(requireContext())
            .setView(dialogView)
            .create()

        vaultname.text = item.vaultname
        uploadBtn.setOnClickListener {
            val eneterdPassword = edittext.text.trim().toString()
            verifyPasswordVault(eneterdPassword, item)
        }

//        BackIb.setOnClickListener {
//            builder.dismiss()
//        }
        builderShowPassword.setOnDismissListener {
            isDialogShowing = false
        }
        isDialogShowing = true

        builderShowPassword.window?.setBackgroundDrawable(
            ContextCompat.getColor(
                requireContext(),
                R.color.transparent
            ).toDrawable()
        )

        builderShowPassword.show()

    }

    private fun verifyPasswordVault(enterpassword: String, item: VaultCretionFireStore) {

        if (enterpassword.isEmpty()) {
            Toast.makeText(requireContext(), "Field Cannot be Empty..", Toast.LENGTH_SHORT).show()
            return
        }

        item.uniqueID?.let { ID ->
            firestore.collection("USERS").document(currentUserId).collection("Vaults").document(ID)
                .get().addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val storedPassword = document.getString("vaultpassword") ?: "Not Found"

                        if (enterpassword == storedPassword) {
                            Toast.makeText(
                                requireContext(),
                                "Password Matched , Entering The Vault",
                                Toast.LENGTH_SHORT
                            ).show()


                            val intent = Intent(requireContext(), VaultShow_Activity::class.java)
                            intent.putExtra("customuserID", currentUserId)
                            intent.putExtra("password", enterpassword)
                            intent.putExtra("vaultID", item.uniqueID)

                            val data = mapOf(
                                "lastAccessedTime" to Timestamp.now()
                            )


                            item.uniqueID?.let {
                                FirebaseFirestore.getInstance().collection("USERS")
                                    .document(currentUserId).collection("Vaults").document(it)
                                    .set(data, SetOptions.merge()).addOnSuccessListener {
                                        Log.d("Added LastAccess","The Last Access Added is ${Timestamp.now()}")
                                    }.addOnFailureListener {
                                        Log.e("Added LastAccess","The Last Access Not Added error : ${it.message}")
                                    }
                            }

                            startActivity(intent)

                            activity?.runOnUiThread {
                                builderShowPassword.dismiss()
                            }

                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Failure : Password Not Matched..",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Vaults Data Not Found",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }.addOnFailureListener { error ->
                    Toast.makeText(
                        requireContext(),
                        "Error Fetching Data : ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

    }

    override fun filterVaults(query: String) {
        val filterList = if (query.isBlank()) {
            fullVaultLists
        } else {
            fullVaultLists.filter {
                it.uniqueID?.contains(query, ignoreCase = true) == true ||
                        it.vaultname?.contains(query, ignoreCase = true) == true
            }
        }

        VaultLists.clear()
        VaultLists.addAll(filterList)
        vaultShowAdapter.notifyDataSetChanged()

        if (VaultLists.isEmpty()) {
            binding.TvNoResultFoundVaultFragment.visibility = View.VISIBLE
        } else {
            binding.TvNoResultFoundVaultFragment.visibility = View.GONE
        }
    }

    fun showPopUpVaultList(item: VaultCretionFireStore, anchor: View) {
        if (isAdded && !isRemoving && !requireActivity().isFinishing) {
            if (popUp == null) {
                val popUpView = layoutInflater.inflate(R.layout.popup_vaultlist, null)
                val shareBtn = popUpView.findViewById<LinearLayout>(R.id.LLSharePopUpVaultList)
//                val deleteBtn = popUpView.findViewById<LinearLayout>(R.id.LLDeletePopUpVaultList)
                val pinnedBtn = popUpView.findViewById<LinearLayout>(R.id.LLPinnedPopUpVaultList)
                val pinnedText = popUpView.findViewById<TextView>(R.id.TvPinPopUPVaultList)

                popUp = PopupWindow(
                    popUpView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                ).apply {
                    elevation = 20f
                    isOutsideTouchable = true

                    setBackgroundDrawable(activity?.getColor(R.color.transparent)?.toDrawable())

                    setOnDismissListener {
                        popUp = null
                    }

                }

                shareBtn.setOnClickListener {

                    val shareBody = """
                             Vault: ${item.vaultname}

                            🔗 Open Vault:
                                https://alok-kumar2024.github.io/Vault-Web/vault.html?userId=$currentUserId&vaultId=${item.uniqueID}
                                """.trimIndent()


                    val shareSub = "TimeVault"

                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"

                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSub)
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody)

                    popUp?.dismiss()

                    startActivity(shareIntent)

                }

                if (item.pinned)
                {
                    pinnedText.text = "UnPin"
                }else{
                    pinnedText.text = "Pin"
                }

                pinnedBtn.setOnClickListener {

                    if (item.pinned)
                    {
                        val data = mapOf(
                            "pinned" to false
                        )

                        item.uniqueID?.let { it1 ->
                            FirebaseFirestore.getInstance().collection("USERS")
                                .document(currentUserId)
                                .collection("Vaults")
                                .document(it1).set(data, SetOptions.merge()).addOnSuccessListener {
                                    pinnedText.text = "UnPin"
                                    popUp?.dismiss()
                                    Log.d("PINNED_True","Pinned was true , upon clicking done it false")
                                }.addOnFailureListener {
                                    Log.d("PINNED_True","Pinned was true , upon clicking could not change it false")
                                }
                        }
                    }else{

                        val data = mapOf(
                            "pinned" to true
                        )

                        item.uniqueID?.let { it1 ->
                            FirebaseFirestore.getInstance().collection("USERS")
                                .document(currentUserId)
                                .collection("Vaults")
                                .document(it1).set(data, SetOptions.merge()).addOnSuccessListener {
                                    pinnedText.text = "Pin"
                                    popUp?.dismiss()
                                    Log.d("PINNED_False","Pinned was false , upon clicking done it true")
                                }.addOnFailureListener {
                                    Log.d("PINNED_False","Pinned was false , upon clicking could not change it true")
                                }
                        }

                    }

                }

            }

            if (popUp!!.isShowing) return

            popUp?.showAsDropDown(anchor)
        }
    }

}