package com.example.timevault.ViewModel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timevault.Model.VaultCretionFireStore
import com.example.timevault.R
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale

class VaultItemShowHomeAdapter(
    private val vaultInfo: MutableList<VaultCretionFireStore>,
    private val onVaultClick : (item : VaultCretionFireStore) -> Unit ,
    private val onSettingsClick : () -> Unit
) :
    RecyclerView.Adapter<VaultItemShowHomeAdapter.VaultFileHolder>() {

    inner class VaultFileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val vaultTitleRv : TextView = itemView.findViewById(R.id.TvShowVaultTitleRv)
        val statusRv : TextView = itemView.findViewById(R.id.TvShowStatusRv)
        val unlockTimeRv : TextView = itemView.findViewById(R.id.TvShowUnlockTimeRv)
        val settingsBtn : ImageButton = itemView.findViewById(R.id.IbSettingsRv)
        val ImageViewLockRv : ImageView = itemView.findViewById(R.id.IvLockRv)
        val fullVaultRv : LinearLayout = itemView.findViewById(R.id.LLWholeRv)

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

        val date = items.unlockTime?.toDate()

        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm a", Locale.getDefault())

        holder.unlockTimeRv.text = sdf.format(date)


        holder.fullVaultRv.setOnClickListener {
            onVaultClick(items)
        }

        holder.settingsBtn.setOnClickListener {
            onSettingsClick()
        }
    }
}