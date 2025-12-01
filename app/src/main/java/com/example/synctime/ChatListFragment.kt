package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var btnNewChat: Button
    private lateinit var adapter: ChatListAdapter
    private var progressBar: ProgressBar? = null
    private var emptyText: TextView? = null
    
    private val chats = mutableListOf<Chat>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)

        recycler = view.findViewById(R.id.recyclerChats)
        btnNewChat = view.findViewById(R.id.btnNewChat)
        
        // Optional: Add these to your layout for better UX
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)

        setupRecyclerView()
        setupNewChatButton()
        observeChats()

        return view
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter(chats) { chatId, chatName ->
            openChat(chatId, chatName)
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    private fun setupNewChatButton() {
        btnNewChat.setOnClickListener {
            // Navigate to NewChatFragment to select a user
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, NewChatFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun openChat(chatId: String, chatName: String) {
        val tag = "ChatFragment_$chatId"
        if (parentFragmentManager.findFragmentByTag(tag) == null) {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, ChatFragment.newInstance(chatId, chatName), tag)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observeChats() {
        progressBar?.visibility = View.VISIBLE
        
        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseRepository.getUserChats().collectLatest { userChats ->
                progressBar?.visibility = View.GONE
                
                chats.clear()
                chats.addAll(userChats)
                adapter.notifyDataSetChanged()

                // Show empty state if no chats
                if (chats.isEmpty()) {
                    emptyText?.visibility = View.VISIBLE
                    emptyText?.text = "No conversations yet.\nTap 'New Chat' to start one!"
                } else {
                    emptyText?.visibility = View.GONE
                }
            }
        }
    }
}
