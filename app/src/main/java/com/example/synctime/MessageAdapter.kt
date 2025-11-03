package com.example.synctime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messages: List<Message>
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

        // If the message was sent by the current user
        if (msg.senderId == "me") {
            holder.textView.setBackgroundResource(R.drawable.message_bubble_me)
            (holder.textView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 60
                marginEnd = 0
            }
            holder.textView.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
        } else {
            holder.textView.setBackgroundResource(R.drawable.message_bubble_them)
            (holder.textView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 0
                marginEnd = 60
            }
            holder.textView.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
    }


    override fun getItemCount() = messages.size
}
