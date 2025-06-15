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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
import com.example.timevault.ViewModel.HomeVaultViewModel
import com.example.timevault.ViewModel.VaultItemShowHomeAdapter
import com.example.timevault.databinding.FragmentHomeBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore


class Home_Fragment : Fragment(),SearchFragments {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var vaultShowAdapter: VaultItemShowHomeAdapter

    private lateinit var builderShowPassword : AlertDialog
    private var isDialogShowing = false

    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore

    private lateinit var vaultViewModelHolder : HomeVaultViewModel

    private var fullVaultLists = mutableListOf<VaultCretionFireStore>()
    private var VaultLists = mutableListOf<VaultCretionFireStore>()


    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val sharetheme = requireActivity().getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme = sharetheme.getString("themeOption", ThemeHelper.SYSTEM) ?: ThemeHelper.SYSTEM
        ThemeHelper.applyTheme(savedTheme)

        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        vaultViewModelHolder = ViewModelProvider(this)[HomeVaultViewModel::class.java]


        val getshare = requireActivity().getSharedPreferences("DATA", Context.MODE_PRIVATE)

        currentUserId = getshare.getString("customuserID", null) ?: "Not Found"

        Log.d("CustomUserID", "The User ID gotten from sharedPreference $currentUserId")

        firestore = FirebaseFirestore.getInstance()

        vaultShowAdapter = VaultItemShowHomeAdapter(VaultLists, onVaultClick = { item ->
            VaultClick(item)
        })
        binding.RVShowingVaultListsHomeFragment.layoutManager =
            LinearLayoutManager(requireContext())
        binding.RVShowingVaultListsHomeFragment.adapter = vaultShowAdapter

        vaultViewModelHolder.showVaultHome.observe(viewLifecycleOwner)
        {list->
            VaultLists.clear()
            VaultLists.addAll(list)

            fullVaultLists.clear()
            fullVaultLists.addAll(list)

            vaultShowAdapter.notifyDataSetChanged()

        }

//        firestore.collection("USERS").document(currentUserId).collection("Vaults")
//            .addSnapshotListener { querysnapShot, FirebaseFirestoreException ->
//                if (FirebaseFirestoreException != null) {
//                    Toast.makeText(
//                        requireContext(),
//                        "Error : Couldnt't Fetch Vaults Info..",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    return@addSnapshotListener
//                }
//                VaultLists.clear()
//                fullVaultLists.clear()
//
//                if (querysnapShot != null && !querysnapShot.isEmpty) {
//                    for (doc in querysnapShot) {
//                        val vaultlist = doc.toObject(VaultCretionFireStore::class.java)
//                        if (vaultlist.unlocked == false) {
//                            fullVaultLists.add(vaultlist)
//                            VaultLists.add(vaultlist)
//                        }
//
//                        vaultShowAdapter.notifyDataSetChanged()
//                    }
//
//                } else {
//                    Toast.makeText(requireContext(), "No vaults yet..", Toast.LENGTH_SHORT).show()
//                }
//            }

        vaultViewModelHolder.isLoading.observe(viewLifecycleOwner)
        { loading->
            if (loading)
            {
                binding.RVShowingVaultListsHomeFragment.visibility = View.GONE
                binding.TvNoResultFoundHomeFragment.visibility = View.GONE
                binding.ProgressbarAnimationHomeFragment.visibility = View.VISIBLE
                binding.ProgressbarAnimationHomeFragment.repeatCount = LottieDrawable.INFINITE
                binding.ProgressbarAnimationHomeFragment.playAnimation()
            }else
            {
                binding.RVShowingVaultListsHomeFragment.visibility = View.VISIBLE
                binding.ProgressbarAnimationHomeFragment.cancelAnimation()
                binding.ProgressbarAnimationHomeFragment.visibility = View.GONE
                if (VaultLists.isEmpty())
                {
                    binding.TvNoResultFoundHomeFragment.visibility = View.VISIBLE
                }else
                {
                    binding.TvNoResultFoundHomeFragment.visibility = View.GONE
                }
            }
        }

        vaultViewModelHolder.fetchVault(currentUserId,requireContext())


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

                            if (datalist?.unlockTime == null)
                            {
                                Toast.makeText(requireContext(),"Vault Locked Forever, Contact Admin For more Detail",Toast.LENGTH_SHORT).show()
                                return@addSnapshotListener
                            }

                            if (datalist.unlocked == false) {
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

        builderShowPassword.window?.setBackgroundDrawable(ContextCompat.getColor(requireContext(),R.color.transparent).toDrawable())

        builderShowPassword.show()

    }

    private fun verifyPasswordVault(enterpassword: String, item: VaultCretionFireStore ) {

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

                            activity?.runOnUiThread{
                                builderShowPassword.dismiss()
                            }

                            val intent = Intent(requireContext(), VaultShow_Activity::class.java)
                            intent.putExtra("customuserID",currentUserId)
                            intent.putExtra("password", enterpassword)
                            intent.putExtra("vaultID", item.uniqueID)
                            startActivity(intent)


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
        val filterList = if (query.isBlank())
        {
            fullVaultLists
        }else
        {
            fullVaultLists.filter {
                        it.uniqueID?.contains(query, ignoreCase = true) == true||
                        it.vaultname?.contains(query, ignoreCase = true) == true||
                        it.status?.contains(query, ignoreCase = true) == true
            }
        }

        VaultLists.clear()
        VaultLists.addAll(filterList)
        vaultShowAdapter.notifyDataSetChanged()

        if (VaultLists.isEmpty())
        {
            binding.TvNoResultFoundHomeFragment.visibility = View.VISIBLE
        }else
        {
            binding.TvNoResultFoundHomeFragment.visibility = View.GONE
        }
    }

}