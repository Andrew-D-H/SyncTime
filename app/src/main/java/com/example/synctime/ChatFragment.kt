package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var input: TextInputEditText
    private lateinit var sendBtn: FloatingActionButton
    private lateinit var adapter: MessageAdapter
    private lateinit var toolbar: MaterialToolbar

    private var chatId: String = ""
    private var chatName: String = "Chat"
    private val messages = mutableListOf<Message>()

    companion object {
        private const val ARG_CHAT_ID = "chatId"
        private const val ARG_CHAT_NAME = "chatName"

        fun newInstance(chatId: String, chatName: String = "Chat") = ChatFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CHAT_ID, chatId)
                putString(ARG_CHAT_NAME, chatName)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        // Get arguments
        chatId = arguments?.getString(ARG_CHAT_ID) ?: ""
        chatName = arguments?.getString(ARG_CHAT_NAME) ?: "Chat"

        // Initialize views
        toolbar = view.findViewById(R.id.chatToolbar)
        recycler = view.findViewById(R.id.recyclerMessages)
        input = view.findViewById(R.id.inputMessage)
        sendBtn = view.findViewById(R.id.btnSend)

        setupToolbar()
        setupRecyclerView()
        setupSendButton()
        observeMessages()

        return view
    }

    private fun setupToolbar() {
        // Set the chat name as title, no subtitle
        toolbar.title = chatName
        toolbar.subtitle = null

        // Handle back button
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Load the actual chat name from Firebase (in case it's different)
        viewLifecycleOwner.lifecycleScope.launch {
            val displayName = FirebaseRepository.getChatDisplayName(chatId)
            if (displayName.isNotBlank() && displayName != "Chat") {
                toolbar.title = displayName
            }
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = FirebaseRepository.getCurrentUserId() ?: ""
        adapter = MessageAdapter(messages, currentUserId)

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true  // Start from bottom

        recycler.layoutManager = layoutManager
        recycler.adapter = adapter
    }

    private fun setupSendButton() {
        sendBtn.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                input.text?.clear()
            }
        }
    }

    private fun sendMessage(text: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                FirebaseRepository.sendMessage(chatId, text)
                // Message will appear via the real-time listener
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Failed to send message: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseRepository.getChatMessages(chatId).collectLatest { newMessages ->
                messages.clear()
                messages.addAll(newMessages)
                adapter.notifyDataSetChanged()

                // Scroll to bottom when new messages arrive
                if (messages.isNotEmpty()) {
                    recycler.scrollToPosition(messages.size - 1)
                }
            }
        }
    }
}