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

class FriendsFragment : Fragment(R.layout.manage_friends) {

    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var pendingAdapter: FriendsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
