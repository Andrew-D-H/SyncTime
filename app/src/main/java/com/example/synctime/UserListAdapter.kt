package com.example.synctime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class UserListAdapter(
    private val users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {

    private var selectedUserId: String? = null

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.chatName)
        val userEmail: TextView = view.findViewById(R.id.chatLastMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // Reuse item_chat layout for user list
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.userName.text = user.displayName
        holder.userEmail.text = user.email

        // Highlight selected user
        if (user.uid == selectedUserId) {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.holo_blue_light)
            )
        } else {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.transparent)
            )
        }

        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount() = users.size

    fun setSelectedUser(userId: String) {
        val previousSelected = selectedUserId
        selectedUserId = userId
        
        // Update UI for previous and new selection
        users.forEachIndexed { index, user ->
            if (user.uid == previousSelected || user.uid == userId) {
                notifyItemChanged(index)
            }
        }
    }
}
