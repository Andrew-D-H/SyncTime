package com.example.synctime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color

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
//        val tint: View = view.findViewById(R.id.bgTint)
        val more: ImageView = view.findViewById(R.id.imgMore)

        init {
            root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position], position)
                }
            }
            more.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMoreClick(items[position], it)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.avatar.setImageResource(item.avatarRes)
        holder.title.text = item.title
        holder.meta.text = item.meta

        if (item.isRead) {
            holder.root.setBackgroundColor(Color.TRANSPARENT)
            holder.dot.visibility = View.GONE
        } else {
            holder.root.setBackgroundColor(Color.parseColor("#F3F0FF")) // light purple
            holder.dot.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = items.size
}

