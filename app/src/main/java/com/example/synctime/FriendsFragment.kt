package com.example.synctime

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FriendsFragment : Fragment(R.layout.friends_fragment), FriendsAdapter.FriendsAdapterCallback {

    private lateinit var friendsAdapter: FriendsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val friendsRecyclerView: RecyclerView = view.findViewById(R.id.friendsRecyclerView)
        friendsAdapter = FriendsAdapter(
            context = requireContext(),
            onFriendSelected = { _, _ -> /* FriendsFragment doesn't use this */ }
        )
        friendsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        friendsRecyclerView.adapter = friendsAdapter
    }

    override fun onFriendRemoved(friendUid: String) {
        Toast.makeText(requireContext(), "Friend removed: $friendUid", Toast.LENGTH_SHORT).show()
    }

    override fun onFriendSelected(friendUid: String, isSelected: Boolean) {
        Log.d("FriendsFragment", "Friend $friendUid selected: $isSelected")
    }
}