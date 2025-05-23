package com.example.timevault.View

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.airbnb.lottie.LottieDrawable
import com.example.timevault.databinding.FragmentSignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class SignIn : Fragment() {
    private var bindin_ : FragmentSignInBinding? = null
    private val binding get() = bindin_!!

    private lateinit var auth : FirebaseAuth

    private lateinit var database : DatabaseReference

    val currentID = FirebaseAuth.getInstance().currentUser!!.uid


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindin_ = FragmentSignInBinding.inflate(inflater,container,false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("USERS")

        binding.btnSignIp.setOnClickListener {
            binding.ProgressbarAnimation.visibility = View.VISIBLE
            binding.ProgressbarAnimation.repeatCount = LottieDrawable.INFINITE
            binding.ProgressbarAnimation.playAnimation()

            val email = binding.etEmailSignIn.text?.trim().toString()
            val password = binding.etPasswordSignIn.text?.trim().toString()

            binding.btnSignIp.isEnabled = true
            if (email.isEmpty() || password.isEmpty())
            {
                Toast.makeText(context,"Fields cannot be Empty.",Toast.LENGTH_SHORT).show()
                binding.ProgressbarAnimation.cancelAnimation()
                binding.ProgressbarAnimation.visibility = View.GONE
            }else{
                auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task->
                    if(task.isSuccessful)
                    {

//                        binding.btnSignIp.isEnabled = false
                        //for preventing from loggin out whenever we close the app same in sign up fragment

//                        val share = requireContext().getSharedPreferences("LOGIN",Context.MODE_PRIVATE)
//                        val edit = share.edit()
//
//                        edit.putBoolean("Login",true).apply()

                        Log.d("currentID"," currentId $currentID")

                        database.orderByChild("userIdFireBase").equalTo(currentID).addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                Log.d("SignIn","Inside 1st onDatachange")
                                if (snapshot.exists())
                                {
                                    var UserID =""
                                    Log.d("SignIn","Inside 1st snapshot exists")
                                    for (usersnapshot in snapshot.children)
                                    {
                                        UserID = usersnapshot.key.toString()
                                    }
                                    Log.d("Custom UserID (Key for datas)","The Custom userID is $UserID")

                                    database.child(UserID).addListenerForSingleValueEvent(object : ValueEventListener{
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            Log.d("SignIn","Inside 2nd onDatachange")
                                            if (snapshot.exists())
                                            {
                                                Log.d("SignIn","Inside 2nd snapshot exists")

                                                val fullname = snapshot.child("name").value.toString() ?: "Unkown"
                                                val emailD = snapshot.child("email").value.toString() ?: "Not Recorded"

                                                Log.d("FullName and Email in SignIn","The FullName $fullname" +
                                                        "The EmailId : $emailD")

                                                val share = requireActivity().getSharedPreferences("DATA",
                                                    Context.MODE_PRIVATE)
                                                val editor = share.edit()
                                                editor.putString("name",fullname).apply()
                                                editor.putString("email",emailD).apply()
                                                editor.putString("customuserID",UserID).apply()

                                                val intent = Intent(context, MainActivity::class.java)
                                                startActivity(intent)
                                                requireActivity().finishAffinity()
                                                binding.ProgressbarAnimation.cancelAnimation()
                                                binding.ProgressbarAnimation.visibility = View.GONE
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            binding.ProgressbarAnimation.cancelAnimation()
                                            binding.ProgressbarAnimation.visibility = View.GONE

                                            Log.d("Error:SignIn","Error:InFetching fulllName and Email ${error.message}")

                                           Toast.makeText(requireContext(),"Couldn't fetch Users Information",Toast.LENGTH_SHORT).show()
                                        }

                                    })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                binding.ProgressbarAnimation.cancelAnimation()
                                binding.ProgressbarAnimation.visibility = View.GONE
                                Log.d("Error:SignIn","Error:InFinding info of currentuserID , error is ${error.message}")
                                Toast.makeText(requireContext(),"Couldn't find of the currentUserID",Toast.LENGTH_SHORT).show()
                            }

                        })


                    }else
                    {
                        Toast.makeText(context,"Couldn't Find the User or Incorrect Email or Password.",Toast.LENGTH_LONG).show()

                        binding.ProgressbarAnimation.cancelAnimation()
                        binding.ProgressbarAnimation.visibility = View.GONE
                    }
                }
            }


        }

        return binding.root
    }


}