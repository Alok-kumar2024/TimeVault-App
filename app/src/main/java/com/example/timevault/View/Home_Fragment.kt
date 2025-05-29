package com.example.timevault.View

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timevault.Model.VaultCretionFireStore
import com.example.timevault.ViewModel.VaultItemShowHomeAdapter
import com.example.timevault.databinding.FragmentHomeBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore


class Home_Fragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var vaultShowAdapter: VaultItemShowHomeAdapter

    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore

    private var VaultLists = mutableListOf<VaultCretionFireStore>()

    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)


        val getshare = requireActivity().getSharedPreferences("DATA", Context.MODE_PRIVATE)

        currentUserId = getshare.getString("customuserID", null) ?: "Not Found"

        Log.d("CustomUserID", "The User ID gotten from sharedPreference $currentUserId")

        firestore = FirebaseFirestore.getInstance()

        vaultShowAdapter = VaultItemShowHomeAdapter(VaultLists, onVaultClick = { item ->
            VaultClick(item)
        }, onSettingsClick = {
            Toast.makeText(requireContext(), "Clicking for Settings", Toast.LENGTH_SHORT).show()
        })
        binding.RVShowingVaultListsHomeFragment.layoutManager =
            LinearLayoutManager(requireContext())
        binding.RVShowingVaultListsHomeFragment.adapter = vaultShowAdapter

        firestore.collection("USERS").document(currentUserId).collection("Vaults")
            .addSnapshotListener { querysnapShot, FirebaseFirestoreException ->
                if (FirebaseFirestoreException != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error : Couldnt't Fetch Vaults Info..",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }
                VaultLists.clear()

                if (querysnapShot != null && !querysnapShot.isEmpty) {
                    for (doc in querysnapShot) {
                        val vaultlist = doc.toObject(VaultCretionFireStore::class.java)
                        VaultLists.add(vaultlist)

                        vaultShowAdapter.notifyDataSetChanged()
                    }

                } else {
                    Toast.makeText(requireContext(), "No vaults yet..", Toast.LENGTH_SHORT).show()
                }
            }

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

                            if (datalist?.unlocked == false) {
                                Toast.makeText(
                                    requireContext(),
                                    "Unlock Time has yet to be Come..",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@addSnapshotListener
                            }

                            if (datalist?.unlocked == true) {

                                showDialog(itemlist)

                            }

                        }
                    }


                }
        }


    }

    private fun showDialog(item: VaultCretionFireStore) {
        val editText = EditText(requireContext()).apply {
            hint = "Enter Your Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Verify Vault Password")
            .setView(editText)
            .setPositiveButton("Verify") { _, _ ->
                val enterpassword = editText.text.toString()
                verifyPasswordVault(enterpassword, item)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
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

                        val intent = Intent(requireContext(),VaultShow_Activity::class.java)
                        intent.putExtra("password",enterpassword)
                        intent.putExtra("vaultID",item.uniqueID)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failure : Password Not Matched..",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Vaults Data Not Found", Toast.LENGTH_SHORT)
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

}