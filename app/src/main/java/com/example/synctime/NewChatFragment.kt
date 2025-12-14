package com.example.synctime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NewChatFragment : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var createButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private val userList = mutableListOf<User>()
    private lateinit var adapter: UserListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views with correct IDs from XML layout
        recyclerView = view.findViewById(R.id.recyclerUsers)
        createButton = view.findViewById(R.id.btnCreate)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = UserListAdapter(userList) { selectedUser ->
            // User clicked - create or find existing chat
            createOrFindChat(selectedUser)
        }
        recyclerView.adapter = adapter

        createButton.setOnClickListener {
            dismiss()
        }

        loadUsers()
    }

    private fun loadUsers() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        // Show loading state
        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()

                for (child in snapshot.children) {
                    // The key is the user's UID in Firebase
                    val databaseKey = child.key ?: continue

                    // Get the user data
                    val user = child.getValue(User::class.java) ?: continue

                    // Set the UID from the database key if not already set
                    if (user.uid.isBlank()) {
                        user.uid = databaseKey
                    }

                    // Skip current user
                    if (user.uid == currentUserId) continue

                    // Only add users that have an email (real users, not test data)
                    // This filters out userA, userB, userC which are test entries
                    if (user.email.isNotBlank()) {
                        userList.add(user)
                        Log.d("NewChatFragment", "Added user: ${user.getActualDisplayName()} (${user.email})")
                    }
                }

                // Hide loading, show empty text if no users
                progressBar.visibility = View.GONE
                if (userList.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = "No users available"
                }

                adapter.notifyDataSetChanged()

                Log.d("NewChatFragment", "Loaded ${userList.size} valid users")
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                emptyText.text = "Error loading users"
                emptyText.visibility = View.VISIBLE

                Log.e("NewChatFragment", "Failed to load users", error.toException())
            }
        })
    }

    private fun createOrFindChat(otherUser: User) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Validate that the other user has a valid UID
        if (otherUser.uid.isBlank()) {
            Toast.makeText(requireContext(), "Invalid user selected", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading while creating chat
        progressBar.visibility = View.VISIBLE

        // Create a consistent chat ID by sorting user IDs
        val chatId = if (currentUserId < otherUser.uid) {
            "${currentUserId}_${otherUser.uid}"
        } else {
            "${otherUser.uid}_${currentUserId}"
        }

        // Initialize the chat in Firebase if it doesn't exist
        val chatRef = FirebaseDatabase.getInstance()
            .getReference("chats")
            .child(chatId)

        chatRef.get().addOnSuccessListener { snapshot ->
            progressBar.visibility = View.GONE

            if (!snapshot.exists()) {
                // Create new chat metadata using the helper method for display name
                val chatName = otherUser.getActualDisplayName()

                val chatData = mapOf(
                    "id" to chatId,
                    "participants" to mapOf(currentUserId to true, otherUser.uid to true),
                    "createdAt" to System.currentTimeMillis(),
                    "lastMessage" to "",
                    "timestamp" to System.currentTimeMillis(),
                    "name" to chatName
                )
                chatRef.setValue(chatData)

                // Also add to userChats index for both users
                val userChatsRef = FirebaseDatabase.getInstance().getReference("userChats")
                userChatsRef.child(currentUserId).child(chatId).setValue(true)
                userChatsRef.child(otherUser.uid).child(chatId).setValue(true)
            }

            // Navigate to ChatFragment with the chatId
            val chatName = otherUser.getActualDisplayName()
            openChatFragment(chatId, chatName)

        }.addOnFailureListener { e ->
            progressBar.visibility = View.GONE

            Log.e("NewChatFragment", "Failed to create/find chat", e)
            Toast.makeText(
                requireContext(),
                "Failed to start chat: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openChatFragment(chatId: String, chatName: String) {
        dismiss() // Close the dialog first

        // Use the newInstance factory method from ChatFragment
        val chatFragment = ChatFragment.newInstance(chatId, chatName)

        // Navigate to ChatFragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, chatFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}