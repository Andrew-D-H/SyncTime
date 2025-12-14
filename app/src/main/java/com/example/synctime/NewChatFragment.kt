package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NewChatFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var btnCreate: Button
    private lateinit var adapter: UserListAdapter
    private var progressBar: ProgressBar? = null
    private var emptyText: TextView? = null

    private val users = mutableListOf<User>()
    private var selectedUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_new_chat, container, false)

        recycler = view.findViewById(R.id.recyclerUsers)
        btnCreate = view.findViewById(R.id.btnCreate)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)

        setupRecyclerView()
        setupCreateButton()
        loadUsers()

        return view
    }

    private fun setupRecyclerView() {
        adapter = UserListAdapter(users) { user ->
            selectedUser = user
            adapter.setSelectedUser(user.uid)
            btnCreate.isEnabled = true
            btnCreate.text = "Chat with ${user.displayName}"
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    private fun setupCreateButton() {
        btnCreate.isEnabled = false
        btnCreate.text = "Select a user"

        btnCreate.setOnClickListener {
            selectedUser?.let { user ->
                createChatWithUser(user)
            }
        }
    }

    private fun loadUsers() {
        progressBar?.visibility = View.VISIBLE
        
        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseRepository.getAllUsers().collectLatest { allUsers ->
                progressBar?.visibility = View.GONE
                
                users.clear()
                users.addAll(allUsers)
                adapter.notifyDataSetChanged()

                if (users.isEmpty()) {
                    emptyText?.visibility = View.VISIBLE
                    emptyText?.text = "No other users found"
                } else {
                    emptyText?.visibility = View.GONE
                }
            }
        }
    }

    private fun createChatWithUser(user: User) {
        btnCreate.isEnabled = false
        btnCreate.text = "Creating..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val chatId = FirebaseRepository.createChat(user.uid, user.displayName)
                
                // Navigate to the new chat
                parentFragmentManager.popBackStack() // Remove NewChatFragment
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(
                        R.id.fragment_container,
                        ChatFragment.newInstance(chatId, user.displayName),
                        "ChatFragment_$chatId"
                    )
                    .addToBackStack(null)
                    .commit()
                    
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Failed to create chat: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                btnCreate.isEnabled = true
                btnCreate.text = "Chat with ${user.displayName}"
            }
        }
    }
}
