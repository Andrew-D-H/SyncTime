package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class ChatFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var input: TextInputEditText
    private lateinit var sendBtn: FloatingActionButton
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()

    companion object {
        private const val CHAT_ID = "chatId"
        fun newInstance(chatId: String) = ChatFragment().apply {
            arguments = Bundle().apply { putString(CHAT_ID, chatId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        recycler = view.findViewById(R.id.recyclerMessages)
        input = view.findViewById(R.id.inputMessage)
        sendBtn = view.findViewById(R.id.btnSend)

        adapter = MessageAdapter(messages)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        sendBtn.setOnClickListener {
            val text = input.text.toString()
            if (text.isNotEmpty()) {
                val msg = Message(
                    senderId = "me",
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
                messages.add(msg)
                adapter.notifyItemInserted(messages.size - 1)
                recycler.scrollToPosition(messages.size - 1)
                input.text?.clear()
            }
        }

        return view
    }
}
