package com.example.timevault.View

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.timevault.R
import com.example.timevault.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private lateinit var database: DatabaseReference
    private lateinit var firebase: FirebaseAuth

    private lateinit var currentID: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this,R.color.black)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPref = getSharedPreferences("notification", MODE_PRIVATE)

        firebase = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("USERS")

        val share = getSharedPreferences(
            "DATA",
            Context.MODE_PRIVATE
        )

        currentID = share.getString("customuserID", null) ?: "Not Found"

        Log.d("CurrentUSerIDEditProfile", "The CurrentID inEditProfile is $currentID")

        binding.IbBackButtonOfSettings.setOnClickListener {
            finish()
        }

        val isEnabled = sharedPref.getBoolean("notifications_enabled", true)
        Log.d("NotificationStatus", "Is Enabled? $isEnabled")

        binding.SwitchNotificationSettings.isChecked = isEnabled

        binding.SwitchNotificationSettings.setOnCheckedChangeListener() { _,isChecked->
            sharedPref.edit().putBoolean("notifications_enabled", isChecked).apply()
            if (isChecked) {
                sharedPref.edit().putBoolean("notifications_enabled", true).apply()
                Log.d("SettingsNotification", "Notifications enabled")
            } else {
                sharedPref.edit().putBoolean("notifications_enabled", false).apply()
                Log.d("SettingsNotification", "Notifications disabled")
            }

        }

        binding.LLOtherVersionSettings.setOnClickListener {
            Toast.makeText(this,"App Version : ${binding.TvOtherVersionNumberSettings}",Toast.LENGTH_SHORT).show()
        }

        binding.LLEditProfileSettings.setOnClickListener {
            val intent = Intent(this, EditProfile_Activity::class.java)
            startActivity(intent)
        }
        binding.IBEditProfileSettings.setOnClickListener {
            val intent = Intent(this, EditProfile_Activity::class.java)
            startActivity(intent)
        }

        binding.LLChangePasswordOptionSettings.setOnClickListener {
            val intent = Intent(this, ChangePassword_Activity::class.java)
            startActivity(intent)
        }

        binding.IBChangePasswordSettings.setOnClickListener {
            val intent = Intent(this, ChangePassword_Activity::class.java)
            startActivity(intent)
        }

        binding.LLLogOutOptionSettings.setOnClickListener {
            logOutAlertBox()
        }

        binding.LLAppVersionOptionSettings.setOnClickListener {
            showAppVersionDialog()
        }
        binding.IBAppVersionSettings.setOnClickListener {
            showAppVersionDialog()
        }
        binding.TVVersionSettings.text = packageManager.getPackageInfo(packageName, 0).versionName


    }

//    private fun switchFragment(fragment: Fragment) {
//        supportFragmentManager.beginTransaction().replace(R.id.FLforFragmentsOfSettings, fragment)
//            .addToBackStack(null).commit()
//    }

    fun logOutAlertBox() {
        val dialogBox = layoutInflater.inflate(R.layout.alertbox_logout, null)

        val imgProfile = dialogBox.findViewById<ImageView>(R.id.IvProfileImageLogOut)
        val name = dialogBox.findViewById<TextView>(R.id.TvUserNameLogout)
        val logoutBtn = dialogBox.findViewById<Button>(R.id.BtnLogOutAlterBox)
        val cancelBtn = dialogBox.findViewById<Button>(R.id.BtnCancelLogOutAlertBox)

        val builder = AlertDialog.Builder(this).setView(dialogBox).setCancelable(false).create()

        database.child(currentID).get().addOnSuccessListener { data ->

            val username = (data.child("name").value ?: "Not Found").toString()
            val imgurl = (data.child("ImgUrl").value ?: "Not Found").toString()

            Log.d(
                "LogoutInfo", "The name is $username.\n" +
                        "The Url is $imgurl."
            )
            Glide.with(this)
                .load(imgurl)
                .placeholder(R.drawable.profile_image_vector)
                .error(R.drawable.error_vector)
                .into(imgProfile)

            name.text = username
        }

        logoutBtn.setOnClickListener {
            firebase.signOut()
            Toast.makeText(this, "SuccessFully Logged Out.", Toast.LENGTH_SHORT).show()

            val share = getSharedPreferences(
                "DATA",
                Context.MODE_PRIVATE
            )
            FirebaseFirestore.getInstance().collection("USERS")
                .document(currentID).update("fcmToken", FieldValue.delete())
                .addOnSuccessListener {
                    Log.d("Logout","Token deleted from FireStore")
                }
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().clear().apply()

            val editor = share.edit()
            editor.clear()
            editor.apply()

            val intent = Intent(this, Sign_In_Up_Activity::class.java)
            startActivity(intent)
            builder.dismiss()

            finishAffinity()

        }

        cancelBtn.setOnClickListener {
            builder.dismiss()
        }

        builder.show()

    }

    fun showAppVersionDialog() {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        val versionCode = packageManager.getPackageInfo(packageName, 0).versionCode

        val message = """
            🕰️ TimeVault – Version $versionName (Build $versionCode)

        Your memories. Secured for your future self.

        🔐 What's New in This Version:
        • Encrypted uploads to Cloudinary – securely store personal notes, messages, and files.
        • Create future events with time-locked content – your data is revealed only when the time comes.
        • Built-in date-time validation – no more accidental past entries.
        • Real-time push notifications via Firebase Cloud Messaging (FCM).
        • Clean Profile section for individuals and organizations.
        • UI polish & background performance improvements.

        🌟 What TimeVault Does:
        Think of TimeVault as a digital vault for your thoughts, promises, goals, or surprises — to your future self.
        Write a letter, upload a memory, or leave a reminder – and unlock it later.
        All data is encrypted and securely stored using Cloudinary.

        🛡️ Security & Trust:
        • End-to-end encrypted before upload.
        • Only you (or future you) can access your stored moments.
        • Built on secure cloud infrastructure.

        🤝 Made with purpose:
        TimeVault is more than an app. It’s a message-in-a-bottle — for yourself.
        Built with care by Alok Kumar & Team DeepSync.

        Thanks for trusting us with your time. ⏳
        """.trimIndent()


        val builder = AlertDialog.Builder(this, R.style.AlertBoxAppVersion)
            .setTitle("About TimeVault")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .create()

        builder.show()

        builder.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.purple_dark))

    }


}