package com.example.timevault.View

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.timevault.databinding.ActivityMainBinding
import com.qamar.curvedbottomnaviagtion.CurvedBottomNavigation
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.timevault.Model.DownloadUtils
import com.example.timevault.Model.Notification
import com.example.timevault.Model.SearchFragments
import com.example.timevault.Model.ThemeHelper
import com.example.timevault.R
import com.example.timevault.ViewModel.BottomPopUp
import com.example.timevault.ViewModel.NotificationShowAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
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

    private val notificationList = mutableListOf<Notification>()
    private lateinit var notificationAdapter : NotificationShowAdapter

    private var bottomItem = mutableListOf<Notification>()
    private lateinit var BottomAdapter: NotificationShowAdapter

    private lateinit var bottomNavViewHolder: BottomPopUp

    private lateinit var uniqueKey: String

    private lateinit var firebase: FirebaseAuth

    private lateinit var database: DatabaseReference

    private val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

    private var profileName: String? = "Loading"
    private var profileEmail: String? = "Loading"
    private lateinit var ImgProfile: String

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

        val sharetheme = getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme = sharetheme.getString("themeOption", ThemeHelper.SYSTEM) ?: ThemeHelper.SYSTEM
        ThemeHelper.applyTheme(savedTheme)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("notification", MODE_PRIVATE)

        bottomNavViewHolder = ViewModelProvider(this)[BottomPopUp::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {
                sharedPref.edit().putBoolean("notifications_enabled", true).apply()
            }
        } else {
            sharedPref.edit().putBoolean("notifications_enabled", true).apply()
        }

        val isEnabled = sharedPref.getBoolean("notifications_enabled", true)
        Log.d("NotificationStatus", "Is Enabled? $isEnabled")

        val share = getSharedPreferences(
            "DATA",
            Context.MODE_PRIVATE
        )
        uniqueKey = share.getString("customuserID", null) ?: "Not Found"

        val isSignIn = share.getBoolean("isSignIn", false) ?: false

        if (!isSignIn) {
            val intent = Intent(this, Sign_In_Up_Activity::class.java)
            startActivity(intent)
            finishAffinity()
        }

        checkUnseenNotifications(uniqueKey) { hasSeen ->

            if (hasSeen) {
                binding.ViewNotificationRedDotMainActivity.visibility = View.VISIBLE
            } else {
                binding.ViewNotificationRedDotMainActivity.visibility = View.GONE
            }

        }

        binding.IbNotificationMainActivity.setOnClickListener {
            showBottomDialog()
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


        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    FirebaseFirestore.getInstance().collection("USERS").document(uniqueKey)
                        .set(mapOf("fcmToken" to token), SetOptions.merge()).addOnSuccessListener {
                            Log.d("FCM", "Token uploaded To FireStore")
                        }.addOnFailureListener {
                            Log.d("FCM", "Token Not Uploaded To firestore")
                        }
                }
            } else {
                Log.e("FCM", "Failed To get Token")
            }
        }
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


        database.child(uniqueKey).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    profileName = snapshot.child("name").value.toString()
                    profileEmail = snapshot.child("email").value.toString()
                    ImgProfile = snapshot.child("ImgUrl").value.toString()

                    Log.d(
                        "UserInfo", "The UserName : $profileName" +
                                "\nThe UserEmail : $profileEmail" +
                                "\nThe UserImage : $ImgProfile"
                    )

                    if (!isFinishing && !isDestroyed) {
                        Glide.with(this@MainActivity)
                            .load(ImgProfile)
                            .placeholder(R.drawable.account_image_vector)
                            .error(R.drawable.error_vector)
                            .into(binding.IVOfMainActivityProfilePic)
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Database", "Error : ${error.message}")
            }

        })

//        profileName = share.getString("name", null) ?: "Not Found"
//
//        profileEmail = share.getString("email", null) ?: "Not Found"

        bottomNavViewHolder.chosenBottom.observe(this@MainActivity)
        { nav ->
            binding.EtSearchVaultMainActivity.text.clear()

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

        binding.EtSearchVaultMainActivity.addTextChangedListener { editable ->
            val searchText = editable.toString()
            val currentFragment =
                supportFragmentManager.findFragmentById(R.id.FrameLayoutMainActivity)

            if (currentFragment is SearchFragments) {
                currentFragment.filterVaults(searchText)
            }
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

                logOutBtn.setOnClickListener {
                    logOutAlertBox()
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

    fun logOutAlertBox() {
        val dialogBox = layoutInflater.inflate(R.layout.alertbox_logout, null)

        val imgProfile = dialogBox.findViewById<ImageView>(R.id.IvProfileImageLogOut)
        val name = dialogBox.findViewById<TextView>(R.id.TvUserNameLogout)
        val logoutBtn = dialogBox.findViewById<Button>(R.id.BtnLogOutAlterBox)
        val cancelBtn = dialogBox.findViewById<Button>(R.id.BtnCancelLogOutAlertBox)

        val builder =
            android.app.AlertDialog.Builder(this).setView(dialogBox).setCancelable(false).create()

        database.child(uniqueKey).get().addOnSuccessListener { data ->

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

            val map = mapOf("fcmToken" to FieldValue.delete())
            FirebaseFirestore.getInstance().collection("USERS")
                .document(uniqueKey).update(map)
                .addOnSuccessListener {
                    Log.d("Logout", "Token deleted from FireStore")
                }
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().clear().apply()
            val share = getSharedPreferences(
                "DATA",
                Context.MODE_PRIVATE
            )
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val sharedPref = getSharedPreferences("notification", MODE_PRIVATE)
        if (requestCode == 1001) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                sharedPref.edit().putBoolean("notifications_enabled", true).apply()
                Log.d("permission", "Notification Permission granted")
            } else {
                sharedPref.edit().putBoolean("notifications_enabled", false).apply()
                Log.d("permission", "Notification permission denied")
            }
        }
    }

    private fun showBottomDialog() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.notification_bottomsheet, null)

        val text: TextView = view.findViewById(R.id.TvNoNotification_NotificationBottomSheet)
        val RvNotification: RecyclerView = view.findViewById(R.id.RvForBottomSheet)

        dialog.setContentView(view)

        RvNotification.layoutManager = LinearLayoutManager(this)

        notificationAdapter =
            NotificationShowAdapter(notificationList, onDeleteClick = { notify ->
                deleteNotification(notify)
            })

        RvNotification.adapter = notificationAdapter

        database.child(uniqueKey).child("Notification").orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        notificationList.clear()

                        for (notification in snapshot.children) {
                            val notify = notification.getValue(Notification::class.java)

                            notify?.let {
                                notificationList.add(notify)
                            }

                            if (notify?.seen == false) {
                                val updateData = mapOf(
                                    "seen" to true
                                )
                                notify.notificationId?.let {
                                    database.child(uniqueKey).child("Notification").child(it)
                                        .updateChildren(updateData)
                                        .addOnSuccessListener {
                                            Log.d("Notification", "Updated Seen to true")
                                        }.addOnFailureListener {
                                            Log.e(
                                                "Notification",
                                                "Couldn't update seen to true , error : ${it.message}"
                                            )
                                        }
                                }
                            }
                        }

                        val sortList = notificationList
                            .sortedWith(compareBy<Notification> { !(it.seen) }
                                .thenByDescending { it.timestamp }).toMutableList()

                        notificationList.clear()
                        notificationList.addAll(sortList)
                        notificationAdapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Notification", "Error Fetching Data , error ${error.message}")
                }

            })

        checkUnseenNotifications(uniqueKey) { hasSeen ->

            if (hasSeen) {
                binding.ViewNotificationRedDotMainActivity.visibility = View.VISIBLE
            } else {
                binding.ViewNotificationRedDotMainActivity.visibility = View.GONE
            }

        }

        if (notificationList.isEmpty()) {
            text.visibility = View.VISIBLE
        } else {
            text.visibility = View.GONE
        }

        dialog.show()

    }

    private fun deleteNotification(notify: Notification) {
        notify.notificationId?.let {
            database.child(uniqueKey)
                .child("Notification")
                .child(it).removeValue().addOnSuccessListener {
                    notificationList.remove(notify)
                    notificationAdapter.notifyDataSetChanged()
                    Toast.makeText(this,"SuccessFully Deleted The Notification",Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this,"Error : Couldn't Delete the Notification",Toast.LENGTH_SHORT).show()
                }

        }

    }

    private fun checkUnseenNotifications(userID: String, callback: (Boolean) -> Unit) {

        val ref =
            FirebaseDatabase.getInstance().getReference("USERS").child(userID).child("Notification")
        ref.orderByChild("seen").equalTo(false)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.exists()) // if there's any unseen notification
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DB", "Error checking unseen notifications: ${error.message}")
                    callback(false)
                }
            })
    }

}

