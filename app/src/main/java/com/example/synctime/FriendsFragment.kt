package com.example.synctime

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


class FriendsFragment : Fragment(R.layout.manage_friends) {
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var pendingAdapter: FriendsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Manage friends/requests setup
        val rvFriends = view.findViewById<RecyclerView>(R.id.rvFriendsList)
        val rvPending = view.findViewById<RecyclerView>(R.id.rvPendingRequests)
        val etSearch = view.findViewById<EditText>(R.id.etSearchFriends)

        // Search setup
        val rvSearch = view.findViewById<RecyclerView>(R.id.rvSearchResults)
        val etSearchUsers = view.findViewById<EditText>(R.id.etSearchUsers)
        val searchAdapter = SearchAdapter(
            onAddClick = { user -> sendFriendRequest(user.uid) },
            onCancelClick = { user -> cancelFriendRequest(user.uid) })
        rvSearch.layoutManager = LinearLayoutManager(requireContext())
        rvSearch.adapter = searchAdapter

        etSearchUsers.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) rvSearch.visibility = View.GONE
                else {
                    rvSearch.visibility = View.VISIBLE
                    searchUsers(query, searchAdapter)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })


        createTestData()  // testing

        // Initialize adapters with empty lists
        friendsAdapter = FriendsAdapter()
//        pendingAdapter.onRemoveClick = { _ -> /* not used for pending */ }
        pendingAdapter = FriendsAdapter()


        // Set adapters and layout managers
        rvFriends.adapter = friendsAdapter
        rvFriends.layoutManager = LinearLayoutManager(requireContext())

        rvPending.adapter = pendingAdapter
        rvPending.layoutManager = LinearLayoutManager(requireContext())

        // Load friends/requests
        loadFriends()
        loadPendingRequests()

        // Handle remove button clicks
        friendsAdapter.onRemoveClick = { friend ->
            Toast.makeText(requireContext(), "Removed ${friend.name}", Toast.LENGTH_SHORT).show()

            // Firebase
            removeFriend(friend.uid)
            friendsAdapter.removeFriend(friend)
        }

        pendingAdapter.onRemoveClick = { friend ->
            Toast.makeText(requireContext(), "Removed ${friend.name}", Toast.LENGTH_SHORT).show()
            pendingAdapter.removeFriend(friend)
        }

        // Handle pending request actions
        pendingAdapter.onAcceptClick = { friend ->
            Toast.makeText(requireContext(), "Accepted ${friend.name}", Toast.LENGTH_SHORT).show()

            // Update Firebase first
            acceptRequest(friend.uid)

            // Move from pending → friends in UI
            pendingAdapter.removeFriend(friend)
            friendsAdapter.addFriend(friend.copy(isPending = false))
        }

        pendingAdapter.onDeclineClick = { friend ->
            Toast.makeText(requireContext(), "Declined ${friend.name}", Toast.LENGTH_SHORT).show()

            // Update Firebase
            declineRequest(friend.uid)

            // Remove from pending list
            pendingAdapter.removeFriend(friend)
        }


        // Search filter
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                friendsAdapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Load friends/requests
    private fun loadFriends() {
        val uid = auth.currentUser?.uid ?: return
        val friendsRef = db.child("users").child(uid).child("friends")

        friendsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendIds = snapshot.children.mapNotNull { it.key }

                if (friendIds.isEmpty()) {
                    friendsAdapter.updateData(emptyList())
                    return
                }

                db.child("users").orderByKey().addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(usersSnap: DataSnapshot) {
                        val friends = friendIds.mapNotNull { id ->
                            val userSnap = usersSnap.child(id)
                            if (!userSnap.exists()) return@mapNotNull null

                            val name = userSnap.child("name").getValue(String::class.java) ?: "Unknown"
                            val profile = userSnap.child("profileURL").getValue(String::class.java)
                            Friend(uid = id, name = name, online = true, profileUrl = profile, isPending = false)
                        }
                        friendsAdapter.updateData(friends)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun loadPendingRequests() {
        val uid = auth.currentUser?.uid ?: return
        val requestsRef = db.child("users").child(uid).child("requests")

        requestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val incomingIds = mutableListOf<String>()

                // Collect IDs of users who sent incoming requests
                for (reqSnap in snapshot.children) {
                    val type = reqSnap.getValue(String::class.java)
                    if (type == "incoming") {
                        reqSnap.key?.let { incomingIds.add(it) }
                    }
                }

                if (incomingIds.isEmpty()) {
                    pendingAdapter.updateData(emptyList())
                    return
                }

                // Fetch all users in one go
                db.child("users").orderByKey().addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(usersSnap: DataSnapshot) {
                        val pending = incomingIds.mapNotNull { id ->
                            val userSnap = usersSnap.child(id)
                            if (!userSnap.exists()) return@mapNotNull null

                            val name = userSnap.child("name").getValue(String::class.java) ?: "Unknown"
                            val profile = userSnap.child("profileURL").getValue(String::class.java)
                            Friend(uid = id, name = name, online = false, profileUrl = profile, isPending = true)
                        }

                        pendingAdapter.updateData(pending)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendFriendRequest(targetId: String) {
        val uid = auth.currentUser?.uid ?: return
        val updates = hashMapOf<String, Any?>(
            "users/$uid/requests/$targetId" to "outgoing",
            "users/$targetId/requests/$uid" to "incoming"
        )

        db.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(requireContext(), "Friend request sent!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to send request", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelFriendRequest(targetId: String) {
        val uid = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any?>(
            "users/$uid/requests/$targetId" to null,
            "users/$targetId/requests/$uid" to null
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Friend request canceled", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to cancel request", Toast.LENGTH_SHORT).show()
            }
    }


    private fun searchUsers(query: String, adapter: SearchAdapter) {
        val uid = auth.currentUser?.uid ?: return

        // First, get the list of current friends
        db.child("users").child(uid).child("friends").get().addOnSuccessListener { friendsSnap ->
            val currentFriends = friendsSnap.children.mapNotNull { it.key }.toSet()

            // Now fetch all users
            listenForPendingRequests { pendingIds ->
                db.child("users").get().addOnSuccessListener { snapshot ->
                    val results = mutableListOf<Friend>()
                    val lowerQuery = query.lowercase().trim()

                    for (child in snapshot.children) {
                        val userId = child.key ?: continue
                        if (userId == uid || currentFriends.contains(userId)) continue // skip self and friends

                        val name = child.child("name").getValue(String::class.java) ?: "Unknown"
                        val profile = child.child("profileURL").getValue(String::class.java)

                        if (lowerQuery.isEmpty() || name.lowercase().contains(lowerQuery)) {
                            val isPending = pendingIds.contains(userId)
                            results.add(
                                Friend(
                                    uid = userId,
                                    name = name,
                                    profileUrl = profile,
                                    isPending = isPending
                                )
                            )
                        }
                    }

                    adapter.updateData(results)
                }
            }
        }
    }




    private fun acceptRequest(requesterId: String) {
        val uid = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any?>(
            "users/$uid/friends/$requesterId" to true,
            "users/$requesterId/friends/$uid" to true,
            "users/$uid/requests/$requesterId" to null,
            "users/$requesterId/requests/$uid" to null
        )

        db.updateChildren(updates)
    }

    private fun declineRequest(requesterId: String) {
        val uid = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any?>(
            "users/$uid/requests/$requesterId" to null,
            "users/$requesterId/requests/$uid" to null
        )

        db.updateChildren(updates)
    }

    private fun removeFriend(friendId: String) {
        val uid = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any?>(
            "users/$uid/friends/$friendId" to null,
            "users/$friendId/friends/$uid" to null
        )

        db.updateChildren(updates)
    }

    private fun listenForPendingRequests(onUpdate: (List<String>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val requestsRef = db.child("users").child(uid).child("requests")

        requestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pendingIds = mutableListOf<String>()
                for (child in snapshot.children) {
                    val type = child.getValue(String::class.java)
                    if (type == "outgoing") pendingIds.add(child.key ?: continue)
                }
                onUpdate(pendingIds)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    // TESTING dummy friends
    private fun createTestData() {
        val uid = auth.currentUser?.uid ?: return
        val usersRef = db.child("users")

        // Create mock friends (these are just fake accounts)
        val mockUsers = mapOf(
            "userA" to mapOf(
                "name" to "Alice",
                "profileURL" to "https://example.com/alice.png"
            ),
            "userB" to mapOf(
                "name" to "Bob",
                "profileURL" to "https://example.com/bob.png"
            ),
            "userC" to mapOf(
                "name" to "Charlie",
                "profileURL" to "https://example.com/charlie.png"
            )
        )

        // ✅ Create or update those mock users (won’t touch your own node)
        usersRef.updateChildren(mockUsers)

        // ✅ Only update your current user’s friends/requests
        val currentUserUpdate = mapOf(
            "friends/userA" to true,
            "friends/userB" to true,
            "requests/userC" to "incoming"
        )

        usersRef.child(uid).updateChildren(currentUserUpdate)
    }
}
