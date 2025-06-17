package com.example.timevault.View

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.example.timevault.Model.DatabaseSignUp
import com.example.timevault.Model.GoogleSignInUtils
import com.example.timevault.Model.ThemeHelper
import com.example.timevault.Model.useGenerateID
import com.example.timevault.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUp : Fragment() {
    private var binding_: FragmentSignUpBinding? = null
    private val binding get() = binding_!!

    private lateinit var auth: FirebaseAuth

    private lateinit var database: DatabaseReference

    private lateinit var addGoogleAccountLauncher: ActivityResultLauncher<Intent>

    private val signUpTimeOutMills = 15000L
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable : Runnable? = null
    private var signUpInProgress = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val sharetheme = requireActivity().getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme = sharetheme.getString("themeOption", ThemeHelper.SYSTEM) ?: ThemeHelper.SYSTEM
        ThemeHelper.applyTheme(savedTheme)

        binding_ = FragmentSignUpBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()

        database = FirebaseDatabase.getInstance().reference

        binding.etFullName.addTextChangedListener { resetSignUpState() }
        binding.etEmail.addTextChangedListener { resetSignUpState() }
        binding.etPassword.addTextChangedListener { resetSignUpState() }
        binding.etPasswordConfirm.addTextChangedListener { resetSignUpState() }

        addGoogleAccountLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result ->
                if (result.resultCode == Activity.RESULT_OK || result.resultCode == Activity.RESULT_CANCELED) {
                    GoogleSignInUtils.googleSignIn(
                        requireContext(), lifecycleScope, addGoogleAccountLauncher, onLogin =
                            {
                                val intent = Intent(requireContext(), MainActivity::class.java)
                                startActivity(intent)
                                requireActivity().finishAffinity()
                            })
                }

            }

        binding.BtnGoogleSignUp.setOnClickListener {
            GoogleSignInUtils.googleSignIn(
                requireContext(), lifecycleScope, addGoogleAccountLauncher, onLogin =
                    {
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                    })
        }


        binding.btnSignUp.setOnClickListener {

            val fullname = binding.etFullName.text?.trim().toString()
            val email = binding.etEmail.text?.trim().toString()
            val password = binding.etPassword.text?.trim().toString()
            val confirmpassword = binding.etPasswordConfirm.text?.trim().toString()

            if (fullname.isEmpty() || email.isEmpty() || password.isEmpty() || confirmpassword.isEmpty()) {
                Toast.makeText(context, "Fields Cannot Empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.ProgressbarAnimation.visibility = View.VISIBLE
            binding.ProgressbarAnimation.repeatCount = LottieDrawable.INFINITE
            binding.ProgressbarAnimation.playAnimation()
            signUpInProgress = true

            timeoutRunnable = Runnable {
                if (signUpInProgress)
                {
                    resetSignUpState()
                    Toast.makeText(requireContext(),"Sign-Up timed put. Please try again Later.. ",Toast.LENGTH_SHORT).show()
                }
            }
            timeoutHandler.postDelayed(timeoutRunnable!!,signUpTimeOutMills)

                if (isvaildemail(email)) {
                    if (password == confirmpassword) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->

                                if (task.isSuccessful) {
//                                val share = requireContext().getSharedPreferences("LOGIN", Context.MODE_PRIVATE)
//                                val edit = share.edit()
//
//                                edit.putBoolean("Login",true).apply()
                                    val userIdFirebase = auth.currentUser!!.uid
                                    val userdata = DatabaseSignUp(userIdFirebase,fullname, email, signUpMethod = "CUSTOM")
                                    Log.d(
                                        "UserDetailsInSignUp", "The user details : " +
                                                "Full Name : $fullname" +
                                                "Email : $email" +
                                                "ImgUrl : "
                                    )

                                    val currentid = useGenerateID(fullname.replace(" ","_"))

                                    Log.d(
                                        "UserIdInSignUp",
                                        "The User Id Default From Firebase is $currentid"
                                    )
                                    val mapUid = mapOf(
                                        userIdFirebase to currentid
                                    )

                                    val mapEmail = mapOf(
                                        encodeEmail(email) to "CUSTOM"
                                    )

                                    database.child("USERS").child(currentid).setValue(userdata)
                                        .addOnCompleteListener { task ->

                                            if (task.isSuccessful) {
                                                val share = requireActivity().getSharedPreferences(
                                                    "DATA",
                                                    Context.MODE_PRIVATE
                                                )

                                                FirebaseDatabase.getInstance().reference.child("UIDMAP").updateChildren(mapUid)
                                                FirebaseDatabase.getInstance().reference.child("EMAILMAP").updateChildren(mapEmail)


                                                val editor = share.edit()
                                                editor.putString("name", fullname).apply()
                                                editor.putString("email", email).apply()
                                                editor.putBoolean("isSignIn", true).apply()
                                                editor.putString("customuserID",currentid).apply()

                                                val intent =
                                                    Intent(context, MainActivity::class.java)
                                                startActivity(intent)

                                                resetSignUpState("Success")
                                                requireActivity().finishAffinity()
//                                                binding.ProgressbarAnimation.visibility = View.GONE
                                                Toast.makeText(
                                                    context,
                                                    "Welcome $fullname..",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(requireContext(),"Error : Couldn't Sync To the Database.",Toast.LENGTH_SHORT).show()

                                                resetSignUpState("Sync Error")
//                                                binding.ProgressbarAnimation.cancelAnimation()
//                                                binding.ProgressbarAnimation.visibility = View.GONE
                                            }
                                        }
                                }else
                                {
                                    val exception = task.exception
                                    when (exception) {
                                        is FirebaseAuthUserCollisionException -> {
                                            // Email already in use
                                            Toast.makeText(context, "Email already registered!", Toast.LENGTH_SHORT).show()
                                        }
                                        is FirebaseAuthWeakPasswordException -> {
                                            Toast.makeText(context, "Weak password!", Toast.LENGTH_SHORT).show()
                                        }
                                        is FirebaseAuthInvalidCredentialsException -> {
                                            Toast.makeText(context, "Invalid email!", Toast.LENGTH_SHORT).show()
                                        }
                                        else -> {
                                            Toast.makeText(context, "Signup failed: ${exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    resetSignUpState("Error : Due To Input Values")
//                                    binding.ProgressbarAnimation.cancelAnimation()
//                                    binding.ProgressbarAnimation.visibility = View.GONE
                                }
                            }

                    } else {
                        Toast.makeText(
                            context,
                            "password and confirm Password not matching.",
                            Toast.LENGTH_SHORT
                        ).show()

                        resetSignUpState("Password Not Matching")
//                        binding.ProgressbarAnimation.cancelAnimation()
//                        binding.ProgressbarAnimation.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(context, "Enter a Valid Email Format.", Toast.LENGTH_SHORT)
                        .show()

                    resetSignUpState("Not Valid Email Format")
//                    binding.ProgressbarAnimation.cancelAnimation()
//                    binding.ProgressbarAnimation.visibility = View.GONE
                }
        }





        return binding.root
    }

    private fun resetSignUpState(reason : String = "Unknown")
    {
        signUpInProgress = false

        timeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }

        binding.ProgressbarAnimation.cancelAnimation()
        binding.ProgressbarAnimation.visibility = View.GONE

        Log.d("SignUpDebug", "Resetting due to: $reason")

    }

    fun isvaildemail(check: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(check).matches()
    }

    fun encodeEmail(email: String): String {
        return email.replace(".", ",")
    }

}