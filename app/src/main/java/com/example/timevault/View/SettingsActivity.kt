package com.example.timevault.View

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.example.timevault.Model.ThemeHelper
import com.example.timevault.R
import com.example.timevault.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.type.Color

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private lateinit var database: DatabaseReference
    private lateinit var firebase: FirebaseAuth

    private lateinit var currentID: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val sharetheme = getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme =
            sharetheme.getString("themeOption", ThemeHelper.SYSTEM) ?: ThemeHelper.SYSTEM
        ThemeHelper.applyTheme(savedTheme)


        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedTheme == ThemeHelper.LIGHT)
        {
            // This line allows your layout to draw behind system
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Make status bar icons dark (good for light backgrounds)
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        firebase = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("USERS")

        val share = getSharedPreferences(
            "DATA", Context.MODE_PRIVATE
        )

        currentID = share.getString("customuserID", null) ?: "Not Found"

        Log.d("CurrentUSerIDEditProfile", "The CurrentID inEditProfile is $currentID")

        binding.IbBackButtonOfSettings.setOnClickListener {
            finish()
        }



        binding.LLOtherVersionSettings.setOnClickListener {
            Toast.makeText(
                this,
                "App Version : ${binding.TvOtherVersionNumberSettings.text}",
                Toast.LENGTH_SHORT
            ).show()
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
        binding.IBLogOutSettings.setOnClickListener {
            logOutAlertBox()
        }

        binding.LLAppVersionOptionSettings.setOnClickListener {
            showAppVersionDialog()
        }
        binding.IBAppVersionSettings.setOnClickListener {
            showAppVersionDialog()
        }
        binding.TVVersionSettings.text = packageManager.getPackageInfo(packageName, 0).versionName

        binding.TVPrivacyPolicySettings.setOnClickListener {
            showDialogPrivacyPlicy()
        }

        binding.TVHelpCenterSettings.setOnClickListener {
            showDialogHelpCenter()
        }
        binding.TVChoosenOptionSettings.text = savedTheme

        binding.LLThemeOptionSettings.setOnClickListener {
            showAlertDialogTheme()
        }
        binding.IBThemeSettings.setOnClickListener {
            showAlertDialogTheme()
        }

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
                "LogoutInfo", "The name is $username.\n" + "The Url is $imgurl."
            )
            if (!isDestroyed && !isFinishing) {
                Glide.with(this)
                    .load(imgurl)
                    .placeholder(R.drawable.profile_image_vector)
                    .error(R.drawable.error_vector)
                    .into(imgProfile)
            }

            name.text = username
        }

        logoutBtn.setOnClickListener {
            firebase.signOut()
            Toast.makeText(this, "SuccessFully Logged Out.", Toast.LENGTH_SHORT).show()

            val share = getSharedPreferences(
                "DATA", Context.MODE_PRIVATE
            )
            FirebaseFirestore.getInstance().collection("USERS").document(currentID)
                .update("fcmToken", FieldValue.delete()).addOnSuccessListener {
                    Log.d("Logout", "Token deleted from FireStore")
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

        builder.window?.setBackgroundDrawable(getColor(R.color.transparent).toDrawable())

        builder.show()

    }

    fun showAppVersionDialog() {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        val versionCode = packageManager.getPackageInfo(packageName, 0).versionCode

        val message = """
                        🕰️ TimeVault – Version $versionName (Build $versionCode)
                        
                        Your memories. Secured for your future self.
                        
                        🔐 About the App:
                        TimeVault is a secure and intelligent vault-based notification system built to help you preserve messages,
                         memories, and moments for the future. Whether it’s a thought, a goal, or a surprise – TimeVault lets you 
                         lock it away and be reminded at the right time.
                        
                        Backed by Firebase and modern cloud architecture, TimeVault offers seamless vault creation, real-time alerts, 
                        and secure storage — all in an intuitive, elegant interface.
                        
                        🧩 Core Features:
                            • Vault Creation:
                              Securely create personal vaults for storing important content tied to specific events or dates. 
                              Every vault is uniquely stored in Firebase for easy retrieval.
                        
                            • Time-Locked Events:
                              Create events with future unlock times – your messages and data remain hidden until the selected moment arrives.
                        
                            • Push Notifications:
                              Receive real-time push alerts through Firebase Cloud Messaging (FCM HTTP v1), keeping you updated on important vault activity.
                            
                            • Email Alerts:
                              Get email notifications delivered directly to your inbox via a secure Node.js backend using integrated Gmail services.
                            
                            • Notification Dashboard:
                              Access all notifications in a sleek bottom sheet interface with drag-handle support, offering smooth and organized viewing.
                        
                            • Notification Deletion:
                              Easily remove outdated or unnecessary notifications – changes reflect instantly in Firebase to keep your app synced.
                            
                            • Smart Search & Filtering:
                              Search vaults by name or unique ID with instant dynamic filtering.
                            
                            • Date-Time Validation:
                              Prevent accidental past entries with built-in validation to ensure accuracy when scheduling events.
                        
                        🎨 User Experience:
                        Crafted with modern Android design principles, TimeVault features:
                        • Polished UI elements
                        • Responsive layouts
                        • Themed components for a visually appealing experience
                        
                        🛡️ Security & Cloud Infrastructure:
                        • Encrypted content handling
                        • Realtime Database & Firestore support
                        • Cloudinary for secure media storage
                        • End-to-end safety for user data
                        
                        🛠️ Tech Stack:
                        • Kotlin (Android Development)
                        • Firebase Realtime DB & Firestore
                        • Firebase Cloud Messaging (HTTP v1)
                        • Node.js + Nodemailer (Email System)
                        • Cloudinary (Media Storage)
                        • Modern Android UI Components (BottomSheet, RecyclerView)
                        
                        💬 From the Creator:
                        TimeVault isn’t just an app — it’s a message in a bottle for your future self.
                         Built with care by **Alok Kumar**, it’s designed to make your memories last, and your intentions matter.
                        
                        Thanks for trusting us with your time. ⏳
                        """.trimIndent()


        val builder =
            AlertDialog.Builder(this, R.style.AlertBoxAppVersion).setTitle("About TimeVault")
                .setMessage(message).setPositiveButton("OK", null).create()

//        builder.window?.setBackgroundDrawable(getColor(R.color.transparent).toDrawable())

        builder.show()

        builder.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.purple_dark))


    }

    private fun showDialogPrivacyPlicy() {

        val privacyPolicyHtml = """
    <b>🔐 Privacy Policy</b><br><br>

    Thank you for using <b>TimeVault</b>.<br><br>

    Your privacy is important to us. This app is designed with your trust and security in mind. 
    Please read the following terms carefully to understand how your data is handled.<br><br>

    <b>📦 Data Collection & Usage:</b><br>
    <ul>
        <li>We store only the data you voluntarily create within vaults (e.g., text content, titles, timestamps).</li>
        <li>Your data is stored securely in Firebase Realtime Database and/or Firestore.</li>
        <li>We do not collect personal identifiers unless required for notifications (e.g., email for alerts).</li>
    </ul><br>

    <b>📩 Notifications & Emails:</b><br>
    <ul>
        <li>TimeVault uses Firebase Cloud Messaging (FCM) to send real-time push notifications.</li>
        <li>Email alerts are sent securely using a Node.js backend via authenticated Gmail service.</li>
        <li>Your email is used only for sending notifications — never shared or sold.</li>
    </ul><br>

    <b>🛡️ Data Security:</b><br>
    <ul>
        <li>All communication between the app and server is encrypted.</li>
        <li>Data is stored in trusted services like Firebase and Cloudinary with strict access controls.</li>
        <li>Your vault contents are never accessed or shared without your permission.</li>
    </ul><br>

    <b>🔍 Analytics & Tracking:</b><br>
    <ul>
        <li>TimeVault does <b>not</b> use any third-party analytics tools.</li>
        <li>No ads, tracking scripts, or profiling are included in the app.</li>
    </ul><br>

    <b>📵 User Control:</b><br>
    <ul>
        <li>You have full control over your data.</li>
        <li>You may delete notifications at any time — they’ll be removed from the server permanently.</li>
    </ul><br>

    <b>📅 Data Retention:</b><br>
    <ul>
        <li>Vault data remains stored until you choose to delete it.</li>
        <li>Notifications stay available unless removed by you.</li>
    </ul><br>

    <b>📜 Legal:</b><br>
    By using TimeVault, you agree to this privacy policy. Continued use of the app means you consent to our terms.<br>
    We may update this policy from time to time and will notify you of major changes.<br><br>

    <b>📧 Contact:</b><br>
    If you have any questions or concerns, feel free to email us at:<br>
    <a href="mailto:timevault001@gmail.com">timevault001@gmail.com</a><br><br>

    <i>Your trust matters. Thank you for using TimeVault. ⏳</i>
""".trimIndent()


        val builder =
            AlertDialog.Builder(this, R.style.AlertBoxAppVersion).setTitle("Privacy Policy")
                .setMessage(
                    HtmlCompat.fromHtml(
                        privacyPolicyHtml,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                )
                .setPositiveButton("OK", null).create()

//        builder.window?.setBackgroundDrawable(getColor(R.color.transparent).toDrawable())

        builder.show()

        builder.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.purple_dark))

        val privacyMessage = builder.findViewById<TextView>(android.R.id.message)
        privacyMessage?.movementMethod = LinkMovementMethod.getInstance()

    }

    private fun showDialogHelpCenter() {

        val helpCenterMessageHtml = """
    <b>📞 Help & Support – TimeVault</b><br><br>

    Need assistance? We’re here to help.<br><br>

    If you encounter any issues while using <b>TimeVault</b>, have suggestions, or simply need guidance on a feature — feel free to reach out to us. We aim to respond as quickly as possible and ensure your experience remains smooth and secure.<br><br>

    <b>🛠️ You can contact us for:</b><br>
    <ul>
        <li>Technical issues or bugs</li>
        <li>Questions about vault creation or notifications</li>
        <li>Help with account or data management</li>
        <li>Clarifications about privacy and security</li>
        <li>Feedback or feature suggestions</li>
    </ul><br>

    <b>📧 Support Email:</b><br>
    <a href="mailto:timevault001@gmail.com">timevault001@gmail.com</a><br><br>

    Please include details like your app version, device model, and a brief description of the issue to help us assist you better.<br><br>

    <i>Thank you for being part of TimeVault. ⏳</i>
""".trimIndent()


        val builder =
            AlertDialog.Builder(this, R.style.AlertBoxAppVersion).setTitle("Help Center")
                .setMessage(
                    HtmlCompat.fromHtml(
                        helpCenterMessageHtml,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                )
                .setPositiveButton("OK", null).create()

//        builder.window?.setBackgroundDrawable(getColor(R.color.transparent).toDrawable())

        builder.show()

        builder.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.purple_dark))

        val helpMessage = builder.findViewById<TextView>(android.R.id.message)
        helpMessage?.movementMethod = LinkMovementMethod.getInstance()

    }

    private fun showAlertDialogTheme() {
        val view = layoutInflater.inflate(R.layout.theme_alertdalog, null)

        val radioGroup = view.findViewById<RadioGroup>(R.id.RGOptionsTheme)
        val btnCancel = view.findViewById<Button>(R.id.BtnCancelTheme)
        val btnOk = view.findViewById<Button>(R.id.BtnOkTheme)

        val sharedPref = getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme = sharedPref.getString("themeOption", ThemeHelper.SYSTEM)

        when (savedTheme) {
            ThemeHelper.LIGHT -> radioGroup.check(R.id.light)
            ThemeHelper.DARK -> radioGroup.check(R.id.dark)
            ThemeHelper.SYSTEM -> radioGroup.check(R.id.system)
        }

        val builder = AlertDialog.Builder(this).setView(view).create()


        btnCancel.setOnClickListener {
            builder.dismiss()
        }

        btnOk.setOnClickListener {
            val radioID = radioGroup.checkedRadioButtonId
            if (radioID == -1) {
                Toast.makeText(this, "Select An Option", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedBtn = view.findViewById<RadioButton>(radioID)
            val theme = selectedBtn.text.toString().lowercase().trim()
            actionApplyTheme(theme)
            builder.dismiss()
        }

        builder.window?.setBackgroundDrawable(getColor(R.color.transparent).toDrawable())

        builder.show()

    }

    private fun actionApplyTheme(option: String) {
        val share = getSharedPreferences("theme", MODE_PRIVATE)

        Log.d("Theme", "Option Choosen $option")

        when (option) {
            "light" -> {
                share.edit().putString("themeOption", ThemeHelper.LIGHT).apply()
                ThemeHelper.applyTheme(ThemeHelper.LIGHT)
                recreate()
            }

            "dark" -> {
                share.edit().putString("themeOption", ThemeHelper.DARK).apply()
                ThemeHelper.applyTheme(ThemeHelper.DARK)
                recreate()
            }

            "system" -> {
                share.edit().putString("themeOption", ThemeHelper.SYSTEM).apply()
                ThemeHelper.applyTheme(ThemeHelper.SYSTEM)
                recreate()
            }

            else -> {
                share.edit().putString("themeOption", ThemeHelper.SYSTEM).apply()
                ThemeHelper.applyTheme(ThemeHelper.SYSTEM)
                recreate()
            }
        }
    }

}