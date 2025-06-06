package com.example.timevault.ViewModel

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timevault.Model.vaultFilesDecrypt
import com.example.timevault.R
import java.text.SimpleDateFormat
import java.util.Locale

class VaultShowAdapter(
    private val fileList : List<vaultFilesDecrypt> ,
    private val onDownloadClick : (vaultFilesDecrypt) -> Unit
) : RecyclerView.Adapter<VaultShowAdapter.HolderVaultShowInVaultShow>() {

    inner class HolderVaultShowInVaultShow(itemview : View) : RecyclerView.ViewHolder(itemview)
    {
        val filename = itemview.findViewById<TextView>(R.id.TvShowFileNameRv)
        val icon = itemview.findViewById<ImageView>(R.id.IvShowImageOfFileType)
        val uploadedDate = itemview.findViewById<TextView>(R.id.TvShowUploadDateRv)
        val downloadBtn = itemview.findViewById<ImageButton>(R.id.IbDownloadFilesRv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderVaultShowInVaultShow {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.vaultfiles_rv,parent,false)
        return HolderVaultShowInVaultShow(view)
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    override fun onBindViewHolder(holder: HolderVaultShowInVaultShow, position: Int) {
        val item = fileList[position]


//        holder.icon.setImageResource()

        val mimeType = when {
            item.file?.endsWith(".pdf") == true -> R.drawable.pdf_icon
            item.file?.endsWith(".jpg") == true || item.file?.endsWith(".jpeg") == true ->R.drawable.image_icon
            item.file?.endsWith(".png") == true -> R.drawable.image_icon
            item.file?.endsWith(".mp4") == true -> R.drawable.video_icon
            else -> R.drawable.vault_unknow_vector
        }

        holder.icon.setImageResource(mimeType)

        holder.filename.text = item.file

        Log.d("VaultShowAdapter", "uploadTime = ${item.uploadTime}")

//        holder.uploadedDate.text =
//            item.uploadTime?.let {
//                try {
//                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // Match the input string format
//                    val outputFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) // Desired display format
//
//                    val parseDate = inputFormat.parse(it)
//
//                    parseDate?.let { date-> outputFormat.format(date) }
//                }catch (e : Exception)
//                {
//                    e.printStackTrace()
//                    "Invalid date"
//                }
//            }
        val date = item.uploadTime?.toDate()

        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm a", Locale.getDefault())
        holder.uploadedDate.text = sdf.format(date)


        holder.downloadBtn.setOnClickListener {
            onDownloadClick(item)
        }

    }
}