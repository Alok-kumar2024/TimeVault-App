package com.example.timevault.ViewModel

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.timevault.Model.VaultCretionFireStore
import com.example.timevault.R
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale

class VaultItemShowHomeAdapter(
    private val vaultInfo: MutableList<VaultCretionFireStore>,
    private val onVaultClick : (item : VaultCretionFireStore) -> Unit ,
    private val onMoreClick : (item : VaultCretionFireStore, View) -> Unit
) :
    RecyclerView.Adapter<VaultItemShowHomeAdapter.VaultFileHolder>() {

        private val handler =Handler(Looper.getMainLooper())
    private val updateRunnable = object  : Runnable {
        override fun run() {
            notifyDataSetChanged()
            handler.postDelayed(this,1000L)
        }
    }
    init {
        handler.post(updateRunnable)
    }

    inner class VaultFileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val vaultTitleRv : TextView = itemView.findViewById(R.id.TvShowVaultTitleRv)
        val statusRv : TextView = itemView.findViewById(R.id.TvShowStatusRv)
        val unlockTimeRv : TextView = itemView.findViewById(R.id.TvShowUnlockTimeRv)
        val MoreIbRv : ImageButton = itemView.findViewById(R.id.overflowBtn)
        val fullVaultRv : CardView = itemView.findViewById(R.id.vaultCard)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultFileHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.vault_lists_rv, parent, false)

        return VaultFileHolder(view)
    }

    override fun getItemCount(): Int {
        return vaultInfo.size
    }

    override fun onBindViewHolder(holder: VaultFileHolder, position: Int) {
        val items = vaultInfo[position]

        holder.vaultTitleRv.text = items.vaultname
        holder.statusRv.text = items.status
//        holder.unlockTimeRv.text = items.unlockTime.toString()

//        if (items.unlocked == true)
//        {
//            holder.ImageViewLockRv.setImageResource(R.drawable.lock_open_vector)
//        }


        if (items.status == "Locked") {
            if (items.unlockTime != null) {
                val date = items.unlockTime?.toDate()?.time
                val currentTime = System.currentTimeMillis()

                val diff = date?.minus(currentTime)

                if (diff != null && diff <= 0)
                {
                    holder.unlockTimeRv.text = "Unlocking..."
                }else
                {
                    val second = diff?.div(1000)
                    val minute = second?.div(60)
                    val hour = minute?.div(60)
                    val days = hour?.div(24)

                    val time = when {
                        days!! > 0 -> "$days day${if (days > 1) "s" else ""} left"
                        hour > 0 -> "$hour hr ${minute % 60} min left"
                        minute > 0 -> "$minute min left"
                        else -> "$second sec left"
                    }

                    holder.unlockTimeRv.text = time
                }

            } else {
                holder.unlockTimeRv.text = "No Date Found"
            }
        }else
        {
            holder.unlockTimeRv.text = "Unlocked"
        }


        holder.fullVaultRv.setOnClickListener {
            onVaultClick(items)
        }

        holder.MoreIbRv.setOnClickListener {
            onMoreClick(items,it)
        }

    }

}