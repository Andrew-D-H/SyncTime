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
    // Firebase
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    // Adapters
    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var pendingAdapter: FriendsAdapter
    private lateinit var searchAdapter: SearchAdapter

class FriendsFragment : Fragment(R.layout.manage_friends) {

    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var pendingAdapter: FriendsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- UI references ---
        val rvFriends = view.findViewById<RecyclerView>(R.id.rvFriendsList)
        val rvPending = view.findViewById<RecyclerView>(R.id.rvPendingRequests)
        val rvSearch = view.findViewById<RecyclerView>(R.id.rvSearchResults)
        val etSearchFriends = view.findViewById<EditText>(R.id.etSearchFriends)
        val etSearchUsers = view.findViewById<EditText>(R.id.etSearchUsers)

        // --- Initialize adapters ---
        friendsAdapter = FriendsAdapter(
            onRemoveClick = { friend ->
                removeFriend(friend.uid)
                friendsAdapter.removeFriend(friend)
                Toast.makeText(requireContext(), "Removed ${friend.name}", Toast.LENGTH_SHORT).show()
            },
            onChatClick = { friend ->
                val chatFragment = ChatFragment.newInstance(friend.uid, friend.name)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, chatFragment)
                    .addToBackStack(null)
                    .commit()
            }
        )

        pendingAdapter = FriendsAdapter(
            isPendingList = true,
            onAcceptClick = { friend ->
                acceptRequest(friend.uid)
                pendingAdapter.removeFriend(friend)
                friendsAdapter.addFriend(friend.copy(isPending = false))
                Toast.makeText(requireContext(), "Accepted ${friend.name}", Toast.LENGTH_SHORT).show()
            },
            onDeclineClick = { friend ->
                declineRequest(friend.uid)
                pendingAdapter.removeFriend(friend)
                Toast.makeText(requireContext(), "Declined ${friend.name}", Toast.LENGTH_SHORT).show()
            }
        )

        searchAdapter = SearchAdapter(
            onAddClick = { user -> sendFriendRequest(user.uid) },
            onCancelClick = { user -> cancelFriendRequest(user.uid) }
        )

        // --- RecyclerViews setup ---
        rvFriends.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = friendsAdapter
        }
        rvPending.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pendingAdapter
        }
        rvSearch.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }

        // -- Setup search listeners ---
        setupSearchListeners(etSearchFriends, etSearchUsers, rvSearch)

        // --- Load data from firebase ---
        loadFriends()
        loadPendingRequests()

        // --- Test data ---
        createTestData()  // optional for testing
    }


    // -------------------------
    // SEARCH LISTENERS
    // -------------------------
    private fun setupSearchListeners(etFriends: EditText, etUsers: EditText, rvSearch: RecyclerView) {
        // Filter friends list
        etFriends.addTextChangedListener(object : TextWatcher {
        val rvFriends = view.findViewById<RecyclerView>(R.id.rvFriendsList)
        val rvPending = view.findViewById<RecyclerView>(R.id.rvPendingRequests)
        val etSearch = view.findViewById<EditText>(R.id.etSearchFriends)

        // Initialize adapters with empty lists
        friendsAdapter = FriendsAdapter()
        pendingAdapter = FriendsAdapter()

        // Set adapters and layout managers
        rvFriends.adapter = friendsAdapter
        rvFriends.layoutManager = LinearLayoutManager(requireContext())

        rvPending.adapter = pendingAdapter
        rvPending.layoutManager = LinearLayoutManager(requireContext())

        // Load dummy data
        friendsAdapter.updateData(getDummyFriends())
        pendingAdapter.updateData(getDummyPending())

        // Handle remove button clicks
        friendsAdapter.onRemoveClick = { friend ->
            Toast.makeText(requireContext(), "Removed ${friend.name}", Toast.LENGTH_SHORT).show()
            friendsAdapter.removeFriend(friend)
        }

        pendingAdapter.onRemoveClick = { friend ->
            Toast.makeText(requireContext(), "Removed ${friend.name}", Toast.LENGTH_SHORT).show()
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

        // Search for new users
        etUsers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                rvSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                if (query.isNotEmpty()) searchUsers(query, searchAdapter)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }


    // -------------------------
    // FIREBASE DATA LOADING
    // -------------------------
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
                val incomingIds = snapshot.children
                    .filter { it.getValue(String::class.java) == "incoming" }
                    .mapNotNull { it.key }

                if (incomingIds.isEmpty()) {
                    pendingAdapter.updateData(emptyList())
                    return
                }

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

    // -------------------------
    // FRIEND REQUESTS ACTIONS
    // -------------------------
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

    // -------------------------
    // SEARCH USERS
    // -------------------------
    private fun searchUsers(query: String, adapter: SearchAdapter) {
        val uid = auth.currentUser?.uid ?: return

        db.child("users").child(uid).child("friends").get().addOnSuccessListener { friendsSnap ->  // get current friends
            val currentFriends = friendsSnap.children.mapNotNull { it.key }.toSet()

            db.child("users").get().addOnSuccessListener { snapshot ->
                val results = mutableListOf<Friend>()

                snapshot.children.forEach { child ->
                    val userId = child.key ?: return@forEach
                    if (userId == uid) return@forEach  // skip yourself

                    // Check if current user already has this friend
                    if (currentFriends.contains(userId)) return@forEach  // skip already added friends

                    // Check if already pending
                    val isPending = child.child("requests").child(uid)
                        .getValue(String::class.java) == "incoming"

                    // Get name and profile
                    val name = child.child("name").getValue(String::class.java) ?: "Unknown"
                    val profile = child.child("profileURL").getValue(String::class.java)

                    if (name.lowercase().contains(query.lowercase())) {
                        results.add(Friend(userId, name, profileUrl = profile, isPending = isPending))
                    }
                }
                // Update adapter
                adapter.updateData(results)
            }
        }
    }

    // -------------------------
    // TESTING DUMMY DATA
    // -------------------------
    private fun createTestData() {
        val uid = auth.currentUser?.uid ?: return
        val currentUserRef = db.child("users").child(uid)
        val updates = mapOf(
            "friends/userA" to true,
            "friends/userB" to true,
            "requests/userC" to "incoming"
        )
        currentUserRef.updateChildren(updates)
    }
    }

    // Dummy data functions
    private fun getDummyFriends(): List<Friend> = listOf(
        Friend("Gandalf the Grey", true),
        Friend("Bill Ponderosa", false),
        Friend("Dr. Heinz Doofenshmirtz", true),
    )

    private fun getDummyPending(): List<Friend> = listOf(
        Friend("Burton Guster", false),
        Friend("Jessica Day", false)
    )
}
