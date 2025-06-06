package com.example.timevault.Model

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import com.example.timevault.R
import com.example.timevault.View.MainActivity
import com.google.firebase.Timestamp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.time.Instant
import java.util.UUID

class MyFireBaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {

        val notificationID =  message.data["notificationId"] ?: return

        val getdata = getSharedPreferences("DATA", MODE_PRIVATE)
        val userID = getdata.getString("customuserID", null) ?: "Not Received"
        Log.d("USERIDvault", "The Custom userid from sharedPreference is $userID")

        val title = message.data["title"] ?: "vault Notification"
        val body = message.data["body"] ?: "You have a new message"

        val data = Notification(notificationID,title,body,System.currentTimeMillis(),false)

        val database = FirebaseDatabase.getInstance()

       val notifyRef =  database.getReference("USERS")
            .child(userID)
           .child("Notification")
            .child(notificationID)

        notifyRef.setValue(data)
            .addOnSuccessListener {
                Log.d("FCM", "Notification saved to Realtime DB")
            }.addOnFailureListener {
                Log.e("FCM", "Failed to save notification: ${it.message}")
            }


        val sharedPref = getSharedPreferences("prefs", MODE_PRIVATE)

        if (!sharedPref.getBoolean("notifications_enabled", true)) {
            Log.d("FCM", "Notification blocked by user settings")

            return
        }

        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this,0,intent,PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this,"vault_channel")
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val channel = NotificationChannel(
                "vault_channel","Vault Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0,notificationBuilder.build())

    }
}