package com.example.timevault.View

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.timevault.databinding.ActivityMainBinding
import com.qamar.curvedbottomnaviagtion.CurvedBottomNavigation
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.timevault.Model.DownloadUtils
import com.example.timevault.R
import com.example.timevault.ViewModel.BottomPopUp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.DexterBuilder
import com.karumi.dexter.DexterBuilder.MultiPermissionListener
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isPopupOpen = false
    private lateinit var profileimg: CardView
    private var popupwindow: PopupWindow? = null
//    private val ShiftDistanceX = 250f

    private lateinit var bottomNavViewHolder: BottomPopUp

    private lateinit var firebase: FirebaseAuth

    private lateinit var database: DatabaseReference

    private val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid

    private var profileName: String? = null
    private var profileEmail: String? = null

    companion object {
        const val Home_id = 1
        const val Vault_id = 2
    }

    private var currentFragment = Home_id

//    private lateinit var permissionLaucnher : ActivityResultLauncher<Array<String>>


    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavViewHolder = ViewModelProvider(this)[BottomPopUp::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        permissionLaucnher =
//            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
//            { result ->
//                val allGranted = result.entries.all { it.value }
//                if (allGranted) {
//                    DownloadUtils.markPermissionDenied(this) // Reset denied flag
//                    Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
//                } else {
//                    DownloadUtils.markPermissionDenied(this)
//                    Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
//                }
//            }

//        if (!DownloadUtils.hasAllPermission(this) && !DownloadUtils.wasPermissionDenied(this)) {
//            permissionLaucnher.launch(DownloadUtils.getNeededPermission().toTypedArray())
//        }

//        if (!currentUserID.isEmpty())
//        {
//            setContentView(R.layout.activity_main)
//        }else{
//            val intent = Intent(this,Sign_In_Up_Activity::class.java)
//            startActivity(intent)
//            finishAffinity()
//        }


        database = FirebaseDatabase.getInstance().getReference("USERS")
        firebase = FirebaseAuth.getInstance()

        if (savedInstanceState == null) {

            Log.d("MainActivity", "Accessing if (savedInstanceState == null) ")
            supportFragmentManager.beginTransaction()
                .replace(R.id.FrameLayoutMainActivity, Home_Fragment()).commit()
        } else {
            Log.d(
                "MainActivity",
                "Accessing savedInstanceState.getInt(\"CURRENT_FRAGMENT\", Home_id) "
            )
            currentFragment = savedInstanceState.getInt("CURRENT_FRAGMENT", Home_id)

            val fragment: Fragment = when (currentFragment) {
                1 -> Home_Fragment()
                2 -> VaultFragment()

                else -> Home_Fragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.FrameLayoutMainActivity, fragment)
                .commit()
        }

        val share = getSharedPreferences(
            "DATA",
            Context.MODE_PRIVATE
        )

        profileName = share.getString("name", null) ?: "Not Found"

        profileEmail = share.getString("email", null) ?: "Not Found"

        bottomNavViewHolder.chosenBottom.observe(this@MainActivity)
        { nav ->

            when (nav) {
                Home_id -> {
                    switchFragment(Home_id)
                }

                Vault_id -> {
                    switchFragment(Vault_id)
                }

                else -> {
                    switchFragment(Home_id)
                }
            }

            binding.bottomNavigation.show(nav)
        }


        bottomNavViewHolder.isPopupShown.observe(this@MainActivity)
        { visible ->
            Log.d("PopupObserver", "Popup visibility changed: $visible")
            if (visible) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    binding.root.post {
                        ShowPopUp()
                    }
                }
            } else HidePopUp()

        }

        if (bottomNavViewHolder.isPopupShown.value == true) {
            binding.root.post {
                ShowPopUp()
            }
        }

        val items = listOf(
            CurvedBottomNavigation.Model(1, getString(R.string.home), R.drawable.home_vector),
            CurvedBottomNavigation.Model(2, getString(R.string.vault), R.drawable.vault_vector),
        )

        binding.bottomNavigation.apply {
            items.forEach { add(it) }
            setOnClickMenuListener {

                bottomNavViewHolder.setBottom(it.id)
            }
            show(Home_id)
        }

        profileimg = binding.CVOfMainActivityProfilePic

        profileimg.setOnClickListener {

            bottomNavViewHolder.togglePopup()

        }

        binding.FloatingButton.setOnClickListener {

            val intent = Intent(this, vaultCreationActivity::class.java)
            startActivity(intent)
        }


    }

    //Outside OnCreate Function

    //Have to Rewrite all in MVVM , so the lifecycle is not reset everytime we rotate the device...

//    private fun animateLeft(ToLeft: Boolean) {
//        if (ToLeft) {
//            profileimg.animate().translationX(-ShiftDistanceX).rotation(360f).setDuration(300)
//                .start()
//        } else {
//            profileimg.animate().translationX(0f).rotation(-360f).setDuration(300).start()
//        }
//    }

    private fun ShowPopUp() {

        Log.d("Popup", "ShowPopUp called")

        if (!this@MainActivity.isFinishing && !this@MainActivity.isDestroyed) {

            if (popupwindow == null) {
                val popupview = layoutInflater.inflate(R.layout.popup_menu, null)
                val settingsbtn = popupview.findViewById<LinearLayout>(R.id.LLSettingsPopUp)
                val tvprofilename = popupview.findViewById<TextView>(R.id.ProfileNamePopUp)
                val tvprofileemail = popupview.findViewById<TextView>(R.id.ProfileEmailPopUp)
                val logOutBtn = popupview.findViewById<LinearLayout>(R.id.LLLogOutPopUp)

                popupwindow = PopupWindow(
                    popupview,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                ).apply {
                    elevation = 20f
                    isOutsideTouchable = true
                    setBackgroundDrawable(getColor(R.color.transparent).toDrawable())

                    setOnDismissListener {
                        bottomNavViewHolder.hidePopup()
                        popupwindow = null
                    }
                }

                settingsbtn.setOnClickListener {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }

                if (!profileName.isNullOrEmpty() && !profileEmail.isNullOrEmpty()) {
                    tvprofilename.text = profileName
                    tvprofileemail.text = profileEmail
                }


            }

            if (popupwindow!!.isShowing) return

            if (profileimg.isAttachedToWindow) {
                popupwindow?.showAsDropDown(profileimg)
            } else {
                profileimg.post {
                    popupwindow?.showAsDropDown(profileimg)
                }
            }


            bottomNavViewHolder.showPopup()


        }

    }

    private fun HidePopUp() {
        Log.d("Popup", "HidePopUp called")
        popupwindow?.dismiss()
        popupwindow = null

    }


    @SuppressLint("CommitTransaction")
    private fun switchFragment(itemId: Int) {
        if (itemId == currentFragment) return

        val fragment: Fragment = when (itemId) {
            1 -> Home_Fragment()
            2 -> VaultFragment()

            else -> Home_Fragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.FrameLayoutMainActivity, fragment)
            .commit()

        currentFragment = itemId
    }

    @SuppressLint("CommitTransaction")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (bottomNavViewHolder.isPopupShown.value == true) {
            HidePopUp()
            return
//            animateLeft(false)

        } else {

            val currentfragment =
                supportFragmentManager.findFragmentById(R.id.FrameLayoutMainActivity)
            if (currentfragment is Home_Fragment) {
                super.onBackPressed()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.FrameLayoutMainActivity, Home_Fragment()).commit()
                binding.bottomNavigation.show(1)
            }
        }
    }

    override fun onStop() {
        super.onStop()

        HidePopUp()
        bottomNavViewHolder.hidePopup()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("CURRENT_FRAGMENT", currentFragment)
    }

//    private fun requestPermission()
//    {
//        val permissionList = mutableListOf<String>()
//
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//        {
//            permissionList.add(android.Manifest.permission.READ_MEDIA_IMAGES)
//            permissionList.add(android.Manifest.permission.READ_MEDIA_VIDEO)
//        }else
//        {
//            permissionList.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
//
//            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
//            {
//                permissionList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
//            }
//        }
//
//        Dexter.withContext(this)
//            .withPermissions(*permissionList.toTypedArray())
//            .withListener(object : MultiplePermissionsListener {
//                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
//                    if (report?.areAllPermissionsGranted() == true) {
//                        Toast.makeText(applicationContext, "Permissions Granted!", Toast.LENGTH_SHORT).show()
//                        // 🔓 You can access and download files including PDFs now
//                    } else {
//                        Toast.makeText(applicationContext, "Permissions Denied!", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                override fun onPermissionRationaleShouldBeShown(
//                    permissions: List<PermissionRequest>,
//                    token: PermissionToken
//                ) {
//                    token.continuePermissionRequest()
//                }
//            }).check()
//    }
}

