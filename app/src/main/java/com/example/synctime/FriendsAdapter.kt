package com.example.synctime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.graphics.Color

class FriendsAdapter :
    RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>(), Filterable {

    private var friendsList: MutableList<Friend> = mutableListOf()
    private var filteredList: MutableList<Friend> = mutableListOf()

    fun addFriend(friend: Friend) {
        friendsList.add(friend)
        filteredList.add(friend)
        notifyItemInserted(filteredList.size - 1)
    }

    fun removeFriend(friend: Friend) {
        friendsList.remove(friend)
        filteredList.remove(friend)
        notifyDataSetChanged()
    }

    fun updateData(newFriends: List<Friend>) {
        friendsList.clear()
        friendsList.addAll(newFriends)
        filteredList.clear()
        filteredList.addAll(newFriends)
        notifyDataSetChanged()
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById<TextView>(R.id.tvFriendName)
        val status = itemView.findViewById<TextView>(R.id.tvFriendStatus)
        val profile = itemView.findViewById<ImageView>(R.id.ivFriendProfile)
        val remove = itemView.findViewById<ImageButton>(R.id.btnRemoveFriend)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    // button clicks
    var onRemoveClick: ((Friend) -> Unit)? = null
    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = filteredList[position]
        holder.name.text = friend.name
        holder.status.text = if (friend.online) "Online" else "Offline"
        holder.status.setTextColor(
            if (friend.online) Color.parseColor("#6E43FF")
            else Color.parseColor("#999999")
        )
        Glide.with(holder.itemView.context)
            .load(friend.profileUrl) // URL or URI stored in Friend object
            .placeholder(R.drawable.baseline_account_circle_24)
            .error(R.drawable.baseline_account_circle_24)
            .circleCrop()
            .into(holder.profile)

        holder.remove.setOnClickListener {
            onRemoveClick?.invoke(friend)
        }
    }

    override fun getItemCount() = filteredList.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase()?.trim() ?: ""
                filteredList = if (query.isEmpty()) friendsList
                else friendsList.filter { it.name.lowercase().contains(query) }.toMutableList()
                return FilterResults().apply { values = filteredList }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = (results?.values as? List<Friend>)?.toMutableList() ?: mutableListOf()
                notifyDataSetChanged()
            }
        }
    }
}
