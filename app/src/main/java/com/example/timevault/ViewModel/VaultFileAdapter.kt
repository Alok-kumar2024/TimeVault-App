package com.example.timevault.ViewModel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timevault.Model.fileType
import com.example.timevault.Model.vaultFileItem
import com.example.timevault.R

class VaultFileAdapter(
    private val file : MutableList<vaultFileItem> ,
    private val onDeleteClick : (vaultFileItem) -> Unit

) : RecyclerView.Adapter<VaultFileAdapter.VaultFileViewHolder>() {

    inner class VaultFileViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView)
    {
        val fileicon : ImageView = itemView.findViewById(R.id.IVFileIconRv)
        val fileName : TextView = itemView.findViewById(R.id.TvFileNameRv)
        val removebtn : ImageButton = itemView.findViewById(R.id.IBremoveButtonRv)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultFileViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.vault_items_rv,parent,false)

        return VaultFileViewHolder(view)
    }

    override fun getItemCount(): Int {
        return file.size
    }

    override fun onBindViewHolder(holder: VaultFileViewHolder, position: Int) {

        val item = file[position]

        val fileIcon = when{
            item.type == fileType.IMAGE -> R.drawable.vault_image_vector
            item.type == fileType.VIDEO -> R.drawable.vault_videofile_vector
            item.type == fileType.PDF -> R.drawable.vault_pdf_vector
            else -> R.drawable.vault_unknow_vector
        }

        holder.fileicon.setImageResource(fileIcon)
        holder.fileName.text = item.fileName
        holder.removebtn.setOnClickListener {
            onDeleteClick(item)
            notifyItemRemoved(position)
        }
    }
}