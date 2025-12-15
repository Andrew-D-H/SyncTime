package com.example.synctime

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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

        recyclerView = view.findViewById(R.id.recyclerUsers)
        createButton = view.findViewById(R.id.btnCreate)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = UserListAdapter(userList) { selectedUser ->
            createOrFindChat(selectedUser)
        }
        recyclerView.adapter = adapter

        createButton.setOnClickListener { dismiss() }

        loadUsers()
    }

    // Helper: safe display name even if displayName is missing
    private fun getActualDisplayName(user: User): String {
        val dn = user.displayName?.trim().orEmpty()
        if (dn.isNotBlank()) return dn

        val email = user.email.trim()
        if (email.isNotBlank()) return email.substringBefore("@")

        return "Unknown"
    }

    private fun loadUsers() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()

                for (child in snapshot.children) {
                    val databaseKey = child.key ?: continue

                    // pull object
                    val user = child.getValue(User::class.java) ?: continue

                    // IMPORTANT: do NOT mutate user.uid (might be a val). Use the key.
                    val uid = if (user.uid.isNotBlank()) user.uid else databaseKey

                    // Skip current user
                    if (uid == currentUserId) continue

                    // Only real users with email
                    if (user.email.isNotBlank()) {
                        // If your User.uid is used elsewhere, this is safe only if uid is a var.
                        // If uid is a val, leave it alone.
                        val uid = if (user.uid.isNotBlank()) user.uid else databaseKey

                        if (uid == currentUserId) continue

                        if (user.email.isNotBlank()) {
                            userList.add(user)
                            Log.d("NewChatFragment", "Added user: ${getActualDisplayName(user)} (${user.email}) uid=$uid")
                        }


                        userList.add(user)
                        Log.d("NewChatFragment", "Added user: ${getActualDisplayName(user)} (${user.email}) uid=$uid")
                    }
                }

                progressBar.visibility = View.GONE

                if (userList.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = "No users available"
                } else {
                    emptyText.visibility = View.GONE
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

        // We rely on uid being present. If your User.uid is sometimes blank, this protects you:
        val otherUserId = otherUser.uid.trim()
        if (otherUserId.isBlank()) {
            Toast.makeText(requireContext(), "Invalid user selected", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        // IMPORTANT: Firebase Realtime Database keys cannot contain '.', '#', '$', '[', ']', '/'
        // Your UID is safe; emails are NOT. So we only use UID here.
        val chatId = if (currentUserId < otherUserId) {
            "${currentUserId}_${otherUserId}"
        } else {
            "${otherUserId}_${currentUserId}"
        }

        val chatRef = FirebaseDatabase.getInstance()
            .getReference("chats")
            .child(chatId)

        chatRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val chatName = getActualDisplayName(otherUser)

                val chatData = mapOf(
                    "id" to chatId,
                    "participants" to mapOf(currentUserId to true, otherUserId to true),
                    "createdAt" to System.currentTimeMillis(),
                    "lastMessage" to "",
                    "timestamp" to System.currentTimeMillis(),
                    "name" to chatName
                )

                chatRef.setValue(chatData)

                val userChatsRef = FirebaseDatabase.getInstance().getReference("userChats")
                userChatsRef.child(currentUserId).child(chatId).setValue(true)
                userChatsRef.child(otherUserId).child(chatId).setValue(true)
            }

            progressBar.visibility = View.GONE

            val chatName = getActualDisplayName(otherUser)
            openChatFragment(chatId, chatName)

        }.addOnFailureListener { e ->
            progressBar.visibility = View.GONE
            Log.e("NewChatFragment", "Failed to create/find chat", e)
            Toast.makeText(requireContext(), "Failed to start chat: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openChatFragment(chatId: String, chatName: String) {
        dismiss()

        val chatFragment = ChatFragment.newInstance(chatId, chatName)

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
