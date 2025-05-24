package com.example.timevault.View

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timevault.Model.VaultCretionFireStore
import com.example.timevault.Model.vaultFileItem
import com.example.timevault.R
import com.example.timevault.ViewModel.VaultShowAdapter
import com.example.timevault.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore


class Home_Fragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var vaultShowAdapter: VaultShowAdapter

    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore

    private  var VaultLists = mutableListOf<VaultCretionFireStore>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val getshare = requireActivity().getSharedPreferences("DATA", Context.MODE_PRIVATE)

        val currentUserId = getshare.getString("customuserID", null) ?: "Not Found"

        Log.d("CustomUserID", "The User ID gotten from sharedPreference $currentUserId")

        firestore = FirebaseFirestore.getInstance()

        vaultShowAdapter = VaultShowAdapter(VaultLists, onSettingsClick = {
            Toast.makeText(requireContext(),"Clicking for Settings",Toast.LENGTH_SHORT).show()
        })
        binding.RVShowingVaultListsHomeFragment.layoutManager = LinearLayoutManager(requireContext())
        binding.RVShowingVaultListsHomeFragment.adapter = vaultShowAdapter

        firestore.collection("USERS").document(currentUserId).collection("Vaults")
            .addSnapshotListener { querysnapShot, FirebaseFirestoreException ->
                if (FirebaseFirestoreException != null)
                {
                    Toast.makeText(requireContext(),"Error : Couldnt't Fetch Vaults Info..",Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (querysnapShot != null && !querysnapShot.isEmpty) {
                    for (doc in querysnapShot)
                    {
                        val vaultlist = doc.toObject(VaultCretionFireStore ::class.java)
                        VaultLists.add(vaultlist)

                        vaultShowAdapter.notifyDataSetChanged()
                    }

                }else{
                    Toast.makeText(requireContext(),"No vaults yet..",Toast.LENGTH_SHORT).show()
                }
            }

        return binding.root
    }

}