package com.example.synctime

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FriendsAdapter(
    private val context: Context,
    private val onFriendSelected: (String, Boolean) -> Unit // Lambda for TripFragment
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    // Define the `FriendsAdapterCallback` for FriendsFragment
    interface FriendsAdapterCallback {
        fun onFriendRemoved(friendUid: String)
        fun onFriendSelected(friendUid: String, isSelected: Boolean)
    }

    private val friendsList: MutableList<Pair<String, Friend>> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val (friendUid, friend) = friendsList[position]

        // Display friend details
        holder.friendName.text = friend.name
        holder.friendStatus.text = if (friend.online) "Online" else "Offline"
        Glide.with(context)
            .load(friend.profileUrl)
            .circleCrop()
            .into(holder.friendProfile)

        // Handle friend selection
        holder.friendCheckbox.setOnCheckedChangeListener(null) // Avoid re-triggering due to recycling
        holder.friendCheckbox.isChecked = false // Default to unchecked
        holder.friendCheckbox.setOnCheckedChangeListener { _, isChecked ->
            onFriendSelected(friendUid, isChecked) // Notify the parent about selection
        }
    }

    override fun getItemCount(): Int = friendsList.size

    fun updateData(newFriends: List<Pair<String, Friend>>) {
        friendsList.clear()
        friendsList.addAll(newFriends)
        notifyDataSetChanged()
    }

    // ViewHolder for displaying friend items
    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val friendName: TextView = itemView.findViewById(R.id.tvFriendName)
        val friendStatus: TextView = itemView.findViewById(R.id.tvFriendStatus)
        val friendProfile: ImageView = itemView.findViewById(R.id.ivFriendProfile)
        val friendCheckbox: CheckBox = itemView.findViewById(R.id.friend_checkbox)
        val removeButton: ImageButton = itemView.findViewById(R.id.btnRemoveFriend)
    }
}