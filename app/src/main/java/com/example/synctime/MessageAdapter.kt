package com.example.synctime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.messageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        holder.textView.text = msg.text

        val layoutParams = holder.textView.layoutParams as ViewGroup.MarginLayoutParams
        val density = holder.itemView.context.resources.displayMetrics.density
        val marginLarge = (60 * density).toInt()
        val marginSmall = (8 * density).toInt()

        // Check if the message was sent by the current user
        if (msg.senderId == currentUserId) {
            // Sent by me - align right with "me" bubble
            holder.textView.setBackgroundResource(R.drawable.message_bubble_me)
            layoutParams.marginStart = marginLarge
            layoutParams.marginEnd = marginSmall
            holder.textView.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
        } else {
            // Sent by other - align left with "them" bubble
            holder.textView.setBackgroundResource(R.drawable.message_bubble_them)
            layoutParams.marginStart = marginSmall
            layoutParams.marginEnd = marginLarge
            holder.textView.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
        
        holder.textView.layoutParams = layoutParams
    }

    override fun getItemCount() = messages.size
}
