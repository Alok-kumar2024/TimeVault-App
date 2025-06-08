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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoogleSignInUtils {

    companion object {
        fun googleSignIn(
            context: Context,
            scope: CoroutineScope,
            launcher: ActivityResultLauncher<Intent>,
            onLogin: () -> Unit
        ) {
            val credentialManager = CredentialManager.create(context)

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(getcredentialOption(context))
                .build()

            scope.launch {
                try {
                    val result = credentialManager.getCredential(context, request)

                    when (result.credential) {
                        is CustomCredential -> {
                            if (result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(result.credential.data)

                                val googleEmail = googleIdTokenCredential.id
                                val googleTokenId = googleIdTokenCredential.idToken
                                val authCredential =
                                    GoogleAuthProvider.getCredential(googleTokenId, null)

                                try {

                                    val methodResult = FirebaseAuth.getInstance()
                                        .fetchSignInMethodsForEmail(googleEmail).await()

                                    val signInMethods = methodResult.signInMethods

                                    if (signInMethods != null && signInMethods.contains("password")) {
                                        // Email registered with password, block Google sign-in
                                        Toast.makeText(
                                            context,
                                            "This email is already registered using Email & Password. Please sign in using that method.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@launch  // Stop further processing here
                                    }

                                    val user =
                                        FirebaseAuth.getInstance()
                                            .signInWithCredential(authCredential)
                                            .await().user

                                    user?.let {
                                        if (it.isAnonymous.not()) {

                                            val data = DatabaseSignUp(
                                                it.uid,
                                                it.displayName,
                                                it.email,
                                                it.photoUrl.toString()
                                            )

                                            val currentId = useGenerateID(it.displayName.toString())
                                            Log.d("Google_currentID", " currentId $currentId")

                                            FirebaseDatabase.getInstance().getReference("USERS")
                                                .orderByChild("email").equalTo(googleEmail)
                                                .addListenerForSingleValueEvent(object :
                                                    ValueEventListener {
                                                    override fun onDataChange(snapshot: DataSnapshot) {
                                                        if (snapshot.exists().not()) {
                                                            FirebaseDatabase.getInstance()
                                                                .getReference("USERS")
                                                                .child(currentId).setValue(data)
                                                                .addOnCompleteListener { task ->
                                                                    if (task.isSuccessful) {
                                                                        Log.d(
                                                                            "Google_Real",
                                                                            "SuccessFully Uploaded To RealTime Database"
                                                                        )
                                                                    } else {
                                                                        Log.d(
                                                                            "Google_Real",
                                                                            "Couldn't Upload To RealTime Database"
                                                                        )
                                                                    }
                                                                }
                                                        }
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                        Log.d(
                                                            "Google_Real",
                                                            "Couldn't fetch RealTime Database Error : ${error.message}"
                                                        )
                                                    }

                                                })

                                            val share = context.getSharedPreferences(
                                                "DATA",
                                                MODE_PRIVATE
                                            )
                                            val editor = share.edit()
                                            editor.putString("name", it.displayName).apply()
                                            editor.putString("email", it.email).apply()
                                            editor.putBoolean("isSignIn", true).apply()
                                            editor.putString("customuserID", currentId).apply()

                                            onLogin.invoke()


                                        }
                                    }
                                } catch (e: Exception) {
                                    if (e.message?.contains("account exists with different credential") == true) {
                                        // Ask user to sign in with Email/Password first, then link accounts
                                        Log.e(
                                            "GoogleSignIn",
                                            "Email already used. Linking required."
                                        )
                                        Toast.makeText(
                                            context,
                                            "User Already Exists , SignIn With Email and Password",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // OPTIONAL: Notify user with dialog to sign in with password and then link Google
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

        fun getIntent(): Intent {
            return Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
                putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
            }
        }

        fun getcredentialOption(context: Context): CredentialOption {
            return GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(context.getString(R.string.server_clientID))
                .build()
        }
    }
}