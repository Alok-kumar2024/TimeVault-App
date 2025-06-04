package com.example.timevault.ViewModel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timevault.Model.Notification
import com.example.timevault.R
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationShowAdapter(
    val NotificationList : MutableList<Notification> ,
    val onDeleteClick : (item : Notification) -> Unit
) : RecyclerView.Adapter<NotificationShowAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView)
    {
        val titleRv : TextView = itemView.findViewById(R.id.TvTitleNotificationRv)
        val bodyRv : TextView = itemView.findViewById(R.id.TvBodyNotificationRv)
        val timeRv : TextView = itemView.findViewById(R.id.TvTimeNotificationRv)
        val deleteBtn : ImageButton = itemView.findViewById(R.id.IbDeleteNotificationRv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.notification_litem_rv,parent,false)
        return NotificationViewHolder(view)
    }

    override fun getItemCount(): Int {
        return NotificationList.size
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = NotificationList[position]

        holder.titleRv.text = item.title
        holder.bodyRv.text = item.body

        val date = item.timestamp?.toDate()

        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm a", Locale.getDefault())

        holder.timeRv.text = sdf.format(date)

        holder.deleteBtn.setOnClickListener {
            onDeleteClick(item)
        }
    }
}