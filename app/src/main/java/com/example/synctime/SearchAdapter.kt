package com.example.synctime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SearchAdapter(
    private var users: MutableList<Friend> = mutableListOf(),
    private val onAddClick: (Friend) -> Unit,
    private val onCancelClick: (Friend) -> Unit
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvFriendName)
        val profile: ImageView = itemView.findViewById(R.id.ivFriendProfile)
        val btnAdd: Button = itemView.findViewById(R.id.btnAddFriend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]

        holder.name.text = user.name

        Glide.with(holder.itemView.context)
            .load(user.profileUrl)
            .placeholder(R.drawable.baseline_account_circle_24)
            .error(R.drawable.baseline_account_circle_24)
            .circleCrop()
            .into(holder.profile)

        // Update button text and enabled state based on pending status
        if (user.isPending) {
            holder.btnAdd.text = "Pending"
            holder.btnAdd.alpha = 0.6f
        } else {
            holder.btnAdd.text = "Add"
            holder.btnAdd.alpha = 1f
        }

        // Handle button clicks
        holder.btnAdd.setOnClickListener {
            if (user.isPending) {
                onCancelClick(user)
                user.isPending = false
            } else {
                onAddClick(user)
                user.isPending = true
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = users.size

    fun updateData(newUsers: List<Friend>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }
}
