package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Import your local adapters/models (adjust names if your folder differs)
import com.example.synctime.ChatFragment
import com.example.synctime.NewChatFragment
import com.example.synctime.Chat
import com.example.synctime.ChatListAdapter

class ChatListFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var btnNewChat: Button
    private lateinit var adapter: ChatListAdapter
    private val chats = mutableListOf<Chat>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)

        // --- initialize views ---
        recycler = view.findViewById(R.id.recyclerChats)
        btnNewChat = view.findViewById(R.id.btnNewChat)

        // --- setup adapter ---
        adapter = ChatListAdapter(chats) { chatId ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ChatFragment.newInstance(chatId))
                .addToBackStack(null)
                .commit()
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // --- mock chat data for now ---
        chats.add(Chat(id = "demo1", name = "Group Chat", lastMessage = "Welcome!", isGroup = true))
        adapter.notifyDataSetChanged()

        // --- handle "New Chat" button ---
        btnNewChat.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NewChatFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}
