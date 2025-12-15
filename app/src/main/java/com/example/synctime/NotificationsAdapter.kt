package com.example.synctime

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NotificationsAdapter(
    private val items: List<Notification>,
    private val onItemClick: (Notification, Int) -> Unit,
    private val onMoreClick: (Notification, View) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.notificationRoot)
        val avatar: ImageView = view.findViewById(R.id.imgAvatar)
        val dot: View = view.findViewById(R.id.viewDot)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val meta: TextView = view.findViewById(R.id.tvMeta)
        val more: ImageView = view.findViewById(R.id.imgMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // Avatar from URL (fallback to default icon)
        Glide.with(holder.avatar.context)
            .load(item.senderProfileUrl)
            .placeholder(R.drawable.baseline_account_circle_24)
            .error(R.drawable.baseline_account_circle_24)
            .circleCrop()
            .into(holder.avatar)

        holder.title.text = item.title

        // If meta is empty, you can show a simple fallback from type
        holder.meta.text = if (item.meta.isNotBlank()) item.meta else item.type

        if (item.isRead) {
            holder.root.setBackgroundColor(Color.TRANSPARENT)
            holder.dot.visibility = View.GONE
        } else {
            holder.root.setBackgroundColor(Color.parseColor("#F3F0FF"))
            holder.dot.visibility = View.VISIBLE
        }

        // CLICK HANDLERS
        holder.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onItemClick(items[pos], pos)
            }
        }

        holder.more.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onMoreClick(items[pos], it)
            }
        }
    }

    override fun getItemCount() = items.size
}
