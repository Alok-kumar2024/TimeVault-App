package com.example.timevault.View

import android.app.Activity
import android.app.AlertDialog
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
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.example.timevault.Model.GoogleSignInUtils
import com.example.timevault.Model.ThemeHelper
import com.example.timevault.R
import com.example.timevault.databinding.FragmentSignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class SignIn : Fragment() {
    private var bindin_: FragmentSignInBinding? = null
    private val binding get() = bindin_!!

    private lateinit var auth: FirebaseAuth

    private lateinit var database: DatabaseReference

    private lateinit var addGoogleAccountLauncher: ActivityResultLauncher<Intent>

    private var isForgotDialogShowing = false

    private val signInTimeOutMills = 15000L
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var signInInProgress = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val sharetheme = requireActivity().getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme =
            sharetheme.getString("themeOption", ThemeHelper.SYSTEM) ?: ThemeHelper.SYSTEM
        ThemeHelper.applyTheme(savedTheme)

        bindin_ = FragmentSignInBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("USERS")

        binding.etEmailSignIn.addTextChangedListener {
            resetSignInState("Input changed")
        }

        binding.etPasswordSignIn.addTextChangedListener {
            resetSignInState("Input changed")
        }

        binding.TvForgotPasswordSignIn.setOnClickListener {
            alertBoxForgotPassword()
        }

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

        binding.BtnGoogleSignIn.setOnClickListener {
            GoogleSignInUtils.googleSignIn(
                requireContext(), lifecycleScope, addGoogleAccountLauncher, onLogin =
                    {
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                    })
        }


        binding.btnSignIn.setOnClickListener {

            val email = binding.etEmailSignIn.text?.trim().toString()
            val password = binding.etPasswordSignIn.text?.trim().toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Fields cannot be Empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (signInInProgress) {
                Toast.makeText(
                    requireContext(),
                    "Already in Progress , Please Wait..",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            signInInProgress = true
            binding.ProgressbarAnimation.visibility = View.VISIBLE
            binding.ProgressbarAnimation.repeatCount = LottieDrawable.INFINITE
            binding.ProgressbarAnimation.playAnimation()

            timeoutRunnable = Runnable {
                if (signInInProgress) {
                    resetSignInState("Sign-in timed out")
                    Toast.makeText(
                        requireContext(),
                        "Sign-in timed out. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            timeoutHandler.postDelayed(timeoutRunnable!!, signInTimeOutMills)

            FirebaseDatabase.getInstance().getReference("EMAILMAP").child(encodeEmail(email))
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val method = snapshot.getValue(String::class.java)

                            if (method == "CUSTOM") {

                                val startTime = System.currentTimeMillis()
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->

                                        if (task.isSuccessful) {
                                            val currentID =
                                                FirebaseAuth.getInstance().currentUser!!.uid
                                            val signInTime = System.currentTimeMillis()
                                            Log.d(
                                                "Timing",
                                                "Sign-in took ${signInTime - startTime}ms"
                                            )


                                            Log.d("currentID", " currentId $currentID")

                                            FirebaseDatabase.getInstance().getReference("UIDMAP").child(currentID)
                                                .addListenerForSingleValueEvent(object :
                                                    ValueEventListener {
                                                    override fun onDataChange(snapshot: DataSnapshot) {
                                                        Log.d("SignIn", "Inside 1st onDatachange")
                                                        val firstQueryTime =
                                                            System.currentTimeMillis()
                                                        Log.d(
                                                            "Timing",
                                                            "First DB query took ${firstQueryTime - signInTime}ms"
                                                        )

                                                        if (snapshot.exists()) {
                                                            var userID = snapshot.getValue(String::class.java)
                                                            Log.d(
                                                                "Custom UserID (Key for datas)",
                                                                "The Custom userID is $userID"
                                                            )

                                                            if (userID != null) {
                                                                database.child(userID)
                                                                    .addListenerForSingleValueEvent(
                                                                        object :
                                                                            ValueEventListener {
                                                                            override fun onDataChange(
                                                                                snapshot: DataSnapshot
                                                                            ) {
                                                                                Log.d(
                                                                                    "SignIn",
                                                                                    "Inside 2nd onDatachange"
                                                                                )
                                                                                if (snapshot.exists()) {
                                                                                    Log.d(
                                                                                        "SignIn",
                                                                                        "Inside 2nd snapshot exists"
                                                                                    )

                                                                                    val fullname =
                                                                                        snapshot.child("name").value.toString()
                                                                                            ?: "Unkown"
                                                                                    val emailD =
                                                                                        snapshot.child("email").value.toString()
                                                                                            ?: "Not Recorded"

                                                                                    Log.d(
                                                                                        "FullName and Email in SignIn",
                                                                                        "The FullName $fullname" +
                                                                                                "The EmailId : $emailD"
                                                                                    )

                                                                                    val share =
                                                                                        requireActivity().getSharedPreferences(
                                                                                            "DATA",
                                                                                            Context.MODE_PRIVATE
                                                                                        )
                                                                                    val editor =
                                                                                        share.edit()
                                                                                    editor.putString(
                                                                                        "name",
                                                                                        fullname
                                                                                    ).apply()
                                                                                    editor.putString(
                                                                                        "email",
                                                                                        emailD
                                                                                    ).apply()
                                                                                    editor.putBoolean(
                                                                                        "isSignIn",
                                                                                        true
                                                                                    ).apply()
                                                                                    editor.putString(
                                                                                        "customuserID",
                                                                                        userID
                                                                                    ).apply()

                                                                                    val intent =
                                                                                        Intent(
                                                                                            context,
                                                                                            MainActivity::class.java
                                                                                        )

                                                                                    resetSignInState("Success")

                                                                                    startActivity(intent)
                                                                                    requireActivity().finishAffinity()
                                                                                } else {
                                                                                    resetSignInState("User not found in inner onDataChange")
                                                                                    Toast.makeText(
                                                                                        requireContext(),
                                                                                        "User Not Found",
                                                                                        Toast.LENGTH_SHORT
                                                                                    ).show()
                                                                                }
                                                                            }

                                                                            override fun onCancelled(
                                                                                error: DatabaseError
                                                                            ) {

                                                                                resetSignInState("User not found")

                                                                                Log.d(
                                                                                    "Error:SignIn",
                                                                                    "Error:InFetching fulllName and Email ${error.message}"
                                                                                )

                                                                                Toast.makeText(
                                                                                    requireContext(),
                                                                                    "Couldn't fetch Users Information",
                                                                                    Toast.LENGTH_SHORT
                                                                                ).show()
                                                                            }

                                                                        })
                                                            }
                                                        }
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {

                                                        resetSignInState("Database error")
                                                        Log.d(
                                                            "Error:SignIn",
                                                            "Error:InFinding info of currentuserID , error is ${error.message}"
                                                        )
                                                        Toast.makeText(
                                                            requireContext(),
                                                            "Couldn't find of the currentUserID",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }

                                                })


                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Couldn't Find the User or Incorrect Email or Password.",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            resetSignInState("Auth failed")
                                        }
                                    }

                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Try SignIn Using Google",
                                    Toast.LENGTH_SHORT
                                ).show()
                                resetSignInState("Google SignIn")
                                return
                            }

                        }else
                        {
                            resetSignInState("No User")
                            Toast.makeText(requireContext(),"No User of This Email Exists",Toast.LENGTH_SHORT).show()
                            return
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            requireContext(),
                            "Error : Couldnt Find Email",
                            Toast.LENGTH_SHORT
                        ).show()

                        resetSignInState("Email Not Found")
                    }

                })

        }

        return binding.root
    }

    private fun resetSignInState(reason: String = "UnKnow") {
        signInInProgress = false

        timeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }

        bindin_?.let {
            binding.ProgressbarAnimation.cancelAnimation()
            binding.ProgressbarAnimation.visibility = View.GONE

        }
        Log.d("SignInDebug", "Resetting due to: $reason")
    }


    fun alertBoxForgotPassword() {

        if (isForgotDialogShowing) return

        val auth = FirebaseAuth.getInstance()

        val dialogView = layoutInflater.inflate(R.layout.alertbox_forgotpassword, null)

        val email = dialogView.findViewById<EditText>(R.id.EtEmailAlertBox_ForgotPassword).text
        val resetBtn = dialogView.findViewById<Button>(R.id.BtnResetPasswordAlertBoxForgotPassword)
        val cancelBtn = dialogView.findViewById<Button>(R.id.BtnCancelAlertBoxForgotPassword)

        val builder =
            AlertDialog.Builder(requireContext()).setView(dialogView).setCancelable(false).create()

        resetBtn.setOnClickListener {
            if (email.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Email Cannot be Empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!checkEmail(email.toString())) {
                Toast.makeText(requireContext(), "Wrong Email Format.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            database.orderByChild("email").equalTo(email.toString())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            auth.sendPasswordResetEmail(email.toString())
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Check Your Email",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        builder.dismiss()
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Error : Couldn't Send Email.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@addOnCompleteListener
                                    }
                                }.addOnFailureListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "No User Found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@addOnFailureListener
                                }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "No User of This Email Exists",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            requireContext(),
                            "Error : Couldn't Verify To database,Try Again Later.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                })

        }

        builder.setOnDismissListener {
            isForgotDialogShowing = false
        }

        isForgotDialogShowing = true

        cancelBtn.setOnClickListener {
            builder.dismiss()
        }

        builder.window?.setBackgroundDrawable(
            requireActivity().getColor(R.color.transparent).toDrawable()
        )


        builder.show()
    }

    fun checkEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun encodeEmail(email: String): String {
        return email.replace(".", ",")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindin_ = null
        timeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }
    }
}