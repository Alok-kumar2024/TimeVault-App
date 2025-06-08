package com.example.timevault.View

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.timevault.Model.ThemeHelper
import com.example.timevault.R
import com.example.timevault.databinding.ActivityChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChangePassword_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityChangePasswordBinding

    private lateinit var firebase: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var currentID: String

    private var isForgotDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharetheme = getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme = sharetheme.getString("themeOption", ThemeHelper.SYSTEM) ?: ThemeHelper.SYSTEM
        ThemeHelper.applyTheme(savedTheme)

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebase = FirebaseAuth.getInstance()

        database = FirebaseDatabase.getInstance().getReference("USERS")

        val share = getSharedPreferences(
            "DATA",
            Context.MODE_PRIVATE
        )
        currentID = share.getString("customuserID", null) ?: "Not Found"

        Log.d("CurrentUSerIDChangePassword", "The CurrentID inEditProfile is $currentID")


        binding.IbBackButtonOfChangePassword.setOnClickListener {
            finish()
        }

        binding.TvForgotPasswordChangePassword.setOnClickListener {
            alertBoxForgotPassword()

        }

        binding.BtnChanegPasswordButton.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            val oldPass = binding.TIEcurrentPasswordChangePassword.text?.trim()
            val newPass = binding.TIENewPasswordChangePassword.text?.trim()
            val newConfirmPass = binding.TIENewPasswordConfirmChangePassword.text?.trim()

            if (user == null) {
                Toast.makeText(this, "User Not Found.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (oldPass.isNullOrEmpty()) {
//                binding.TIEcurrentPasswordChangePassword.error = "This is an Required Field."
                Toast.makeText(this, "Current Password Cannot be Empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPass.isNullOrEmpty()) {
                Toast.makeText(this, "New Password Cannot be Empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newConfirmPass.isNullOrEmpty()) {
                Toast.makeText(this, "Confirm Password cannot be Empty. ", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (newPass.toString() == newConfirmPass.toString()) {
                val credential = EmailAuthProvider.getCredential(user.email!!, oldPass.toString())

                user.reauthenticate(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        user.updatePassword(newPass.toString())
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(
                                        this,
                                        "SuccessFully Changed Password.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Error : Couldn't Update Password.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Incorrect Current Password.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            } else {
                Toast.makeText(
                    this,
                    "New Password and Confirm Password Not Matched",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
        }
    }

    fun alertBoxForgotPassword() {

        if (isForgotDialogShowing) return
        val auth = FirebaseAuth.getInstance()

        val dialogView = layoutInflater.inflate(R.layout.alertbox_forgotpassword, null)

        val email = dialogView.findViewById<EditText>(R.id.EtEmailAlertBox_ForgotPassword).text
        val resetBtn = dialogView.findViewById<Button>(R.id.BtnResetPasswordAlertBoxForgotPassword)
        val cancelBtn = dialogView.findViewById<Button>(R.id.BtnCancelAlertBoxForgotPassword)

        val builder = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

        resetBtn.setOnClickListener {
            if (email.isNullOrEmpty()) {
                Toast.makeText(this, "Email Cannot be Empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!checkEmail(email.toString())) {
                Toast.makeText(this, "Wrong Email Format.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            database.child(currentID).orderByChild("email").equalTo(email.toString())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            auth.sendPasswordResetEmail(email.toString())
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(
                                            this@ChangePassword_Activity,
                                            "Check Your Email",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        builder.dismiss()
                                    } else {
                                        Toast.makeText(
                                            this@ChangePassword_Activity,
                                            "Error : Couldn't Send Email.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@addOnCompleteListener
                                    }
                                }.addOnFailureListener {
                                Toast.makeText(
                                    this@ChangePassword_Activity,
                                    "No User Found",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@addOnFailureListener
                            }
                        } else {
                            Toast.makeText(
                                this@ChangePassword_Activity,
                                "No User of This Email Exists",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@ChangePassword_Activity,
                            "Error : Couldn't Verify To database,Try Again Later.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                })

        }

        cancelBtn.setOnClickListener {
            builder.dismiss()
        }

        builder.setOnDismissListener {
            isForgotDialogShowing = false
        }

        isForgotDialogShowing = true

        builder.show()
    }

    fun checkEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}