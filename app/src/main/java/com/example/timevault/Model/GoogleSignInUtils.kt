package com.example.timevault.Model

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.timevault.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoogleSignInUtils {

    companion object {

        private lateinit var currentId: String

        fun googleSignIn(
            context: Context,
            scope: CoroutineScope,
            launcher: ActivityResultLauncher<Intent>,
            onLogin: () -> Unit,
        ) {
            val credentialManager = CredentialManager.create(context)

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(getcredentialOption(context))
                .build()

            scope.launch {
                try {
                    val result = credentialManager.getCredential(context, request)

                    when (val credential = result.credential) {
                        is CustomCredential -> {
                            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(credential.data)
                                val googleEmail = googleIdTokenCredential.id
                                val googleTokenId = googleIdTokenCredential.idToken
                                val authCredential =
                                    GoogleAuthProvider.getCredential(googleTokenId, null)
                                val encodeEmail = googleEmail.replace(".", ",")

                                try {
//                                    val methodResult = FirebaseAuth.getInstance()
//                                        .fetchSignInMethodsForEmail(googleEmail).await()
//                                    val signInMethods = methodResult.signInMethods
//
//                                    val isExistingUser = !signInMethods.isNullOrEmpty()
//
//                                    if (isExistingUser) {
                                    val emailRef = FirebaseDatabase.getInstance()
                                        .getReference("EMAILMAP")
                                        .child(encodeEmail)

                                    emailRef.addListenerForSingleValueEvent(object :
                                        ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {

                                            if (snapshot.exists()) {
                                                val method =
                                                    snapshot.getValue(String::class.java)

                                                if (method == "GOOGLE") {
                                                    proceedGoogleSignIn(
                                                        context,
                                                        authCredential,
                                                        googleEmail,
                                                        encodeEmail,
                                                        scope,
                                                        onLogin
                                                    )
                                                } else if (method == "CUSTOM") {
                                                    Toast.makeText(
                                                        context,
                                                        "This email is registered with Email & Password. Please sign in using that.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    // This should not happen – handle missing EMAILMAP record
                                                    Toast.makeText(
                                                        context,
                                                        "Unknown sign-in method. Please contact support.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else {
                                                // New user, set sign-in method as GOOGLE

                                                val mapemail = mapOf(
                                                    encodeEmail to "GOOGLE"
                                                )
                                                FirebaseDatabase.getInstance()
                                                    .getReference("EMAILMAP")
                                                    .updateChildren(mapemail)
                                                    .addOnSuccessListener {
                                                        proceedGoogleSignIn(
                                                            context,
                                                            authCredential,
                                                            googleEmail,
                                                            encodeEmail,
                                                            scope,
                                                            onLogin
                                                        )
                                                    }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(
                                                context,
                                                "Error checking sign-in method",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
//                                    } else {
//                                        // New user, set sign-in method as GOOGLE
//                                        FirebaseDatabase.getInstance().getReference("EMAILMAP")
//                                            .child(encodeEmail)
//                                            .setValue("GOOGLE")
//                                            .addOnSuccessListener {
//                                                proceedGoogleSignIn(
//                                                    context,
//                                                    authCredential,
//                                                    googleEmail,
//                                                    encodeEmail,
//                                                    scope,
//                                                    onLogin
//                                                )
//                                            }
//                                    }
                                } catch (e: Exception) {
                                    if (e.message?.contains("account exists with different credential") == true) {
                                        Toast.makeText(
                                            context,
                                            "User Already Exists , SignIn With Email and Password",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                } catch (e: NoCredentialException) {
                    launcher.launch(getIntent())
                } catch (e: GetCredentialException) {
                    e.printStackTrace()
                }
            }
        }

        private fun proceedGoogleSignIn(
            context: Context,
            authCredential: com.google.firebase.auth.AuthCredential,
            googleEmail: String,
            encodeEmail: String,
            scope: CoroutineScope,
            onLogin: () -> Unit
        ) {
            scope.launch {
                try {
                    val user = FirebaseAuth.getInstance()
                        .signInWithCredential(authCredential)
                        .await().user

                    user?.let {
                        if (!it.isAnonymous) {
                            val data = DatabaseSignUp(
                                it.uid,
                                it.displayName,
                                it.email,
                                it.photoUrl.toString(),
                                "GOOGLE"
                            )

                            FirebaseDatabase.getInstance().getReference("USERS")
                                .orderByChild("email").equalTo(googleEmail)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (!snapshot.exists()) {
                                            currentId = useGenerateID(it.displayName.toString().replace(" ","_"))
                                            Log.d("Google_currentID", "Generated ID: $currentId")

                                            // Add to USERS
                                            FirebaseDatabase.getInstance()
                                                .getReference("USERS")
                                                .child(currentId)
                                                .setValue(data)

                                            // Add to UIDMAP
                                            val mapUid = mapOf(it.uid to currentId)
                                            FirebaseDatabase.getInstance()
                                                .getReference("UIDMAP")
                                                .updateChildren(mapUid)
                                        } else {
                                            currentId = snapshot.children.first().key.toString()
                                            Log.d(
                                                "Google_currentID",
                                                "Fetched existing ID: $currentId"
                                            )
                                        }

                                        // SharedPrefs
                                        val share =
                                            context.getSharedPreferences("DATA", MODE_PRIVATE)
                                        with(share.edit()) {
                                            putString("name", it.displayName)
                                            putString("email", it.email)
                                            putBoolean("isSignIn", true)
                                            putString("customuserID", currentId)
                                            apply()
                                        }

                                        onLogin.invoke()
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.d("Google_Real", "Realtime DB Error: ${error.message}")
                                    }
                                })
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun getIntent(): Intent {
            return Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
                putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
            }
        }

        private fun getcredentialOption(context: Context): CredentialOption {
            return GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(context.getString(R.string.server_clientID))
                .build()
        }
    }
}
